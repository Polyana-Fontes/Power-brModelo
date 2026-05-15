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
import kotlin.test.assertTrue

class ConceptualLayoutQualityTest {

    @Test
    fun segmentsIntersectProper_detectsCrossingDiagonals() {
        // Arrange
        val ax = 0
        val ay = 0
        val bx = 100
        val by = 100
        val cx = 0
        val cy = 100
        val dx = 100
        val dy = 0

        // Act
        val hit = segmentsIntersectProper(ax, ay, bx, by, cx, cy, dx, dy)

        // Assert
        assertTrue(hit)
    }

    @Test
    fun segmentsIntersectProper_disjointParallelSegments() {
        // Arrange
        val ax = 0
        val ay = 0
        val bx = 100
        val ay2 = 0
        val cx = 0
        val cy = 10
        val dx = 100
        val dy = 10

        // Act
        val hit = segmentsIntersectProper(ax, ay, bx, ay2, cx, cy, dx, dy)

        // Assert
        assertFalse(hit)
    }

    @Test
    fun analyzeConceptualLayoutQuality_detectsBoxOverlap() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 40, 40))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(20, 20, 40, 40))
        val schema = ConceptualSchema(elements = mapOf(1 to e1, 2 to e2), nextId = 3)

        // Act
        val report = analyzeConceptualLayoutQuality(schema, null)

        // Assert
        assertTrue(report.overlaps.any { it.elementIdA == 1 && it.elementIdB == 2 })
        assertTrue(report.hasAnyIssue)
    }

    @Test
    fun analyzeConceptualLayoutQuality_detectsTightClearanceWithoutOverlap() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 40, 40))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(45, 0, 40, 40))
        val schema = ConceptualSchema(elements = mapOf(1 to e1, 2 to e2), nextId = 3)

        // Act
        val report = analyzeConceptualLayoutQuality(schema, null)

        // Assert
        assertEquals(0, report.overlaps.size)
        assertTrue(report.tightClearances.any { it.gapPx == 5 })
    }

    @Test
    fun analyzeConceptualLayoutQuality_scopeExcludesNonTouchingPairs() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 40, 40))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(20, 20, 40, 40))
        val e3 = SchemaElement.Entity(3, "C", ElementPosition(500, 500, 40, 40))
        val schema = ConceptualSchema(elements = mapOf(1 to e1, 2 to e2, 3 to e3), nextId = 4)

        // Act
        val report = analyzeConceptualLayoutQuality(schema, setOf(3))

        // Assert
        assertEquals(0, report.overlaps.size)
        assertFalse(report.hasAnyIssue)
    }

    @Test
    fun analyzeConceptualLayoutQuality_detectsCrossingCenterSegments() {
        // Arrange — centers (20,20)-(80,80) vs (20,80)-(80,20)
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 40, 40))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(60, 60, 40, 40))
        val e3 = SchemaElement.Entity(3, "C", ElementPosition(0, 60, 40, 40))
        val e4 = SchemaElement.Entity(4, "D", ElementPosition(60, 0, 40, 40))
        val c1 = Connection(id = 10, elementIdA = 1, elementIdB = 2, showCardinality = false)
        val c2 = Connection(id = 11, elementIdA = 3, elementIdB = 4, showCardinality = false)
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to e3, 4 to e4),
            connections = listOf(c1, c2),
            nextId = 12,
        )

        // Act
        val report = analyzeConceptualLayoutQuality(schema, null)

        // Assert
        assertTrue(report.lineCrossings.any { it.connectionIdA == 10 && it.connectionIdB == 11 })
    }

    @Test
    fun conceptualLayoutAgentSignals_overlap_setsHintSpacing() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 40, 40))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(20, 20, 40, 40))
        val schema = ConceptualSchema(elements = mapOf(1 to e1, 2 to e2), nextId = 3)
        val report = analyzeConceptualLayoutQuality(schema, null)

        // Act
        val signals = conceptualLayoutAgentSignals(report, schema)

        // Assert
        assertTrue(signals.hasBlockingOverlap)
        assertEquals(listOf(1, 2), signals.affectedElementIds)
        assertEquals("spacing", signals.agentHint)
    }

    @Test
    fun conceptualLayoutAgentSignals_crossing_withoutOverlap_usesRoutingHintAndSchemaEndpoints() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 40, 40))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(60, 60, 40, 40))
        val e3 = SchemaElement.Entity(3, "C", ElementPosition(0, 60, 40, 40))
        val e4 = SchemaElement.Entity(4, "D", ElementPosition(60, 0, 40, 40))
        val c1 = Connection(id = 10, elementIdA = 1, elementIdB = 2, showCardinality = false)
        val c2 = Connection(id = 11, elementIdA = 3, elementIdB = 4, showCardinality = false)
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to e3, 4 to e4),
            connections = listOf(c1, c2),
            nextId = 12,
        )
        val report = analyzeConceptualLayoutQuality(schema, null)

        // Act
        val signals = conceptualLayoutAgentSignals(report, schema)

        // Assert
        assertFalse(signals.hasBlockingOverlap)
        assertEquals(listOf(1, 2, 3, 4), signals.affectedElementIds)
        assertEquals("routing", signals.agentHint)
    }
}
