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
import kotlin.test.assertIs

class ConceptualProceduralToolPlacementTest {

    @Test
    fun `entity overrides apply after placement`() {
        // Arrange
        val empty = ConceptualSchema()

        // Act
        val r = empty.placeProceduralConceptualTool(
            ConceptualProceduralToolKind.ENTITY,
            topLeftX = 11,
            topLeftY = 22,
            overrides = ConceptualProceduralToolOverrides(
                observations = "note",
                dictionary = "dict",
            ),
        )

        // Assert
        val ok = assertIs<ConceptualProceduralToolPlacementResult.Ok>(r)
        val ent = assertIs<SchemaElement.Entity>(ok.element)
        assertEquals(11, ent.position.x)
        assertEquals(22, ent.position.y)
        assertEquals("note", ent.observations)
        assertEquals("dict", ent.dictionary)
    }

    @Test
    fun `duplicate entity display name returns name_conflict`() {
        // Arrange
        val (s0, id0) = ConceptualSchema().placeConceptualItem(ConceptualPlacementKind.PlainEntity, 0, 0)
        val ent0 = s0.elements[id0] as SchemaElement.Entity
        val s1 = s0.withElement(ent0.copy(name = "Cliente"))

        // Act
        val r = s1.placeProceduralConceptualTool(
            ConceptualProceduralToolKind.ENTITY,
            100,
            100,
            overrides = ConceptualProceduralToolOverrides(name = "Cliente"),
        )

        // Assert
        val err = assertIs<ConceptualProceduralToolPlacementResult.Err>(r)
        assertEquals("name_conflict", err.code)
    }

    @Test
    fun `allowDuplicateCanvasLabels permits duplicate entity display name`() {
        // Arrange
        val (s0, id0) = ConceptualSchema().placeConceptualItem(ConceptualPlacementKind.PlainEntity, 0, 0)
        val ent0 = s0.elements[id0] as SchemaElement.Entity
        val s1 = s0.withElement(ent0.copy(name = "Cliente"))

        // Act
        val r = s1.placeProceduralConceptualTool(
            ConceptualProceduralToolKind.ENTITY,
            100,
            100,
            overrides = ConceptualProceduralToolOverrides(name = "Cliente", allowDuplicateCanvasLabels = true),
        )

        // Assert
        val ok = assertIs<ConceptualProceduralToolPlacementResult.Ok>(r)
        assertEquals("Cliente", (ok.element as SchemaElement.Entity).name)
    }

    @Test
    fun `allowDuplicateCanvasLabels permits duplicate inner relationship on associative`() {
        // Arrange
        val (s0, rid) = ConceptualSchema().placeConceptualItem(ConceptualPlacementKind.Relationship, 0, 0)
        val rel = s0.elements[rid] as SchemaElement.Relationship
        val s1 = s0.withElement(rel.copy(name = "Reserva"))

        // Act
        val r = s1.placeProceduralConceptualTool(
            ConceptualProceduralToolKind.ASSOCIATIVE_ENTITY,
            50,
            50,
            overrides = ConceptualProceduralToolOverrides(
                relationshipName = "Reserva",
                allowDuplicateCanvasLabels = true,
            ),
        )

        // Assert
        val ok = assertIs<ConceptualProceduralToolPlacementResult.Ok>(r)
        val ae = ok.element as SchemaElement.AssociativeEntity
        assertEquals("Reserva", ae.relationshipName)
    }

    @Test
    fun `invalid arrow code returns error`() {
        // Arrange
        val empty = ConceptualSchema()

        // Act
        val r = empty.placeProceduralConceptualTool(
            ConceptualProceduralToolKind.RELATIONSHIP,
            0,
            0,
            overrides = ConceptualProceduralToolOverrides(arrowDirectionCode = 99),
        )

        // Assert
        val err = assertIs<ConceptualProceduralToolPlacementResult.Err>(r)
        assertEquals("invalid_arrow_direction_code", err.code)
    }

    @Test
    fun `associative inner name must not collide with existing relationship`() {
        // Arrange
        val (s0, rid) = ConceptualSchema().placeConceptualItem(ConceptualPlacementKind.Relationship, 0, 0)
        val rel = s0.elements[rid] as SchemaElement.Relationship
        val s1 = s0.withElement(rel.copy(name = "Reserva"))

        // Act
        val r = s1.placeProceduralConceptualTool(
            ConceptualProceduralToolKind.ASSOCIATIVE_ENTITY,
            50,
            50,
            overrides = ConceptualProceduralToolOverrides(relationshipName = "Reserva"),
        )

        // Assert
        val err = assertIs<ConceptualProceduralToolPlacementResult.Err>(r)
        assertEquals("relationship_name_conflict", err.code)
    }

    @Test
    fun `annotation overrides apply after placement`() {
        // Arrange
        val empty = ConceptualSchema()

        // Act
        val r = empty.placeProceduralConceptualTool(
            ConceptualProceduralToolKind.ANNOTATION,
            topLeftX = 5,
            topLeftY = 7,
            overrides = ConceptualProceduralToolOverrides(
                name = "Note",
                observations = "body",
                dictionary = "dd",
                annotationColorArgb = 12345,
                annotationTypeCode = AnnotationType.BOX.code,
                alignmentCode = TextAlignment.CENTER.code,
                annotationAutoSize = false,
                annotationWidth = 200,
                annotationHeight = 40,
            ),
        )

        // Assert
        val ok = assertIs<ConceptualProceduralToolPlacementResult.Ok>(r)
        val ann = assertIs<SchemaElement.Annotation>(ok.element)
        assertEquals(5, ann.position.x)
        assertEquals(7, ann.position.y)
        assertEquals(200, ann.position.width)
        assertEquals(40, ann.position.height)
        assertEquals("Note", ann.name)
        assertEquals("body", ann.observations)
        assertEquals("dd", ann.dictionary)
        assertEquals(12345, ann.color)
        assertEquals(AnnotationType.BOX, ann.annotationType)
        assertEquals(TextAlignment.CENTER, ann.alignment)
        assertEquals(false, ann.autoSize)
    }

    @Test
    fun `invalid annotation type code returns error`() {
        // Arrange
        val empty = ConceptualSchema()

        // Act
        val r = empty.placeProceduralConceptualTool(
            ConceptualProceduralToolKind.ANNOTATION,
            0,
            0,
            overrides = ConceptualProceduralToolOverrides(annotationTypeCode = 9),
        )

        // Assert
        val err = assertIs<ConceptualProceduralToolPlacementResult.Err>(r)
        assertEquals("invalid_annotation_type_code", err.code)
    }

    @Test
    fun `entity placement ignores invalid arrow override`() {
        // Arrange
        val empty = ConceptualSchema()

        // Act
        val r = empty.placeProceduralConceptualTool(
            ConceptualProceduralToolKind.ENTITY,
            0,
            0,
            overrides = ConceptualProceduralToolOverrides(arrowDirectionCode = 99),
        )

        // Assert
        assertIs<ConceptualProceduralToolPlacementResult.Ok>(r)
    }
}
