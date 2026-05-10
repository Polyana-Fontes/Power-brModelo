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
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConceptualLinkObjectsTest {

    private val pos = ElementPosition(0, 0, 100, 80)

    @Test
    fun `validate rejects inner associative end linked to outer of same associative`() {
        // Arrange
        val assoc = SchemaElement.AssociativeEntity(
            id = 1,
            name = "EntAssoc1",
            position = pos,
            relationshipName = "R",
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to assoc),
            nextId = 10,
        )
        val inner = ConceptualLinkPick(1, isAssociativeOuterEntitySide = false)
        val outer = ConceptualLinkPick(1, isAssociativeOuterEntitySide = true)

        // Act
        val r = validateAndBuildConceptualLink(schema, inner, outer, newConnectionId = 99)

        // Assert
        val err = assertIs<ConceptualLinkValidationResult.Error>(r)
        assertTrue(err.message.isNotBlank())
    }

    @Test
    fun `validate builds connection with outer entity end on associative when ordered`() {
        // Arrange
        val assoc = SchemaElement.AssociativeEntity(id = 1, name = "EA", position = pos)
        val rel = SchemaElement.Relationship(id = 2, name = "Rel", position = ElementPosition(200, 0, 80, 60))
        val schema = ConceptualSchema(
            elements = mapOf(1 to assoc, 2 to rel),
            nextId = 10,
        )
        val outer = ConceptualLinkPick(1, isAssociativeOuterEntitySide = true)
        val relPick = ConceptualLinkPick(2, isAssociativeOuterEntitySide = false)

        // Act
        val r = validateAndBuildConceptualLink(schema, outer, relPick, newConnectionId = 50)

        // Assert
        val ok = assertIs<ConceptualLinkValidationResult.Ok>(r)
        assertTrue(ok.connection.useAssociativeOuterForEndB)
        assertTrue(!ok.connection.useAssociativeOuterForEndA)
        assertTrue(ok.connection.elementIdA == 2)
        assertTrue(ok.connection.elementIdB == 1)
    }
}
