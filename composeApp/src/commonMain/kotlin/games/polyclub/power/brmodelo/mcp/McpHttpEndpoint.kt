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

/** Servlet path registered by the desktop MCP transport (Jetty). */
internal const val MCP_SERVLET_PATH = "/mcp"

/** HTTP URL clients use for the streamable MCP endpoint, from persisted settings. */
internal fun mcpServerUrlFromStoredSettings(): String {
    val (bindHostRaw, portRaw, _) = McpSettingsStore.load()
    val host = bindHostRaw.trim().ifBlank { "127.0.0.1" }
    val port = portRaw.coerceIn(1, 65535)
    return "http://$host:$port$MCP_SERVLET_PATH"
}
