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
 * Search/replace helpers for MCP `tabs__patch_model_xml` (serialized MER XML text).
 */
internal object McpModelXmlPatch {

    /**
     * Applies [oldString] → [newString] on [currentXml].
     *
     * When [replaceAll] is false, [oldString] must occur **exactly once** (Cursor-style single edit).
     * When true, every occurrence is replaced (still one undo step in the editor).
     *
     * @return `(resultXml, errorCode)` — on success `errorCode` is null; on failure `resultXml` is null.
     */
    fun applyXmlStringPatch(
        currentXml: String,
        oldString: String,
        newString: String,
        replaceAll: Boolean,
    ): Pair<String?, String?> {
        if (oldString.isEmpty()) {
            return null to "old_string_must_not_be_empty"
        }
        if (!currentXml.contains(oldString)) {
            return null to "old_string_not_found"
        }
        if (!replaceAll) {
            val first = currentXml.indexOf(oldString)
            val last = currentXml.lastIndexOf(oldString)
            if (first != last) {
                return null to "old_string_not_unique"
            }
            return currentXml.replaceFirst(oldString, newString) to null
        }
        return currentXml.replace(oldString, newString) to null
    }
}
