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

class ConceptualLinkMcpPatternTest {

    @Test
    fun classify_entityEntity_isNewRelationship() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 100, 80))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(200, 100, 100, 80))
        val before = ConceptualSchema(elements = mapOf(1 to e1, 2 to e2), nextId = 10)
        val after = assertIs<ConceptualLinkValidationResult.Ok>(
            validateAndBuildConceptualLink(before, ConceptualLinkPick(1), ConceptualLinkPick(2)),
        ).schema

        // Act
        val pattern = classifyMcpLinkObjectsPattern(before, after)

        // Assert
        assertEquals("entity_entity_new_relationship", pattern)
    }

    @Test
    fun classify_associativeOuterToRelationship_isOuterBridge() {
        // Arrange
        val pos = ElementPosition(0, 0, 100, 80)
        val assoc = SchemaElement.AssociativeEntity(id = 1, name = "EA", position = pos)
        val rel = SchemaElement.Relationship(id = 2, name = "Rel", position = ElementPosition(200, 0, 80, 60))
        val before = ConceptualSchema(elements = mapOf(1 to assoc, 2 to rel), nextId = 10)
        val after = assertIs<ConceptualLinkValidationResult.Ok>(
            validateAndBuildConceptualLink(
                before,
                ConceptualLinkPick(1, isAssociativeOuterEntitySide = true),
                ConceptualLinkPick(2),
            ),
        ).schema

        // Act
        val pattern = classifyMcpLinkObjectsPattern(before, after)

        // Assert
        assertEquals("entity_associative_outer_bridge", pattern)
    }
}
