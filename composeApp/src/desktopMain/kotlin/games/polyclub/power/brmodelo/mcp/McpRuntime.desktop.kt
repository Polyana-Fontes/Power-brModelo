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

internal actual class McpRuntime {

    companion object {
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

    /**
     * MCP clients (e.g. Cursor) list **concrete** resources via `resources/list`; URI templates alone
     * are not enough. Registers one `brmodelo://model/{index}` resource per open tab and refreshes
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
            val uri = modelResourceUri(index)
            val rawTitle = tab.displayTitle()
            val title = if (rawTitle.length > 120) rawTitle.take(117) + "..." else rawTitle
            val spec = McpServerFeatures.SyncResourceSpecification(
                McpSchema.Resource.builder()
                    .uri(uri)
                    .name("conceptual_model_$index")
                    .title("Tab $index — $title")
                    .description(
                        "Live in-memory conceptual schema for editor tab index $index " +
                            "(same XML as a saved brModelo export).",
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
        val index = parseModelResourceTabIndex(uri)
            ?: return McpSchema.ReadResourceResult(
                listOf(
                    McpSchema.TextResourceContents(
                        uri,
                        "text/plain",
                        "Invalid or missing tab index in URI: $uri",
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
        return parseModelResourceTabIndex(uri)
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
                        val uri = modelResourceUri(index)
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
                description = "Opens a new empty conceptual model tab and selects it.",
                schema = """{"type":"object","properties":{},"additionalProperties":false}""",
            ) { _, _ ->
                val newIndex = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt -1
                    b.onAddBlankTab()
                    b.current().selectedIndex
                }
                okText("""{"ok":true,"selectedIndex":$newIndex}""")
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
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{"path":{"type":"string","minLength":1,"description":"Absolute path to a .xml or .brm file"}},"required":["path"],"additionalProperties":false}""",
            ) { _, req ->
                val path = stringArg(req, "path") ?: return@syncTool err("path required")
                val errMsg = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt "bindings_unavailable"
                    b.onOpenModelFileAtPath(path)
                }
                if (errMsg != null) {
                    return@syncTool err(errMsg)
                }
                okText("""{"ok":true,"path":${jsonString(path)}}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.OPEN_XML,
                title = "Open conceptual XML from text",
                description = "Parses UTF-8 conceptual XML and opens a new dirty tab (basename fileName only, no path; used as the model title). " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{"fileName":{"type":"string","minLength":1,"description":"Basename only, e.g. modelo.xml"},"xml":{"type":"string","description":"Conceptual schema XML (UTF-8)"}},"required":["fileName","xml"],"additionalProperties":false}""",
            ) { _, req ->
                val fileName = stringArg(req, "fileName") ?: return@syncTool err("fileName required")
                val xml = rawStringArg(req, "xml") ?: return@syncTool err("xml required")
                val errMsg = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt "bindings_unavailable"
                    b.onOpenXmlAsUnsavedTab(fileName, xml)
                }
                if (errMsg != null) {
                    return@syncTool err(errMsg)
                }
                okText("""{"ok":true,"fileName":${jsonString(fileName)}}""")
            },
        )
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
