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

import games.polyclub.kbrmodelo.ModelWorkingDirectories
import com.formdev.flatlaf.util.SystemFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.SwingUtilities

internal actual suspend fun showNativeFilePicker(): PickedFile? = withContext(Dispatchers.IO) {
    var result: PickedFile? = null
    val latch = java.util.concurrent.CountDownLatch(1)
    SwingUtilities.invokeLater {
        try {
            val chooser = SystemFileChooser().apply {
                dialogTitle = "Abrir modelo brModelo"
                addChoosableFileFilter(
                    SystemFileChooser.FileNameExtensionFilter(
                        "Arquivos brModelo (*.xml, *.brM)",
                        "xml",
                        "brM",
                        "brm",
                    ),
                )
                isMultiSelectionEnabled = false
                ModelWorkingDirectories.lastVisitedDirectoryPath?.let { path ->
                    val d = File(path)
                    if (d.isDirectory) currentDirectory = d
                }
            }
            val status = chooser.showOpenDialog(null)
            if (status == SystemFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile ?: return@invokeLater
                ModelWorkingDirectories.rememberDirectoryOfFile(file.absolutePath)
                result = PickedFile(
                    name = file.nameWithoutExtension,
                    bytes = file.readBytes(),
                    diskPath = file.absolutePath,
                )
            }
        } finally {
            latch.countDown()
        }
    }
    latch.await()
    result
}
