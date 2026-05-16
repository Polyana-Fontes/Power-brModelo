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

/** MCP tools that return diagram rasters for agents (desktop server only). */
internal object McpExportToolNames {
    const val EXPORT_GROUP = "export"
    const val EXPORT_SEPARATOR = "__"

    private fun exportTool(suffix: String): String = "$EXPORT_GROUP$EXPORT_SEPARATOR$suffix"

    /** Raster of a chosen set of canvas elements (subset clipboard-style preview). */
    val SUBSET_RASTER = exportTool("subset_raster")

    /** Snapshot of the user's current canvas selection on a tab (JSON); optional raster like subset_raster when imageFormat is set. */
    val CURRENT_CANVAS_SELECTION = exportTool("current_canvas_selection")

    /** Structured JSON for one or more canvas element ids; optional raster (Ctrl+C-style subgraph) like subset_raster. */
    val CANVAS_ELEMENTS_DETAIL = exportTool("canvas_elements_detail")
}
