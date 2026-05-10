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

package games.polyclub.power.brmodelo.ui.clipboard

import games.polyclub.power.brmodelo.domain.decodeClipboardPayload

/** Platform text clipboard; returns false / null when unavailable or denied. */
internal expect fun brModeloClipboardSetPlainText(text: String): Boolean

internal expect fun brModeloClipboardGetPlainText(): String?

/**
 * Preferred path for Ctrl+C / Ctrl+V flows: tries the platform async clipboard first
 * (e.g. [navigator.clipboard] on Wasm), then falls back to [brModeloClipboardSetPlainText] /
 * [brModeloClipboardGetPlainText], and finally the in-memory store.
 */
internal expect suspend fun brModeloClipboardTryWritePlainTextAsync(text: String): Boolean

internal expect suspend fun brModeloClipboardTryReadPlainTextAsync(): String?

/**
 * Persists conceptual payloads to the OS clipboard when possible, and always keeps a
 * process-local copy so paste still works if the system API fails.
 */
internal object BrModeloConceptualClipboardStore {

    private var memoryFallback: String? = null

    /**
     * Puts [text] into the in-memory fallback immediately so Paste can be enabled in the same frame
     * (before [writePreferred] finishes async clipboard I/O).
     */
    internal fun mirrorLocalClipboardText(text: String) {
        memoryFallback = text
    }

    /** True when [text] is a decodable conceptual clipboard payload (MER/XML body). */
    internal fun isDecodableConceptualPayload(text: String): Boolean =
        decodeClipboardPayload(text.trim()) != null

    /**
     * Whether Paste can run: in-memory mirror or sync OS clipboard contains a decodable conceptual payload.
     */
    internal fun hasPasteableConceptualPayload(): Boolean {
        memoryFallback?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            if (isDecodableConceptualPayload(raw)) return true
        }
        return try {
            val t = brModeloClipboardGetPlainText()?.trim().orEmpty()
            t.isNotEmpty() && isDecodableConceptualPayload(t)
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Writes [text] using [brModeloClipboardTryWritePlainTextAsync] first, then updates
     * the in-memory fallback (always).
     */
    suspend fun writePreferred(text: String) {
        try {
            brModeloClipboardTryWritePlainTextAsync(text)
        } catch (_: Throwable) {
            /* ignore */
        }
        memoryFallback = text
    }

    /**
     * Reads async clipboard text first, then sync [brModeloClipboardGetPlainText], then memory.
     */
    suspend fun readPreferred(): String? {
        val fromAsync = try {
            brModeloClipboardTryReadPlainTextAsync()
        } catch (_: Throwable) {
            null
        }
        if (!fromAsync.isNullOrBlank()) return fromAsync
        val fromSync = try {
            brModeloClipboardGetPlainText()
        } catch (_: Throwable) {
            null
        }
        if (!fromSync.isNullOrBlank()) return fromSync
        return memoryFallback
    }
}
