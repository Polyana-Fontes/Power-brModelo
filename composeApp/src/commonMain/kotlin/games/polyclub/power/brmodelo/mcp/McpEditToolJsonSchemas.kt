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
 * JSON Schemas for MCP `edit__*` tools (strict keys; runtime mirrors inspector allowlists per element kind).
 */
internal object McpEditToolJsonSchemas {

    private const val TAB_RESOURCE_URI_PROP =
        """"resourceUri":{"type":"string","minLength":1,"description":"Live tab URI from tabs__list_open (resourceUri, resourceUriPng, or resourceUriJpeg — same tab)."}"""

    private const val ELEMENT_POSITION =
        """"position":{"type":"object","properties":{"x":{"type":"integer"},"y":{"type":"integer"},"width":{"type":"integer"},"height":{"type":"integer"}},"additionalProperties":false}"""

    private const val CARDINALITY_POSITION =
        """"cardinalityPosition":{"type":"object","properties":{"x":{"type":"integer"},"y":{"type":"integer"},"width":{"type":"integer"},"height":{"type":"integer"}},"additionalProperties":false}"""

    val EDIT_MODEL: String = """{"type":"object","properties":{$TAB_RESOURCE_URI_PROP,"patch":{"type":"object","properties":{"name":{"type":"string"},"author":{"type":"string"},"observations":{"type":"string"}},"minProperties":1,"additionalProperties":false}},"required":["resourceUri","patch"],"additionalProperties":false}"""

    val EDIT_CANVAS_ELEMENT: String = """{"type":"object","properties":{$TAB_RESOURCE_URI_PROP,"elementId":{"type":"integer","minimum":0},"patch":{"type":"object","properties":{"name":{"type":"string"},"observations":{"type":"string"},"dictionary":{"type":"string"},$ELEMENT_POSITION,"labelColorArgb":{"type":["integer","null"]},"labelBold":{"type":"boolean"},"labelItalic":{"type":"boolean"},"arrowDirectionCode":{"type":"integer"},"showName":{"type":"boolean"},"relationshipName":{"type":"string"},"relationshipObservations":{"type":"string"},"relationshipDictionary":{"type":"string"},"autoSize":{"type":"boolean"},"isIdentifier":{"type":"boolean"},"isOptional":{"type":"boolean"},"isMultiValued":{"type":"boolean"},"cardinalityMin":{"type":"integer"},"cardinalityMax":{"type":"integer"},"valueType":{"type":"string"},"complement":{"type":"string"},"isPartial":{"type":"boolean"},"annotationColorArgb":{"type":["integer","null"]},"annotationTypeCode":{"type":"integer"},"alignmentCode":{"type":"integer"}},"minProperties":1,"additionalProperties":false}},"required":["resourceUri","elementId","patch"],"additionalProperties":false}"""

    val EDIT_CONNECTION: String = """{"type":"object","properties":{$TAB_RESOURCE_URI_PROP,"connectionId":{"type":"integer","minimum":0},"patch":{"type":"object","properties":{"cardinalityCode":{"type":["integer","null"]},"showCardinality":{"type":"boolean"},"orientationCode":{"type":"integer"},"cardinalityFixed":{"type":"boolean"},"isWeak":{"type":"boolean"},"cardinalityRole":{"type":"string"},"cardinalityObservations":{"type":"string"},"cardinalityDictionary":{"type":"string"},"cardinalityAutoSize":{"type":"boolean"},$CARDINALITY_POSITION,"useAssociativeOuterForEndA":{"type":"boolean"},"useAssociativeOuterForEndB":{"type":"boolean"},"labelColorArgb":{"type":["integer","null"]},"labelBold":{"type":"boolean"},"labelItalic":{"type":"boolean"},"labelUnderline":{"type":"boolean"},"labelStrikeThrough":{"type":"boolean"},"labelFontFamilyName":{"type":["string","null"]},"labelFontSizePoints":{"type":["integer","null"]},"labelFontScript":{"type":["string","null"]}},"minProperties":1,"additionalProperties":false}},"required":["resourceUri","connectionId","patch"],"additionalProperties":false}"""

    val EDIT_HIDDEN_ATTRIBUTE: String = """{"type":"object","properties":{$TAB_RESOURCE_URI_PROP,"holderElementId":{"type":"integer","minimum":0},"path":{"type":"array","items":{"type":"integer","minimum":0},"minItems":1},"patch":{"type":"object","properties":{"name":{"type":"string"},"type":{"type":"string"},"isIdentifier":{"type":"boolean"},"isOptional":{"type":"boolean"},"isMultiValued":{"type":"boolean"},"cardinalityMin":{"type":"integer"},"cardinalityMax":{"type":"integer"},"observations":{"type":"string"},"dictionary":{"type":"string"},$ELEMENT_POSITION},"minProperties":1,"additionalProperties":false}},"required":["resourceUri","holderElementId","path","patch"],"additionalProperties":false}"""

    val EDIT_CANVAS_SELECTION: String =
        """{"type":"object","properties":{$TAB_RESOURCE_URI_PROP,"elementIds":{"type":"array","items":{"type":"integer"},"description":"Canvas element ids (multi-pick). Omit or [] clears element picks."},"cardinalityConnectionIds":{"type":"array","items":{"type":"integer"},"description":"Connection ids for cardinality-label picks. Omit or [] clears."},"requestWindowFocus":{"type":"boolean","description":"When true, also brings the editor window to the foreground (use sparingly; see server instructions)."}},"required":["resourceUri"],"additionalProperties":false}"""

    val EDIT_CANVAS_SELECTION_RECTANGLE: String =
        """{"type":"object","properties":{$TAB_RESOURCE_URI_PROP,"x0":{"type":"integer","description":"Schema-space X of rectangle corner A (integer coordinates; same space as element positions)."},"y0":{"type":"integer","description":"Schema-space Y of rectangle corner A."},"x1":{"type":"integer","description":"Schema-space X of rectangle corner B."},"y1":{"type":"integer","description":"Schema-space Y of rectangle corner B."},"mergeMode":{"type":"string","enum":["add","replace","subtract"],"description":"add: union band with current selection; replace: selection becomes only picks in the band; subtract: remove picks in the band from the current selection."},"dryRun":{"type":"boolean","description":"When true, returns geometric hits and projected selection without changing the UI; requestWindowFocus is ignored."},"requestWindowFocus":{"type":"boolean","description":"When true and dryRun is false, raises the editor window (use sparingly; see server instructions)."}},"required":["resourceUri","x0","y0","x1","y1","mergeMode"],"additionalProperties":false}"""
}
