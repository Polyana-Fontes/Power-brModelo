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
 * Registered MCP tool names for editor tabs. Centralised so the category separator
 * (or whole naming scheme) can be changed in one place — e.g. some clients only allow
 * letters, digits, and underscores.
 */
internal object McpTabToolNames {
    const val TAB_TOOL_CATEGORY = "tabs"
    const val TAB_TOOL_SEPARATOR = "__"

    private fun tabTool(suffix: String): String = "$TAB_TOOL_CATEGORY$TAB_TOOL_SEPARATOR$suffix"

    val LIST_OPEN = tabTool("list_open")
    val SELECT = tabTool("select")
    val SELECT_RESOURCE = tabTool("select_resource")
    val CLOSE = tabTool("close")
    val CLOSE_RESOURCE = tabTool("close_resource")
    val NEW_CONCEPTUAL_MODEL = tabTool("new_conceptual_model")
    val SAVE = tabTool("save")
    val SAVE_RESOURCE = tabTool("save_resource")
    val OPEN_FILE = tabTool("open_file")
    val OPEN_XML = tabTool("open_xml")
    val REPLACE_MODEL_XML = tabTool("replace_model_xml")
    val PATCH_MODEL_XML = tabTool("patch_model_xml")
}
