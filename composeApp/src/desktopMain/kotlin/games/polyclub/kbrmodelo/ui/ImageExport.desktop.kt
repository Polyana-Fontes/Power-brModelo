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

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.CountDownLatch
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

actual suspend fun saveExportedImage(bitmap: ImageBitmap, isJpeg: Boolean, name: String): Unit =
    withContext(Dispatchers.IO) {
        val ext        = if (isJpeg) "jpg"  else "png"
        val formatName = if (isJpeg) "JPEG" else "PNG"

        // JPEG does not support alpha; composite onto the same gray used as canvas background.
        val awtImage: BufferedImage = if (isJpeg) {
            val src = bitmap.toAwtImage()
            BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB).also { rgb ->
                val g2d = rgb.createGraphics()
                g2d.color = java.awt.Color(0xE8, 0xE8, 0xE8)
                g2d.fillRect(0, 0, src.width, src.height)
                g2d.drawImage(src, 0, 0, null)
                g2d.dispose()
            }
        } else {
            bitmap.toAwtImage()
        }

        val latch = CountDownLatch(1)
        SwingUtilities.invokeLater {
            val chooser = JFileChooser().apply {
                dialogTitle    = if (isJpeg) "Exportar como JPEG" else "Exportar como PNG"
                selectedFile   = File("${name.ifBlank { "modelo" }}.$ext")
                fileFilter     = FileNameExtensionFilter(
                    "$formatName (*.$ext)",
                    ext,
                )
            }
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (!file.name.lowercase().endsWith(".$ext")) {
                    file = File("${file.path}.$ext")
                }
                ImageIO.write(awtImage, formatName, file)
            }
            latch.countDown()
        }
        latch.await()
    }
