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
        append("Procedural canvas placement tools use the prefix \"tools")
        append(McpProceduralToolsToolNames.TOOLS_SEPARATOR)
        append("\". ")
        append("Conceptual search tools use the prefix \"")
        append(McpSearchToolNames.SEARCH_GROUP)
        append(McpSearchToolNames.SEARCH_SEPARATOR)
        append("\" (same behaviour as the editor **Localizar** UI). ")
        append("Use only letters, digits, and underscores in tool names where the client is strict.\n\n")

        append("## MCP resources (resources/list + resources/read)\n\n")

        append("### Live editor tabs\n")
        append("- One URI per open conceptual tab, returning the current in-memory XML: ")
        append("brmodelo://model/{editorTabSessionId}.xml — the id is the stable tab session id from ")
        append(McpTabToolNames.LIST_OPEN)
        append(" (field `id`), not the list index, so the URI stays valid when other tabs are closed.\n")
        append("Tools that accept a tab by resource URI (select/close/save by URI) work **only** with these live tab URIs.\n")
        append("Successful calls to ")
        append(McpTabToolNames.NEW_CONCEPTUAL_MODEL)
        append(", ")
        append(McpTabToolNames.OPEN_XML)
        append(", and ")
        append(McpTabToolNames.OPEN_FILE)
        append(" return JSON with createdTabIndex, createdResourceUri, selectedIndex, and selectedResourceUri ")
        append("(created identifies the tab that received the new model; selected is the focused tab after the call).\n\n")

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

        append("\n### XML editing on open tabs (each call = one undo step)\n")
        append("- ")
        append(McpTabToolNames.REPLACE_MODEL_XML)
        append(": replace the whole conceptual MER XML of a tab (UTF-8). Provide tabIndex or a live tab resource URI from ")
        append(McpTabToolNames.LIST_OPEN)
        append(".\n- ")
        append(McpTabToolNames.PATCH_MODEL_XML)
        append(": search/replace on the tab's serialized MER XML, then re-parse; set replace_all true only when you intend to change every occurrence of old_string.\n")

        append("\n### Resource text utilities (")
        append(McpResourceUtilityToolNames.RESOURCE_UTILITY_GROUP)
        append(McpResourceUtilityToolNames.RESOURCE_UTILITY_SEPARATOR)
        append("… tools)\n")
        append("These tools read or search the **plain text** of any registered resource URI (same bodies as resources/read): ")
        append(McpResourceUtilityToolNames.READ_FULL)
        append(" (full body), ")
        append(McpResourceUtilityToolNames.READ_LINES)
        append(" (1-based inclusive line range), ")
        append(McpResourceUtilityToolNames.READ_RANGE)
        append(" (0-based Kotlin substring range, end index exclusive), ")
        append(McpResourceUtilityToolNames.SEARCH)
        append(" (literal), ")
        append(McpResourceUtilityToolNames.SEARCH_REGEX)
        append(". Search results include character indices and 1-based line/column for each match.\n")

        append("\n### Procedural canvas tools (")
        append(McpProceduralToolsToolNames.TOOLS_GROUP)
        append(McpProceduralToolsToolNames.TOOLS_SEPARATOR)
        append("…)\n")
        append("Place entities, relationships, and associative entities step-by-step with the same domain rules as the ribbon tools, ")
        append("without changing the user's currently selected canvas tool: ")
        append(McpProceduralToolsToolNames.PLACE_ENTITY)
        append(", ")
        append(McpProceduralToolsToolNames.PLACE_RELATIONSHIP)
        append(", ")
        append(McpProceduralToolsToolNames.PLACE_ASSOCIATIVE_ENTITY)
        append(". ")
        append("Apply specialization on an existing entity id (baseEntityId; tabIndex or live tab URI): ")
        append(McpProceduralToolsToolNames.APPLY_SPECIALIZATION_BASIC)
        append(" (triangle only), ")
        append(McpProceduralToolsToolNames.APPLY_SPECIALIZATION_TREE)
        append(" (small tree: specialization + child entity; set boolean `exclusive`: true = restricted, false = optional). ")
        append("Link two diagram endpoints like the editor **Ligar Objetos** tool (entity↔relationship, entity↔entity with automatic relationship creation, specialization↔plain entity, etc.): ")
        append(McpProceduralToolsToolNames.LINK_OBJECTS)
        append(" — returns new connections and any new relationship/self-relationship JSON; optional arguments adjust cardinalities and relationship fields at creation time. ")
        append("Each placement or specialization call returns JSON for the new primary element (id, bounds, names, style); ")
        append(McpProceduralToolsToolNames.LINK_OBJECTS)
        append(" returns a richer JSON body (see tool description).\n")

        append("\n### Conceptual attributes (")
        append(McpAttributeToolsToolNames.ATTRIBUTE_TOOLS_GROUP)
        append(McpAttributeToolsToolNames.ATTRIBUTE_TOOLS_SEPARATOR)
        append("…)\n")
        append("Add canvas attributes like the attribute ribbon tool (without switching the active tool): ")
        append(McpAttributeToolsToolNames.APPLY_ATTRIBUTE)
        append(" (`attributeVariant`: basic, identifier, multivalued, optional; optional `attachSide` left/top/right/bottom; optional `overrides`). ")
        append("Placement hints for agents (heuristics, not enforced limits): keep attributes for the same owner on the **same side** when possible — **prefer the right** side for consistency; ")
        append("when `attachSide` is omitted the editor picks the least crowded side with a **right-side tie-break**. ")
        append("If a chosen side would **cover** important entities or relationships on the diagram, pick another `attachSide` so labels stay readable. ")
        append("At default entity size (~102×66 px) and default attribute boxes (~73×16), about **five** attributes per **left/right** edge usually stay legible; ")
        append("about **twenty-four** stacked along **top/bottom** still feels comfortable — beyond that layouts tend to feel tight. ")
        append("Create a composite with explicit leaf `children` (no nested composite in one call; optional `nestedHiddenAttributes` on the new parent): ")
        append(McpAttributeToolsToolNames.APPLY_COMPOSITE_ATTRIBUTE)
        append(". ")
        append("Append full recursive hidden-attribute trees to any holder that supports ocultos in the inspector: ")
        append(McpAttributeToolsToolNames.APPLY_HIDDEN_ATTRIBUTE)
        append(" (`roots` array). Simple/composite responses include `element` JSON; hidden append returns `holderElementId` and `appendedRootCount`.\n")

        append("\n### Conceptual search (")
        append(McpSearchToolNames.SEARCH_GROUP)
        append(McpSearchToolNames.SEARCH_SEPARATOR)
        append("…)\n")
        append("Find in-memory diagram text with the same rules as **Localizar** (accent-insensitive substring, optional type filters, dictionary/observations flags, 400-hit cap): ")
        append(McpSearchToolNames.FIND)
        append(". Blank `query` lists every candidate in the selected type flags (same 400-hit cap; `matchedIn` may be empty). ")
        append("Apply one hit to focus the editor (selection, inspector tab, hidden-attribute path, canvas pan): ")
        append(McpSearchToolNames.APPLY_HIT)
        append(". Pass the `hit` object echoed from a prior find call; include tabIndex or a live tab resource URI.\n")
    }
}
