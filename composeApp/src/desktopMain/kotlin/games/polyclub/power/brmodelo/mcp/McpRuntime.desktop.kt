/*
 * Power brModelo - Kotlin port of brModelo 3.0 originally written in Pascal
 * Copyright (C) 2026  Polyana Fontes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package games.polyclub.power.brmodelo.mcp

import games.polyclub.power.brmodelo.BuildInfo
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolKind
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolOverrides
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ConceptualSearchHit
import games.polyclub.power.brmodelo.domain.ConceptualSearchOutcome
import games.polyclub.power.brmodelo.domain.ConceptualSearchTextScope
import games.polyclub.power.brmodelo.domain.ConceptualSearchTypeFilters
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolVariant
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaXmlSerializer
import io.modelcontextprotocol.json.McpJsonDefaults
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpServerFeatures
import io.modelcontextprotocol.server.McpSyncServer
import io.modelcontextprotocol.server.McpSyncServerExchange
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider
import io.modelcontextprotocol.spec.McpSchema
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import java.util.function.BiFunction
import javax.swing.SwingUtilities

private fun <T> runOnEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    val box = arrayOfNulls<Any>(1)
    var error: Throwable? = null
    SwingUtilities.invokeAndWait {
        try {
            box[0] = block()
        } catch (t: Throwable) {
            error = t
        }
    }
    error?.let { throw it }
    @Suppress("UNCHECKED_CAST")
    return box[0] as T
}

/**
 * Tab snapshot after an MCP tool that may create or switch tabs (created vs selected + stable URIs).
 */
private data class McpTabSelectionChange(
    val createdTabIndex: Int,
    val selectedTabIndex: Int,
    val createdResourceUri: String,
    val selectedResourceUri: String,
)

internal actual class McpRuntime {

    companion object {
        private const val MAX_RESOURCE_UTILITY_MATCHES = 2000

        private const val CONCEPTUAL_MER_DTD_CLASSPATH = "mcp/conceptual-mer.dtd"

        private val conceptualMerDtdClasspathText: String by lazy {
            val stream = McpRuntime::class.java.classLoader.getResourceAsStream(CONCEPTUAL_MER_DTD_CLASSPATH)
                ?: error("Missing classpath resource: $CONCEPTUAL_MER_DTD_CLASSPATH")
            stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        }

        private fun loadUtf8ClasspathOrThrow(path: String): String {
            val stream = McpRuntime::class.java.classLoader.getResourceAsStream(path)
                ?: error("Missing classpath resource: $path")
            return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        }

        private val exampleMerBodiesByUri: Map<String, String> by lazy {
            mcpClasspathXmlExamples.associate { ex ->
                ex.resourceUri to loadUtf8ClasspathOrThrow(ex.classpathPath)
            }
        }
    }

    private val bindingsRef = AtomicReference<McpUiBindings?>(null)
    private val settingsOpener = AtomicReference<(() -> Unit)?>(null)

    private var jetty: Server? = null
    private var mcp: McpSyncServer? = null

    actual fun setSettingsDialogOpener(opener: () -> Unit) {
        settingsOpener.set(opener)
    }

    actual fun updateBindings(bindings: McpUiBindings?) {
        bindingsRef.set(bindings)
    }

    actual fun openSettingsDialog() {
        val opener = settingsOpener.get()
        SwingUtilities.invokeLater {
            opener?.invoke()
        }
    }

    actual fun isServerRunning(): Boolean = jetty?.isStarted == true

    actual fun onTabsChanged() {
        try {
            syncOpenModelTabResources()
        } catch (_: Exception) {
            // Ignore if transport is torn down.
        }
    }

    actual fun shutdown() {
        stopServerInternal(notify = false)
    }

    actual fun stopServer() {
        stopServerInternal(notify = true)
    }

    private fun stopServerInternal(notify: Boolean) {
        runCatching {
            mcp?.close()
        }
        mcp = null
        runCatching {
            jetty?.stop()
        }
        jetty = null
        if (notify) {
            bindingsRef.get()?.onServerRunningChanged(false)
        }
    }

    actual fun startServer(): Boolean {
        if (jetty?.isStarted == true) return true
        val (bindHostRaw, portRaw, allowLanHosts) = McpSettingsStore.load()
        val bindHost = bindHostRaw.trim().ifBlank { "127.0.0.1" }
        val port = portRaw.coerceIn(1, 65535)

        val jsonMapper = McpJsonDefaults.getMapper()
        val security = buildSecurityValidator(bindHost, allowLanHosts)
        val transport = HttpServletStreamableServerTransportProvider.builder()
            .jsonMapper(jsonMapper)
            .mcpEndpoint(MCP_SERVLET_PATH)
            .securityValidator(security)
            .build()

        val tools = buildTools(jsonMapper)
        val serverCapabilities = McpSchema.ServerCapabilities.builder()
            .tools(false)
            .resources(false, true)
            .logging()
            .build()

        val server = try {
            McpServer.sync(transport)
                .serverInfo("Power-brModelo", BuildInfo.VERSION)
                .capabilities(serverCapabilities)
                .instructions(McpServerInstructions.build())
                .tools(tools)
                .build()
        } catch (t: Throwable) {
            bindingsRef.get()?.onNotifyUser("MCP: falha ao criar servidor: ${t.message}")
            return false
        }

        mcp = server
        runCatching {
            ensureConceptualMerDtdMcpResource(server)
            ensureExampleMerMcpResources(server)
        }.onFailure {
            bindingsRef.get()?.onNotifyUser("MCP: não foi possível registar resources estáticos: ${it.message}")
        }

        return try {
            val jettyServer = Server(InetSocketAddress(bindHost, port))
            val context = ServletContextHandler(ServletContextHandler.NO_SESSIONS).apply {
                contextPath = "/"
                addServlet(ServletHolder(transport), "${MCP_SERVLET_PATH}/*")
            }
            jettyServer.handler = context
            jettyServer.start()
            jetty = jettyServer
            syncOpenModelTabResources()
            bindingsRef.get()?.onNotifyUser("MCP: servidor em http://$bindHost:$port$MCP_SERVLET_PATH")
            bindingsRef.get()?.onServerRunningChanged(true)
            true
        } catch (t: Throwable) {
            runCatching { server.close() }
            mcp = null
            bindingsRef.get()?.onNotifyUser("MCP: falha ao iniciar Jetty: ${t.message}")
            false
        }
    }

    private fun buildSecurityValidator(bindHost: String, allowLanHosts: Boolean): DefaultServerTransportSecurityValidator {
        val hosts = LinkedHashSet<String>()
        /*
         * Browsers and IDE MCP clients send Host as "hostname:port". DefaultServerTransportSecurityValidator
         * only accepts an exact string or a "hostname:*" wildcard (see SDK validateHost).
         */
        hosts.add("127.0.0.1:*")
        hosts.add("127.0.0.1")
        hosts.add("localhost:*")
        hosts.add("localhost")
        hosts.add("[::1]:*")
        hosts.add("[::1]")
        val trimmedBind = bindHost.trim()
        if (trimmedBind.isNotBlank() && trimmedBind != "0.0.0.0") {
            hosts.add("$trimmedBind:*")
            hosts.add(trimmedBind)
        }
        if (allowLanHosts) {
            hosts.add("0.0.0.0:*")
            hosts.add("0.0.0.0")
        }
        val b = DefaultServerTransportSecurityValidator.builder()
        hosts.forEach { b.allowedHost(it) }
        /*
         * Streamable HTTP clients may send Origin; without a matching allowedOrigin the SDK returns 403.
         */
        b.allowedOrigin("http://127.0.0.1:*")
        b.allowedOrigin("http://localhost:*")
        b.allowedOrigin("https://127.0.0.1:*")
        b.allowedOrigin("https://localhost:*")
        if (trimmedBind.isNotBlank() && trimmedBind != "0.0.0.0" && trimmedBind != "127.0.0.1" && trimmedBind != "localhost") {
            b.allowedOrigin("http://$trimmedBind:*")
            b.allowedOrigin("https://$trimmedBind:*")
        }
        return b.build()
    }

    private fun mcpTabSelectionChangeFromBeforeAfter(
        before: McpTabSnapshot,
        after: McpTabSnapshot,
    ): McpTabSelectionChange? {
        val selectedIdx = after.selectedIndex
        if (selectedIdx !in after.sessions.indices) return null
        val createdIdx = mcpCreatedTabIndexAfterOpen(before.sessions, after.sessions, selectedIdx)
        if (createdIdx !in after.sessions.indices) return null
        return McpTabSelectionChange(
            createdTabIndex = createdIdx,
            selectedTabIndex = selectedIdx,
            createdResourceUri = modelResourceUriForSession(after.sessions[createdIdx].id),
            selectedResourceUri = modelResourceUriForSession(after.sessions[selectedIdx].id),
        )
    }

    /**
     * MCP clients (e.g. Cursor) list **concrete** resources via `resources/list`; URI templates alone
     * are not enough. Registers one `brmodelo://model/{editorTabSessionId}.xml` resource per open tab and refreshes
     * the list when tabs change.
     */
    private fun syncOpenModelTabResources() {
        val server = mcp ?: return
        val snapshot = runOnEdt {
            bindingsRef.get()?.current()
        } ?: return

        val modelPrefix = "brmodelo://model/"
        val existingUris = server.listResources()
            .map { it.uri() }
            .filter { it.startsWith(modelPrefix) }
        for (uri in existingUris) {
            server.removeResource(uri)
        }
        for (index in snapshot.sessions.indices) {
            val tab = snapshot.sessions[index]
            val uri = modelResourceUriForSession(tab.id)
            val rawTitle = tab.displayTitle()
            val title = if (rawTitle.length > 120) rawTitle.take(117) + "..." else rawTitle
            val spec = McpServerFeatures.SyncResourceSpecification(
                McpSchema.Resource.builder()
                    .uri(uri)
                    .name("conceptual_model_$index")
                    .title("Tab $index — $title")
                    .description(
                        "Live in-memory conceptual schema for editor tab index $index " +
                            "(stable resource id: session ${tab.id}; same XML as a saved brModelo export).",
                    )
                    .mimeType("application/xml")
                    .build(),
                BiFunction { _: McpSyncServerExchange, req: McpSchema.ReadResourceRequest ->
                    readModelResource(req.uri())
                },
            )
            server.addResource(spec)
        }
        server.notifyResourcesListChanged()
    }

    private fun readModelResource(uri: String): McpSchema.ReadResourceResult {
        val index = runOnEdt {
            val b = bindingsRef.get() ?: return@runOnEdt null
            tabIndexForModelResourceUri(uri, b.current().sessions)
        }
            ?: return McpSchema.ReadResourceResult(
                listOf(
                    McpSchema.TextResourceContents(
                        uri,
                        "text/plain",
                        "Invalid or unknown model resource URI: $uri",
                    ),
                ),
            )
        val xml = runOnEdt {
            val b = bindingsRef.get()
            val schema = b?.current()?.schemaForTab(index)
            if (schema == null) {
                null
            } else {
                ConceptualSchemaXmlSerializer.serialize(schema)
            }
        }
        if (xml == null) {
            return McpSchema.ReadResourceResult(
                listOf(
                    McpSchema.TextResourceContents(
                        uri,
                        "text/plain",
                        "No model for tab index $index.",
                    ),
                ),
            )
        }
        return McpSchema.ReadResourceResult(
            listOf(McpSchema.TextResourceContents(uri, "application/xml", xml)),
        )
    }

    /**
     * Plain UTF-16 text for any URI registered on this MCP server (same payload as `resources/read`).
     * Returns `(text, null)` on success or `(null, errorCode)` on failure.
     */
    private fun resolveRegisteredResourcePlainText(uri: String): Pair<String?, String?> {
        val u = uri.trim()
        if (u.isEmpty()) return null to "uri_required"

        val tabIdx = runOnEdt {
            val b = bindingsRef.get() ?: return@runOnEdt null
            tabIndexForModelResourceUri(u, b.current().sessions)
        }
        if (tabIdx != null) {
            return runOnEdt {
                val b = bindingsRef.get() ?: return@runOnEdt (null to "bindings_unavailable")
                val schema = b.current().schemaForTab(tabIdx) ?: return@runOnEdt (null to "no_model_for_tab")
                ConceptualSchemaXmlSerializer.serialize(schema) to null
            }
        }
        if (u == conceptualMerDtdResourceUri()) {
            return conceptualMerDtdClasspathText to null
        }
        val example = exampleMerBodiesByUri[u]
        if (example != null) {
            return example to null
        }
        return null to "unknown_resource_uri"
    }

    private fun ensureConceptualMerDtdMcpResource(server: McpSyncServer) {
        val uri = conceptualMerDtdResourceUri()
        if (server.listResources().any { it.uri() == uri }) {
            return
        }
        val spec = McpServerFeatures.SyncResourceSpecification(
            McpSchema.Resource.builder()
                .uri(uri)
                .name("conceptual_mer_xml_dtd")
                .title("brModelo conceptual MER XML (informative DTD)")
                .description(
                    "External subset documenting the conceptual MER XML save format for agents. " +
                        "Not used by the application parser for validation.",
                )
                .mimeType("application/xml-dtd")
                .build(),
            BiFunction { _: McpSyncServerExchange, req: McpSchema.ReadResourceRequest ->
                readConceptualMerDtdResource(req.uri())
            },
        )
        server.addResource(spec)
        server.notifyResourcesListChanged()
    }

    private fun ensureExampleMerMcpResources(server: McpSyncServer) {
        val existing = server.listResources().map { it.uri() }.toSet()
        for (ex in mcpClasspathXmlExamples) {
            if (ex.resourceUri in existing) {
                continue
            }
            val spec = McpServerFeatures.SyncResourceSpecification(
                McpSchema.Resource.builder()
                    .uri(ex.resourceUri)
                    .name(ex.resourceListingName)
                    .title(ex.resourceTitle)
                    .description(
                        "Static example MER XML (read-only). Not an editor tab — tab tools that take a tab URI must not use this URI.",
                    )
                    .mimeType("application/xml")
                    .build(),
                BiFunction { _: McpSyncServerExchange, req: McpSchema.ReadResourceRequest ->
                    readClasspathExampleMerResource(req.uri())
                },
            )
            server.addResource(spec)
        }
        server.notifyResourcesListChanged()
    }

    private fun readClasspathExampleMerResource(uri: String): McpSchema.ReadResourceResult {
        val body = exampleMerBodiesByUri[uri]
            ?: return McpSchema.ReadResourceResult(
                listOf(
                    McpSchema.TextResourceContents(
                        uri,
                        "text/plain",
                        "Unknown example MER URI: $uri",
                    ),
                ),
            )
        return McpSchema.ReadResourceResult(
            listOf(McpSchema.TextResourceContents(uri, "application/xml", body)),
        )
    }

    private fun readConceptualMerDtdResource(uri: String): McpSchema.ReadResourceResult {
        val expected = conceptualMerDtdResourceUri()
        if (uri != expected) {
            return McpSchema.ReadResourceResult(
                listOf(
                    McpSchema.TextResourceContents(
                        uri,
                        "text/plain",
                        "Unknown conceptual DTD URI: $uri",
                    ),
                ),
            )
        }
        val body = conceptualMerDtdClasspathText
        return McpSchema.ReadResourceResult(
            listOf(McpSchema.TextResourceContents(uri, "application/xml-dtd", body)),
        )
    }

    private fun runCloseTabAtIndex(idx: Int, discard: Boolean): String = runOnEdt {
        val b = bindingsRef.get() ?: return@runOnEdt "bindings_unavailable"
        val snap = b.current()
        if (idx !in snap.sessions.indices) return@runOnEdt "invalid_index"
        val tab = snap.sessions[idx]
        if (!discard && tab.needsCloseConfirmation()) {
            return@runOnEdt "needs_user_confirmation_or_set_discardUnsavedChanges_true"
        }
        if (discard) {
            b.onForceCloseTab(idx)
        } else {
            b.onRequestCloseTab(idx)
        }
        "ok"
    }

    private fun tabIndexFromModelResourceUri(req: McpSchema.CallToolRequest): Int? {
        val uri = stringArg(req, "uri") ?: return null
        return runOnEdt {
            val b = bindingsRef.get() ?: return@runOnEdt null
            tabIndexForModelResourceUri(uri, b.current().sessions)
        }
    }

    /** Resolves [tabIndex] if present; otherwise a live tab URI (`brmodelo://model/{sessionId}.xml` or legacy index form). */
    private fun tabIndexFromTabOrUriArgs(req: McpSchema.CallToolRequest): Int? {
        val explicit = intArg(req, "tabIndex")
        if (explicit != null) {
            return explicit
        }
        return tabIndexFromModelResourceUri(req)
    }

    private fun buildTools(jsonMapper: io.modelcontextprotocol.json.McpJsonMapper): List<McpServerFeatures.SyncToolSpecification> {
        return listOf(
            syncTool(
                jsonMapper,
                name = McpTabToolNames.LIST_OPEN,
                title = "List open tabs",
                description = "Returns JSON for each open editor tab (index, id, title, dirty, filePath, resourceUri) and the selected index. " +
                    "Static DTD and example MER resources are listed only in MCP server instructions (not tab rows).",
                schema = """{"type":"object","properties":{},"additionalProperties":false}""",
            ) { _, _ ->
                val json = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt """{"error":"bindings_unavailable"}"""
                    val snap = b.current()
                    val rows = snap.sessions.mapIndexed { index, tab ->
                        val uri = modelResourceUriForSession(tab.id)
                        """{"index":$index,"id":${tab.id},"title":${jsonString(tab.displayTitle())},"dirty":${tab.hasUnsavedChanges()},"filePath":${jsonString(tab.schema.filePath)},"resourceUri":${jsonString(uri)}}"""
                    }
                    """{"selectedIndex":${snap.selectedIndex},"tabs":[${rows.joinToString(",")}]}"""
                }
                okText(json)
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.SELECT,
                title = "Select tab",
                description = "Brings the given tab index to the foreground.",
                schema = """{"type":"object","properties":{"tabIndex":{"type":"integer","minimum":0}},"required":["tabIndex"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = intArg(req, "tabIndex") ?: return@syncTool err("tabIndex required")
                val ok = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt false
                    val snap = b.current()
                    if (idx !in snap.sessions.indices) return@runOnEdt false
                    b.onSelectTab(idx)
                    true
                }
                if (!ok) return@syncTool err("invalid_tab_index")
                okText("""{"ok":true,"selectedIndex":$idx}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.SELECT_RESOURCE,
                title = "Select tab by model resource URI",
                description = "Selects an open editor tab using only the live model-tab resource URI from list_open. " +
                    "Does not apply to static example or DTD resources. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{"uri":{"type":"string","minLength":1}},"required":["uri"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromModelResourceUri(req) ?: return@syncTool err("invalid_or_missing_resource_uri")
                val ok = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt false
                    val snap = b.current()
                    if (idx !in snap.sessions.indices) return@runOnEdt false
                    b.onSelectTab(idx)
                    true
                }
                if (!ok) return@syncTool err("invalid_tab_index")
                okText("""{"ok":true,"selectedIndex":$idx}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.CLOSE,
                title = "Close tab",
                description = "Closes a tab. When discardUnsavedChanges is true, unsaved edits are dropped immediately. " +
                    "Otherwise the UI may prompt the user; this tool cannot wait for that dialog.",
                schema = """{"type":"object","properties":{"tabIndex":{"type":"integer","minimum":0},"discardUnsavedChanges":{"type":"boolean"}},"required":["tabIndex"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = intArg(req, "tabIndex") ?: return@syncTool err("tabIndex required")
                val discard = boolArg(req, "discardUnsavedChanges") == true
                val message = runCloseTabAtIndex(idx, discard)
                if (message != "ok") {
                    return@syncTool err(message)
                }
                okText("""{"ok":true,"closedIndex":$idx}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.CLOSE_RESOURCE,
                title = "Close tab by model resource URI",
                description = "Same as close but identifies the tab with the live model-tab resource URI from list_open. " +
                    "Does not apply to static example or DTD resources. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{"uri":{"type":"string","minLength":1},"discardUnsavedChanges":{"type":"boolean"}},"required":["uri"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromModelResourceUri(req) ?: return@syncTool err("invalid_or_missing_resource_uri")
                val discard = boolArg(req, "discardUnsavedChanges") == true
                val message = runCloseTabAtIndex(idx, discard)
                if (message != "ok") {
                    return@syncTool err(message)
                }
                okText("""{"ok":true,"closedIndex":$idx}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.NEW_CONCEPTUAL_MODEL,
                title = "New conceptual model tab",
                description = "Opens a new empty conceptual model tab and selects it. " +
                    "On success the JSON includes createdTabIndex and createdResourceUri for the new tab " +
                    "(brmodelo://model/{editorTabSessionId}.xml), plus selectedIndex and selectedResourceUri for the tab that is selected after the call. " +
                    "Resource URIs use the stable tab session id so they stay valid if other tabs are closed. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{},"additionalProperties":false}""",
            ) { _, _ ->
                val change = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt null
                    val before = b.current()
                    b.onAddBlankTab()
                    val after = b.current()
                    mcpTabSelectionChangeFromBeforeAfter(before, after)
                } ?: return@syncTool err("bindings_unavailable")
                okText(tabSelectionChangeSuccessJson(change))
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.SAVE,
                title = "Save tab",
                description = "Runs the same save path as the editor (optional Save-As when saveAs is true). " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{"tabIndex":{"type":"integer","minimum":0},"saveAs":{"type":"boolean"}},"required":["tabIndex"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = intArg(req, "tabIndex") ?: return@syncTool err("tabIndex required")
                val saveAs = boolArg(req, "saveAs") == true
                val ok = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt false
                    b.onSaveTab(idx, saveAs)
                }
                if (!ok) {
                    return@syncTool err("save_cancelled_or_failed")
                }
                okText("""{"ok":true,"savedIndex":$idx,"saveAs":$saveAs}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.SAVE_RESOURCE,
                title = "Save tab by model resource URI",
                description = "Same as save but identifies the tab with the live model-tab resource URI returned for that tab in list_open. " +
                    "Does not apply to static example or DTD resources. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{"uri":{"type":"string","minLength":1},"saveAs":{"type":"boolean"}},"required":["uri"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromModelResourceUri(req) ?: return@syncTool err("invalid_or_missing_tab_resource_uri")
                val saveAs = boolArg(req, "saveAs") == true
                val ok = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt false
                    b.onSaveTab(idx, saveAs)
                }
                if (!ok) {
                    return@syncTool err("save_cancelled_or_failed")
                }
                okText("""{"ok":true,"savedIndex":$idx,"saveAs":$saveAs}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.OPEN_FILE,
                title = "Open model file",
                description = "Loads a brModelo XML or .brm file from an absolute path on disk (same as opening from disk in the editor). " +
                    "On success the JSON includes createdTabIndex/createdResourceUri for the tab that received the model (new or reused) " +
                    "and selectedIndex/selectedResourceUri for the tab that ends up selected. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{"path":{"type":"string","minLength":1,"description":"Absolute path to a .xml or .brm file"}},"required":["path"],"additionalProperties":false}""",
            ) { _, req ->
                val path = stringArg(req, "path") ?: return@syncTool err("path required")
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt Pair("bindings_unavailable", null)
                    val before = b.current()
                    val errMsg = b.onOpenModelFileAtPath(path)
                    if (errMsg != null) return@runOnEdt Pair(errMsg, null)
                    val after = b.current()
                    val change = mcpTabSelectionChangeFromBeforeAfter(before, after)
                        ?: return@runOnEdt Pair("invalid_tab_state", null)
                    Pair(null, change)
                }
                val errMsg = outcome.first
                val change = outcome.second
                if (errMsg != null) {
                    return@syncTool err(errMsg)
                }
                if (change == null) {
                    return@syncTool err("invalid_tab_state")
                }
                okText(tabSelectionChangeSuccessJson(change, """"path":${jsonString(path)}"""))
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.OPEN_XML,
                title = "Open conceptual XML from text",
                description = "Parses UTF-8 conceptual XML and opens a new dirty tab (basename fileName only, no path; used as the model title). " +
                    "On success the JSON includes createdTabIndex/createdResourceUri and selectedIndex/selectedResourceUri after the operation. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{"fileName":{"type":"string","minLength":1,"description":"Basename only, e.g. modelo.xml"},"xml":{"type":"string","description":"Conceptual schema XML (UTF-8)"}},"required":["fileName","xml"],"additionalProperties":false}""",
            ) { _, req ->
                val fileName = stringArg(req, "fileName") ?: return@syncTool err("fileName required")
                val xml = rawStringArg(req, "xml") ?: return@syncTool err("xml required")
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt Pair("bindings_unavailable", null)
                    val before = b.current()
                    val errMsg = b.onOpenXmlAsUnsavedTab(fileName, xml)
                    if (errMsg != null) return@runOnEdt Pair(errMsg, null)
                    val after = b.current()
                    val change = mcpTabSelectionChangeFromBeforeAfter(before, after)
                        ?: return@runOnEdt Pair("invalid_tab_state", null)
                    Pair(null, change)
                }
                val errMsg = outcome.first
                val change = outcome.second
                if (errMsg != null) {
                    return@syncTool err(errMsg)
                }
                if (change == null) {
                    return@syncTool err("invalid_tab_state")
                }
                okText(tabSelectionChangeSuccessJson(change, """"fileName":${jsonString(fileName)}"""))
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.REPLACE_MODEL_XML,
                title = "Replace tab conceptual XML",
                description = "Parses UTF-8 MER XML and replaces the entire conceptual schema of one open tab in a single undoable step. " +
                    "Provide tabIndex or a live tab resource URI (see MCP server instructions). " +
                    "Preserves the tab's disk path metadata. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{"tabIndex":{"type":"integer","minimum":0},"uri":{"type":"string","minLength":1},"xml":{"type":"string","description":"Full MER XML (UTF-8)"}},"required":["xml"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                val xml = rawStringArg(req, "xml") ?: return@syncTool err("xml required")
                val errMsg = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt "bindings_unavailable"
                    val snap = b.current()
                    if (idx !in snap.sessions.indices) return@runOnEdt "invalid_tab_index"
                    b.onReplaceModelXmlAtTab(idx, xml)
                }
                if (errMsg != null) {
                    return@syncTool err(errMsg)
                }
                okText("""{"ok":true,"tabIndex":$idx}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.PATCH_MODEL_XML,
                title = "Patch tab conceptual XML (search/replace)",
                description = "Serializes the tab's current conceptual MER to XML, applies old_string→new_string, re-parses, and commits in one undoable step (Cursor-style single edit when replace_all is false). " +
                    "Provide tabIndex or a live tab resource URI (see MCP server instructions). " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{"tabIndex":{"type":"integer","minimum":0},"uri":{"type":"string","minLength":1},"old_string":{"type":"string","minLength":1},"new_string":{"type":"string"},"replace_all":{"type":"boolean"}},"required":["old_string"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                val oldStr = rawStringArg(req, "old_string") ?: return@syncTool err("old_string required")
                val newStrArg = req.arguments()["new_string"]
                val newStr = when (newStrArg) {
                    null -> ""
                    is String -> newStrArg
                    else -> newStrArg.toString()
                }
                val replaceAll = boolArg(req, "replace_all") == true
                val errMsg = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt "bindings_unavailable"
                    val snap = b.current()
                    if (idx !in snap.sessions.indices) return@runOnEdt "invalid_tab_index"
                    b.onPatchModelXmlAtTab(idx, oldStr, newStr, replaceAll)
                }
                if (errMsg != null) {
                    return@syncTool err(errMsg)
                }
                okText("""{"ok":true,"tabIndex":$idx,"replaceAll":$replaceAll}""")
            },
        ) + buildConceptualSearchTools(jsonMapper) + buildResourceUtilityTools(jsonMapper) + buildProceduralTools(jsonMapper) + buildAttributeTools(jsonMapper)
    }

    private fun buildConceptualSearchTools(
        jsonMapper: io.modelcontextprotocol.json.McpJsonMapper,
    ): List<McpServerFeatures.SyncToolSpecification> {
        val tabUri = """"tabIndex":{"type":"integer","minimum":0},"uri":{"type":"string","minLength":1}"""
        val findSchema = """{"type":"object","properties":{$tabUri,"query":{"type":"string","description":"Substring; omit or use empty string to list all items in the selected include* categories (400-hit cap)."},"includeEntities":{"type":"boolean"},"includeRelationships":{"type":"boolean"},"includeAssociativeEntities":{"type":"boolean"},"includeSpecializations":{"type":"boolean"},"includeCanvasAttributes":{"type":"boolean"},"includeHiddenAttributes":{"type":"boolean"},"includeCardinalityLabels":{"type":"boolean"},"observationBox":{"type":"boolean","description":"Include Annotation (observation box) elements"},"searchDictionary":{"type":"boolean"},"searchObservations":{"type":"boolean"}},"additionalProperties":false}"""
        val applySchema = """{"type":"object","properties":{$tabUri,"hit":{"type":"object","description":"Echo one hit object from search__find (kind + ids + optional geometry).","additionalProperties":true}},"required":["hit"],"additionalProperties":false}"""
        return listOf(
            syncTool(
                jsonMapper,
                name = McpSearchToolNames.FIND,
                title = "Search conceptual schema",
                description = "Substring search (accent- and case-insensitive) over the same targets as the editor **Localizar** dialog; " +
                    "omit or leave `query` empty to list every candidate in the selected type flags (useful e.g. all entities). " +
                    "At most 400 hits are returned; when truncated, `truncated` is true and `matchCount` is the full count before the cap. " +
                    "Omit all include* booleans (or set none true) to include every category. " +
                    "Use `observationBox` for Annotation elements (distinct from element observations text). " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = findSchema,
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                val query = rawStringArg(req, "query") ?: ""
                val typeFilters = conceptualSearchTypeFiltersFromToolArgs(req)
                val textScope = conceptualSearchTextScopeFromToolArgs(req)
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt ConceptualSearchOutcome.Err("bindings_unavailable")
                    val snap = b.current()
                    if (idx !in snap.sessions.indices) return@runOnEdt ConceptualSearchOutcome.Err("invalid_tab_index")
                    b.onConceptualSearchFind(idx, query.trim(), typeFilters, textScope)
                }
                okText(conceptualSearchFindResultJson(outcome))
            },
            syncTool(
                jsonMapper,
                name = McpSearchToolNames.APPLY_HIT,
                title = "Apply conceptual search hit",
                description = "Selects the hit target on the canvas, opens the matching inspector tab (Selection vs hidden attributes), " +
                    "reveals the hidden-attribute branch when applicable, and pans the canvas to the hit bounds. " +
                    "Pass `tabIndex` or a live model resource URI; then pass the `hit` object from a prior search__find response. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = applySchema,
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                @Suppress("UNCHECKED_CAST")
                val hitMap = req.arguments()["hit"] as? Map<String, Any?> ?: return@syncTool err("hit_required")
                val errMsg = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt "bindings_unavailable"
                    val snap = b.current()
                    if (idx !in snap.sessions.indices) return@runOnEdt "invalid_tab_index"
                    val schema = snap.sessions[idx].schema
                    val hit = parseConceptualSearchHitFromClient(schema, hitMap) ?: return@runOnEdt "invalid_hit_payload"
                    b.onSelectTab(idx)
                    b.onConceptualSearchApplyHit(idx, hit)
                }
                if (errMsg != null) {
                    return@syncTool err(errMsg)
                }
                okText("""{"ok":true,"tabIndex":$idx}""")
            },
        )
    }

    private fun conceptualSearchTypeFiltersFromToolArgs(req: McpSchema.CallToolRequest): ConceptualSearchTypeFilters =
        ConceptualSearchTypeFilters(
            includeEntities = boolArg(req, "includeEntities") == true,
            includeRelationships = boolArg(req, "includeRelationships") == true,
            includeAssociativeEntities = boolArg(req, "includeAssociativeEntities") == true,
            includeSpecializations = boolArg(req, "includeSpecializations") == true,
            includeCanvasAttributes = boolArg(req, "includeCanvasAttributes") == true,
            includeHiddenAttributes = boolArg(req, "includeHiddenAttributes") == true,
            includeCardinalityLabels = boolArg(req, "includeCardinalityLabels") == true,
            includeObservationBoxes = boolArg(req, "observationBox") == true ||
                boolArg(req, "includeObservationBoxes") == true,
        )

    private fun conceptualSearchTextScopeFromToolArgs(req: McpSchema.CallToolRequest): ConceptualSearchTextScope =
        ConceptualSearchTextScope(
            searchDictionary = boolArg(req, "searchDictionary") != false,
            searchObservations = boolArg(req, "searchObservations") != false,
        )

    private fun conceptualSearchFindResultJson(outcome: ConceptualSearchOutcome): String =
        when (outcome) {
            is ConceptualSearchOutcome.Err ->
                """{"ok":false,"error":${jsonString(outcome.code)}}"""
            is ConceptualSearchOutcome.Ok -> {
                val r = outcome.result
                val hitsJson = r.hits.joinToString(",") { conceptualSearchHitToJson(it) }
                """{"ok":true,"truncated":${r.truncated},"matchCount":${r.totalMatched},"returnedCount":${r.hits.size},"hits":[$hitsJson]}"""
            }
        }

    private fun conceptualSearchHitToJson(hit: ConceptualSearchHit): String =
        when (hit) {
            is ConceptualSearchHit.ElementHit -> {
                val p = hit.position
                val pos = """{"x":${p.x},"y":${p.y},"width":${p.width},"height":${p.height}}"""
                val matched = hit.matchedIn.joinToString(",") { jsonString(it) }
                """{"kind":"element","elementId":${hit.elementId},"elementKindKey":${jsonString(hit.elementKindKey)},"title":${jsonString(hit.title)},"matchedIn":[$matched],"position":$pos}"""
            }
            is ConceptualSearchHit.CardinalityHit -> {
                val matched = hit.matchedIn.joinToString(",") { jsonString(it) }
                val posJson = hit.position?.let { p ->
                    """{"x":${p.x},"y":${p.y},"width":${p.width},"height":${p.height}}"""
                } ?: "null"
                """{"kind":"cardinality","connectionId":${hit.connectionId},"title":${jsonString(hit.title)},"matchedIn":[$matched],"position":$posJson}"""
            }
            is ConceptualSearchHit.HiddenHit -> {
                val path = hit.path.joinToString(",")
                val matched = hit.matchedIn.joinToString(",") { jsonString(it) }
                """{"kind":"hidden","ownerElementId":${hit.ownerElementId},"path":[$path],"displayName":${jsonString(hit.displayName)},"matchedIn":[$matched]}"""
            }
        }

    private fun parseConceptualSearchHitFromClient(
        schema: ConceptualSchema,
        hitMap: Map<String, Any?>,
    ): ConceptualSearchHit? {
        val kind = (hitMap["kind"] as? String)?.lowercase()?.trim() ?: return null
        return when (kind) {
            "element" -> {
                val id = intFromMcpAny(hitMap["elementId"]) ?: return null
                val el = schema.elements[id] ?: return null
                val pos = elementPositionFromMcpAny(hitMap["position"]) ?: el.position
                val title = (hitMap["title"] as? String) ?: el.name
                val kindKey = (hitMap["elementKindKey"] as? String) ?: "entity"
                val matched = stringListFromMcpAny(hitMap["matchedIn"])
                ConceptualSearchHit.ElementHit(
                    elementId = id,
                    elementKindKey = kindKey,
                    title = title,
                    matchedIn = matched,
                    position = pos,
                )
            }
            "cardinality" -> {
                val cid = intFromMcpAny(hitMap["connectionId"]) ?: return null
                val title = (hitMap["title"] as? String) ?: ""
                val matched = stringListFromMcpAny(hitMap["matchedIn"])
                val pos = elementPositionFromMcpAny(hitMap["position"])
                ConceptualSearchHit.CardinalityHit(
                    connectionId = cid,
                    title = title,
                    matchedIn = matched,
                    position = pos,
                )
            }
            "hidden" -> {
                val owner = intFromMcpAny(hitMap["ownerElementId"]) ?: return null
                if (schema.elements[owner] == null) return null
                val path = intListFromMcpAny(hitMap["path"])
                val display = (hitMap["displayName"] as? String) ?: ""
                val matched = stringListFromMcpAny(hitMap["matchedIn"])
                ConceptualSearchHit.HiddenHit(
                    ownerElementId = owner,
                    path = path,
                    displayName = display,
                    matchedIn = matched,
                )
            }
            else -> null
        }
    }

    private fun intFromMcpAny(raw: Any?): Int? =
        when (raw) {
            null -> null
            is Int -> raw
            is Long -> raw.toInt()
            is Double -> raw.toInt()
            is Number -> raw.toInt()
            else -> raw.toString().toIntOrNull()
        }

    private fun intListFromMcpAny(raw: Any?): List<Int> {
        if (raw !is List<*>) return emptyList()
        return raw.mapNotNull { intFromMcpAny(it) }
    }

    private fun stringListFromMcpAny(raw: Any?): List<String> {
        if (raw !is List<*>) return emptyList()
        return raw.mapNotNull { elem ->
            when (elem) {
                is String -> elem
                else -> elem?.toString()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun elementPositionFromMcpAny(raw: Any?): ElementPosition? {
        val m = raw as? Map<String, Any?> ?: return null
        val x = intFromMcpAny(m["x"]) ?: return null
        val y = intFromMcpAny(m["y"]) ?: return null
        val w = intFromMcpAny(m["width"]) ?: return null
        val h = intFromMcpAny(m["height"]) ?: return null
        return ElementPosition(x, y, w, h)
    }

    private fun buildResourceUtilityTools(
        jsonMapper: io.modelcontextprotocol.json.McpJsonMapper,
    ): List<McpServerFeatures.SyncToolSpecification> {
        val uriProp =
            """"uri":{"type":"string","minLength":1,"description":"Registered MCP resource URI (live tab, DTD, or classpath example — see server instructions)."}"""
        return listOf(
            syncTool(
                jsonMapper,
                name = McpResourceUtilityToolNames.READ_FULL,
                title = "Read full MCP resource text",
                description = "Returns the entire UTF-16 text body for a registered resource URI (same content as resources/read). " +
                    "Prefer this when you need the exact serialized string without HTTP resource round-trips.",
                schema = """{"type":"object","properties":{$uriProp},"required":["uri"],"additionalProperties":false}""",
            ) { _, req ->
                val uri = stringArg(req, "uri") ?: return@syncTool err("uri required")
                val (text, code) = resolveRegisteredResourcePlainText(uri)
                if (text == null) {
                    return@syncTool err(code ?: "read_failed")
                }
                okText(
                    """{"ok":true,"uri":${jsonString(uri)},"characterLength":${text.length},"content":${jsonString(text)}}""",
                )
            },
            syncTool(
                jsonMapper,
                name = McpResourceUtilityToolNames.READ_LINES,
                title = "Read MCP resource text by line range",
                description = "Returns a slice of the resource text using 1-based line numbers inclusive on both ends (newline character is `\\n`). " +
                    "If endLine is past EOF, the slice ends at the last line.",
                schema = """{"type":"object","properties":{$uriProp,"startLine":{"type":"integer","minimum":1},"endLine":{"type":"integer","minimum":1}},"required":["uri","startLine","endLine"],"additionalProperties":false}""",
            ) { _, req ->
                val uri = stringArg(req, "uri") ?: return@syncTool err("uri required")
                val startLine = intArg(req, "startLine") ?: return@syncTool err("startLine required")
                val endLine = intArg(req, "endLine") ?: return@syncTool err("endLine required")
                val (text, code) = resolveRegisteredResourcePlainText(uri)
                if (text == null) {
                    return@syncTool err(code ?: "read_failed")
                }
                val (slice, err) = McpResourceTextOps.sliceLines1Based(text, startLine, endLine)
                if (slice == null) {
                    return@syncTool err(err ?: "line_slice_failed")
                }
                okText(
                    """{"ok":true,"uri":${jsonString(uri)},"startLine":$startLine,"endLine":$endLine,"content":${jsonString(slice)}}""",
                )
            },
            syncTool(
                jsonMapper,
                name = McpResourceUtilityToolNames.READ_RANGE,
                title = "Read MCP resource text by character index range",
                description = "Returns text.substring(startIndex, endIndex) using Kotlin/Java semantics: startIndex is inclusive, " +
                    "endIndex is exclusive, both are 0-based UTF-16 code unit indices.",
                schema = """{"type":"object","properties":{$uriProp,"startIndex":{"type":"integer","minimum":0},"endIndex":{"type":"integer","minimum":0}},"required":["uri","startIndex","endIndex"],"additionalProperties":false}""",
            ) { _, req ->
                val uri = stringArg(req, "uri") ?: return@syncTool err("uri required")
                val startIndex = intArg(req, "startIndex") ?: return@syncTool err("startIndex required")
                val endIndex = intArg(req, "endIndex") ?: return@syncTool err("endIndex required")
                val (text, code) = resolveRegisteredResourcePlainText(uri)
                if (text == null) {
                    return@syncTool err(code ?: "read_failed")
                }
                val (slice, err) = McpResourceTextOps.sliceByCharRange(text, startIndex, endIndex)
                if (slice == null) {
                    return@syncTool err(err ?: "range_slice_failed")
                }
                okText(
                    """{"ok":true,"uri":${jsonString(uri)},"startIndex":$startIndex,"endIndex":$endIndex,"content":${jsonString(slice)}}""",
                )
            },
            syncTool(
                jsonMapper,
                name = McpResourceUtilityToolNames.SEARCH,
                title = "Search literal text in an MCP resource",
                description = "Non-overlapping literal search in the resolved resource body. " +
                    "Each match includes 0-based start/end character indices (end exclusive) and 1-based line/column for both ends. " +
                    "At most $MAX_RESOURCE_UTILITY_MATCHES matches are returned; if truncated, `truncated` is true.",
                schema = """{"type":"object","properties":{$uriProp,"query":{"type":"string","minLength":1}},"required":["uri","query"],"additionalProperties":false}""",
            ) { _, req ->
                val uri = stringArg(req, "uri") ?: return@syncTool err("uri required")
                val query = rawStringArg(req, "query") ?: return@syncTool err("query required")
                val (text, code) = resolveRegisteredResourcePlainText(uri)
                if (text == null) {
                    return@syncTool err(code ?: "read_failed")
                }
                val (matches, qerr) = McpResourceTextOps.findAllLiteral(text, query)
                if (qerr != null) {
                    return@syncTool err(qerr)
                }
                val total = matches.size
                val truncated = total > MAX_RESOURCE_UTILITY_MATCHES
                val limited = if (truncated) matches.take(MAX_RESOURCE_UTILITY_MATCHES) else matches
                okText(resourceSearchResultJson(uri, limited, total, truncated))
            },
            syncTool(
                jsonMapper,
                name = McpResourceUtilityToolNames.SEARCH_REGEX,
                title = "Search with regex in an MCP resource",
                description = "Runs Kotlin/Java regex.findAll on the resource body with MULTILINE enabled so `^` and `$` match line boundaries. " +
                    "Optional dotMatchesAll maps to RegexOption.DOT_MATCHES_ALL. " +
                    "Each match includes 0-based start/end character indices (end exclusive) and 1-based line/column for both ends. " +
                    "At most $MAX_RESOURCE_UTILITY_MATCHES matches are returned; if truncated, `truncated` is true.",
                schema = """{"type":"object","properties":{$uriProp,"pattern":{"type":"string","minLength":1},"dotMatchesAll":{"type":"boolean"}},"required":["uri","pattern"],"additionalProperties":false}""",
            ) { _, req ->
                val uri = stringArg(req, "uri") ?: return@syncTool err("uri required")
                val pattern = rawStringArg(req, "pattern") ?: return@syncTool err("pattern required")
                val dotAll = boolArg(req, "dotMatchesAll") == true
                val (text, code) = resolveRegisteredResourcePlainText(uri)
                if (text == null) {
                    return@syncTool err(code ?: "read_failed")
                }
                val (matches, perr) = McpResourceTextOps.findAllRegex(text, pattern, dotAll)
                if (perr != null) {
                    return@syncTool err(perr)
                }
                val total = matches.size
                val truncated = total > MAX_RESOURCE_UTILITY_MATCHES
                val limited = if (truncated) matches.take(MAX_RESOURCE_UTILITY_MATCHES) else matches
                okText(resourceSearchResultJson(uri, limited, total, truncated))
            },
        )
    }

    private fun buildProceduralTools(
        jsonMapper: io.modelcontextprotocol.json.McpJsonMapper,
    ): List<McpServerFeatures.SyncToolSpecification> {
        val tabUri = """"tabIndex":{"type":"integer","minimum":0},"uri":{"type":"string","minLength":1}"""
        val xy = """"x":{"type":"integer"},"y":{"type":"integer"}"""
        val textFields = """"name":{"type":"string"},"observations":{"type":"string"},"dictionary":{"type":"string"}"""
        val labelStyle =
            """"labelColorArgb":{"type":"integer"},"labelBold":{"type":"boolean"},"labelItalic":{"type":"boolean"}"""
        val relArrow = """"arrowDirectionCode":{"type":"integer","minimum":0,"maximum":8},"showName":{"type":"boolean"}"""
        val assocInner =
            """"relationshipName":{"type":"string"},"relationshipObservations":{"type":"string"},"relationshipDictionary":{"type":"string"}"""
        val baseEntityIdProp = """"baseEntityId":{"type":"integer","minimum":0}"""
        val specializationBasicSchema =
            """{"type":"object","properties":{$tabUri,$baseEntityIdProp},"required":["baseEntityId"],"additionalProperties":false}"""
        val specializationChildSchema =
            """{"type":"object","properties":{$tabUri,$baseEntityIdProp,"exclusive":{"type":"boolean"}},"required":["baseEntityId","exclusive"],"additionalProperties":false}"""
        val linkEndPickSchema =
            """{"type":"object","properties":{"elementId":{"type":"integer","minimum":0},"associativeOuterEntitySide":{"type":"boolean","description":"True when the pick is the outer entity rectangle of an associative entity."}},"required":["elementId"],"additionalProperties":false}"""
        val relationshipOverridesSchema =
            """{"type":"object","properties":{$textFields,$labelStyle,$relArrow},"additionalProperties":false}"""
        val connectionPatchSchema =
            """{"type":"object","properties":{"cardinalityCode":{"type":"integer","minimum":1,"maximum":4},"showCardinality":{"type":"boolean"},"orientationCode":{"type":"integer","minimum":0,"maximum":3},"cardinalityFixed":{"type":"boolean"},"isWeak":{"type":"boolean"},"cardinalityRole":{"type":"string"},"cardinalityObservations":{"type":"string"},"cardinalityDictionary":{"type":"string"},"cardinalityAutoSize":{"type":"boolean"}},"additionalProperties":false}"""
        val linkObjectsSchema =
            """{"type":"object","properties":{$tabUri,"endA":$linkEndPickSchema,"endB":$linkEndPickSchema,"relationshipOverrides":$relationshipOverridesSchema,"connection":$connectionPatchSchema,"connectionOverrides":{"type":"array","items":$connectionPatchSchema}},"required":["endA","endB"],"additionalProperties":false}"""
        return listOf(
            syncTool(
                jsonMapper,
                name = McpProceduralToolsToolNames.PLACE_ENTITY,
                title = "Place conceptual entity (procedural)",
                description = "Inserts a plain entity at (x,y) using the same allocation rules as the Entity canvas tool, " +
                    "then optionally overrides name, notes, dictionary, weak flag, and label style. " +
                    "Does **not** switch the user's active ribbon/canvas tool. " +
                    "Returns the placed element as JSON (id, geometry, names, style). " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$tabUri,$xy,$textFields,"isWeak":{"type":"boolean"},$labelStyle},"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                val x = intArg(req, "x") ?: 64
                val y = intArg(req, "y") ?: 64
                val overrides = proceduralOverridesForEntity(req)
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onPlaceProceduralConceptualToolAtTab(idx, ConceptualProceduralToolKind.ENTITY, x, y, overrides)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpProceduralToolsToolNames.PLACE_RELATIONSHIP,
                title = "Place conceptual relationship (procedural)",
                description = "Inserts a relationship diamond at (x,y) with the same defaults as the Relationship canvas tool, " +
                    "then optionally overrides name, notes, dictionary, arrow direction (0–8, see domain ArrowDirection), and showName. " +
                    "Does **not** switch the user's active ribbon/canvas tool. " +
                    "Returns the placed element as JSON. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$tabUri,$xy,$textFields,$relArrow},"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                val x = intArg(req, "x") ?: 64
                val y = intArg(req, "y") ?: 64
                val overrides = proceduralOverridesForRelationship(req)
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onPlaceProceduralConceptualToolAtTab(idx, ConceptualProceduralToolKind.RELATIONSHIP, x, y, overrides)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpProceduralToolsToolNames.PLACE_ASSOCIATIVE_ENTITY,
                title = "Place conceptual associative entity (procedural)",
                description = "Inserts an associative entity at (x,y) with auto-generated outer and inner relationship names, " +
                    "then optionally overrides outer/inner names, notes, dictionaries, and inner arrow direction (0–8). " +
                    "Does **not** switch the user's active ribbon/canvas tool. " +
                    "Returns the placed element as JSON. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$tabUri,$xy,$textFields,$assocInner,$labelStyle,"arrowDirectionCode":{"type":"integer","minimum":0,"maximum":8}},"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                val x = intArg(req, "x") ?: 64
                val y = intArg(req, "y") ?: 64
                val overrides = proceduralOverridesForAssociative(req)
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onPlaceProceduralConceptualToolAtTab(
                        idx,
                        ConceptualProceduralToolKind.ASSOCIATIVE_ENTITY,
                        x,
                        y,
                        overrides,
                    )
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpProceduralToolsToolNames.LINK_OBJECTS,
                title = "Link conceptual objects (Ligar Objetos)",
                description = "Completes one conceptual **Ligar Objetos** action in a single call: provide two diagram endpoints (endA/endB) as element picks, " +
                    "mirroring two clicks with the editor link tool. Supported pairs follow the same domain rules as the canvas (entity↔relationship, entity↔entity, specialization↔plain entity, etc.). " +
                    "Linking **two entities** creates a new relationship diamond between them and two legs automatically (same as the ribbon). " +
                    "Optional `relationshipOverrides` adjusts the new relationship or self-relationship (name, notes, arrow, showName where applicable). " +
                    "Optional `connection` (single leg) or `connectionOverrides` (array) adjusts new connection cardinalities and line metadata; " +
                    "when two new legs are created (entity–entity case), send two patches in order **[endA leg, endB leg]** (ascending new connection id matches this order). " +
                    "Specialization↔entity accepts a single `connection` patch; only **plain** entities may connect to a specialization triangle (not associative outers). " +
                    "The editor may promote an optional specialization to restricted when a third subtype link is added — see domain behaviour. " +
                    "Returns `newConnections`, and `newRelationship` / `newSelfRelationship` JSON when those elements were created. " +
                    "Does **not** switch the user's active canvas tool. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = linkObjectsSchema,
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                val args = req.arguments()
                val endA = McpLinkObjectsToolArgs.parseEndPick(args["endA"])
                    ?: return@syncTool err("endA_invalid")
                val endB = McpLinkObjectsToolArgs.parseEndPick(args["endB"])
                    ?: return@syncTool err("endB_invalid")
                val relO = McpLinkObjectsToolArgs.parseRelationshipOverrides(args["relationshipOverrides"])
                val (connErr, connList) = McpLinkObjectsToolArgs.parseConnectionOverrides(
                    args["connectionOverrides"],
                    args["connection"],
                )
                if (connErr != null) {
                    return@syncTool err(connErr)
                }
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onLinkConceptualObjectsAtTab(idx, endA, endB, relO, connList)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpProceduralToolsToolNames.APPLY_SPECIALIZATION_BASIC,
                title = "Apply basic conceptual specialization on entity",
                description = "Creates the optional specialization triangle under the given entity (same rules as the basic Especialização ribbon tool). " +
                    "Does **not** switch the user's active canvas tool. " +
                    "Returns JSON for the new specialization element. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = specializationBasicSchema,
            ) { _, req ->
                applySpecializationToolFromRequest(req, ConceptualSpecializationToolVariant.Basic)
            },
            syncTool(
                jsonMapper,
                name = McpProceduralToolsToolNames.APPLY_SPECIALIZATION_TREE,
                title = "Apply conceptual specialization tree (with child entity)",
                description = "Creates a specialization node and a connected child entity (small tree). Set `exclusive` to true for restricted specialization (ribbon A / EspRestrita), " +
                    "or false for optional specialization (ribbon B / EspOpicional). " +
                    "Does **not** switch the user's active canvas tool. " +
                    "Returns JSON for the new specialization element. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = specializationChildSchema,
            ) { _, req ->
                val exclusive = boolArg(req, "exclusive")
                    ?: return@syncTool err("exclusive_required")
                val variant = if (exclusive) {
                    ConceptualSpecializationToolVariant.ExclusiveWithEntityCreation
                } else {
                    ConceptualSpecializationToolVariant.NonExclusiveWithEntityCreation
                }
                applySpecializationToolFromRequest(req, variant)
            },
        )
    }

    private fun buildAttributeTools(
        jsonMapper: io.modelcontextprotocol.json.McpJsonMapper,
    ): List<McpServerFeatures.SyncToolSpecification> {
        val tabUri = """"tabIndex":{"type":"integer","minimum":0},"uri":{"type":"string","minLength":1}"""
        val targetId = """"targetElementId":{"type":"integer","minimum":0}"""
        val holderId = """"holderElementId":{"type":"integer","minimum":0}"""
        val attachSideProp =
            """"attachSide":{"type":"string","description":"Optional: left, top, right, bottom. Omit to auto-pick the least crowded side (tie-break right)."}"""
        val variantProp =
            """"attributeVariant":{"type":"string","enum":["basic","identifier","multivalued","optional"],"description":"Maps to ribbon attribute tool variants (not composite)."}"""
        val overridesProp =
            """"overrides":{"type":"object","additionalProperties":true,"description":"Optional: name, observations, dictionary, valueType, complement, minCardinality, maxCardinality, isIdentifier, isOptional, isMultiValued."}"""
        val simpleSchema =
            """{"type":"object","properties":{$tabUri,$targetId,$attachSideProp,$variantProp,$overridesProp},"required":["targetElementId","attributeVariant"],"additionalProperties":false}"""
        val childrenProp =
            """"children":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":true},"description":"Leaf field specs only; do not send canvas composite children here."}"""
        val nestedHid =
            """"nestedHiddenAttributes":{"type":"array","items":{"type":"object","additionalProperties":true},"description":"Optional ocultos stored on the new composite parent."}"""
        val compositeSchema =
            """{"type":"object","properties":{$tabUri,$targetId,$attachSideProp,$childrenProp,$nestedHid},"required":["targetElementId","children"],"additionalProperties":false}"""
        val rootsProp =
            """"roots":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":true},"description":"Recursive HiddenAttribute trees (name, type, isIdentifier, isOptional, minCardinality, maxCardinality, position, children, nestedHiddenAttributes, observations, dictionary)."}"""
        val hiddenSchema =
            """{"type":"object","properties":{$tabUri,$holderId,$rootsProp},"required":["holderElementId","roots"],"additionalProperties":false}"""
        return listOf(
            syncTool(
                jsonMapper,
                name = McpAttributeToolsToolNames.APPLY_ATTRIBUTE,
                title = "Place simple conceptual attribute",
                description = "Adds a non-composite attribute to an entity, relationship, associative entity, or composite attribute (same rules as the canvas attribute tool). " +
                    "Optional attachSide selects the owner edge; omit to auto-pick the least crowded side (tie-break right), then the editor runs organize on that side only. " +
                    "Prefer keeping siblings on the same side (often right); if that side would obscure diagram objects, choose another side — see MCP server instructions for density heuristics. " +
                    "Does **not** switch the active canvas tool. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = simpleSchema,
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                val targetIdVal = intArg(req, "targetElementId") ?: return@syncTool err("targetElementId_required")
                val variant = McpAttributeToolArgs.parseSimpleVariant(req.arguments()["attributeVariant"])
                    ?: return@syncTool err("attributeVariant_invalid")
                val side = McpAttributeToolArgs.parseAttachSide(req.arguments()["attachSide"])
                val overrides = McpAttributeToolArgs.parseSimpleOverrides(req.arguments()["overrides"])
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplySimpleConceptualAttributeAtTab(idx, targetIdVal, variant, side, overrides)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpAttributeToolsToolNames.APPLY_COMPOSITE_ATTRIBUTE,
                title = "Place composite conceptual attribute with leaf children",
                description = "Creates a composite attribute with one or more **simple** canvas children in one step (no nested composite children in this call). " +
                    "Optional attachSide follows the same rules as apply_attribute (default least crowded, tie-break right; prefer same side / avoid covering diagram objects). " +
                    "Optional nestedHiddenAttributes attach to the new composite parent. " +
                    "Does **not** switch the active canvas tool. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = compositeSchema,
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                val targetIdVal = intArg(req, "targetElementId") ?: return@syncTool err("targetElementId_required")
                val rawChildren = req.arguments()["children"]
                if (rawChildren !is List<*>) {
                    return@syncTool err("children_required")
                }
                validateCompositeLeafRequestItems(rawChildren)?.let { return@syncTool err(it) }
                val leafSpecs = McpAttributeToolArgs.parseCompositeLeafSpecs(rawChildren)
                    ?: return@syncTool err("children_invalid")
                val nestedList = when (val nr = req.arguments()["nestedHiddenAttributes"]) {
                    null -> emptyList()
                    else -> McpAttributeToolArgs.parseHiddenRoots(nr) ?: return@syncTool err("nestedHiddenAttributes_invalid")
                }
                val side = McpAttributeToolArgs.parseAttachSide(req.arguments()["attachSide"])
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplyCompositeConceptualAttributeAtTab(idx, targetIdVal, side, leafSpecs, nestedList)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpAttributeToolsToolNames.APPLY_HIDDEN_ATTRIBUTE,
                title = "Append hidden attribute forest",
                description = "Appends one or more recursive hidden-attribute trees to any element that supports ocultos in the inspector (entity, relationship, associative entity, attribute, self-relationship, specialization, annotation). " +
                    "Names must be unique across the request and against existing canvas and hidden names. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = hiddenSchema,
            ) { _, req ->
                val idx = tabIndexFromTabOrUriArgs(req) ?: return@syncTool err("tabIndex_or_uri_required")
                val holderId = intArg(req, "holderElementId") ?: return@syncTool err("holderElementId_required")
                val rawRoots = req.arguments()["roots"] ?: return@syncTool err("roots_required")
                val roots = McpAttributeToolArgs.parseHiddenRoots(rawRoots) ?: return@syncTool err("roots_invalid")
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplyHiddenAttributeForestAtTab(idx, holderId, roots)
                }
                proceduralToolOutcomeToResult(outcome)
            },
        )
    }

    private fun validateCompositeLeafRequestItems(raw: List<*>): String? {
        for (item in raw) {
            if (item !is Map<*, *>) return "children_item_invalid"
            @Suppress("UNCHECKED_CAST")
            val m = item as Map<String, Any?>
            val ch = m["children"] as? List<*>
            if (ch != null && ch.isNotEmpty()) return "composite_leaf_nested_children_not_supported"
            val nh = m["nestedHiddenAttributes"] as? List<*>
            if (nh != null && nh.isNotEmpty()) return "composite_leaf_nested_hidden_not_supported"
        }
        return null
    }

    private fun applySpecializationToolFromRequest(
        req: McpSchema.CallToolRequest,
        variant: ConceptualSpecializationToolVariant,
    ): McpSchema.CallToolResult {
        val idx = tabIndexFromTabOrUriArgs(req) ?: return err("tabIndex_or_uri_required")
        val baseId = intArg(req, "baseEntityId") ?: return err("baseEntityId_required")
        val outcome = runOnEdt {
            val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
            b.onApplyConceptualSpecializationAtTab(idx, baseId, variant)
        }
        return proceduralToolOutcomeToResult(outcome)
    }

    private fun proceduralOverridesForEntity(req: McpSchema.CallToolRequest): ConceptualProceduralToolOverrides =
        ConceptualProceduralToolOverrides(
            name = optionalKeyedTrimmedString(req, "name"),
            observations = optionalKeyedRawString(req, "observations"),
            dictionary = optionalKeyedRawString(req, "dictionary"),
            isWeak = optionalBoolArg(req, "isWeak"),
            labelColorArgb = optionalIntArg(req, "labelColorArgb"),
            labelBold = optionalBoolArg(req, "labelBold"),
            labelItalic = optionalBoolArg(req, "labelItalic"),
        )

    private fun proceduralOverridesForRelationship(req: McpSchema.CallToolRequest): ConceptualProceduralToolOverrides =
        ConceptualProceduralToolOverrides(
            name = optionalKeyedTrimmedString(req, "name"),
            observations = optionalKeyedRawString(req, "observations"),
            dictionary = optionalKeyedRawString(req, "dictionary"),
            labelColorArgb = optionalIntArg(req, "labelColorArgb"),
            labelBold = optionalBoolArg(req, "labelBold"),
            labelItalic = optionalBoolArg(req, "labelItalic"),
            arrowDirectionCode = optionalIntArg(req, "arrowDirectionCode"),
            showName = optionalBoolArg(req, "showName"),
        )

    private fun proceduralOverridesForAssociative(req: McpSchema.CallToolRequest): ConceptualProceduralToolOverrides =
        ConceptualProceduralToolOverrides(
            name = optionalKeyedTrimmedString(req, "name"),
            observations = optionalKeyedRawString(req, "observations"),
            dictionary = optionalKeyedRawString(req, "dictionary"),
            relationshipName = optionalKeyedTrimmedString(req, "relationshipName"),
            relationshipObservations = optionalKeyedRawString(req, "relationshipObservations"),
            relationshipDictionary = optionalKeyedRawString(req, "relationshipDictionary"),
            labelColorArgb = optionalIntArg(req, "labelColorArgb"),
            labelBold = optionalBoolArg(req, "labelBold"),
            labelItalic = optionalBoolArg(req, "labelItalic"),
            arrowDirectionCode = optionalIntArg(req, "arrowDirectionCode"),
        )

    private fun proceduralToolOutcomeToResult(outcome: McpProceduralToolApplyOutcome): McpSchema.CallToolResult {
        val errMsg = outcome.error
        if (errMsg != null) {
            return err(errMsg)
        }
        val ej = outcome.elementJson ?: return err("internal_no_element_json")
        if (outcome.isFullResponseJson) {
            return okText(ej)
        }
        return okText("""{"ok":true,"tabIndex":${outcome.tabIndex},"element":$ej}""")
    }

    private fun optionalKeyedTrimmedString(req: McpSchema.CallToolRequest, key: String): String? {
        if (!req.arguments().containsKey(key)) return null
        val raw = req.arguments()[key] ?: return ""
        return when (raw) {
            is String -> raw
            else -> raw.toString()
        }.trim()
    }

    private fun optionalKeyedRawString(req: McpSchema.CallToolRequest, key: String): String? {
        if (!req.arguments().containsKey(key)) return null
        val raw = req.arguments()[key] ?: return ""
        return when (raw) {
            is String -> raw
            else -> raw.toString()
        }
    }

    private fun optionalIntArg(req: McpSchema.CallToolRequest, key: String): Int? {
        if (!req.arguments().containsKey(key)) return null
        return intArg(req, key)
    }

    private fun optionalBoolArg(req: McpSchema.CallToolRequest, key: String): Boolean? {
        if (!req.arguments().containsKey(key)) return null
        return boolArg(req, key)
    }

    private fun resourceSearchResultJson(
        uri: String,
        matches: List<McpTextMatchSpan>,
        totalMatchCount: Int,
        truncated: Boolean,
    ): String {
        val arr = matches.joinToString(prefix = "[", postfix = "]", separator = ",") { m ->
            """{"startCharacterIndex":${m.startIndex},"endCharacterIndexExclusive":${m.endIndexExclusive},"startLine1":${m.startLine1},"startColumn1":${m.startColumn1},"endLine1":${m.endLine1},"endColumn1":${m.endColumn1},"match":${jsonString(m.match)}}"""
        }
        return """{"ok":true,"uri":${jsonString(uri)},"matchCount":$totalMatchCount,"truncated":$truncated,"matches":$arr}"""
    }

    private fun syncTool(
        jsonMapper: io.modelcontextprotocol.json.McpJsonMapper,
        name: String,
        title: String,
        description: String,
        schema: String,
        handler: BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult>,
    ): McpServerFeatures.SyncToolSpecification {
        val tool = McpSchema.Tool.builder()
            .name(name)
            .title(title)
            .description(description)
            .inputSchema(jsonMapper, schema)
            .build()
        return McpServerFeatures.SyncToolSpecification(tool, handler)
    }

    private fun intArg(req: McpSchema.CallToolRequest, key: String): Int? {
        val raw = req.arguments()[key] ?: return null
        return when (raw) {
            is Int -> raw
            is Long -> raw.toInt()
            is Double -> raw.toInt()
            is Number -> raw.toInt()
            else -> raw.toString().toIntOrNull()
        }
    }

    private fun stringArg(req: McpSchema.CallToolRequest, key: String): String? {
        val raw = req.arguments()[key] ?: return null
        return when (raw) {
            is String -> raw
            else -> raw.toString()
        }.trim().takeIf { it.isNotEmpty() }
    }

    private fun rawStringArg(req: McpSchema.CallToolRequest, key: String): String? {
        val raw = req.arguments()[key] ?: return null
        val s = when (raw) {
            is String -> raw
            else -> raw.toString()
        }
        return s.takeIf { it.isNotEmpty() }
    }

    private fun boolArg(req: McpSchema.CallToolRequest, key: String): Boolean? {
        val raw = req.arguments()[key] ?: return null
        return when (raw) {
            is Boolean -> raw
            else -> raw.toString().toBooleanStrictOrNull() ?: raw.toString().toBoolean()
        }
    }

    private fun tabSelectionChangeSuccessJson(
        change: McpTabSelectionChange,
        extraFieldsJson: String = "",
    ): String {
        return buildString {
            append(
                """{"ok":true,"createdTabIndex":${change.createdTabIndex},"selectedIndex":${change.selectedTabIndex},"createdResourceUri":${jsonString(change.createdResourceUri)},"selectedResourceUri":${jsonString(change.selectedResourceUri)}""",
            )
            if (extraFieldsJson.isNotEmpty()) {
                append(',')
                append(extraFieldsJson)
            }
            append('}')
        }
    }

    private fun okText(json: String): McpSchema.CallToolResult =
        McpSchema.CallToolResult.builder()
            .addTextContent(json)
            .build()

    private fun err(message: String): McpSchema.CallToolResult =
        McpSchema.CallToolResult.builder()
            .isError(true)
            .addTextContent("""{"ok":false,"error":${jsonString(message)}}""")
            .build()

    private fun jsonString(s: String): String {
        val escaped = buildString(s.length + 8) {
            append('"')
            for (ch in s) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (ch.code < 32) append("\\u%04x".format(ch.code)) else append(ch)
                }
            }
            append('"')
        }
        return escaped
    }
}
