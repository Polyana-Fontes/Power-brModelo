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
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConceptualOrganizeAttributesTest {

    @Test
    fun `organize on one side leaves other side Y unchanged`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E1",
            position = ElementPosition(100, 100, 102, 66),
        )
        val topAttr = SchemaElement.Attribute(
            id = 2,
            name = "Top1",
            position = ElementPosition(120, 50, 73, 16),
            ownerId = 1,
        )
        val rightAttr = SchemaElement.Attribute(
            id = 3,
            name = "Right1",
            position = ElementPosition(250, 120, 73, 16),
            ownerId = 1,
        )
        var schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to topAttr, 3 to rightAttr),
            connections = listOf(
                Connection(1, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(2, 3, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 4,
        )

        // Act
        schema = organizeAttributesOnOwnerSide(schema, 1, ConceptualAttributeAttachPonto.RIGHT)

        // Assert
        val topAfter = schema.elements[2] as SchemaElement.Attribute
        val rightAfter = schema.elements[3] as SchemaElement.Attribute
        assertEquals(topAttr.position.y, topAfter.position.y, "Top-side attribute must not move when organizing RIGHT only")
        assertNotEquals(rightAttr.position.y, rightAfter.position.y, "Right-side attribute should be repositioned along Divida spacing")
    }

    @Test
    fun `canOrganize menu false for entity without attributes`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 10, 10))
        val schema = ConceptualSchema(elements = mapOf(1 to ent), nextId = 2)

        // Act & Assert
        assertFalse(canOrganizeAttributesMenu(schema, 1))
    }

    @Test
    fun `canOrganize menu true for entity with visible attribute`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val a = SchemaElement.Attribute(2, "A", ElementPosition(200, 30, 73, 16), ownerId = 1)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to a),
            connections = listOf(Connection(1, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL)),
            nextId = 3,
        )

        // Act & Assert
        assertTrue(canOrganizeAttributesMenu(schema, 1))
    }

    @Test
    fun `apply menu organize runs on entity with attributes`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(100, 100, 102, 66))
        val a = SchemaElement.Attribute(2, "A", ElementPosition(300, 125, 73, 16), ownerId = 1)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to a),
            connections = listOf(Connection(1, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL)),
            nextId = 3,
        )

        // Act
        val out = applyOrganizeAttributesMenuAction(schema, 1)

        // Assert
        assertNotNull(out)
        val moved = out.elements[2] as SchemaElement.Attribute
        assertTrue(moved.position.x < a.position.x || moved.position.y != a.position.y)
    }

    @Test
    fun `multi-select entity alone runs full organize like single selection`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(100, 100, 102, 66))
        val a = SchemaElement.Attribute(2, "A", ElementPosition(300, 125, 73, 16), ownerId = 1)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to a),
            connections = listOf(Connection(1, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL)),
            nextId = 3,
        )
        val multi = CanvasSelection.Multiple(elementIds = setOf(1))

        // Act & Assert
        assertTrue(canOrganizeAttributesMenuSelection(schema, multi))
        val out = applyOrganizeAttributesMenuAction(schema, multi)
        assertNotNull(out)
        val moved = out.elements[2] as SchemaElement.Attribute
        assertTrue(moved.position.x < a.position.x || moved.position.y != a.position.y)
    }

    @Test
    fun `multi-select only one direct attribute reorganizes that attribute only`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(100, 100, 102, 66))
        val a1 = SchemaElement.Attribute(2, "A1", ElementPosition(300, 125, 73, 16), ownerId = 1)
        val a2 = SchemaElement.Attribute(3, "A2", ElementPosition(300, 200, 73, 16), ownerId = 1)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to a1, 3 to a2),
            connections = listOf(
                Connection(1, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(2, 3, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 4,
        )
        val multi = CanvasSelection.Multiple(elementIds = setOf(2))

        // Act
        val out = applyOrganizeAttributesMenuAction(schema, multi)

        // Assert
        assertNotNull(out)
        val after1 = out.elements[2] as SchemaElement.Attribute
        val after2 = out.elements[3] as SchemaElement.Attribute
        assertTrue(after1.position != a1.position)
        assertEquals(a2.position, after2.position)
    }

    @Test
    fun `canOrganize false when multi-select has no organizable picks`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 10, 10))
        val schema = ConceptualSchema(elements = mapOf(1 to ent), nextId = 2)
        val multi = CanvasSelection.Multiple(elementIds = setOf(1))

        // Act & Assert
        assertFalse(canOrganizeAttributesMenuSelection(schema, multi))
    }

    @Test
    fun `organize with only composite selected relayouts children without moving composite`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(100, 100, 102, 66),
        )
        val compPos = ElementPosition(220, 115, 80, 32)
        val child1 = SchemaElement.Attribute(
            id = 3,
            name = "C1",
            position = ElementPosition(500, 500, 73, 16),
            ownerId = 5,
        )
        val child2 = SchemaElement.Attribute(
            id = 4,
            name = "C2",
            position = ElementPosition(600, 600, 73, 16),
            ownerId = 5,
        )
        val composite = SchemaElement.Attribute(
            id = 5,
            name = "Comp",
            position = compPos,
            ownerId = 1,
            childAttributeIds = listOf(3, 4),
            multiValuedCount = 2,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 3 to child1, 4 to child2, 5 to composite),
            connections = listOf(
                Connection(10, 5, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(11, 3, 5, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(12, 4, 5, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 20,
        )

        // Act
        val out = applyOrganizeAttributesMenuAction(schema, 5)

        // Assert
        assertNotNull(out)
        val compAfter = out.elements[5] as SchemaElement.Attribute
        assertEquals(compPos, compAfter.position)
        val c1After = out.elements[3] as SchemaElement.Attribute
        val c2After = out.elements[4] as SchemaElement.Attribute
        assertTrue(
            c1After.position != child1.position || c2After.position != child2.position,
            "Children should be repositioned along the composite bar",
        )
    }

    @Test
    fun `organize composite only leaves sibling direct attributes unmoved on entity`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(100, 100, 102, 66))
        val sibling = SchemaElement.Attribute(
            id = 2,
            name = "Sib",
            position = ElementPosition(220, 50, 73, 16),
            ownerId = 1,
        )
        val compPos = ElementPosition(400, 200, 80, 32)
        val child1 = SchemaElement.Attribute(3, "C1", ElementPosition(900, 900, 73, 16), ownerId = 5)
        val child2 = SchemaElement.Attribute(4, "C2", ElementPosition(910, 910, 73, 16), ownerId = 5)
        val composite = SchemaElement.Attribute(
            id = 5,
            name = "Comp",
            position = compPos,
            ownerId = 1,
            childAttributeIds = listOf(3, 4),
            multiValuedCount = 2,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to sibling, 3 to child1, 4 to child2, 5 to composite),
            connections = listOf(
                Connection(10, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(11, 5, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(12, 3, 5, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(13, 4, 5, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 20,
        )

        // Act
        val out = applyOrganizeAttributesMenuAction(schema, 5)

        // Assert
        assertNotNull(out)
        assertEquals(compPos, (out.elements[5] as SchemaElement.Attribute).position)
        assertEquals(sibling.position, (out.elements[2] as SchemaElement.Attribute).position)
    }

    @Test
    fun `organize entity attributes shares Divida Y with relationship on same side`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(100, 100, 102, 66))
        val rel = SchemaElement.Relationship(2, "R", ElementPosition(350, 110, 60, 60))
        val a1 = SchemaElement.Attribute(3, "A1", ElementPosition(220, 105, 50, 16), ownerId = 1)
        val a2 = SchemaElement.Attribute(4, "A2", ElementPosition(220, 120, 50, 16), ownerId = 1)
        val a3 = SchemaElement.Attribute(5, "A3", ElementPosition(220, 135, 50, 16), ownerId = 1)
        val a4 = SchemaElement.Attribute(6, "A4", ElementPosition(220, 150, 50, 16), ownerId = 1)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to rel, 3 to a1, 4 to a2, 5 to a3, 6 to a4),
            connections = listOf(
                Connection(1, 1, 2, Cardinality.ZERO_TO_MANY, showCardinality = true, orientation = LineOrientation.HORIZONTAL),
                Connection(2, 3, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(3, 4, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(4, 5, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(5, 6, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 7,
        )

        // Act
        val out = organizeAttributesForConceptualOwner(schema, 1)

        // Assert
        fun cy(id: Int): Int {
            val p = (out.elements[id] as SchemaElement.Attribute).position
            return p.y + p.height / 2
        }
        // Divida: height 66, five connections on ponto 3 → tam = 11; sort by other Top → A1, R, A2, A3, A4.
        assertEquals(111, cy(3))
        assertEquals(133, cy(4))
        assertEquals(144, cy(5))
        assertEquals(155, cy(6))
    }

    @Test
    fun `organize right edge attributes only keeps connection list order not sorted by top`() {
        // Arrange: three right-side attributes; connection list order differs from sort-by-Top order.
        val ent = SchemaElement.Entity(1, "E", ElementPosition(100, 100, 102, 66))
        val aFirst = SchemaElement.Attribute(2, "First", ElementPosition(250, 200, 40, 16), ownerId = 1)
        val aSecond = SchemaElement.Attribute(3, "Second", ElementPosition(250, 110, 40, 16), ownerId = 1)
        val aThird = SchemaElement.Attribute(4, "Third", ElementPosition(250, 140, 40, 16), ownerId = 1)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to aFirst, 3 to aSecond, 4 to aThird),
            connections = listOf(
                Connection(10, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(11, 3, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(12, 4, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 20,
        )

        // Act
        val out = organizeAttributesForConceptualOwner(schema, 1)

        // Assert: Pascal `OrganizeAtributos` / legacy Kotlin use `Height div (n+1)` in `FLigacoes` walk order.
        fun cy(id: Int): Int {
            val p = (out.elements[id] as SchemaElement.Attribute).position
            return p.y + p.height / 2
        }
        val tam = ent.position.height / 4
        val y0 = ent.position.y
        assertEquals(y0 + tam * 1, cy(2))
        assertEquals(y0 + tam * 2, cy(3))
        assertEquals(y0 + tam * 3, cy(4))
    }

    @Test
    fun `organize bottom edge with relationship uses slot X as attribute left like Pascal PT dot X`() {
        // Arrange: two bottom attributes + relationship on bottom (ponto 4); Divida orders by other Left.
        val ent = SchemaElement.Entity(1, "E", ElementPosition(100, 100, 100, 60))
        val rel = SchemaElement.Relationship(2, "R", ElementPosition(40, 220, 70, 70))
        val a1 = SchemaElement.Attribute(3, "A1", ElementPosition(105, 180, 50, 16), ownerId = 1)
        val a2 = SchemaElement.Attribute(4, "A2", ElementPosition(125, 180, 50, 16), ownerId = 1)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to rel, 3 to a1, 4 to a2),
            connections = listOf(
                Connection(10, 1, 2, Cardinality.ZERO_TO_MANY, showCardinality = true, orientation = LineOrientation.HORIZONTAL),
                Connection(11, 3, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(12, 4, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 20,
        )

        // Act
        val out = organizeAttributesForConceptualOwner(schema, 1)

        // Assert: W=100, n=3 → tam=25; sort by other Left: rel(40), A1(105), A2(125) → X = 125, 150, 175.
        assertEquals(150, (out.elements[3] as SchemaElement.Attribute).position.x)
        assertEquals(175, (out.elements[4] as SchemaElement.Attribute).position.x)
    }
}
