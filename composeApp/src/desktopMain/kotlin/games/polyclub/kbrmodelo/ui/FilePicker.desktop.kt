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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

internal actual suspend fun showNativeFilePicker(): ByteArray? = withContext(Dispatchers.IO) {
    var result: ByteArray? = null
    val latch = java.util.concurrent.CountDownLatch(1)
    javax.swing.SwingUtilities.invokeLater {
        val chooser = JFileChooser().apply {
            dialogTitle = "Abrir modelo brModelo"
            fileFilter = FileNameExtensionFilter("Arquivos brModelo (*.xml, *.brM)", "xml", "brM", "brm")
            isMultiSelectionEnabled = false
        }
        val status = chooser.showOpenDialog(null)
        if (status == JFileChooser.APPROVE_OPTION) {
            result = chooser.selectedFile.readBytes()
        }
        latch.countDown()
    }
    latch.await()
    result
}
