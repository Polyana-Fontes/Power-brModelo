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
import games.polyclub.power.brmodelo.domain.ConceptualLayoutQualityReport
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolKind
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolOverrides
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.analyzeConceptualLayoutQuality
import games.polyclub.power.brmodelo.domain.ConceptualLinkConnectionOverridePatch
import games.polyclub.power.brmodelo.domain.ConceptualLinkPick
import games.polyclub.power.brmodelo.domain.syntheticClickOnOwnerSideCenter
import games.polyclub.power.brmodelo.domain.ConceptualSearchHit
import games.polyclub.power.brmodelo.domain.ConceptualSearchOutcome
import games.polyclub.power.brmodelo.domain.ConceptualSearchTextScope
import games.polyclub.power.brmodelo.domain.ConceptualSearchTypeFilters
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolVariant
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.ConceptualAttributeAttachPonto
import games.polyclub.power.brmodelo.domain.CanvasSelectionRectangleMergeMode
import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaXmlSerializer
import games.polyclub.power.brmodelo.domain.tryBuildCanvasSelectionFromMcpPickLists
import games.polyclub.power.brmodelo.domain.elementIdsForClipboard
import games.polyclub.power.brmodelo.domain.selectedPickCount
import games.polyclub.power.brmodelo.domain.toMultiPickSets
import games.polyclub.power.brmodelo.ui.ConceptualSubsetRasterFormat
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
import java.util.Base64
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
    val createdResourceUri: String,
    val selectedResourceUri: String,
    val createdResourceUriPng: String,
    val createdResourceUriJpeg: String,
    val selectedResourceUriPng: String,
    val selectedResourceUriJpeg: String,
    val createdSessionId: Long,
    val selectedSessionId: Long,
)

internal actual class McpRuntime {

    companion object {
        private const val MAX_RESOURCE_UTILITY_MATCHES = 2000

        /** JSON Schema fragment: required live tab URI (see [McpTabToolNames.LIST_OPEN]). */
        private const val TAB_TOOL_RESOURCE_URI_SCHEMA_PROP =
            """"resourceUri":{"type":"string","minLength":1,"description":"Live tab URI from tabs__list_open (resourceUri, resourceUriPng, or resourceUriJpeg for the same tab)."}"""

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
        val createdId = after.sessions[createdIdx].id
        val selectedId = after.sessions[selectedIdx].id
        return McpTabSelectionChange(
            createdResourceUri = modelResourceUriForSession(createdId),
            selectedResourceUri = modelResourceUriForSession(selectedId),
            createdResourceUriPng = modelResourcePngUriForSession(createdId),
            createdResourceUriJpeg = modelResourceJpgUriForSession(createdId),
            selectedResourceUriPng = modelResourcePngUriForSession(selectedId),
            selectedResourceUriJpeg = modelResourceJpgUriForSession(selectedId),
            createdSessionId = createdId,
            selectedSessionId = selectedId,
        )
    }

    /**
     * New blank tab: [createdSessionId] comes from [McpUiBindings.onAddBlankTab]. Do **not** require a second
     * [McpUiBindings.current] snapshot to list the new id: Compose may not have refreshed the bindings closure
     * on the same EDT tick, so `after.sessions` could still omit the appended tab even though it exists in the app.
     */
    private fun mcpTabSelectionChangeForNewConceptualTab(
        before: McpTabSnapshot,
        createdSessionId: Long,
    ): McpTabSelectionChange? {
        if (before.sessions.isEmpty()) return null
        val selIdx = before.selectedIndex.coerceIn(0, before.sessions.lastIndex)
        val selectedSession = before.sessions[selIdx]
        return McpTabSelectionChange(
            createdResourceUri = modelResourceUriForSession(createdSessionId),
            selectedResourceUri = modelResourceUriForSession(selectedSession.id),
            createdResourceUriPng = modelResourcePngUriForSession(createdSessionId),
            createdResourceUriJpeg = modelResourceJpgUriForSession(createdSessionId),
            selectedResourceUriPng = modelResourcePngUriForSession(selectedSession.id),
            selectedResourceUriJpeg = modelResourceJpgUriForSession(selectedSession.id),
            createdSessionId = createdSessionId,
            selectedSessionId = selectedSession.id,
        )
    }

    /**
     * Registers `brmodelo://model/{sessionId}.xml`, `.png`, and `.jpg` per open tab (PNG/JPEG match **Exportar em PNG/JPEG**)
     * and refreshes the list when tabs change.
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
            val rawTitle = tab.displayTitle()
            val title = if (rawTitle.length > 120) rawTitle.take(117) + "..." else rawTitle
            val xmlUri = modelResourceUriForSession(tab.id)
            val pngUri = modelResourcePngUriForSession(tab.id)
            val jpgUri = modelResourceJpgUriForSession(tab.id)

            fun register(
                uri: String,
                nameSuffix: String,
                titleSuffix: String,
                mime: String,
                description: String,
            ) {
                val spec = McpServerFeatures.SyncResourceSpecification(
                    McpSchema.Resource.builder()
                        .uri(uri)
                        .name("conceptual_model_${index}_$nameSuffix")
                        .title("Tab $index — $title ($titleSuffix)")
                        .description(description)
                        .mimeType(mime)
                        .build(),
                    BiFunction { _: McpSyncServerExchange, req: McpSchema.ReadResourceRequest ->
                        readLiveModelTabResource(req.uri())
                    },
                )
                server.addResource(spec)
            }

            register(
                uri = xmlUri,
                nameSuffix = "xml",
                titleSuffix = "MER XML",
                mime = "application/xml",
                description =
                    "Live in-memory conceptual schema for editor tab index $index " +
                        "(stable id: session ${tab.id}; same XML as a saved brModelo export).",
            )
            register(
                uri = pngUri,
                nameSuffix = "png",
                titleSuffix = "PNG preview",
                mime = "image/png",
                description =
                    "Raster preview of tab $index (session ${tab.id}), same rendering as **Exportar em PNG** " +
                        "(transparent background, cropped to diagram bounds).",
            )
            register(
                uri = jpgUri,
                nameSuffix = "jpg",
                titleSuffix = "JPEG preview",
                mime = "image/jpeg",
                description =
                    "Raster preview of tab $index (session ${tab.id}), same rendering as **Exportar em JPEG** " +
                        "(opaque canvas-gray background, cropped to diagram bounds).",
            )
        }
        server.notifyResourcesListChanged()
    }

    private fun readLiveModelTabResource(uri: String): McpSchema.ReadResourceResult {
        val parsed = parseLiveModelTabResourceUri(uri)
        if (parsed == null) {
            return McpSchema.ReadResourceResult(
                listOf(
                    McpSchema.TextResourceContents(
                        uri,
                        "text/plain",
                        "Invalid or unknown model resource URI: $uri",
                    ),
                ),
            )
        }
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
        val surface = parsed.surface
        if (surface == null || surface == LiveModelTabResourceSurface.Xml) {
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

        val bytes = runOnEdt {
            val b = bindingsRef.get() ?: return@runOnEdt null
            when (surface) {
                LiveModelTabResourceSurface.Png -> b.onEncodeTabConceptualMenuExportPng(index)
                LiveModelTabResourceSurface.Jpeg -> b.onEncodeTabConceptualMenuExportJpeg(index)
                else -> null
            }
        }
        if (bytes == null || bytes.isEmpty()) {
            return McpSchema.ReadResourceResult(
                listOf(
                    McpSchema.TextResourceContents(
                        uri,
                        "text/plain",
                        "Failed to encode raster preview for tab index $index.",
                    ),
                ),
            )
        }
        val mime = when (surface) {
            LiveModelTabResourceSurface.Png -> "image/png"
            LiveModelTabResourceSurface.Jpeg -> "image/jpeg"
            else -> "application/octet-stream"
        }
        val b64 = Base64.getEncoder().encodeToString(bytes)
        val blob = McpSchema.BlobResourceContents(uri, mime, b64)
        return McpSchema.ReadResourceResult(listOf(blob))
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
            if (!isLiveModelTabXmlPlainTextResourceUri(u)) {
                return null to "binary_tab_resource_use_resources_read"
            }
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

    private fun tabIndexFromResourceUriArg(req: McpSchema.CallToolRequest): Int? {
        val uri = stringArg(req, "resourceUri") ?: return null
        return runOnEdt {
            val b = bindingsRef.get() ?: return@runOnEdt null
            tabIndexForModelResourceUri(uri, b.current().sessions)
        }
    }

    private fun runSelectTabWithOptionalWindowFocus(req: McpSchema.CallToolRequest, tabIndex: Int): McpSchema.CallToolResult {
        val wantFocus = boolArg(req, "requestWindowFocus") == true
        val selectedResourceUri = runOnEdt {
            val b = bindingsRef.get() ?: return@runOnEdt null
            val snap = b.current()
            if (tabIndex !in snap.sessions.indices) return@runOnEdt null
            val prev = snap.selectedIndex
            b.onSelectTab(tabIndex)
            if (wantFocus) {
                b.onRequestAppWindowFocus()
                b.onShowMcpAgentUserNotice(
                    McpAgentUserNotice(
                        activeTabChanged = tabIndex != prev,
                        windowFocused = true,
                    ),
                )
            }
            val tab = snap.sessions[tabIndex]
            modelResourceUriForSession(tab.id)
        }
        if (selectedResourceUri == null) return err("invalid_tab_index")
        return okText("""{"ok":true,"selectedResourceUri":${jsonString(selectedResourceUri)},"requestWindowFocus":$wantFocus}""")
    }

    private fun buildTools(jsonMapper: io.modelcontextprotocol.json.McpJsonMapper): List<McpServerFeatures.SyncToolSpecification> {
        return listOf(
            syncTool(
                jsonMapper,
                name = McpTabToolNames.LIST_OPEN,
                title = "List open tabs",
                description = "Returns JSON for each open editor tab (stable session id, title, dirty, filePath, resourceUri for MER XML, " +
                    "resourceUriPng and resourceUriJpeg for raster previews matching **Exportar em PNG/JPEG**) and selectedResourceUri for the focused tab's MER XML. " +
                    "Root fields `selectedTabIndex` (0-based) and `selectedTabSessionId` identify the tab that is **currently selected in the editor UI**; each row includes `tabIndex` and `isSelected`. " +
                    "Read PNG/JPEG via MCP resources/read (base64 blob). " +
                    "Static DTD and example MER resources are listed only in MCP server instructions (not tab rows).",
                schema = """{"type":"object","properties":{},"additionalProperties":false}""",
            ) { _, _ ->
                val json = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt """{"error":"bindings_unavailable"}"""
                    val snap = b.current()
                    if (snap.sessions.isEmpty()) {
                        return@runOnEdt """{"selectedTabIndex":-1,"selectedTabSessionId":null,"selectedResourceUri":null,"tabs":[]}"""
                    }
                    val selIdx = snap.selectedIndex.coerceIn(0, snap.sessions.lastIndex)
                    val selectedSessionId = snap.sessions.getOrNull(selIdx)?.id
                    val rows = snap.sessions.mapIndexed { tabIndex, tab ->
                        val uri = modelResourceUriForSession(tab.id)
                        val png = modelResourcePngUriForSession(tab.id)
                        val jpg = modelResourceJpgUriForSession(tab.id)
                        val isSel = tabIndex == selIdx
                        """{"tabIndex":$tabIndex,"isSelected":$isSel,"id":${tab.id},"title":${jsonString(tab.displayTitle())},"dirty":${tab.hasUnsavedChanges()},"filePath":${jsonString(tab.schema.filePath)},"resourceUri":${jsonString(uri)},"resourceUriPng":${jsonString(png)},"resourceUriJpeg":${jsonString(jpg)}}"""
                    }
                    val selectedUri = snap.sessions.getOrNull(selIdx)?.let { modelResourceUriForSession(it.id) }
                    val selectedJson = selectedUri?.let { jsonString(it) } ?: "null"
                    val selectedIdJson = selectedSessionId?.toString() ?: "null"
                    """{"selectedTabIndex":$selIdx,"selectedTabSessionId":$selectedIdJson,"selectedResourceUri":$selectedJson,"tabs":[${rows.joinToString(",")}]}"""
                }
                okText(json)
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.SELECT,
                title = "Select tab",
                description = "Brings the tab identified by `resourceUri` to the foreground (use any live tab URI from tabs__list_open: " +
                    "resourceUri, resourceUriPng, or resourceUriJpeg for the same tab). " +
                    "Optional `requestWindowFocus` (default false) also raises the Power-brModelo window — use only when you want the user to notice the app (see MCP server instructions). " +
                    "When true, the user sees a short snackbar that an MCP action changed focus/tab. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$TAB_TOOL_RESOURCE_URI_SCHEMA_PROP,"requestWindowFocus":{"type":"boolean","description":"Bring the editor window to the front after selecting the tab."}},"required":["resourceUri"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                runSelectTabWithOptionalWindowFocus(req, idx)
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.FOCUS_WINDOW,
                title = "Bring editor window to front",
                description = "Raises the Power-brModelo desktop window and requests focus. " +
                    "Use sparingly when you want the user to look at the editor (see MCP server instructions). " +
                    "Shows a short snackbar that an MCP action brought the window forward.",
                schema = """{"type":"object","properties":{},"additionalProperties":false}""",
            ) { _, _ ->
                val ok = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt false
                    b.onRequestAppWindowFocus()
                    b.onShowMcpAgentUserNotice(McpAgentUserNotice(windowFocused = true))
                    true
                }
                if (!ok) return@syncTool err("bindings_unavailable")
                okText("""{"ok":true}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.CLOSE,
                title = "Close tab",
                description = "Closes the tab identified by `resourceUri` (any live tab URI from tabs__list_open). " +
                    "When discardUnsavedChanges is true, unsaved edits are dropped immediately. " +
                    "Otherwise the UI may prompt the user; this tool cannot wait for that dialog. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$TAB_TOOL_RESOURCE_URI_SCHEMA_PROP,"discardUnsavedChanges":{"type":"boolean"}},"required":["resourceUri"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val closedUri = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt null
                    val snap = b.current()
                    val tab = snap.sessions.getOrNull(idx) ?: return@runOnEdt null
                    modelResourceUriForSession(tab.id)
                } ?: return@syncTool err("invalid_tab_index")
                val discard = boolArg(req, "discardUnsavedChanges") == true
                val message = runCloseTabAtIndex(idx, discard)
                if (message != "ok") {
                    return@syncTool err(message)
                }
                okText("""{"ok":true,"closedResourceUri":${jsonString(closedUri)}}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.NEW_CONCEPTUAL_MODEL,
                title = "New conceptual model tab",
                description = "Opens a new empty conceptual model tab **without** switching the editor's focused tab (the user's current diagram stays selected in the UI). " +
                    "On success the JSON includes createdResourceUri (.xml), createdResourceUriPng, createdResourceUriJpeg " +
                    "for the new tab, plus selectedResourceUri, selectedResourceUriPng, and selectedResourceUriJpeg for the tab that **remains** focused after the call (usually the one the user was on). " +
                    "Use createdResourceUri for subsequent MCP tools targeting the new diagram. " +
                    "Raster URIs match **Exportar em PNG/JPEG**; read them with resources/read. " +
                    "Resource URIs use the stable tab session id so they stay valid if other tabs are closed. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{},"additionalProperties":false}""",
            ) { _, _ ->
                val change = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt null
                    val before = b.current()
                    val createdSessionId = b.onAddBlankTab()
                    mcpTabSelectionChangeForNewConceptualTab(before, createdSessionId)
                } ?: return@syncTool err("bindings_unavailable")
                okText(tabSelectionChangeSuccessJson(change))
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.SAVE,
                title = "Save tab",
                description = "Runs the same save path as the editor for the tab identified by `resourceUri` (any live tab URI from tabs__list_open). " +
                    "Optional Save-As when saveAs is true. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$TAB_TOOL_RESOURCE_URI_SCHEMA_PROP,"saveAs":{"type":"boolean"}},"required":["resourceUri"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val savedUri = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt null
                    val snap = b.current()
                    val tab = snap.sessions.getOrNull(idx) ?: return@runOnEdt null
                    modelResourceUriForSession(tab.id)
                } ?: return@syncTool err("invalid_tab_index")
                val saveAs = boolArg(req, "saveAs") == true
                val ok = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt false
                    b.onSaveTab(idx, saveAs)
                }
                if (!ok) {
                    return@syncTool err("save_cancelled_or_failed")
                }
                okText("""{"ok":true,"savedResourceUri":${jsonString(savedUri)},"saveAs":$saveAs}""")
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.OPEN_FILE,
                title = "Open model file",
                description = "Loads a brModelo XML or .brm file from an absolute path on disk (same as opening from disk in the editor). " +
                    "On success the JSON includes createdResourceUri, createdResourceUriPng, createdResourceUriJpeg " +
                    "for the tab that received the model (new or reused) and selectedResourceUri, selectedResourceUriPng, selectedResourceUriJpeg for the tab that ends up selected. " +
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
                    "On success the JSON includes createdResourceUri, createdResourceUriPng, createdResourceUriJpeg " +
                    "and selectedResourceUri, selectedResourceUriPng, selectedResourceUriJpeg after the operation. " +
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
                    "Identify the tab with `resourceUri` from tabs__list_open (resourceUri, resourceUriPng, or resourceUriJpeg; XML body is always required). " +
                    "Preserves the tab's disk path metadata. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$TAB_TOOL_RESOURCE_URI_SCHEMA_PROP,"xml":{"type":"string","description":"Full MER XML (UTF-8)"}},"required":["resourceUri","xml"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
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
                val bodyJson = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt """{"ok":true}"""
                    val snap = b.current()
                    val tab = snap.sessions.getOrNull(idx) ?: return@runOnEdt """{"ok":true}"""
                    val rUri = jsonString(modelResourceUriForSession(tab.id))
                    val schema = b.current().schemaForTab(idx) ?: return@runOnEdt """{"ok":true,"resourceUri":$rUri}"""
                    val report = analyzeConceptualLayoutQuality(schema, null)
                    McpLayoutQualityJson.mergeLayoutQualityIntoJsonObjectBody("""{"ok":true,"resourceUri":$rUri}""", report, schema)
                }
                okText(bodyJson)
            },
            syncTool(
                jsonMapper,
                name = McpTabToolNames.PATCH_MODEL_XML,
                title = "Patch tab conceptual XML (search/replace)",
                description = "Serializes the tab's current conceptual MER to XML, applies old_string→new_string, re-parses, and commits in one undoable step (Cursor-style single edit when replace_all is false). " +
                    "Identify the tab with `resourceUri` from tabs__list_open (`resourceUri`, `resourceUriPng`, or `resourceUriJpeg`). " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$TAB_TOOL_RESOURCE_URI_SCHEMA_PROP,"old_string":{"type":"string","minLength":1},"new_string":{"type":"string"},"replace_all":{"type":"boolean"}},"required":["resourceUri","old_string"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
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
                val bodyJson = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt """{"ok":true,"replaceAll":$replaceAll}"""
                    val snap = b.current()
                    val tab = snap.sessions.getOrNull(idx) ?: return@runOnEdt """{"ok":true,"replaceAll":$replaceAll}"""
                    val rUri = jsonString(modelResourceUriForSession(tab.id))
                    val schema = b.current().schemaForTab(idx) ?: return@runOnEdt """{"ok":true,"resourceUri":$rUri,"replaceAll":$replaceAll}"""
                    val report = analyzeConceptualLayoutQuality(schema, null)
                    McpLayoutQualityJson.mergeLayoutQualityIntoJsonObjectBody(
                        """{"ok":true,"resourceUri":$rUri,"replaceAll":$replaceAll}""",
                        report,
                        schema,
                    )
                }
                okText(bodyJson)
            },
        ) + buildOperationTools(jsonMapper) + buildExportTools(jsonMapper) + buildConceptualSearchTools(jsonMapper) + buildResourceUtilityTools(jsonMapper) + buildProceduralTools(jsonMapper) + buildAttributeTools(jsonMapper)
    }

    private fun buildOperationTools(
        jsonMapper: io.modelcontextprotocol.json.McpJsonMapper,
    ): List<McpServerFeatures.SyncToolSpecification> {
        val tabUri = TAB_TOOL_RESOURCE_URI_SCHEMA_PROP
        val organizeSchema =
            """{"type":"object","properties":{$tabUri,"sides":{"type":"array","items":{"type":"string","enum":["left","top","right","bottom"]},"description":"Optional: reorganize only these attach sides of each affected owner (in left→top→right→bottom order). Omit the property or pass [] to match **Operações → Organizar Atributos** (all sides)."}},"required":["resourceUri"],"additionalProperties":false}"""
        val layoutQualitySchema =
            """{"type":"object","properties":{$tabUri,"elementIds":{"type":"array","items":{"type":"integer"},"description":"Optional: only report overlaps, tight clearances, and line crossings that involve at least one of these canvas element ids. Omit or [] to scan the entire tab diagram."}},"required":["resourceUri"],"additionalProperties":false}"""
        val moveCanvasSchema =
            """{"type":"object","properties":{$tabUri,"elementIds":{"type":"array","items":{"type":"integer","minimum":0},"minItems":1,"description":"Canvas element ids to translate together."},"deltaX":{"type":"integer","description":"Horizontal translation in schema pixels (negative = left)."},"deltaY":{"type":"integer","description":"Vertical translation in schema pixels (negative = up)."},"moveOwnedCanvasAttributes":{"type":"boolean","description":"When true (default), also move every on-canvas attribute whose owner is among the moved elements (closure), preserving relative placement like dragging the owner."}},"required":["resourceUri","elementIds","deltaX","deltaY"],"additionalProperties":false}"""
        return listOf(
            syncTool(
                jsonMapper,
                name = McpOperationToolNames.ORGANIZE_ATTRIBUTES,
                title = "Organize attributes (Operations menu)",
                description = "Runs the same **Operações → Organizar Atributos** layout pass as the desktop editor on the tab's current canvas selection (multi-select aware). " +
                    "Optional `sides` lists which owner edges to reorganize (`left`, `top`, `right`, `bottom`); omit or `[]` reorganizes every side like the menu. " +
                    "One undo step. Pass `resourceUri` from tabs__list_open. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = organizeSchema,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val (sides, sidesErr) = parseOrganizeAttributeSidesFromRequest(req)
                if (sidesErr != null) return@syncTool err(sidesErr)
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplyOrganizeAttributesMenuAtTab(idx, sides)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpOperationToolNames.MOVE_CANVAS_ELEMENTS,
                title = "Move canvas elements (cardinality-aware)",
                description = "Translates canvas elements by `deltaX`/`deltaY` in schema pixels (same space as sidebar position fields). " +
                    "When `moveOwnedCanvasAttributes` is true (default), every on-canvas attribute whose owner is among the moved elements is included automatically, preserving relative placement like dragging the owner on the canvas. " +
                    "Cardinality labels follow the same fixed vs floating rules as canvas drag (floating recomputed from geometry; fixed boxes shift with their link endpoints). " +
                    "Success JSON merges `layoutQuality` (overlaps, tight clearances, approximate link crossings, plus `hasBlockingOverlap` / `agentHint`) scoped to the moved element ids. One undo step. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = moveCanvasSchema,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val elementIds = intListFromMcpAny(req.arguments()["elementIds"])
                if (elementIds == null) return@syncTool err("elementIds_invalid")
                if (elementIds.isEmpty()) return@syncTool err("elementIds_empty")
                val deltaX = intArg(req, "deltaX") ?: return@syncTool err("deltaX_required")
                val deltaY = intArg(req, "deltaY") ?: return@syncTool err("deltaY_required")
                val moveOwned = boolArg(req, "moveOwnedCanvasAttributes") != false
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplyMoveCanvasElementsAtTab(idx, elementIds, deltaX, deltaY, moveOwned)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpOperationToolNames.LAYOUT_QUALITY,
                title = "Layout overlap and crossing diagnostics",
                description = "Returns geometric hints for MCP agents: axis-aligned element box overlaps, uncomfortably small edge-to-edge gaps (see tightClearanceThresholdPx), and approximate connection-segment crossings (straight center-to-center segments — compare with tab PNG/JPEG resources or export subset raster for real routing). " +
                    "The `layoutQuality` object also includes `hasBlockingOverlap`, `affectedElementIds`, and optional `agentHint` (`spacing` / `routing`) for quick automation checks. " +
                    "Pass `resourceUri` from tabs__list_open. Optional elementIds narrows reported issues to pairs/crossings touching those ids; omit or [] scans the whole diagram. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = layoutQualitySchema,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val scope = layoutQualityElementIdScopeFromRequest(req)
                val json = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt """{"ok":false,"error":${jsonString("bindings_unavailable")}}"""
                    val snap = b.current()
                    if (idx !in snap.sessions.indices) {
                        return@runOnEdt """{"ok":false,"error":${jsonString("invalid_tab_index")}}"""
                    }
                    val schema = snap.sessions[idx].schema
                    val report = analyzeConceptualLayoutQuality(schema, scope)
                    McpLayoutQualityJson.layoutQualityInspectToolSuccessJson(
                        modelResourceUriForSession(snap.sessions[idx].id),
                        scope,
                        report,
                        schema,
                    )
                }
                okText(json)
            },
        )
    }

    private fun layoutQualityElementIdScopeFromRequest(req: McpSchema.CallToolRequest): Set<Int>? {
        if (!req.arguments().containsKey("elementIds")) return null
        val raw = req.arguments()["elementIds"] ?: return null
        if (raw !is List<*>) return null
        val ids = intListFromMcpAny(raw)
        return ids.toSet().takeIf { it.isNotEmpty() }
    }

    private fun parseOrganizeAttributeSidesFromRequest(req: McpSchema.CallToolRequest): Pair<Set<ConceptualAttributeAttachPonto>?, String?> {
        if (!req.arguments().containsKey("sides")) return null to null
        val raw = req.arguments()["sides"] ?: return null to null
        if (raw !is List<*>) return null to "sides_must_be_array"
        if (raw.isEmpty()) return null to null
        val out = LinkedHashSet<ConceptualAttributeAttachPonto>()
        for (e in raw) {
            val s = when (e) {
                is String -> e.trim().lowercase()
                else -> e.toString().trim().lowercase()
            }
            when (s) {
                "left" -> out.add(ConceptualAttributeAttachPonto.LEFT)
                "top" -> out.add(ConceptualAttributeAttachPonto.TOP)
                "right" -> out.add(ConceptualAttributeAttachPonto.RIGHT)
                "bottom" -> out.add(ConceptualAttributeAttachPonto.BOTTOM)
                else -> return null to "sides_invalid_token"
            }
        }
        return out to null
    }

    private fun buildExportTools(
        jsonMapper: io.modelcontextprotocol.json.McpJsonMapper,
    ): List<McpServerFeatures.SyncToolSpecification> {
        val tabUri = TAB_TOOL_RESOURCE_URI_SCHEMA_PROP
        val subsetSchema =
            """{"type":"object","properties":{$tabUri,"elementIds":{"type":"array","items":{"type":"integer"},"minItems":1,"description":"Canvas element ids; attribute trees on selected holders are included automatically (same closure as Ctrl+C on those picks). Connections appear when both endpoints are in the expanded id set."},"format":{"type":"string","enum":["png","jpg","jpeg"],"description":"png = transparent (clipboard-style preview); jpg or jpeg = opaque canvas-gray (menu JPEG export style)."}},"required":["resourceUri","elementIds","format"],"additionalProperties":false}"""
        val currentSelectionSchema =
            """{"type":"object","properties":{$tabUri,"imageFormat":{"type":"string","enum":["png","jpg","jpeg"],"description":"Omit for JSON-only. When set, the tool result also includes MCP image content (base64) of the selected subgraph — same expansion as Ctrl+C / """ +
                McpExportToolNames.SUBSET_RASTER +
                """."}},"required":["resourceUri"],"additionalProperties":false}"""
        return listOf(
            syncTool(
                jsonMapper,
                name = McpExportToolNames.SUBSET_RASTER,
                title = "Export subset diagram as PNG or JPEG",
                description = "Renders only the given canvas element ids (plus their attribute trees like Ctrl+C) into a tight-cropped raster — the same subgraph as the clipboard image when those objects are selected. " +
                    "Use format `png` for transparent background (clipboard-style) or `jpg`/`jpeg` for opaque canvas-gray (same as **Exportar em JPEG**). " +
                    "Identify the tab with `resourceUri` from tabs__list_open. " +
                    "On success the tool result includes MCP image content (base64) plus JSON metadata (dimensions, expanded ids). " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = subsetSchema,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val requested = intListFromMcpAny(req.arguments()["elementIds"])
                if (requested.isEmpty()) {
                    return@syncTool err("elementIds_required_or_invalid")
                }
                val format = subsetRasterFormatFromToolArg(req) ?: return@syncTool err("format_must_be_png_jpg_or_jpeg")
                val formatLabel = stringArg(req, "format")?.trim()?.lowercase() ?: "png"
                val (encoded, errCode) = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt null to "bindings_unavailable"
                    if (idx !in b.current().sessions.indices) return@runOnEdt null to "invalid_tab_index"
                    val enc = b.onEncodeConceptualElementSubsetRaster(idx, requested, format)
                        ?: return@runOnEdt null to "subset_raster_encode_failed"
                    enc to null
                }
                if (errCode != null) {
                    return@syncTool err(errCode)
                }
                val enc = encoded!!
                val resourceUriJson = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt "null"
                    val tab = b.current().sessions.getOrNull(idx) ?: return@runOnEdt "null"
                    jsonString(modelResourceUriForSession(tab.id))
                }
                val json =
                    """{"ok":true,"resourceUri":$resourceUriJson,"format":${jsonString(formatLabel)},"mimeType":${jsonString(enc.mimeType)},"widthPx":${enc.widthPx},"heightPx":${enc.heightPx},"elementIdsRequested":${jsonIntArray(requested)},"elementIdsExpanded":${jsonIntArray(enc.expandedElementIds)}}"""
                okJsonPlusMcpImage(json, enc.mimeType, enc.bytes)
            },
            syncTool(
                jsonMapper,
                name = McpExportToolNames.CURRENT_CANVAS_SELECTION,
                title = "Read current canvas selection",
                description = "Returns the user's live canvas selection on a tab as JSON (`selectionKind`, `elementIds`, `cardinalityConnectionIds`, `selectedPickCount`, and `rasterSeedElementIds` — the same element-id closure used for Ctrl+C / subset raster, including attribute trees and cardinality endpoints). " +
                    "Omit `imageFormat` for data-only. When `imageFormat` is `png`, `jpg`, or `jpeg`, the result also includes MCP image content (base64) of that subgraph (transparent PNG vs opaque JPEG styling matches " +
                    McpExportToolNames.SUBSET_RASTER +
                    "). " +
                    "Identify the tab with `resourceUri` from tabs__list_open. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = currentSelectionSchema,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val imageFmtRaw = stringArg(req, "imageFormat")
                val wantRaster = imageFmtRaw != null
                val encFormat = when (imageFmtRaw?.lowercase()?.trim()) {
                    null -> null
                    "png" -> ConceptualSubsetRasterFormat.PngTransparentBackground
                    "jpg", "jpeg" -> ConceptualSubsetRasterFormat.JpegOpaqueCanvasGrayBackground
                    else -> return@syncTool err("imageFormat_must_be_png_jpg_or_jpeg")
                }
                val imageFormatJson = imageFmtRaw?.trim()?.lowercase()?.let { jsonString(it) } ?: "null"
                val (json, imageBytes, imageMime) = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt Triple(null, null, null)
                    val snap = b.current()
                    if (idx !in snap.sessions.indices) return@runOnEdt Triple("invalid_tab_index", null, null)
                    val tab = snap.sessions[idx]
                    val schema = tab.schema
                    val sel = tab.selection
                    val kind = when (sel) {
                        CanvasSelection.None -> "none"
                        is CanvasSelection.Element -> "element"
                        is CanvasSelection.Cardinality -> "cardinality"
                        is CanvasSelection.Multiple -> "multiple"
                    }
                    val (eSet, cSet) = sel.toMultiPickSets()
                    val eList = eSet.sorted()
                    val cList = cSet.sorted()
                    val pickCount = sel.selectedPickCount()
                    val seeds = elementIdsForClipboard(schema, sel).sorted()
                    val resourceUriJson = jsonString(modelResourceUriForSession(tab.id))
                    if (wantRaster && encFormat != null) {
                        if (seeds.isEmpty()) {
                            return@runOnEdt Triple("empty_selection", null, null)
                        }
                        val enc = b.onEncodeConceptualElementSubsetRaster(idx, seeds, encFormat)
                            ?: return@runOnEdt Triple("subset_raster_encode_failed", null, null)
                        val jsonText =
                            """{"ok":true,"resourceUri":$resourceUriJson,"selectionKind":${jsonString(kind)},"selectedPickCount":$pickCount,"elementIds":${jsonIntArray(eList)},"cardinalityConnectionIds":${jsonIntArray(cList)},"rasterSeedElementIds":${jsonIntArray(seeds)},"imageFormat":$imageFormatJson,"hasRaster":true,"mimeType":${jsonString(enc.mimeType)},"widthPx":${enc.widthPx},"heightPx":${enc.heightPx},"elementIdsExpanded":${jsonIntArray(enc.expandedElementIds)}}"""
                        return@runOnEdt Triple(jsonText, enc.bytes, enc.mimeType)
                    }
                    val jsonText =
                        """{"ok":true,"resourceUri":$resourceUriJson,"selectionKind":${jsonString(kind)},"selectedPickCount":$pickCount,"elementIds":${jsonIntArray(eList)},"cardinalityConnectionIds":${jsonIntArray(cList)},"rasterSeedElementIds":${jsonIntArray(seeds)},"hasRaster":false}"""
                    Triple(jsonText, null, null)
                }
                when (json) {
                    null -> return@syncTool err("bindings_unavailable")
                    "invalid_tab_index" -> return@syncTool err("invalid_tab_index")
                    "empty_selection" -> return@syncTool err("empty_selection_cannot_render_raster")
                    "subset_raster_encode_failed" -> return@syncTool err("subset_raster_encode_failed")
                    else -> {
                        if (imageBytes != null && imageMime != null) {
                            return@syncTool okJsonPlusMcpImage(json, imageMime, imageBytes)
                        }
                        return@syncTool okText(json)
                    }
                }
            },
        )
    }

    private fun subsetRasterFormatFromToolArg(req: McpSchema.CallToolRequest): ConceptualSubsetRasterFormat? {
        val f = stringArg(req, "format")?.lowercase()?.trim() ?: return null
        return when (f) {
            "png" -> ConceptualSubsetRasterFormat.PngTransparentBackground
            "jpg", "jpeg" -> ConceptualSubsetRasterFormat.JpegOpaqueCanvasGrayBackground
            else -> null
        }
    }

    private fun jsonIntArray(ids: List<Int>): String = ids.joinToString(separator = ",", prefix = "[", postfix = "]")

    private fun buildConceptualSearchTools(
        jsonMapper: io.modelcontextprotocol.json.McpJsonMapper,
    ): List<McpServerFeatures.SyncToolSpecification> {
        val tabUri = TAB_TOOL_RESOURCE_URI_SCHEMA_PROP
        val findSchema = """{"type":"object","properties":{$tabUri,"query":{"type":"string","description":"Substring; omit or use empty string to list all items in the selected include* categories (400-hit cap)."},"includeEntities":{"type":"boolean"},"includeRelationships":{"type":"boolean"},"includeAssociativeEntities":{"type":"boolean"},"includeSpecializations":{"type":"boolean"},"includeCanvasAttributes":{"type":"boolean"},"includeHiddenAttributes":{"type":"boolean"},"includeCardinalityLabels":{"type":"boolean"},"observationBox":{"type":"boolean","description":"Include Annotation (observation box) elements"},"searchDictionary":{"type":"boolean"},"searchObservations":{"type":"boolean"}},"required":["resourceUri"],"additionalProperties":false}"""
        val applySchema = """{"type":"object","properties":{$tabUri,"hit":{"type":"object","description":"Echo one hit object from search__find (kind + ids + optional geometry).","additionalProperties":true}},"required":["resourceUri","hit"],"additionalProperties":false}"""
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
                    "Pass `resourceUri` from tabs__list_open (`.xml`, `.png`, or `.jpg` URI suffix resolves to the same tab). " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = findSchema,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
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
                    "Pass `resourceUri` from tabs__list_open (`resourceUri`, `resourceUriPng`, or `resourceUriJpeg` for the same tab); " +
                    "then pass the `hit` object from a prior search__find response. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = applySchema,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
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
                val resourceUriJson = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt null
                    val tab = b.current().sessions.getOrNull(idx) ?: return@runOnEdt null
                    jsonString(modelResourceUriForSession(tab.id))
                } ?: return@syncTool err("invalid_tab_index")
                okText("""{"ok":true,"resourceUri":$resourceUriJson}""")
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
            """"uri":{"type":"string","minLength":1,"description":"Registered MCP resource URI (live tab XML, DTD, or classpath example — see server instructions). Live tab .png/.jpg previews are binary; use resources/read, not these text tools."}"""
        return listOf(
            syncTool(
                jsonMapper,
                name = McpResourceUtilityToolNames.READ_FULL,
                title = "Read full MCP resource text",
                description = "Returns the entire UTF-16 text body for a registered resource URI when that URI is plain text (same content as resources/read for tab XML, DTD, and examples). " +
                    "Does not apply to live tab `.png`/`.jpg` resources — use MCP resources/read for those. " +
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

    private data class ParsedLinkObjectsToolCall(
        val tabIdx: Int,
        val endA: ConceptualLinkPick,
        val endB: ConceptualLinkPick,
        val relO: ConceptualProceduralToolOverrides?,
        val connList: List<ConceptualLinkConnectionOverridePatch>?,
        val dryRun: Boolean,
    )

    private fun parseLinkObjectsToolCall(req: McpSchema.CallToolRequest): Pair<String?, ParsedLinkObjectsToolCall?> {
        val idx = tabIndexFromResourceUriArg(req) ?: return "resource_uri_required" to null
        val args = req.arguments()
        val endA = McpLinkObjectsToolArgs.parseEndPick(args["endA"]) ?: return "endA_invalid" to null
        val endB = McpLinkObjectsToolArgs.parseEndPick(args["endB"]) ?: return "endB_invalid" to null
        val relO = McpLinkObjectsToolArgs.parseRelationshipOverrides(args["relationshipOverrides"])
        val (connErr, connList) = McpLinkObjectsToolArgs.parseConnectionOverrides(
            args["connectionOverrides"],
            args["connection"],
        )
        if (connErr != null) {
            return connErr to null
        }
        val dryRun = boolArg(req, "dryRun") == true
        return null to ParsedLinkObjectsToolCall(idx, endA, endB, relO, connList, dryRun)
    }

    private fun buildProceduralTools(
        jsonMapper: io.modelcontextprotocol.json.McpJsonMapper,
    ): List<McpServerFeatures.SyncToolSpecification> {
        val tabUri = TAB_TOOL_RESOURCE_URI_SCHEMA_PROP
        val xy = """"x":{"type":"integer"},"y":{"type":"integer"}"""
        val textFields = """"name":{"type":"string"},"observations":{"type":"string"},"dictionary":{"type":"string"}"""
        val allowDuplicateCanvasLabelsProp =
            """"allowDuplicateCanvasLabels":{"type":"boolean","description":"When true, explicit name overrides may match an existing entity-like or relationship-style label (default false returns name_conflict / relationship_name_conflict)."}"""
        val labelStyle =
            """"labelColorArgb":{"type":"integer"},"labelBold":{"type":"boolean"},"labelItalic":{"type":"boolean"}"""
        val relArrow = """"arrowDirectionCode":{"type":"integer","minimum":0,"maximum":8},"showName":{"type":"boolean"}"""
        val annotationPlacementProps =
            """"annotationColorArgb":{"type":"integer"},"annotationTypeCode":{"type":"integer","minimum":0,"maximum":2},"alignmentCode":{"type":"integer","minimum":0,"maximum":2},"autoSize":{"type":"boolean"},"width":{"type":"integer","minimum":5},"height":{"type":"integer","minimum":5}"""
        val placeObservationSchema =
            """{"type":"object","properties":{$tabUri,$xy,$textFields,$labelStyle,$annotationPlacementProps},"additionalProperties":false}"""
        val assocInner =
            """"relationshipName":{"type":"string"},"relationshipObservations":{"type":"string"},"relationshipDictionary":{"type":"string"}"""
        val baseEntityIdProp = """"baseEntityId":{"type":"integer","minimum":0}"""
        val specializationBasicSchema =
            """{"type":"object","properties":{$tabUri,$baseEntityIdProp},"required":["baseEntityId"],"additionalProperties":false}"""
        val specializationChildSchema =
            """{"type":"object","properties":{$tabUri,$baseEntityIdProp,"exclusive":{"type":"boolean"}},"required":["baseEntityId","exclusive"],"additionalProperties":false}"""
        val linkEndPickSchema =
            """{"type":"object","properties":{"elementId":{"type":"integer","minimum":0},"associativeOuterEntitySide":{"type":"boolean","description":"True when the pick is the outer entity rectangle of an associative entity; false targets the inner diamond (relationship / miolo). Tool ${McpProceduralToolsToolNames.LINK_EXISTING_ENDPOINTS} may rewrite entity+outer picks to entity+inner when both ends would otherwise be entity-side."}},"required":["elementId"],"additionalProperties":false}"""
        val relationshipOverridesSchema =
            """{"type":"object","properties":{$textFields,$labelStyle,$relArrow,$allowDuplicateCanvasLabelsProp},"additionalProperties":false}"""
        val connectionPatchSchema =
            """{"type":"object","properties":{"cardinalityCode":{"type":"integer","minimum":1,"maximum":4},"showCardinality":{"type":"boolean"},"orientationCode":{"type":"integer","minimum":0,"maximum":3},"cardinalityFixed":{"type":"boolean"},"isWeak":{"type":"boolean"},"cardinalityRole":{"type":"string"},"cardinalityObservations":{"type":"string"},"cardinalityDictionary":{"type":"string"},"cardinalityAutoSize":{"type":"boolean"}},"additionalProperties":false}"""
        val attachSideForAutoSelf =
            """"attachSide":{"type":"string","enum":["left","top","right","bottom","1","2","3","4"],"description":"Which side of the entity receives the self-relationship diamond (same idea as apply_attribute attachSide)."}"""
        val entityElementIdProp = """"entityElementId":{"type":"integer","minimum":0}"""
        val associativeOuterForAutoSelf =
            """"associativeOuterEntitySide":{"type":"boolean","description":"True when entityElementId is an associative entity and the pick is its outer rectangle."}"""
        val autoSelfRelationshipSchema =
            """{"type":"object","properties":{$tabUri,$entityElementIdProp,$associativeOuterForAutoSelf,$attachSideForAutoSelf,"relationshipOverrides":$relationshipOverridesSchema,"connection":$connectionPatchSchema,"connectionOverrides":{"type":"array","items":$connectionPatchSchema}},"required":["entityElementId"],"additionalProperties":false}"""
        val dryRunForLinkObjects =
            """"dryRun":{"type":"boolean","description":"Optional (default false). When true, validate and return preview JSON (including projected layoutQuality) without committing the tab — no undo entry."}"""
        val linkObjectsSchema =
            """{"type":"object","properties":{$tabUri,"endA":$linkEndPickSchema,"endB":$linkEndPickSchema,"relationshipOverrides":$relationshipOverridesSchema,"connection":$connectionPatchSchema,"connectionOverrides":{"type":"array","items":$connectionPatchSchema},$dryRunForLinkObjects},"required":["endA","endB"],"additionalProperties":false}"""
        return listOf(
            syncTool(
                jsonMapper,
                name = McpProceduralToolsToolNames.PLACE_ENTITY,
                title = "Place conceptual entity (procedural)",
                description = "Inserts a plain entity at (x,y) using the same allocation rules as the Entity canvas tool, " +
                    "then optionally overrides name, notes, dictionary, weak flag, and label style. " +
                    "Optional `allowDuplicateCanvasLabels` true allows an explicit `name` to match an existing entity-like label (default false returns `name_conflict`). " +
                    "Does **not** switch the user's active ribbon/canvas tool. " +
                    "Returns the placed element as JSON (id, geometry, names, style). " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$tabUri,$xy,$textFields,$allowDuplicateCanvasLabelsProp,"isWeak":{"type":"boolean"},$labelStyle},"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
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
                    "Optional `allowDuplicateCanvasLabels` true allows an explicit `name` to match an existing relationship-style label (default false returns `name_conflict`). " +
                    "Does **not** switch the user's active ribbon/canvas tool. " +
                    "Returns the placed element as JSON. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$tabUri,$xy,$textFields,$allowDuplicateCanvasLabelsProp,$relArrow},"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
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
                    "The `relationshipName` argument (when overridden) is the **inner** diamond label (Pascal \"realiza\" semantics); " +
                    "external links from other entities may still route through a **separate** intermediate relationship when the editor/domain requires it — same as human **Ligar Objetos** behaviour. " +
                    "After placement, wire the diagram with ${McpProceduralToolsToolNames.LINK_EXISTING_ENDPOINTS} when you only need legs between **existing** endpoints (inner/miolo vs outer is documented on that tool; it refuses plain entity–entity auto-diamond creation), " +
                    "or ${McpProceduralToolsToolNames.LINK_OBJECTS} when you need the full editor behaviour including **new** midpoint diamonds (entity–entity, entity–associative outer bridge, etc.). " +
                    "Optional `allowDuplicateCanvasLabels` true allows explicit outer `name` and/or inner `relationshipName` to match existing canvas labels (default false returns `name_conflict` / `relationship_name_conflict`). " +
                    "Does **not** switch the user's active ribbon/canvas tool. " +
                    "Returns the placed element as JSON. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = """{"type":"object","properties":{$tabUri,$xy,$textFields,$allowDuplicateCanvasLabelsProp,$assocInner,$labelStyle,"arrowDirectionCode":{"type":"integer","minimum":0,"maximum":8}},"additionalProperties":false}""",
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
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
                name = McpProceduralToolsToolNames.PLACE_OBSERVATION,
                title = "Place observation box (Observação)",
                description = "Inserts a canvas observation / annotation box at (x,y) with the same defaults as the ribbon **Observação** tool " +
                    "(caption, geometry, hint/box style). " +
                    "Optional fields: `name` (caption), `observations`, `dictionary`, `labelColorArgb` / `labelBold` / `labelItalic` (text style), " +
                    "`annotationColorArgb` (Windows COLORREF background), `annotationTypeCode` (0 plain, 1 hint, 2 box), `alignmentCode` (0 left, 1 center, 2 right), " +
                    "`autoSize`, and `width` / `height` (minimum 5 px each) to override the default 150×22 box. " +
                    "Does **not** switch the user's active ribbon/canvas tool. " +
                    "Returns the placed element as JSON. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = placeObservationSchema,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val x = intArg(req, "x") ?: 64
                val y = intArg(req, "y") ?: 64
                val overrides = proceduralOverridesForObservation(req)
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onPlaceProceduralConceptualToolAtTab(idx, ConceptualProceduralToolKind.ANNOTATION, x, y, overrides)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpProceduralToolsToolNames.LINK_OBJECTS,
                title = "Link conceptual objects (full editor / Ligar Objetos)",
                description = "Full **Ligar Objetos** semantics in one call: same domain rules as the canvas link tool, including **automatic** creation of a new relationship diamond when linking **two plain entities** or when the editor needs an **entity ↔ associative-outer** bridge diamond. " +
                    "Provide two diagram endpoints (`endA` / `endB`) as element picks. " +
                    "For **inner associative / \"miolo\"** participation (entity ↔ inner diamond of an associative entity), set `\"associativeOuterEntitySide\": false` on that pick (`true` only for the outer entity rectangle). " +
                    "When you only want to connect **already placed** elements without creating a new midpoint relationship from a plain entity–entity pair, prefer ${McpProceduralToolsToolNames.LINK_EXISTING_ENDPOINTS} instead (same JSON schema; it rejects those auto-create paths and can rewrite entity+associative-outer to entity+inner). " +
                    "Optional `relationshipOverrides` adjusts any **new** relationship or self-relationship the domain creates (name, notes, arrow, showName, `allowDuplicateCanvasLabels` when you intentionally reuse a canvas label); they do **not** retarget legs onto the inner associative diamond when the editor materialized a separate bridge relationship. " +
                    "Optional `connection` (single leg) or `connectionOverrides` (array) adjusts new connection cardinalities and line metadata; " +
                    "when two new legs are created (entity–entity case), send two patches in order **[endA leg, endB leg]** (ascending new connection id matches this order). " +
                    "Specialization↔entity accepts a single `connection` patch; only **plain** entities may connect to a specialization triangle (not associative outers). " +
                    "The editor may promote an optional specialization to restricted when a third subtype link is added — see domain behaviour. " +
                    "Returns `newConnections`, `newRelationship` / `newSelfRelationship` JSON when those elements were created, plus `linkPattern` (structural hint for agents). " +
                    "Optional `dryRun` true validates and returns the same JSON shape plus `wouldCreate` **without** mutating the tab (no undo step); merged `layoutQuality` reflects the **would-be** geometry. " +
                    "Does **not** switch the user's active canvas tool. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = linkObjectsSchema,
            ) { _, req ->
                val (parseErr, parsed) = parseLinkObjectsToolCall(req)
                if (parseErr != null) {
                    return@syncTool err(parseErr)
                }
                val p = parsed!!
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onLinkConceptualObjectsAtTab(p.tabIdx, p.endA, p.endB, p.relO, p.connList, null, p.dryRun, false)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpProceduralToolsToolNames.LINK_EXISTING_ENDPOINTS,
                title = "Link conceptual endpoints already on canvas",
                description = "Same JSON schema as ${McpProceduralToolsToolNames.LINK_OBJECTS}, but for **wiring existing** diagram pieces: it **refuses** plain **entity ↔ entity** picks that would auto-create a new relationship diamond (error code suggests ${McpProceduralToolsToolNames.LINK_OBJECTS} or placing a relationship first) and **refuses** associative-**outer** ↔ associative-**outer** pairs that need the full bridge tool. " +
                    "When one end is a plain entity-side pick and the other is an associative **outer** rectangle, the server **rewrites** that associative pick to the **inner** diamond (`associativeOuterEntitySide: false`) so the link targets the associative relationship (miolo), matching the usual agent intent. " +
                    "To keep true **outer** participation (bridge / extra diamond behaviour), call ${McpProceduralToolsToolNames.LINK_OBJECTS} instead and inspect `linkPattern` (e.g. `entity_associative_outer_bridge`). " +
                    "All other supported pairs (entity↔relationship, specialization↔plain entity, inner associative legs, etc.) behave like the editor. " +
                    "Optional `relationshipOverrides`, `connection`, `connectionOverrides`, and `dryRun` match ${McpProceduralToolsToolNames.LINK_OBJECTS}. " +
                    "Does **not** switch the user's active canvas tool. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = linkObjectsSchema,
            ) { _, req ->
                val (parseErr, parsed) = parseLinkObjectsToolCall(req)
                if (parseErr != null) {
                    return@syncTool err(parseErr)
                }
                val p = parsed!!
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onLinkConceptualObjectsAtTab(p.tabIdx, p.endA, p.endB, p.relO, p.connList, null, p.dryRun, true)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpProceduralToolsToolNames.AUTO_SELF_RELATIONSHIP,
                title = "Create entity auto-relationship (Auto Relacionar)",
                description = "Creates a self-relationship on a plain entity or the outer rectangle of an associative entity, " +
                    "same rules as the **Auto Relacionar** canvas tool. " +
                    "Optional `attachSide` (`left`, `top`, `right`, `bottom` or codes 1–4) chooses which side of the owner box receives the diamond, matching attribute-tool edge semantics; omit for the legacy right-side placement. " +
                    "Optional `relationshipOverrides` and `connection` / `connectionOverrides` (exactly **two** patches, ascending new connection id) adjust the new self-relationship and both cardinality legs at creation time — same fields as ${McpProceduralToolsToolNames.LINK_OBJECTS} / ${McpProceduralToolsToolNames.LINK_EXISTING_ENDPOINTS}. " +
                    "Returns `newConnections` and `newSelfRelationship` JSON. Does **not** switch the user's active canvas tool. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = autoSelfRelationshipSchema,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val args = req.arguments()
                val entityId = intArg(req, "entityElementId") ?: return@syncTool err("entityElementId_required")
                val outer = boolArg(req, "associativeOuterEntitySide") == true
                val pick = ConceptualLinkPick(elementId = entityId, isAssociativeOuterEntitySide = outer)
                val sideOpt = McpAttributeToolArgs.parseAttachSide(args["attachSide"])
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
                    val snap = b.current()
                    if (idx !in snap.sessions.indices) {
                        return@runOnEdt McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val ownerEl = snap.sessions[idx].schema.elements[entityId]
                        ?: return@runOnEdt McpProceduralToolApplyOutcome.err("element_not_found")
                    val click = sideOpt?.let { syntheticClickOnOwnerSideCenter(ownerEl.position, it) }
                    b.onLinkConceptualObjectsAtTab(idx, pick, pick, relO, connList, click, false, false)
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
        val tabUri = TAB_TOOL_RESOURCE_URI_SCHEMA_PROP
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
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
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
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
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
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val holderId = intArg(req, "holderElementId") ?: return@syncTool err("holderElementId_required")
                val rawRoots = req.arguments()["roots"] ?: return@syncTool err("roots_required")
                val roots = McpAttributeToolArgs.parseHiddenRoots(rawRoots) ?: return@syncTool err("roots_invalid")
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplyHiddenAttributeForestAtTab(idx, holderId, roots)
                }
                proceduralToolOutcomeToResult(outcome)
            },
        ) + buildEditTools(jsonMapper)
    }

    private fun buildEditTools(
        jsonMapper: io.modelcontextprotocol.json.McpJsonMapper,
    ): List<McpServerFeatures.SyncToolSpecification> {
        return listOf(
            syncTool(
                jsonMapper,
                name = McpEditToolNames.MODEL,
                title = "Edit conceptual model metadata",
                description = "Updates model name, author, and/or observations for the tab (same fields as the inspector when nothing is selected on the canvas). " +
                    "Identify the tab with `resourceUri` from tabs__list_open (`resourceUri`, `resourceUriPng`, or `resourceUriJpeg`). " +
                    "Each successful call creates one undo step. Unknown patch keys or empty patches return a descriptive error.",
                schema = McpEditToolJsonSchemas.EDIT_MODEL,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val patchRaw = req.arguments()["patch"] ?: return@syncTool err("patch_required")
                val patch = parseStringKeyedMap(patchRaw) ?: return@syncTool err("patch_invalid")
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplyEditConceptualModelAtTab(idx, patch)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpEditToolNames.CANVAS_ELEMENT,
                title = "Edit canvas element properties",
                description = "Updates properties of a diagram element (entity, relationship, associative entity, attribute, specialization, self-relationship, annotation) " +
                    "using the same allowlists as the inspector sidebar. Keys that do not apply to the element kind return field_not_applicable_to_element_kind. " +
                    "Attribute auto-size and composite bar layout follow the same post-processing as the inspector. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = McpEditToolJsonSchemas.EDIT_CANVAS_ELEMENT,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val elementId = intArg(req, "elementId") ?: return@syncTool err("elementId_required")
                val patchRaw = req.arguments()["patch"] ?: return@syncTool err("patch_required")
                val patch = parseStringKeyedMap(patchRaw) ?: return@syncTool err("patch_invalid")
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplyEditCanvasElementAtTab(idx, elementId, patch)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpEditToolNames.CONNECTION,
                title = "Edit connection / cardinality properties",
                description = "Updates a connection row (cardinality label, orientation, weak participation, associative outer flags, etc.) like the inspector when a cardinality link is selected. " +
                    "Toggling fixed position, auto-size, or showing cardinality may materialize label bounds using the same layout rules as the UI. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = McpEditToolJsonSchemas.EDIT_CONNECTION,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val connectionId = intArg(req, "connectionId") ?: return@syncTool err("connectionId_required")
                val patchRaw = req.arguments()["patch"] ?: return@syncTool err("patch_required")
                val patch = parseStringKeyedMap(patchRaw) ?: return@syncTool err("patch_invalid")
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplyEditConnectionAtTab(idx, connectionId, patch)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpEditToolNames.HIDDEN_ATTRIBUTE,
                title = "Edit hidden attribute node",
                description = "Patches one hidden-attribute node identified by holderElementId and a path of merged child indices (children first, then nested ocultos), matching the inspector tree. " +
                    "The forest must keep unique non-blank names after the edit. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = McpEditToolJsonSchemas.EDIT_HIDDEN_ATTRIBUTE,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val holderId = intArg(req, "holderElementId") ?: return@syncTool err("holderElementId_required")
                val path = intListArg(req.arguments()["path"]) ?: return@syncTool err("path_invalid")
                val patchRaw = req.arguments()["patch"] ?: return@syncTool err("patch_required")
                val patch = parseStringKeyedMap(patchRaw) ?: return@syncTool err("patch_invalid")
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplyEditHiddenAttributeAtTab(idx, holderId, path, patch)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpEditToolNames.CANVAS_SELECTION_RECTANGLE,
                title = "Edit canvas selection by rectangle",
                description = "Marquee-selects in schema coordinates (x0,y0,x1,y1) using the same geometry as the canvas rectangle tool. " +
                    "mergeMode add unions band picks with the current selection; replace keeps only picks in the band; subtract removes band picks from the selection. " +
                    "dryRun returns objectsInBand, selectionUiAfter (projection), and selectionSymmetricDelta without mutating the UI (requestWindowFocus is ignored). " +
                    "When applying changes, optional requestWindowFocus raises the editor — use only to show the user something (see MCP server instructions). " +
                    "A Portuguese snackbar appears when the selection changes and/or the window is focused. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = McpEditToolJsonSchemas.EDIT_CANVAS_SELECTION_RECTANGLE,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val x0 = intArg(req, "x0") ?: return@syncTool err("x0_required")
                val y0 = intArg(req, "y0") ?: return@syncTool err("y0_required")
                val x1 = intArg(req, "x1") ?: return@syncTool err("x1_required")
                val y1 = intArg(req, "y1") ?: return@syncTool err("y1_required")
                val mode = canvasSelectionRectangleMergeModeArg(req) ?: return@syncTool err("mergeMode_invalid")
                val dryRun = optionalBoolArg(req, "dryRun") == true
                val wantFocus = boolArg(req, "requestWindowFocus") == true
                val outcome = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt McpProceduralToolApplyOutcome.err("bindings_unavailable")
                    b.onApplyCanvasSelectionRectangleAtTab(idx, x0, y0, x1, y1, mode, dryRun, wantFocus)
                }
                proceduralToolOutcomeToResult(outcome)
            },
            syncTool(
                jsonMapper,
                name = McpEditToolNames.CANVAS_SELECTION,
                title = "Set canvas selection",
                description = "Replaces the diagram multi-selection from `elementIds` and/or `cardinalityConnectionIds` (validated against the tab schema), like rectangle or Shift picks. " +
                    "Omit each array or pass [] to clear that side. Optional `requestWindowFocus` raises the editor window — use only when you want the user to notice the app (see MCP server instructions). " +
                    "When selection or focus changes, the user sees a short snackbar that an MCP action did it. " +
                    McpServerInstructions.MER_XML_REFERENCE_SEE_INSTRUCTIONS,
                schema = McpEditToolJsonSchemas.EDIT_CANVAS_SELECTION,
            ) { _, req ->
                val idx = tabIndexFromResourceUriArg(req) ?: return@syncTool err("resource_uri_required")
                val args = req.arguments()
                val rawE = args["elementIds"]
                val elementIds = when (rawE) {
                    null -> emptyList()
                    is List<*> -> intListArg(rawE) ?: return@syncTool err("elementIds_invalid")
                    else -> return@syncTool err("elementIds_must_be_array")
                }
                val rawC = args["cardinalityConnectionIds"]
                val cardinalityIds = when (rawC) {
                    null -> emptyList()
                    is List<*> -> intListArg(rawC) ?: return@syncTool err("cardinalityConnectionIds_invalid")
                    else -> return@syncTool err("cardinalityConnectionIds_must_be_array")
                }
                val wantFocus = boolArg(req, "requestWindowFocus") == true
                val errMsg = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt "bindings_unavailable"
                    val snap = b.current()
                    if (idx !in snap.sessions.indices) return@runOnEdt "invalid_tab_index"
                    val schema = snap.sessions[idx].schema
                    val (sel, verr) = tryBuildCanvasSelectionFromMcpPickLists(schema, elementIds, cardinalityIds)
                    if (verr != null) return@runOnEdt verr
                    b.onSetCanvasSelectionAtTab(idx, sel!!)
                    if (wantFocus) {
                        b.onRequestAppWindowFocus()
                    }
                    b.onShowMcpAgentUserNotice(
                        McpAgentUserNotice(
                            selectionChanged = true,
                            windowFocused = wantFocus,
                        ),
                    )
                    null
                }
                if (errMsg != null) return@syncTool err(errMsg)
                val wfJson = if (wantFocus) "true" else "false"
                val resourceUriJson = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt null
                    val tab = b.current().sessions.getOrNull(idx) ?: return@runOnEdt null
                    jsonString(modelResourceUriForSession(tab.id))
                } ?: return@syncTool err("invalid_tab_index")
                okText("""{"ok":true,"resourceUri":$resourceUriJson,"requestWindowFocus":$wfJson}""")
            },
        )
    }

    private fun parseStringKeyedMap(raw: Any?): Map<String, Any?>? {
        if (raw !is Map<*, *>) return null
        val out = LinkedHashMap<String, Any?>()
        for ((k, v) in raw) {
            if (k !is String) return null
            out[k] = v
        }
        return out
    }

    private fun intListArg(raw: Any?): List<Int>? {
        if (raw !is List<*>) return null
        val out = ArrayList<Int>(raw.size)
        for (e in raw) {
            when (e) {
                is Int -> out.add(e)
                is Long -> out.add(e.toInt())
                is Double -> out.add(e.toInt())
                is Number -> out.add(e.toInt())
                else -> return null
            }
        }
        return out
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
        val idx = tabIndexFromResourceUriArg(req) ?: return err("resource_uri_required")
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
            allowDuplicateCanvasLabels = optionalBoolArg(req, "allowDuplicateCanvasLabels"),
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
            allowDuplicateCanvasLabels = optionalBoolArg(req, "allowDuplicateCanvasLabels"),
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
            allowDuplicateCanvasLabels = optionalBoolArg(req, "allowDuplicateCanvasLabels"),
        )

    private data class LayoutQualityMergeBundle(
        val report: ConceptualLayoutQualityReport,
        val schema: ConceptualSchema?,
    )

    private fun layoutQualityMergeBundle(outcome: McpProceduralToolApplyOutcome): LayoutQualityMergeBundle? {
        val scan = outcome.layoutQualityScan ?: return null
        scan.reportOverride?.let { ro ->
            return LayoutQualityMergeBundle(ro, scan.schemaForLayoutQualityJson)
        }
        return runOnEdt {
            val b = bindingsRef.get() ?: return@runOnEdt null
            val schema = b.current().schemaForTab(scan.tabIndex) ?: return@runOnEdt null
            LayoutQualityMergeBundle(
                analyzeConceptualLayoutQuality(schema, scan.touchedElementIds),
                schema,
            )
        }
    }

    private fun proceduralToolOutcomeToResult(outcome: McpProceduralToolApplyOutcome): McpSchema.CallToolResult {
        val errMsg = outcome.error
        if (errMsg != null) {
            return err(errMsg)
        }
        val ej = outcome.elementJson ?: return err("internal_no_element_json")
        val lqBundle = layoutQualityMergeBundle(outcome)
        if (outcome.isFullResponseJson) {
            val text = if (lqBundle != null) {
                McpLayoutQualityJson.mergeLayoutQualityIntoJsonObjectBody(ej, lqBundle.report, lqBundle.schema)
            } else {
                ej
            }
            return okText(text)
        }
        val resourceUriJson = runOnEdt {
            val b = bindingsRef.get() ?: return@runOnEdt null
            val tab = b.current().sessions.getOrNull(outcome.tabIndex) ?: return@runOnEdt null
            jsonString(modelResourceUriForSession(tab.id))
        } ?: "null"
        val text = if (lqBundle != null) {
            val lq = McpLayoutQualityJson.layoutQualityObjectJson(lqBundle.report, lqBundle.schema)
            """{"ok":true,"resourceUri":$resourceUriJson,"element":$ej,"layoutQuality":$lq}"""
        } else {
            """{"ok":true,"resourceUri":$resourceUriJson,"element":$ej}"""
        }
        return okText(text)
    }

    private fun proceduralOverridesForObservation(req: McpSchema.CallToolRequest): ConceptualProceduralToolOverrides =
        ConceptualProceduralToolOverrides(
            name = optionalKeyedTrimmedString(req, "name"),
            observations = optionalKeyedRawString(req, "observations"),
            dictionary = optionalKeyedRawString(req, "dictionary"),
            labelColorArgb = optionalIntArg(req, "labelColorArgb"),
            labelBold = optionalBoolArg(req, "labelBold"),
            labelItalic = optionalBoolArg(req, "labelItalic"),
            annotationColorArgb = optionalIntArg(req, "annotationColorArgb"),
            annotationTypeCode = optionalIntArg(req, "annotationTypeCode"),
            alignmentCode = optionalIntArg(req, "alignmentCode"),
            annotationAutoSize = optionalBoolArg(req, "autoSize"),
            annotationWidth = optionalIntArg(req, "width"),
            annotationHeight = optionalIntArg(req, "height"),
        )

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

    private fun canvasSelectionRectangleMergeModeArg(req: McpSchema.CallToolRequest): CanvasSelectionRectangleMergeMode? {
        val raw = stringArg(req, "mergeMode") ?: return null
        return when (raw.lowercase()) {
            "add" -> CanvasSelectionRectangleMergeMode.ADD
            "replace" -> CanvasSelectionRectangleMergeMode.REPLACE
            "subtract" -> CanvasSelectionRectangleMergeMode.SUBTRACT
            else -> null
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
                """{"ok":true,"createdResourceUri":${jsonString(change.createdResourceUri)},"selectedResourceUri":${jsonString(change.selectedResourceUri)},"createdResourceUriPng":${jsonString(change.createdResourceUriPng)},"createdResourceUriJpeg":${jsonString(change.createdResourceUriJpeg)},"selectedResourceUriPng":${jsonString(change.selectedResourceUriPng)},"selectedResourceUriJpeg":${jsonString(change.selectedResourceUriJpeg)},"createdSessionId":${change.createdSessionId},"selectedSessionId":${change.selectedSessionId}""",
            )
            if (extraFieldsJson.isNotEmpty()) {
                append(',')
                append(extraFieldsJson)
            }
            append('}')
        }
    }

    private fun okJsonPlusMcpImage(json: String, mimeType: String, imageBytes: ByteArray): McpSchema.CallToolResult {
        val b64 = Base64.getEncoder().encodeToString(imageBytes)
        val annotations = McpSchema.Annotations(emptyList<McpSchema.Role>(), null)
        val image = McpSchema.ImageContent(annotations, b64, mimeType)
        return McpSchema.CallToolResult.builder()
            .addTextContent(json)
            .addContent(image)
            .build()
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
