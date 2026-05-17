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

package games.polyclub.power.brmodelo.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConceptualSchemaDataDictionaryReportTest {

    @Test
    fun `collect entries sorts by object name and maps Pascal type labels`() {
        // Arrange
        val z = SchemaElement.Entity(
            id = 1,
            name = "Zebra",
            position = ElementPosition(0, 0, 10, 10),
            dictionary = "dz",
        )
        val a = SchemaElement.Entity(
            id = 2,
            name = "Anta",
            position = ElementPosition(0, 0, 10, 10),
            dictionary = "da",
        )
        val schema = ConceptualSchema(
            name = "t",
            elements = mapOf(1 to z, 2 to a),
        )

        // Act
        val rows = collectConceptualSchemaDictionaryReportEntries(schema)

        // Assert
        assertEquals(listOf("Anta", "Zebra"), rows.map { it.objectName })
        assertEquals("Entidade", rows[0].typeLabel)
        assertEquals("da", rows[0].dictionary)
        assertEquals("Entidade", rows[1].typeLabel)
        assertEquals("dz", rows[1].dictionary)
    }

    @Test
    fun `associative entity uses only entity dictionary for report body`() {
        // Arrange
        val ea = SchemaElement.AssociativeEntity(
            id = 1,
            name = "EA1",
            position = ElementPosition(0, 0, 10, 10),
            dictionary = "outer",
            relationshipDictionary = "inner",
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ea))

        // Act
        val row = collectConceptualSchemaDictionaryReportEntries(schema).single()

        // Assert
        assertEquals("Entidade associativa", row.typeLabel)
        assertEquals("outer", row.dictionary)
        assertTrue(!formatConceptualDataDictionaryPlainText(listOf(row)).contains("inner"))
    }
}
