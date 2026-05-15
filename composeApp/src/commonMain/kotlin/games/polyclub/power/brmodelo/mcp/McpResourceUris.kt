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

import games.polyclub.power.brmodelo.ui.EditorTabSession

/** MCP resource URI for the informative conceptual MER XML DTD (classpath-backed on desktop JVM). */
internal fun conceptualMerDtdResourceUri(): String = "brmodelo://schema/conceptual-mer.dtd"

/**
 * MCP resource URI for in-memory conceptual XML: `brmodelo://model/{editorTabSessionId}.xml`.
 * Uses the stable [EditorTabSession.id] so the URI does not change when other tabs are closed
 * and list indices shift.
 */
internal fun modelResourceUriForSession(editorTabSessionId: Long): String =
    "brmodelo://model/$editorTabSessionId.xml"

/** Same diagram raster as **Exportar em PNG** (transparent background, tight crop). */
internal fun modelResourcePngUriForSession(editorTabSessionId: Long): String =
    "brmodelo://model/$editorTabSessionId.png"

/** Same diagram raster as **Exportar em JPEG** (gray canvas background, tight crop, quality 95 in the encoder). */
internal fun modelResourceJpgUriForSession(editorTabSessionId: Long): String =
    "brmodelo://model/$editorTabSessionId.jpg"

internal enum class LiveModelTabResourceSurface {
    Xml,
    Png,
    Jpeg,
}

internal data class ParsedLiveModelTabResource(
    val tabKey: String,
    val surface: LiveModelTabResourceSurface?,
)

/**
 * Parses `brmodelo://model/…` into a tab key and surface, or `null` if not a model URI.
 * [ParsedLiveModelTabResource.surface] is `null` for legacy list-index URIs without a file suffix.
 */
internal fun parseLiveModelTabResourceUri(uri: String): ParsedLiveModelTabResource? {
    val marker = "brmodelo://model/"
    val idx = uri.indexOf(marker)
    if (idx < 0) return null
    val tail = uri.substring(idx + marker.length).substringBefore('/').substringBefore('?')
    val low = tail.lowercase()
    return when {
        low.endsWith(".jpeg") ->
            ParsedLiveModelTabResource(tail.dropLast(5), LiveModelTabResourceSurface.Jpeg)
        low.endsWith(".jpg") ->
            ParsedLiveModelTabResource(tail.dropLast(4), LiveModelTabResourceSurface.Jpeg)
        low.endsWith(".png") ->
            ParsedLiveModelTabResource(tail.dropLast(4), LiveModelTabResourceSurface.Png)
        low.endsWith(".xml") ->
            ParsedLiveModelTabResource(tail.dropLast(4), LiveModelTabResourceSurface.Xml)
        else ->
            ParsedLiveModelTabResource(tail, null)
    }
}

/** True for tab XML (`.xml` or legacy index URI); false for `.png`/`.jpg` previews. */
internal fun isLiveModelTabXmlPlainTextResourceUri(uri: String): Boolean {
    val parsed = parseLiveModelTabResourceUri(uri) ?: return false
    return parsed.surface == LiveModelTabResourceSurface.Xml || parsed.surface == null
}

/**
 * Resolves a live model resource URI to the current **list index** of that tab, or `null` if unknown.
 *
 * Accepts:
 * - `brmodelo://model/{id}.xml|.png|.jpg` — matches [EditorTabSession.id] (preferred).
 * - Legacy `brmodelo://model/{n}` with no suffix — treated as a **tab list index** `n` (older clients).
 */
internal fun tabIndexForModelResourceUri(uri: String, sessions: List<EditorTabSession>): Int? {
    val parsed = parseLiveModelTabResourceUri(uri) ?: return null
    return if (parsed.surface != null) {
        val sessionId = parsed.tabKey.toLongOrNull() ?: return null
        val tabIdx = sessions.indexOfFirst { it.id == sessionId }
        tabIdx.takeIf { it >= 0 }
    } else {
        val legacyIndex = parsed.tabKey.toIntOrNull() ?: return null
        legacyIndex.takeIf { it in sessions.indices }
    }
}

/**
 * Index of the tab that received new content: first session whose id did not exist in [before],
 * or [selectedAfter] when no new session id appears (reuse/replace-in-place).
 */
internal fun mcpCreatedTabIndexAfterOpen(
    before: List<EditorTabSession>,
    after: List<EditorTabSession>,
    selectedAfter: Int,
): Int {
    val beforeIds = before.map { it.id }.toSet()
    val newIdx = after.withIndex().firstOrNull { it.value.id !in beforeIds }?.index
    return newIdx ?: selectedAfter.coerceIn(0, (after.size - 1).coerceAtLeast(0))
}

/** JSON string literal (escaped for use inside MCP tool JSON bodies built outside [McpRuntime]). */
internal fun mcpJsonStringLiteral(value: String): String {
    val escaped = buildString(value.length + 8) {
        append('"')
        for (ch in value) {
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
