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

package games.polyclub.power.brmodelo.ui.canvas

import androidx.compose.ui.geometry.Offset
import games.polyclub.power.brmodelo.domain.ConceptualLinkPick
import games.polyclub.power.brmodelo.domain.ConceptualLinkValidationResult
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.domain.normalizeConceptualLinkPickForAutoSelfRelationshipTool
import games.polyclub.power.brmodelo.domain.validateAndBuildConceptualLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConceptualLinkPickHitTestTest {

    @Test
    fun hitTestConceptualLinkPick_associative_inner_vs_outer() {
        // Arrange
        val pos = ElementPosition(0, 0, 100, 80)
        val assoc = SchemaElement.AssociativeEntity(id = 7, name = "EA", position = pos)
        val schema = ConceptualSchema(elements = mutableMapOf(7 to assoc))
        val inner = ElementPosition(
            x = pos.x + 15,
            y = pos.y + 15,
            width = (pos.width - 30).coerceAtLeast(10),
            height = (pos.height - 30).coerceAtLeast(10),
        )
        val cx = inner.x + inner.width / 2f
        val cy = inner.y + inner.height / 2f

        // Act
        val innerPick = hitTestConceptualLinkPick(schema, Offset(cx, cy))

        // Assert
        assertNotNull(innerPick)
        assertEquals(7, innerPick.elementId)
        assertEquals(false, innerPick.isAssociativeOuterEntitySide)

        // Act — corner of outer frame, outside inner rhombus
        val outerPick = hitTestConceptualLinkPick(schema, Offset(pos.x + 2f, pos.y + 2f))

        // Assert
        assertNotNull(outerPick)
        assertEquals(7, outerPick.elementId)
        assertEquals(true, outerPick.isAssociativeOuterEntitySide)
    }

    @Test
    fun relationshipDiamondContains_axisAlignedRhombus() {
        // Arrange
        val inner = ElementPosition(0, 0, 40, 40)

        // Act
        val centreInside = relationshipDiamondContains(inner, Offset(20f, 20f))
        val outsideTop = relationshipDiamondContains(inner, Offset(20f, -1f))

        // Assert
        assertEquals(true, centreInside)
        assertEquals(false, outsideTop)
    }

    @Test
    fun hitTestConceptualLinkPick_empty_returns_null() {
        // Arrange
        val schema = ConceptualSchema()

        // Act
        val pick = hitTestConceptualLinkPick(schema, Offset(10f, 10f))

        // Assert
        assertNull(pick)
    }

    @Test
    fun hitTestConceptualAttributeToolHoverPick_associative_inner_vs_outer() {
        // Arrange
        val pos = ElementPosition(0, 0, 100, 80)
        val assoc = SchemaElement.AssociativeEntity(id = 7, name = "EA", position = pos)
        val schema = ConceptualSchema(elements = mutableMapOf(7 to assoc))
        val inner = ElementPosition(
            x = pos.x + 15,
            y = pos.y + 15,
            width = (pos.width - 30).coerceAtLeast(10),
            height = (pos.height - 30).coerceAtLeast(10),
        )
        val cx = inner.x + inner.width / 2f
        val cy = inner.y + inner.height / 2f

        // Act
        val innerHover = hitTestConceptualAttributeToolHoverPick(schema, Offset(cx, cy))

        // Assert
        assertNotNull(innerHover)
        assertEquals(7, innerHover.elementId)
        assertFalse(innerHover.isAssociativeOuterEntitySide)

        // Act — corner of outer frame, outside inner rhombus
        val outerHover = hitTestConceptualAttributeToolHoverPick(schema, Offset(pos.x + 2f, pos.y + 2f))

        // Assert
        assertNotNull(outerHover)
        assertEquals(7, outerHover.elementId)
        assertTrue(outerHover.isAssociativeOuterEntitySide)
    }

    @Test
    fun hitTestConceptualAttributeToolHoverPick_annotation_returns_null() {
        // Arrange
        val ann = SchemaElement.Annotation(
            id = 1,
            name = "Note",
            position = ElementPosition(0, 0, 50, 30),
        )
        val schema = ConceptualSchema(elements = mutableMapOf(1 to ann))

        // Act
        val pick = hitTestConceptualAttributeToolHoverPick(schema, Offset(25f, 15f))

        // Assert
        assertNull(pick)
    }

    @Test
    fun hitTestConceptualAttributeToolHoverPick_entity_returns_pick() {
        // Arrange
        val ent = SchemaElement.Entity(id = 2, name = "E", position = ElementPosition(0, 0, 40, 30))
        val schema = ConceptualSchema(elements = mutableMapOf(2 to ent))

        // Act
        val pick = hitTestConceptualAttributeToolHoverPick(schema, Offset(20f, 15f))

        // Assert
        assertNotNull(pick)
        assertEquals(2, pick.elementId)
        assertFalse(pick.isAssociativeOuterEntitySide)
    }

    @Test
    fun hitTestPlainEntityId_plain_entity_returns_id() {
        // Arrange
        val ent = SchemaElement.Entity(id = 3, name = "E", position = ElementPosition(10, 10, 80, 40))
        val schema = ConceptualSchema(elements = mutableMapOf(3 to ent))

        // Act
        val id = hitTestPlainEntityId(schema, Offset(50f, 30f))

        // Assert
        assertEquals(3, id)
    }

    @Test
    fun hitTestPlainEntityId_associative_only_returns_null() {
        // Arrange
        val pos = ElementPosition(0, 0, 100, 80)
        val assoc = SchemaElement.AssociativeEntity(id = 7, name = "EA", position = pos)
        val schema = ConceptualSchema(elements = mutableMapOf(7 to assoc))
        val inner = ElementPosition(
            x = pos.x + 15,
            y = pos.y + 15,
            width = (pos.width - 30).coerceAtLeast(10),
            height = (pos.height - 30).coerceAtLeast(10),
        )
        val cx = inner.x + inner.width / 2f
        val cy = inner.y + inner.height / 2f

        // Act
        val innerId = hitTestPlainEntityId(schema, Offset(cx, cy))
        val outerId = hitTestPlainEntityId(schema, Offset(pos.x + 2f, pos.y + 2f))

        // Assert
        assertNull(innerId)
        assertNull(outerId)
    }

    @Test
    fun hitTestAutoSelfRelationshipToolHoverPick_empty_returns_null() {
        // Arrange
        val schema = ConceptualSchema()

        // Act
        val pick = hitTestAutoSelfRelationshipToolHoverPick(schema, Offset(10f, 10f))

        // Assert
        assertNull(pick)
    }

    @Test
    fun hitTestAutoSelfRelationshipToolHoverPick_entity_centre_returns_pick() {
        // Arrange
        val ent = SchemaElement.Entity(id = 1, name = "A", position = ElementPosition(10, 20, 100, 90))
        val schema = ConceptualSchema(elements = mutableMapOf(1 to ent), nextId = 10)

        // Act
        val pick = hitTestAutoSelfRelationshipToolHoverPick(schema, Offset(60f, 65f))

        // Assert
        assertNotNull(pick)
        assertEquals(1, pick.elementId)
        assertFalse(pick.isAssociativeOuterEntitySide)
    }

    @Test
    fun hitTestAutoSelfRelationshipToolHoverPick_associative_inner_maps_to_outer_pick() {
        // Arrange
        val pos = ElementPosition(0, 0, 100, 80)
        val assoc = SchemaElement.AssociativeEntity(id = 7, name = "EA", position = pos)
        val schema = ConceptualSchema(elements = mutableMapOf(7 to assoc), nextId = 20)
        val inner = ElementPosition(
            x = pos.x + 15,
            y = pos.y + 15,
            width = (pos.width - 30).coerceAtLeast(10),
            height = (pos.height - 30).coerceAtLeast(10),
        )
        val cx = inner.x + inner.width / 2f
        val cy = inner.y + inner.height / 2f

        // Act
        val pick = hitTestAutoSelfRelationshipToolHoverPick(schema, Offset(cx, cy))

        // Assert
        assertNotNull(pick)
        assertEquals(7, pick.elementId)
        assertTrue(pick.isAssociativeOuterEntitySide)
    }

    @Test
    fun normalizeConceptualLinkPickForAutoSelfRelationshipTool_associative_inner_becomes_outer() {
        // Arrange
        val pos = ElementPosition(0, 0, 100, 80)
        val assoc = SchemaElement.AssociativeEntity(id = 7, name = "EA", position = pos)
        val schema = ConceptualSchema(elements = mutableMapOf(7 to assoc))
        val innerPick = ConceptualLinkPick(elementId = 7, isAssociativeOuterEntitySide = false)

        // Act
        val normalized = normalizeConceptualLinkPickForAutoSelfRelationshipTool(schema, innerPick)

        // Assert
        assertEquals(7, normalized.elementId)
        assertTrue(normalized.isAssociativeOuterEntitySide)
    }

    @Test
    fun associative_auto_self_inner_raw_validate_errors_normalized_matches_outer_body() {
        // Arrange
        val pos = ElementPosition(0, 0, 100, 80)
        val assoc = SchemaElement.AssociativeEntity(id = 7, name = "EA", position = pos)
        val schema = ConceptualSchema(elements = mutableMapOf(7 to assoc), nextId = 20)
        val inner = ElementPosition(
            x = pos.x + 15,
            y = pos.y + 15,
            width = (pos.width - 30).coerceAtLeast(10),
            height = (pos.height - 30).coerceAtLeast(10),
        )
        val cx = inner.x + inner.width / 2f
        val cy = inner.y + inner.height / 2f
        val click = Offset(cx, cy)
        val rawInnerPick = hitTestConceptualLinkPick(schema, click)
        assertNotNull(rawInnerPick)
        assertFalse(rawInnerPick.isAssociativeOuterEntitySide)
        val outerPick = ConceptualLinkPick(elementId = 7, isAssociativeOuterEntitySide = true)

        // Act
        val err = validateAndBuildConceptualLink(schema, rawInnerPick, rawInnerPick, click)
        val okInnerNormalized =
            validateAndBuildConceptualLink(
                schema,
                normalizeConceptualLinkPickForAutoSelfRelationshipTool(schema, rawInnerPick),
                normalizeConceptualLinkPickForAutoSelfRelationshipTool(schema, rawInnerPick),
                click,
            )
        val okOuter = validateAndBuildConceptualLink(schema, outerPick, outerPick, click)

        // Assert
        assertIs<ConceptualLinkValidationResult.Error>(err)
        val okA = assertIs<ConceptualLinkValidationResult.Ok>(okInnerNormalized)
        val okB = assertIs<ConceptualLinkValidationResult.Ok>(okOuter)
        assertEquals(okB.schema.selfRelationships.map { it.position }, okA.schema.selfRelationships.map { it.position })
        assertEquals(okB.schema.connections.size, okA.schema.connections.size)
    }

    @Test
    fun hitTestAutoSelfRelationshipToolHoverPick_associative_outer_returns_outer_pick() {
        // Arrange
        val pos = ElementPosition(0, 0, 100, 80)
        val assoc = SchemaElement.AssociativeEntity(id = 7, name = "EA", position = pos)
        val schema = ConceptualSchema(elements = mutableMapOf(7 to assoc), nextId = 20)

        // Act
        val pick = hitTestAutoSelfRelationshipToolHoverPick(schema, Offset(pos.x + 2f, pos.y + 2f))

        // Assert
        assertNotNull(pick)
        assertEquals(7, pick.elementId)
        assertTrue(pick.isAssociativeOuterEntitySide)
    }
}
