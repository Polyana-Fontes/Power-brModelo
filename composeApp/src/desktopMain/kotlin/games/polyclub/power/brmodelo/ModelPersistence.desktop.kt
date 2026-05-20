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

package games.polyclub.power.brmodelo

import com.formdev.flatlaf.util.SystemFileChooser
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaXmlSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import javax.swing.SwingUtilities

internal actual suspend fun saveConceptualSchemaXml(
    schema: ConceptualSchema,
    suggestedBaseName: String,
    pickLocation: Boolean,
    explicitPath: String?,
): ConceptualSchema? = withContext(Dispatchers.IO) {
    val targetPath = when {
        explicitPath != null -> explicitPath
        pickLocation -> pickXmlSavePath(schema, sanitizeBaseName(suggestedBaseName)) ?: return@withContext null
        else -> {
            val p = schema.filePath
            require(p.isNotBlank()) { "save path must be set when pickLocation is false" }
            p
        }
    }

    val pathChosenByUserOrAgent = explicitPath != null || pickLocation
    val updatedSchema = if (pathChosenByUserOrAgent) {
        schema.copy(
            name = File(targetPath).nameWithoutExtension,
            filePath = targetPath,
            openedFromBrm = false,
        )
    } else {
        schema.copy(
            filePath = targetPath,
            openedFromBrm = false,
        )
    }

    val xml = ConceptualSchemaXmlSerializer.serialize(updatedSchema)
    val outFile = File(targetPath)
    outFile.parentFile?.mkdirs()
    outFile.writeText(xml, StandardCharsets.UTF_8)

    ModelWorkingDirectories.rememberDirectoryOfFile(targetPath)

    updatedSchema
}

private fun sanitizeBaseName(raw: String): String {
    val t = raw.trim().ifBlank { "modelo" }
    return t.replace(Regex("""[\\/:*?"<>|]"""), "_")
}

private fun preferredSaveDirectory(schema: ConceptualSchema): File? {
    if (schema.filePath.isNotBlank()) {
        val parent = File(schema.filePath).parentFile
        if (parent != null && parent.isDirectory) return parent
    }
    val last = ModelWorkingDirectories.lastVisitedDirectoryPath ?: return null
    val dir = File(last)
    return if (dir.isDirectory) dir else null
}

private fun pickXmlSavePath(schema: ConceptualSchema, suggestedBaseName: String): String? {
    var result: String? = null
    val latch = java.util.concurrent.CountDownLatch(1)
    SwingUtilities.invokeLater {
        try {
            val fc = SystemFileChooser().apply {
                dialogTitle = "Salvar modelo como XML"
                fileSelectionMode = SystemFileChooser.FILES_ONLY
                addChoosableFileFilter(
                    SystemFileChooser.FileNameExtensionFilter("XML brModelo (*.xml)", "xml"),
                )
                preferredSaveDirectory(schema)?.let { currentDirectory = it }
                selectedFile = File(currentDirectory, "$suggestedBaseName.xml")
            }
            val status = fc.showSaveDialog(null)
            if (status == SystemFileChooser.APPROVE_OPTION) {
                val sel = fc.selectedFile ?: return@invokeLater
                val file = if (!sel.name.lowercase().endsWith(".xml")) {
                    File(sel.parentFile, sel.name + ".xml")
                } else {
                    sel
                }
                result = file.absolutePath
            }
        } finally {
            latch.countDown()
        }
    }
    latch.await()
    return result
}
