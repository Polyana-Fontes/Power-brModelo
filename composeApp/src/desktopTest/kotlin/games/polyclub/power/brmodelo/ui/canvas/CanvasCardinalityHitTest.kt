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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.Cardinality
import games.polyclub.power.brmodelo.domain.ConceptualLinkPick
import games.polyclub.power.brmodelo.domain.ConceptualLinkValidationResult
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.Connection
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.domain.validateAndBuildConceptualLink
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private fun headlessTextMeasurer(): TextMeasurer {
    val density = Density(density = 1f, fontScale = 1f)
    val resolver = createFontFamilyResolver()
    return TextMeasurer(resolver, density, LayoutDirection.Ltr)
}

/** Single new connection from entity–relationship link (not entity–entity, which adds two). */
private fun singleNewConnection(before: ConceptualSchema, ok: ConceptualLinkValidationResult.Ok): Connection {
    val after = ok.schema
    val newIds = after.connections.map { it.id }.toSet() - before.connections.map { it.id }.toSet()
    return after.connections.single { it.id in newIds }
}

class CanvasCardinalityHitTest {

    @Test
    fun `hitTestCardinality matches label center after enrich like link creation`() {
        // Arrange
        val textMeasurer = headlessTextMeasurer()
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
        )
        val conn = singleNewConnection(base, assertIs<ConceptualLinkValidationResult.Ok>(ok))
        val schemaRaw = base.copy(connections = listOf(conn))
        val enriched = enrichConnectionWithInitialCardinalityPosition(schemaRaw, conn, textMeasurer)
        assertTrue(enriched.cardinalityPosition != null)
        val schema = schemaRaw.copy(connections = listOf(enriched))

        val rect = cardinalityLabelInteractionRect(schema, enriched, textMeasurer)
        assertTrue(rect != null, "expected interaction rect for cardinality label")

        // Act
        val center = Offset((rect!!.left + rect.right) / 2f, (rect.top + rect.bottom) / 2f)
        val hit = hitTestCardinality(schema, center, textMeasurer)

        // Assert
        val card = assertIs<CanvasSelection.Cardinality>(hit)
        assertTrue(card.connectionId == enriched.id)
    }

    @Test
    fun `hitTestCardinality tolerates points near label edge inside padded rect`() {
        // Arrange
        val textMeasurer = headlessTextMeasurer()
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
        )
        val conn = singleNewConnection(base, assertIs<ConceptualLinkValidationResult.Ok>(ok))
        val schemaRaw = base.copy(connections = listOf(conn))
        val enriched = enrichConnectionWithInitialCardinalityPosition(schemaRaw, conn, textMeasurer)
        val schema = schemaRaw.copy(connections = listOf(enriched))
        val rect = cardinalityLabelInteractionRect(schema, enriched, textMeasurer)!!

        // Act — slightly inside the left edge (padding should include this)
        val nearLeft = Offset(rect.left + 2f, (rect.top + rect.bottom) / 2f)
        val hit = hitTestCardinality(schema, nearLeft, textMeasurer)

        // Assert
        val card = assertIs<CanvasSelection.Cardinality>(hit)
        assertTrue(card.connectionId == enriched.id)
        assertTrue(abs(nearLeft.x - rect.left) < 8f)
    }

    @Test
    fun `materializeCardinalityPositionForFixed yields stored box so highlight uses file-style bounds`() {
        // Arrange
        val textMeasurer = headlessTextMeasurer()
        val entity = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(400, 200, 120, 70))
        val rel = SchemaElement.Relationship(id = 2, name = "R", position = ElementPosition(80, 200, 90, 55))
        val base = ConceptualSchema(elements = mapOf(1 to entity, 2 to rel), nextId = 100)
        val ok = validateAndBuildConceptualLink(
            base,
            ConceptualLinkPick(2),
            ConceptualLinkPick(1),
        )
        val conn = singleNewConnection(base, assertIs<ConceptualLinkValidationResult.Ok>(ok))
        val schema = base.copy(connections = listOf(conn))
        assertTrue(conn.cardinalityPosition == null)

        // Act
        val materialized = materializeCardinalityPositionForFixed(schema, conn, textMeasurer)
        assertTrue(materialized != null)
        val schemaStored = schema.copy(
            connections = schema.connections.map {
                if (it.id == conn.id) it.copy(cardinalityPosition = materialized) else it
            },
        )
        val highlightPos = cardinalityLabelHighlightElementPosition(
            schemaStored,
            schemaStored.connections.first { it.id == conn.id },
            textMeasurer,
        )

        // Assert
        assertTrue(highlightPos == materialized)
    }

    @Test
    fun withCardinalityPositionsAfterElementsMovedByDelta_translatesFixedCardinalityBox() {
        // Arrange
        val textMeasurer = headlessTextMeasurer()
        val entity = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(10, 5, 100, 50))
        val rel = SchemaElement.Relationship(id = 2, name = "R", position = ElementPosition(200, 0, 80, 50))
        val conn = Connection(
            id = 10,
            elementIdA = 2,
            elementIdB = 1,
            cardinality = Cardinality.ONE_TO_ONE,
            showCardinality = true,
            cardinalityFixed = true,
            cardinalityPosition = ElementPosition(150, 80, 36, 20),
            cardinalityAutoSize = false,
        )
        val schemaAfterMove = ConceptualSchema(
            elements = mapOf(1 to entity, 2 to rel),
            connections = listOf(conn),
        )

        // Act
        val next = schemaAfterMove.withCardinalityPositionsAfterElementsMovedByDelta(
            movedElementIds = setOf(1),
            dx = 10,
            dy = 5,
            selectedCardinalityConnectionIds = emptySet(),
            textMeasurer = textMeasurer,
        )

        // Assert
        val pos = next.connections.single().cardinalityPosition!!
        assertEquals(160, pos.x)
        assertEquals(85, pos.y)
    }

    @Test
    fun withRecalculatedFloatingCardinalityPositions_overwritesStaleFloatingCardinalityBox() {
        // Arrange — two legs from one relationship; then corrupt the first leg's stored label (simulates stale geometry)
        val tm = headlessTextMeasurer()
        val rel = SchemaElement.Relationship(3, "R", ElementPosition(200, 40, 90, 55))
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(120, 200, 90, 55))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(280, 200, 90, 55))
        val base = ConceptualSchema(elements = mapOf(1 to e1, 2 to e2, 3 to rel), nextId = 50)

        val ok1 = assertIs<ConceptualLinkValidationResult.Ok>(
            validateAndBuildConceptualLink(base, ConceptualLinkPick(3), ConceptualLinkPick(1)),
        )
        val ids0 = base.connections.map { it.id }.toSet()
        var s = ok1.schema
        val nc1 = s.connections.single { it.id !in ids0 }
        val c1e = enrichConnectionWithInitialCardinalityPosition(s, nc1, tm)
        s = s.copy(connections = s.connections.map { if (it.id == nc1.id) c1e else it })

        val ok2 = assertIs<ConceptualLinkValidationResult.Ok>(
            validateAndBuildConceptualLink(s, ConceptualLinkPick(3), ConceptualLinkPick(2)),
        )
        val ids1 = s.connections.map { it.id }.toSet()
        s = ok2.schema
        val nc2 = s.connections.single { it.id !in ids1 }
        val c2e = enrichConnectionWithInitialCardinalityPosition(s, nc2, tm)
        s = s.copy(connections = s.connections.map { if (it.id == nc2.id) c2e else it })

        val c1 = s.connections.first { it.elementIdB == 1 }
        val good = checkNotNull(c1.cardinalityPosition)
        val corrupted = good.copy(x = good.x + 500)
        s = s.copy(connections = s.connections.map { if (it.id == c1.id) c1.copy(cardinalityPosition = corrupted) else it })

        // Act
        val sRecalc = s.withRecalculatedFloatingCardinalityPositions(textMeasurer = tm)
        val c1After = sRecalc.connections.first { it.elementIdB == 1 }

        // Assert
        val expected = materializeCardinalityPositionForFixed(sRecalc, c1After, tm)!!
        assertEquals(expected, c1After.cardinalityPosition)
        assertNotEquals(corrupted, c1After.cardinalityPosition)
    }

    @Test
    fun syncFloatingCardinalityLayoutAfterMutationFromBaseline_matchesPerElementCardinalitySync() {
        // Arrange — relationship–entity link with stored floating cardinality; entity moves and label is corrupted
        val tm = headlessTextMeasurer()
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(10, 20, 90, 55))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(280, 200, 90, 55))
        val rel = SchemaElement.Relationship(3, "R", ElementPosition(150, 40, 85, 50))
        val baseline = ConceptualSchema(elements = mapOf(1 to e1, 2 to e2, 3 to rel), nextId = 100)
        val ok = assertIs<ConceptualLinkValidationResult.Ok>(
            validateAndBuildConceptualLink(baseline, ConceptualLinkPick(3), ConceptualLinkPick(1)),
        )
        var withConn = ok.schema
        val newConn = withConn.connections.single()
        val enriched = enrichConnectionWithInitialCardinalityPosition(withConn, newConn, tm)
        withConn = withConn.copy(
            connections = withConn.connections.map { if (it.id == newConn.id) enriched else it },
        )
        val e1Moved = e1.copy(position = e1.position.copy(x = e1.position.x + 30, y = e1.position.y + 10))
        val mutated = withConn.withElement(e1Moved).copy(
            connections = withConn.connections.map { c ->
                val p = c.cardinalityPosition
                if (p != null) c.copy(cardinalityPosition = p.copy(x = p.x + 999)) else c
            },
        )

        // Act
        val synced = mutated.syncFloatingCardinalityLayoutAfterMutationFromBaseline(
            baseline = withConn,
            textMeasurer = tm,
            rehomeConnectionsAbsentInBaseline = false,
        )
        val sequential = mutated.afterCardinalitySyncForElementBoundsChange(1, e1.position, tm)

        // Assert
        assertEquals(
            sequential.connections.single().cardinalityPosition,
            synced.connections.single().cardinalityPosition,
        )
    }
}
