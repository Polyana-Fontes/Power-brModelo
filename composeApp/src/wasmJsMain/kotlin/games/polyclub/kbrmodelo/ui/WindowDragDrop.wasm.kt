/*
 * KbrModelo - Kotlin port of brModelo 3.0 originally written in Pascal
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

package games.polyclub.kbrmodelo.ui

// All js() calls in Kotlin/Wasm must be the single expression of a top-level function.

/**
 * Registers `dragover` / `dragleave` / `drop` listeners on `document.body`.
 * Results are stored in JS globals `window._kbrDragOver` (boolean) and
 * `window._kbrDropFile` (data-URL string | null).
 */
internal actual fun setupWindowDragDrop(): Unit = js(
    """
    (function() {
        window._kbrDragOver = false;
        window._kbrDropFile  = null;
        document.body.addEventListener('dragenter', function(e) { e.preventDefault(); window._kbrDragOver = true; });
        document.body.addEventListener('dragover',  function(e) { e.preventDefault(); window._kbrDragOver = true; });
        document.body.addEventListener('dragleave', function(e) {
            if (!e.relatedTarget || !document.body.contains(e.relatedTarget)) {
                window._kbrDragOver = false;
            }
        });
        document.body.addEventListener('drop', function(e) {
            e.preventDefault();
            window._kbrDragOver = false;
            var files = e.dataTransfer && e.dataTransfer.files;
            if (!files || files.length === 0) return;
            var file = files[0];
            var name = file.name;
            var reader = new FileReader();
            // Store as "filename\x00dataUrl" so Kotlin can extract both.
            reader.onload  = function(evt) { window._kbrDropFile = name + '\x00' + evt.target.result; };
            reader.onerror = function()    { window._kbrDropFile = null; };
            reader.readAsDataURL(file);
        });
    })()
    """
)

internal actual fun isWindowDragActive(): Boolean = js("window._kbrDragOver === true")

/**
 * Reads and atomically clears `window._kbrDropFile`.
 * Returns a [PickedFile] with the filename (no extension) and decoded bytes,
 * or null if no file was dropped since the last call.
 */
internal actual fun consumeWindowDropFile(): PickedFile? {
    val encoded = readAndClearDropFile() ?: return null
    val separatorIdx = encoded.indexOf('\u0000')
    val rawName = if (separatorIdx >= 0) encoded.substring(0, separatorIdx) else ""
    val dataUrl = if (separatorIdx >= 0) encoded.substring(separatorIdx + 1) else encoded
    val nameNoExt = rawName.substringBeforeLast('.')
    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    val bytes = runCatching {
        kotlin.io.encoding.Base64.Default.decode(dataUrl.substringAfter(","))
    }.getOrNull() ?: return null
    return PickedFile(name = nameNoExt, bytes = bytes)
}

// Single-expression helper — performs the read+clear in one js() call.
private fun readAndClearDropFile(): String? = js(
    """
    (function() {
        if (!window._kbrDropFile) return null;
        var f = String(window._kbrDropFile);
        window._kbrDropFile = null;
        return f;
    })()
    """
)
