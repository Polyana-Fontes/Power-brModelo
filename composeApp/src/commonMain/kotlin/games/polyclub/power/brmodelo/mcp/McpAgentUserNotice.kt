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

/**
 * What the MCP server changed in the live UI so the app can show a short user-facing notice.
 */
internal data class McpAgentUserNotice(
    val selectionChanged: Boolean = false,
    val windowFocused: Boolean = false,
    /** True when the focused tab index actually changed (not when only raising the window on the same tab). */
    val activeTabChanged: Boolean = false,
)

/** Short Portuguese snackbar line for MCP-driven UI changes. */
internal fun formatMcpAgentUserNoticePtBr(notice: McpAgentUserNotice): String {
    if (!notice.selectionChanged && !notice.windowFocused && !notice.activeTabChanged) return ""
    val parts = mutableListOf<String>()
    if (notice.selectionChanged) parts.add("alterou a seleção no diagrama")
    if (notice.activeTabChanged) parts.add("mudou a aba ativa")
    if (notice.windowFocused) parts.add("trouxe o Power brModelo para a frente")
    return buildString {
        append("Ação do assistente (MCP): ")
        append(parts.joinToString(separator = " · "))
        append('.')
    }
}
