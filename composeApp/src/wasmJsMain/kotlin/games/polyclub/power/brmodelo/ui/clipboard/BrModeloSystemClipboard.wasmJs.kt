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

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal actual fun brModeloClipboardSetPlainText(text: String): Boolean = false

internal actual fun brModeloClipboardGetPlainText(): String? = null

/**
 * Uses the browser [Clipboard API](https://developer.mozilla.org/en-US/docs/Web/API/Clipboard_API)
 * when available (typically after a user gesture such as Ctrl+C / Ctrl+V).
 */
@OptIn(ExperimentalEncodingApi::class)
internal actual suspend fun brModeloClipboardTryWriteTextAndPngAsync(text: String, pngBytes: ByteArray?): Boolean =
    suspendCancellableCoroutine { cont ->
        val b64 = pngBytes?.let { Base64.Default.encode(it) }
        wasmClipboardWriteTextAndOptionalPngBase64(text, b64) { ok -> cont.resume(ok) }
    }

internal actual suspend fun brModeloClipboardTryWritePlainTextAsync(text: String): Boolean =
    brModeloClipboardTryWriteTextAndPngAsync(text, null)

internal actual suspend fun brModeloClipboardTryReadPlainTextAsync(): String? =
    suspendCancellableCoroutine { cont ->
        wasmClipboardReadPreferredPayload { t -> cont.resume(t) }
    }

private fun wasmClipboardWriteTextAndOptionalPngBase64(
    text: String,
    pngBase64: String?,
    callback: (Boolean) -> Unit,
): Unit = js(
    """
    (function(t, b64, cb) {
        try {
            if (!navigator.clipboard) { cb(false); return; }
            var wt = navigator.clipboard.writeText;
            var wr = navigator.clipboard.write;
            if (!b64) {
                if (wt) {
                    wt.call(navigator.clipboard, t).then(function() { cb(true); }).catch(function() { cb(false); });
                } else { cb(false); }
                return;
            }
            if (wr && typeof ClipboardItem !== "undefined") {
                var enc = new TextEncoder().encode(t);
                var payloadBlob = new Blob([enc], { type: "application/octet-stream" });
                var bin = atob(b64);
                var arr = new Uint8Array(bin.length);
                for (var i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
                var pngBlob = new Blob([arr], { type: "image/png" });
                var item = new ClipboardItem({ "image/png": pngBlob, "application/octet-stream": payloadBlob });
                wr.call(navigator.clipboard, [item]).then(function() { cb(true); }).catch(function() {
                    if (wt) {
                        wt.call(navigator.clipboard, t).then(function() { cb(true); }).catch(function() { cb(false); });
                    } else { cb(false); }
                });
            } else if (wt) {
                wt.call(navigator.clipboard, t).then(function() { cb(true); }).catch(function() { cb(false); });
            } else { cb(false); }
        } catch (e) { cb(false); }
    })(text, pngBase64, callback)
    """
)

/** Prefers legacy [readText], then **application/octet-stream** / **text/plain** from [read]. */
private fun wasmClipboardReadPreferredPayload(callback: (String?) -> Unit): Unit = js(
    """
    (function(cb) {
        function finish(s) { cb(s); }
        try {
            if (!navigator.clipboard) { finish(null); return; }
            var rt = navigator.clipboard.readText;
            if (rt) {
                rt.call(navigator.clipboard).then(function(t) {
                    if (t && t.length > 0) { finish(t); return; }
                    readItemsOrNull();
                }).catch(function() { readItemsOrNull(); });
            } else {
                readItemsOrNull();
            }
            function readItemsOrNull() {
                var rr = navigator.clipboard.read;
                if (!rr) { finish(null); return; }
                rr.call(navigator.clipboard).then(function(items) {
                    var dec = new TextDecoder("utf-8");
                    function tryOctet(i) {
                        if (i >= items.length) { tryPlainItems(0); return; }
                        var item = items[i];
                        var types = item.types || [];
                        if (types.indexOf("application/octet-stream") >= 0) {
                            item.getType("application/octet-stream").then(function(blob) {
                                blob.arrayBuffer().then(function(buf) {
                                    finish(dec.decode(buf));
                                }).catch(function() { tryOctet(i + 1); });
                            }).catch(function() { tryOctet(i + 1); });
                        } else {
                            tryOctet(i + 1);
                        }
                    }
                    function tryPlainItems(i) {
                        if (i >= items.length) { finish(null); return; }
                        var item = items[i];
                        var types = item.types || [];
                        if (types.indexOf("text/plain") >= 0) {
                            item.getType("text/plain").then(function(blob) {
                                blob.text().then(function(s) { finish(s); }).catch(function() { tryPlainItems(i + 1); });
                            }).catch(function() { tryPlainItems(i + 1); });
                        } else {
                            tryPlainItems(i + 1);
                        }
                    }
                    tryOctet(0);
                }).catch(function() { finish(null); });
            }
        } catch (e) { finish(null); }
    })(callback)
    """
)
