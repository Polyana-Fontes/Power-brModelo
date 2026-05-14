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
 * Frozen conceptual MER XML shipped on the desktop JVM classpath for MCP
 * `resources/read` (agent reference only). Source copies stay under `desktopTest/resources`;
 * these JVM resource paths are duplicates with stable names.
 */
internal data class McpClasspathXmlExample(
    /** MCP `resources/list` URI (not an editor tab). */
    val resourceUri: String,
    /** Short stable name for MCP resource listing. */
    val resourceListingName: String,
    /** Path relative to the desktop JAR classpath root. */
    val classpathPath: String,
    val resourceTitle: String,
    /** One line for the server MCP instructions block. */
    val instructionsSummaryLine: String,
)

internal val mcpClasspathXmlExamples: List<McpClasspathXmlExample> = listOf(
    McpClasspathXmlExample(
        resourceUri = "brmodelo://example/mer_boutique_inn",
        resourceListingName = "example_mer_boutique_inn",
        classpathPath = "mcp/examples/mer_boutique_inn.xml",
        resourceTitle = "Example MER — boutique inn (pousada)",
        instructionsSummaryLine = "Hospitality / pousada scenario (from MER-PousadaSolDaManha test data)",
    ),
    McpClasspathXmlExample(
        resourceUri = "brmodelo://example/mer_minimal_entity_link",
        resourceListingName = "example_mer_minimal_entity_link",
        classpathPath = "mcp/examples/mer_minimal_entity_link.xml",
        resourceTitle = "Example MER — minimal entity + relationship",
        instructionsSummaryLine = "Smallest non-trivial diagram (from exemplo-simples test data)",
    ),
    McpClasspathXmlExample(
        resourceUri = "brmodelo://example/mer_default_value_flags",
        resourceListingName = "example_mer_default_value_flags",
        classpathPath = "mcp/examples/mer_default_value_flags.xml",
        resourceTitle = "Example MER — default attribute / label flags",
        instructionsSummaryLine = "Default values and flags showcase (from valores-padroes test data)",
    ),
    McpClasspathXmlExample(
        resourceUri = "brmodelo://example/mer_rich_visual_style",
        resourceListingName = "example_mer_rich_visual_style",
        classpathPath = "mcp/examples/mer_rich_visual_style.xml",
        resourceTitle = "Example MER — rich fonts, colours, layout",
        instructionsSummaryLine = "Heavy visual styling sample (from altamente-personalizado test data)",
    ),
    McpClasspathXmlExample(
        resourceUri = "brmodelo://example/mer_multi_component_palette",
        resourceListingName = "example_mer_multi_component_palette",
        classpathPath = "mcp/examples/mer_multi_component_palette.xml",
        resourceTitle = "Example MER — many component kinds on one canvas",
        instructionsSummaryLine = "Broad palette of conceptual constructs (from teste-varios-componentes test data)",
    ),
)
