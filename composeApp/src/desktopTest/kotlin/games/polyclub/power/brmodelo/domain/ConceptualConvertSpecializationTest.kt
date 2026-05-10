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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConceptualConvertSpecializationTest {

    @Test
    fun `menu convert to restricted requires optional pedinte and sibling subtype to collect`() {
        // Arrange
        val base = SchemaElement.Entity(1, "Pessoa", ElementPosition(0, 0, 102, 66))
        val child = SchemaElement.Entity(2, "A", ElementPosition(200, 0, 102, 66))
        val only = SchemaElement.Specialization(
            id = 3,
            name = "E",
            position = ElementPosition(40, 80, 25, 31),
            baseEntityId = 1,
            type = SpecializationType.OPTIONAL,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to base, 2 to child, 3 to only),
            connections = listOf(
                Connection(10, 3, 1, showCardinality = false),
                Connection(11, 3, 2, showCardinality = false),
            ),
            nextId = 20,
        )

        // Act & Assert
        assertFalse(canConvertOptionalSpecializationsToRestrictedMenu(schema, CanvasSelection.Element(3)))
    }

    @Test
    fun `convert optional specs to restricted removes sibling triangles and links subtypes to pedinte`() {
        // Arrange
        val base = SchemaElement.Entity(1, "Pessoa", ElementPosition(100, 10, 102, 66))
        val childA = SchemaElement.Entity(2, "A", ElementPosition(10, 200, 102, 66))
        val childB = SchemaElement.Entity(3, "B", ElementPosition(220, 200, 102, 66))
        val ped = SchemaElement.Specialization(
            id = 4,
            name = "Ped",
            position = ElementPosition(120, 80, 25, 31),
            baseEntityId = 1,
            type = SpecializationType.OPTIONAL,
        )
        val other = SchemaElement.Specialization(
            id = 5,
            name = "Out",
            position = ElementPosition(200, 80, 25, 31),
            baseEntityId = 1,
            type = SpecializationType.OPTIONAL,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to base, 2 to childA, 3 to childB, 4 to ped, 5 to other),
            connections = listOf(
                Connection(10, 4, 1, showCardinality = false),
                Connection(11, 4, 2, showCardinality = false),
                Connection(12, 5, 1, showCardinality = false),
                Connection(13, 5, 3, showCardinality = false),
            ),
            nextId = 100,
        )
        assertTrue(canConvertOptionalSpecializationsToRestrictedMenu(schema, CanvasSelection.Element(4)))

        // Act
        val out = applyConvertOptionalSpecializationsToRestricted(schema, CanvasSelection.Element(4))

        // Assert
        assertNotNull(out)
        assertNull(out.elements[5])
        val merged = out.elements[4] as SchemaElement.Specialization
        assertEquals(SpecializationType.RESTRICTED, merged.type)
        assertEquals(3, out.connectionsOf(4).size)
    }

    @Test
    fun `menu convert to optionals requires restricted spec with at least three links`() {
        // Arrange
        val base = SchemaElement.Entity(1, "Pessoa", ElementPosition(0, 0, 102, 66))
        val child = SchemaElement.Entity(2, "A", ElementPosition(200, 0, 102, 66))
        val spec = SchemaElement.Specialization(
            id = 3,
            name = "E",
            position = ElementPosition(40, 80, 51, 31),
            baseEntityId = 1,
            type = SpecializationType.RESTRICTED,
        )
        val schemaTwo = ConceptualSchema(
            elements = mapOf(1 to base, 2 to child, 3 to spec),
            connections = listOf(
                Connection(10, 3, 1, showCardinality = false),
                Connection(11, 3, 2, showCardinality = false),
            ),
            nextId = 20,
        )

        // Act & Assert
        assertFalse(canConvertRestrictedSpecializationToOptionalsMenu(schemaTwo, CanvasSelection.Element(3)))
    }

    @Test
    fun `convert restricted to optionals keeps first two schema-order links and creates optional per removed subtype`() {
        // Arrange
        val base = SchemaElement.Entity(1, "Pessoa", ElementPosition(100, 10, 102, 66))
        val child1 = SchemaElement.Entity(2, "F", ElementPosition(10, 200, 102, 66))
        val child2 = SchemaElement.Entity(3, "M", ElementPosition(220, 200, 102, 66))
        val spec = SchemaElement.Specialization(
            id = 4,
            name = "Esp",
            position = ElementPosition(120, 120, 51, 31),
            baseEntityId = 1,
            type = SpecializationType.RESTRICTED,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to base, 2 to child1, 3 to child2, 4 to spec),
            connections = listOf(
                Connection(10, 4, 1, showCardinality = false),
                Connection(11, 4, 2, showCardinality = false),
                Connection(12, 4, 3, showCardinality = false),
            ),
            nextId = 100,
        )
        assertTrue(canConvertRestrictedSpecializationToOptionalsMenu(schema, CanvasSelection.Element(4)))

        // Act
        val out = applyConvertRestrictedSpecializationToOptionals(schema, CanvasSelection.Element(4))

        // Assert
        assertNotNull(out)
        val orig = out.elements[4] as SchemaElement.Specialization
        assertEquals(SpecializationType.OPTIONAL, orig.type)
        assertEquals(2, out.connectionsOf(4).size)
        val newSpecs = out.specializations.filter { it.baseEntityId == 1 && it.id != 4 }
        assertEquals(1, newSpecs.size)
        val spawned = newSpecs.single()
        assertEquals(SpecializationType.OPTIONAL, spawned.type)
        assertEquals(2, out.connectionsOf(spawned.id).size)
    }
}
