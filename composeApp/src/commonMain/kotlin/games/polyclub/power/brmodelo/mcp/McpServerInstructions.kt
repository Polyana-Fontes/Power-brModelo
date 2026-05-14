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
 * MCP `server.instructions` text (English). Cursor and other clients expose this
 * to the model — keep the canonical URI index here instead of duplicating it
 * across individual tool descriptions.
 */
internal object McpServerInstructions {

    /**
     * Appended to tools that load or author MER XML so agents look at the
     * instruction block instead of guessing paths on disk.
     */
    const val MER_XML_REFERENCE_SEE_INSTRUCTIONS: String =
        "Authoritative MCP resource URIs (DTD, frozen example MER files, live tab XML) are listed in this server's MCP instructions from resources/list — use those URIs; do not search the host filesystem for sample XML."

    fun build(): String = buildString {
        append("This MCP server controls the Power-brModelo desktop editor. ")
        append("Tab tool names use the prefix \"tabs")
        append(McpTabToolNames.TAB_TOOL_SEPARATOR)
        append("\" (category + suffix). ")
        append("Use only letters, digits, and underscores in tool names where the client is strict.\n\n")

        append("## MCP resources (resources/list + resources/read)\n\n")

        append("### Live editor tabs\n")
        append("- One URI per open conceptual tab, returning the current in-memory XML: ")
        append("brmodelo://model/{index} where {index} is the tab index from tool ")
        append(McpTabToolNames.LIST_OPEN)
        append(".\n")
        append("Tools that accept a tab by resource URI (select/close/save by URI) work **only** with these live tab URIs.\n\n")

        append("### Static references (always listed while the server runs)\n\n")
        append("DTD (informative for agents; the editor does **not** validate loads against it):\n- ")
        append(conceptualMerDtdResourceUri())
        append("\n\nFrozen example MER XML (copies of desktop test fixtures; read-only, **not** tabs):\n")
        for (ex in mcpClasspathXmlExamples) {
            append("- ")
            append(ex.instructionsSummaryLine)
            append("\n  ")
            append(ex.resourceUri)
            append('\n')
        }
        append("\nWhen authoring new XML for ")
        append(McpTabToolNames.OPEN_XML)
        append(", start from the DTD plus one of the examples above ")
        append("(via resources/read), then adapt — do not rely on random files from the user's disk.\n")
    }
}
