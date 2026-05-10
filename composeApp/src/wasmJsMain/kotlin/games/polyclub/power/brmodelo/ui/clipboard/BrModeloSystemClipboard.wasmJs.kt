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

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal actual fun brModeloClipboardSetPlainText(text: String): Boolean = false

internal actual fun brModeloClipboardGetPlainText(): String? = null

/**
 * Uses the browser [Clipboard API](https://developer.mozilla.org/en-US/docs/Web/API/Clipboard_API)
 * when available (typically after a user gesture such as Ctrl+C / Ctrl+V).
 */
internal actual suspend fun brModeloClipboardTryWritePlainTextAsync(text: String): Boolean =
    suspendCancellableCoroutine { cont ->
        wasmClipboardWriteText(text) { ok -> cont.resume(ok) }
    }

internal actual suspend fun brModeloClipboardTryReadPlainTextAsync(): String? =
    suspendCancellableCoroutine { cont ->
        wasmClipboardReadText { t -> cont.resume(t) }
    }

private fun wasmClipboardWriteText(text: String, callback: (Boolean) -> Unit): Unit = js(
    """
    (function(t, cb) {
        try {
            if (!navigator.clipboard || !navigator.clipboard.writeText) { cb(false); return; }
            navigator.clipboard.writeText(t).then(function() { cb(true); }).catch(function() { cb(false); });
        } catch (e) { cb(false); }
    })(text, callback)
    """
)

private fun wasmClipboardReadText(callback: (String?) -> Unit): Unit = js(
    """
    (function(cb) {
        try {
            if (!navigator.clipboard || !navigator.clipboard.readText) { cb(null); return; }
            navigator.clipboard.readText().then(function(t) { cb(t); }).catch(function() { cb(null); });
        } catch (e) { cb(null); }
    })(callback)
    """
)
