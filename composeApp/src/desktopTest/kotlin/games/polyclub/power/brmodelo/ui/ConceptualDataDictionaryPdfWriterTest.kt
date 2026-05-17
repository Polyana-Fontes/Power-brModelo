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

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ConceptualDataDictionaryPdfWriterTest {

    @Test
    fun `write produces a non-empty PDF with standard header`() {
        // Arrange
        val file = File.createTempFile("brmodelo-dic-test", ".pdf")
        try {
            val body = buildString {
                appendLine("Dicionário de dados")
                appendLine()
                appendLine("001 — Entidade: Anta")
                appendLine()
                appendLine("Sample dictionary body.")
            }

            // Act
            ConceptualDataDictionaryPdfWriter.write(file, body)

            // Assert
            assertTrue(file.isFile)
            assertTrue(file.length() > 200L)
            val header = file.inputStream().use { it.readNBytes(5) }
            assertTrue(header.contentEquals("%PDF-".toByteArray(Charsets.US_ASCII)))
        } finally {
            file.delete()
        }
    }
}
