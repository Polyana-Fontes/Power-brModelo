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
import java.util.concurrent.atomic.AtomicReference
import java.util.function.BiFunction
import javax.swing.SwingUtilities

private const val MCP_ENDPOINT = "/mcp"

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

internal actual class BrModeloMcpRuntime {

    private val bindingsRef = AtomicReference<BrModeloMcpUiBindings?>(null)
    private val settingsOpener = AtomicReference<(() -> Unit)?>(null)

    private var jetty: Server? = null
    private var mcp: McpSyncServer? = null

    actual fun setSettingsDialogOpener(opener: () -> Unit) {
        settingsOpener.set(opener)
    }

    actual fun updateBindings(bindings: BrModeloMcpUiBindings?) {
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
        val server = mcp ?: return
        try {
            server.notifyResourcesListChanged()
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
        val (bindHostRaw, portRaw, allowLanHosts) = BrModeloMcpSettingsStore.load()
        val bindHost = bindHostRaw.trim().ifBlank { "127.0.0.1" }
        val port = portRaw.coerceIn(1, 65535)

        val jsonMapper = McpJsonDefaults.getMapper()
        val security = buildSecurityValidator(bindHost, allowLanHosts)
        val transport = HttpServletStreamableServerTransportProvider.builder()
            .jsonMapper(jsonMapper)
            .mcpEndpoint(MCP_ENDPOINT)
            .securityValidator(security)
            .build()

        val tools = buildTools(jsonMapper)
        val resourceTemplate = McpServerFeatures.SyncResourceTemplateSpecification(
            McpSchema.ResourceTemplate.builder()
                .uriTemplate("brmodelo://model/{tabIndex}")
                .name("conceptual_model_xml")
                .title("Conceptual model (in-memory XML)")
                .description(
                    "Serializes the in-memory conceptual schema for the given tab index exactly like " +
                        "a saved brModelo XML export, without writing to disk.",
                )
                .mimeType("application/xml")
                .build(),
            BiFunction { _: McpSyncServerExchange, req: McpSchema.ReadResourceRequest ->
                readModelResource(req.uri())
            },
        )

        val server = try {
            McpServer.sync(transport)
                .serverInfo("Power-brModelo", BuildInfo.VERSION)
                .instructions(
                    "This MCP server controls the Power-brModelo desktop editor. " +
                        "Use tools to manage editor tabs; read resources brmodelo://model/{tabIndex} for live XML.",
                )
                .tools(tools)
                .resourceTemplates(resourceTemplate)
                .build()
        } catch (t: Throwable) {
            bindingsRef.get()?.onNotifyUser("MCP: falha ao criar servidor: ${t.message}")
            return false
        }

        mcp = server

        return try {
            val jettyServer = Server(InetSocketAddress(bindHost, port))
            val context = ServletContextHandler(ServletContextHandler.NO_SESSIONS).apply {
                contextPath = "/"
                addServlet(ServletHolder(transport), "$MCP_ENDPOINT/*")
            }
            jettyServer.handler = context
            jettyServer.start()
            jetty = jettyServer
            bindingsRef.get()?.onNotifyUser("MCP: servidor em http://$bindHost:$port$MCP_ENDPOINT")
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
        hosts.add("127.0.0.1")
        hosts.add("localhost")
        hosts.add("[::1]")
        if (bindHost != "0.0.0.0" && bindHost.isNotBlank()) {
            hosts.add(bindHost)
        }
        if (allowLanHosts) {
            hosts.add("0.0.0.0")
        }
        val b = DefaultServerTransportSecurityValidator.builder()
        hosts.forEach { b.allowedHost(it) }
        return b.build()
    }

    private fun readModelResource(uri: String): McpSchema.ReadResourceResult {
        val index = parseBrModeloModelResourceTabIndex(uri)
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

    private fun buildTools(jsonMapper: io.modelcontextprotocol.json.McpJsonMapper): List<McpServerFeatures.SyncToolSpecification> {
        return listOf(
            syncTool(
                jsonMapper,
                name = "list_open_models",
                title = "List open models",
                description = "Returns JSON describing each open editor tab (index, id, title, dirty, filePath).",
                schema = """{"type":"object","properties":{},"additionalProperties":false}""",
            ) { _, _ ->
                val json = runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt """{"error":"bindings_unavailable"}"""
                    val snap = b.current()
                    val rows = snap.sessions.mapIndexed { index, tab ->
                        """{"index":$index,"id":${tab.id},"title":${jsonString(tab.displayTitle())},"dirty":${tab.hasUnsavedChanges()},"filePath":${jsonString(tab.schema.filePath)}}"""
                    }
                    """{"selectedIndex":${snap.selectedIndex},"tabs":[${rows.joinToString(",")}]}"""
                }
                okText(json)
            },
            syncTool(
                jsonMapper,
                name = "select_model_tab",
                title = "Select model tab",
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
                name = "new_blank_model_tab",
                title = "New blank model tab",
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
                name = "close_model_tab",
                title = "Close model tab",
                description = "Closes a tab. When discardUnsavedChanges is true, unsaved edits are dropped immediately. " +
                    "Otherwise the UI may prompt the user; this tool cannot wait for that dialog.",
                schema = """{"type":"object","properties":{"tabIndex":{"type":"integer","minimum":0},"discardUnsavedChanges":{"type":"boolean"}},"required":["tabIndex"],"additionalProperties":false}""",
            ) { _, req ->
                val idx = intArg(req, "tabIndex") ?: return@syncTool err("tabIndex required")
                val discard = boolArg(req, "discardUnsavedChanges") == true
                val message = runOnEdt {
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
                if (message != "ok") {
                    return@syncTool err(message)
                }
                okText("""{"ok":true,"closedIndex":$idx}""")
            },
            syncTool(
                jsonMapper,
                name = "save_model_tab",
                title = "Save model tab",
                description = "Runs the same save path as the editor (optional Save-As when saveAs is true).",
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
                name = "open_model_file",
                title = "Open model from file",
                description = "Opens the native file picker and loads a model (same as the main menu Open).",
                schema = """{"type":"object","properties":{},"additionalProperties":false}""",
            ) { _, _ ->
                runOnEdt {
                    val b = bindingsRef.get() ?: return@runOnEdt
                    b.onOpenFile()
                }
                okText("""{"ok":true}""")
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
