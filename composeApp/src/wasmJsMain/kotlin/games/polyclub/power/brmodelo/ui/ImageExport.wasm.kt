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

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage

actual suspend fun saveExportedImage(bitmap: ImageBitmap, isJpeg: Boolean, name: String) {
    val format   = if (isJpeg) EncodedImageFormat.JPEG else EncodedImageFormat.PNG
    val quality  = if (isJpeg) 95 else 100
    val mimeType = if (isJpeg) "image/jpeg" else "image/png"
    val ext      = if (isJpeg) ".jpg" else ".png"
    val filename = "${name.ifBlank { "modelo" }}$ext"

    val skiaImage = SkiaImage.makeFromBitmap(bitmap.asSkiaBitmap())
    val data = skiaImage.encodeToData(format, quality) ?: return

    @OptIn(ExperimentalEncodingApi::class)
    val base64 = Base64.encode(data.bytes)

    triggerBase64Download(base64, mimeType, filename)
}

/**
 * Triggers a browser file download from a base64-encoded data URL.
 *
 * Creates a hidden `<a>` element, sets its href to a data URL constructed from
 * [base64] with the given [mimeType], and programmatically clicks it.
 */
private fun triggerBase64Download(base64: String, mimeType: String, filename: String): Unit = js(
    """
    (function(b64, mime, name) {
        var link = document.createElement('a');
        link.href = 'data:' + mime + ';base64,' + b64;
        link.download = name;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    })(base64, mimeType, filename)
    """
)
