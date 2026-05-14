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
import kotlin.test.assertTrue

class ConceptualSchemaSearchTest {

    @Test
    fun `foldStringForSearch strips diacritics and lowercases`() {
        // Arrange
        val raw = "Ônibus FUNção"

        // Act
        val folded = foldStringForSearch(raw)

        // Assert
        assertEquals("onibus funcao", folded)
        assertTrue(containsFolded("ônibus", "onibus"))
    }

    @Test
    fun `empty query lists every entity when only entity filter is on`() {
        // Arrange
        val (s1, e1) = ConceptualSchema().placeConceptualItem(ConceptualPlacementKind.PlainEntity, 0, 0)
        val (s2, e2) = s1.placeConceptualItem(ConceptualPlacementKind.PlainEntity, 120, 0)
        val filters = ConceptualSearchTypeFilters(
            includeEntities = true,
            includeRelationships = false,
            includeAssociativeEntities = false,
            includeSpecializations = false,
            includeCanvasAttributes = false,
            includeHiddenAttributes = false,
            includeCardinalityLabels = false,
            includeObservationBoxes = false,
        )
        val scope = ConceptualSearchTextScope(searchDictionary = false, searchObservations = false)

        // Act
        val r = s2.searchConceptualModel("", filters, scope)

        // Assert
        val ok = assertIs<ConceptualSearchOutcome.Ok>(r)
        val ids = ok.result.hits.filterIsInstance<ConceptualSearchHit.ElementHit>().map { it.elementId }.toSet()
        assertEquals(setOf(e1, e2), ids)
    }
    @Test
    fun `no type selected behaves as all types`() {
        // Arrange
        val (s, id) = ConceptualSchema().placeConceptualItem(ConceptualPlacementKind.PlainEntity, 0, 0)
        val ent = s.elements[id] as SchemaElement.Entity
        val schema = s.withElement(ent.copy(name = "ClienteMista"))
        val noneSelected = ConceptualSearchTypeFilters(
            includeEntities = false,
            includeRelationships = false,
            includeAssociativeEntities = false,
            includeSpecializations = false,
            includeCanvasAttributes = false,
            includeHiddenAttributes = false,
            includeCardinalityLabels = false,
            includeObservationBoxes = false,
        )
        val scope = ConceptualSearchTextScope(searchDictionary = false, searchObservations = false)

        // Act
        val r = schema.searchConceptualModel("cliente", noneSelected, scope)

        // Assert
        val ok = assertIs<ConceptualSearchOutcome.Ok>(r)
        assertTrue(ok.result.hits.any { it is ConceptualSearchHit.ElementHit })
    }

    @Test
    fun `associative searches inner relationship name`() {
        // Arrange
        val (s0, id) = ConceptualSchema().placeConceptualItem(ConceptualPlacementKind.AssociativeEntity, 0, 0)
        val assoc = s0.elements[id] as SchemaElement.AssociativeEntity
        val schema = s0.withElement(assoc.copy(relationshipName = "ReservaInterna"))
        val filters = ConceptualSearchTypeFilters(
            includeEntities = false,
            includeRelationships = false,
            includeAssociativeEntities = true,
            includeSpecializations = false,
            includeCanvasAttributes = false,
            includeHiddenAttributes = false,
            includeCardinalityLabels = false,
            includeObservationBoxes = false,
        )
        val scope = ConceptualSearchTextScope(searchDictionary = false, searchObservations = false)

        // Act
        val r = schema.searchConceptualModel("reserva", filters, scope)

        // Assert
        val ok = assertIs<ConceptualSearchOutcome.Ok>(r)
        val hit = ok.result.hits.filterIsInstance<ConceptualSearchHit.ElementHit>().single()
        assertTrue(hit.matchedIn.contains("innerRelationshipName"))
    }

    @Test
    fun `attribute valueType is always searchable`() {
        // Arrange
        val (s1, rid) = ConceptualSchema().placeConceptualItem(ConceptualPlacementKind.Relationship, 200, 0)
        val rel = s1.elements[rid] as SchemaElement.Relationship
        val (s2, attrId) = s1.allocateId()
        val attr = SchemaElement.Attribute(
            id = attrId,
            name = "x",
            position = ElementPosition(10, 10, 40, 20),
            ownerId = rel.id,
            valueType = "VARCHAR",
            complement = "100",
        )
        val schema = s2.withElement(rel).withElement(attr)
        val filters = ConceptualSearchTypeFilters(
            includeEntities = false,
            includeRelationships = false,
            includeAssociativeEntities = false,
            includeSpecializations = false,
            includeCanvasAttributes = true,
            includeHiddenAttributes = false,
            includeCardinalityLabels = false,
            includeObservationBoxes = false,
        )
        val scope = ConceptualSearchTextScope(searchDictionary = false, searchObservations = false)

        // Act
        val r = schema.searchConceptualModel("varchar", filters, scope)

        // Assert
        val ok = assertIs<ConceptualSearchOutcome.Ok>(r)
        val hit = ok.result.hits.filterIsInstance<ConceptualSearchHit.ElementHit>().single { it.elementId == attrId }
        assertTrue(hit.matchedIn.contains("valueType"))
    }
}
