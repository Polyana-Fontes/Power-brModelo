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
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.ConceptualLinkPick
import games.polyclub.power.brmodelo.domain.ConceptualLinkValidationResult
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.domain.validateAndBuildConceptualLink
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CanvasCardinalityHitTest {

    @Test
    fun `hitTestCardinality matches fallback label when cardinalityPosition is null`() {
        // Arrange
        val entity = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(400, 200, 120, 70))
        val rel = SchemaElement.Relationship(id = 2, name = "R", position = ElementPosition(80, 200, 90, 55))
        val base = ConceptualSchema(
            elements = mapOf(1 to entity, 2 to rel),
            nextId = 100,
        )
        val ok = validateAndBuildConceptualLink(
            base,
            ConceptualLinkPick(2),
            ConceptualLinkPick(1),
            newConnectionId = 50,
        )
        val conn = assertIs<ConceptualLinkValidationResult.Ok>(ok).connection
        assertTrue(conn.cardinalityPosition == null)
        val schema = base.copy(connections = listOf(conn))

        val rect = cardinalityLabelInteractionRect(schema, conn)
        assertTrue(rect != null, "expected a fallback interaction rect for new links")

        // Act
        val center = Offset((rect!!.left + rect.right) / 2f, (rect.top + rect.bottom) / 2f)
        val hit = hitTestCardinality(schema, center)

        // Assert
        val card = assertIs<CanvasSelection.Cardinality>(hit)
        assertTrue(card.connectionId == conn.id)
    }

    @Test
    fun `hitTestCardinality tolerates points near label edge inside padded rect`() {
        // Arrange
        val entity = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(300, 150, 100, 60))
        val rel = SchemaElement.Relationship(id = 2, name = "R", position = ElementPosition(40, 150, 80, 50))
        val base = ConceptualSchema(
            elements = mapOf(1 to entity, 2 to rel),
            nextId = 100,
        )
        val ok = validateAndBuildConceptualLink(
            base,
            ConceptualLinkPick(2),
            ConceptualLinkPick(1),
            newConnectionId = 51,
        )
        val conn = assertIs<ConceptualLinkValidationResult.Ok>(ok).connection
        val schema = base.copy(connections = listOf(conn))
        val rect = cardinalityLabelInteractionRect(schema, conn)!!

        // Act — slightly inside the left edge (padding should include this)
        val nearLeft = Offset(rect.left + 2f, (rect.top + rect.bottom) / 2f)
        val hit = hitTestCardinality(schema, nearLeft)

        // Assert
        val card = assertIs<CanvasSelection.Cardinality>(hit)
        assertTrue(card.connectionId == conn.id)
        assertTrue(abs(nearLeft.x - rect.left) < 8f)
    }

    @Test
    fun `materializeCardinalityPositionForFixed yields stored box so highlight uses file-style bounds`() {
        // Arrange
        val entity = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(400, 200, 120, 70))
        val rel = SchemaElement.Relationship(id = 2, name = "R", position = ElementPosition(80, 200, 90, 55))
        val base = ConceptualSchema(elements = mapOf(1 to entity, 2 to rel), nextId = 100)
        val ok = validateAndBuildConceptualLink(
            base,
            ConceptualLinkPick(2),
            ConceptualLinkPick(1),
            newConnectionId = 52,
        )
        val conn = assertIs<ConceptualLinkValidationResult.Ok>(ok).connection
        val schema = base.copy(connections = listOf(conn))
        assertTrue(conn.cardinalityPosition == null)

        // Act
        val materialized = materializeCardinalityPositionForFixed(schema, conn)
        assertTrue(materialized != null)
        val schemaStored = schema.copy(
            connections = schema.connections.map {
                if (it.id == conn.id) it.copy(cardinalityPosition = materialized) else it
            },
        )
        val highlightPos = cardinalityLabelHighlightElementPosition(
            schemaStored,
            schemaStored.connections.first { it.id == conn.id },
        )

        // Assert
        assertTrue(highlightPos == materialized)
    }
}
