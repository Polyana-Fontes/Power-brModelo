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

package games.polyclub.power.brmodelo.ui

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Opens the browser's native file picker dialog by creating a hidden
 * `<input type="file">` element and programmatically clicking it.
 *
 * The file is read as a data URL (base64) via FileReader, then decoded
 * to a ByteArray in Kotlin. Suspends until the user selects a file or
 * closes the dialog.
 */
internal actual suspend fun showNativeFilePicker(): PickedFile? =
    suspendCancellableCoroutine { cont ->
        // The JS callback receives "filename\x00dataUrl", or null on cancel/error.
        triggerFileInput { encoded ->
            if (encoded == null) {
                cont.resume(null)
            } else {
                val separatorIdx = encoded.indexOf('\u0000')
                val rawName  = if (separatorIdx >= 0) encoded.substring(0, separatorIdx) else ""
                val dataUrl  = if (separatorIdx >= 0) encoded.substring(separatorIdx + 1) else encoded
                val nameNoExt = rawName.substringBeforeLast('.')
                @OptIn(ExperimentalEncodingApi::class)
                val bytes = runCatching {
                    Base64.decode(dataUrl.substringAfter(","))
                }.getOrNull()
                cont.resume(bytes?.let { PickedFile(name = nameNoExt, bytes = it) })
            }
        }
    }

/**
 * Creates a hidden `<input type="file">` in the DOM, triggers it, reads
 * the selected file as a data URL and calls [callback] with the result
 * encoded as `"filename\x00dataUrl"`, or null on cancel/error.
 *
 * The [callback] parameter is a Kotlin lambda; Kotlin/Wasm JS exposes it
 * as a callable JavaScript function inside the `js()` block.
 */
private fun triggerFileInput(callback: (String?) -> Unit): Unit = js(
    """
    (function(cb) {
        var input = document.createElement('input');
        input.type = 'file';
        input.accept = '.xml,.brM,.brm';
        input.style.display = 'none';
        document.body.appendChild(input);
        var cleanup = function() {
            try { document.body.removeChild(input); } catch(e) {}
        };
        input.addEventListener('change', function() {
            var file = input.files && input.files[0];
            if (file) {
                var name = file.name;
                var reader = new FileReader();
                reader.onload  = function(e) { cleanup(); cb(name + '\x00' + e.target.result); };
                reader.onerror = function()  { cleanup(); cb(null); };
                reader.readAsDataURL(file);
            } else {
                cleanup();
                cb(null);
            }
        });
        // If the user closes the dialog without picking (focus returns to window)
        window.addEventListener('focus', function onFocus() {
            window.removeEventListener('focus', onFocus);
            setTimeout(function() {
                if (input.files && input.files.length === 0) { cleanup(); cb(null); }
            }, 500);
        });
        input.click();
    })(callback)
    """
)
