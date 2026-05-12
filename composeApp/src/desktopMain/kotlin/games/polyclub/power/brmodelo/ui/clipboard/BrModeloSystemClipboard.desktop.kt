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

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.text.Charsets

internal actual fun brModeloClipboardSetPlainText(text: String): Boolean =
    try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        true
    } catch (_: Exception) {
        false
    }

/** Reads `text/plain` first (legacy), then **application/octet-stream** as UTF-8 (brModelo + PNG copy). */
internal actual fun brModeloClipboardGetPlainText(): String? {
    return try {
        val transferable = Toolkit.getDefaultToolkit().systemClipboard.getContents(null) ?: return null
        readConceptualPayloadStringFromTransferable(transferable)
    } catch (_: Exception) {
        null
    }
}

private fun readConceptualPayloadStringFromTransferable(transferable: Transferable): String? {
    if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        val s = transferable.getTransferData(DataFlavor.stringFlavor) as? String
        if (!s.isNullOrBlank()) return s
    }
    val octet = transferable.transferDataFlavors.find {
        it.primaryType == "application" && it.subType == "octet-stream"
    } ?: return null
    if (!transferable.isDataFlavorSupported(octet)) return null
    val data = transferable.getTransferData(octet)
    val bytes = when (data) {
        is InputStream -> data.use { it.readBytes() }
        is ByteArray -> data
        else -> return null
    }
    return bytes.toString(Charsets.UTF_8)
}

/**
 * UTF-8 payload as **application/octet-stream** (with optional PNG first) so chat apps are less
 * likely to paste XML; [readConceptualPayloadStringFromTransferable] still decodes the payload.
 */
private class PayloadOctetAndOptionalPngTransferable(
    private val text: String,
    private val pngBytes: ByteArray?,
) : Transferable {

    private val payloadBytes = text.toByteArray(Charsets.UTF_8)

    private val payloadFlavor = DataFlavor("application/octet-stream", "brModelo payload")

    private val pngFlavor: DataFlavor? = pngBytes?.let {
        try {
            DataFlavor("image/png", "PNG")
        } catch (_: Throwable) {
            null
        }
    }

    private val flavors: Array<DataFlavor> =
        if (pngFlavor != null) arrayOf(pngFlavor, payloadFlavor)
        else arrayOf(payloadFlavor)

    override fun getTransferDataFlavors(): Array<DataFlavor> = flavors

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
        flavors.any { it.equals(flavor) }

    override fun getTransferData(flavor: DataFlavor): Any {
        if (flavor.equals(payloadFlavor)) {
            return ByteArrayInputStream(payloadBytes)
        }
        val pf = pngFlavor
        if (pf != null && flavor.equals(pf) && pngBytes != null) {
            return ByteArrayInputStream(pngBytes)
        }
        throw UnsupportedFlavorException(flavor)
    }
}

internal actual suspend fun brModeloClipboardTryWriteTextAndPngAsync(text: String, pngBytes: ByteArray?): Boolean =
    try {
        val contents = PayloadOctetAndOptionalPngTransferable(text, pngBytes)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(contents, null)
        true
    } catch (_: Exception) {
        false
    }

internal actual suspend fun brModeloClipboardTryWritePlainTextAsync(text: String): Boolean =
    brModeloClipboardTryWriteTextAndPngAsync(text, null)

internal actual suspend fun brModeloClipboardTryReadPlainTextAsync(): String? =
    brModeloClipboardGetPlainText()
