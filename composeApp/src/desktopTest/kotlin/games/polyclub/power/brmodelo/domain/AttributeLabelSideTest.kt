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

class AttributeLabelSideTest {

    @Test
    fun `effective label side west of owner matches Pascal MePonto 3 bullet right`() {
        // Arrange — attribute strictly left of owner box (owner sector 1)
        val owner = ElementPosition(100, 100, 80, 60)
        val attrWest = ElementPosition(10, 110, 70, 16)

        // Act
        val effective = effectiveAttributeLabelSide(owner, attrWest, AttributeLabelSide.BULLET_LEFT)

        // Assert — stub leaves the attribute’s right edge → OrientacaoD / Direito
        assertEquals(AttributeLabelSide.BULLET_RIGHT, effective)
    }

    @Test
    fun `effective label side east of owner matches Pascal MePonto 1 bullet left`() {
        // Arrange — attribute strictly right of owner (owner sector 3)
        val owner = ElementPosition(100, 100, 80, 60)
        val attrEast = ElementPosition(200, 110, 70, 16)

        // Act
        val effective = effectiveAttributeLabelSide(owner, attrEast, AttributeLabelSide.BULLET_RIGHT)

        // Assert — stub leaves the attribute’s left edge → OrientacaoE / Esquerdo
        assertEquals(AttributeLabelSide.BULLET_LEFT, effective)
    }

    @Test
    fun `effective label side above owner keeps stored side`() {
        // Arrange — attribute above owner (owner sector 2); Paint does not force 1 or 3
        val owner = ElementPosition(100, 100, 80, 60)
        val attrAbove = ElementPosition(110, 50, 70, 16)
        val stored = AttributeLabelSide.BULLET_RIGHT

        // Act
        val effective = effectiveAttributeLabelSide(owner, attrAbove, stored)

        // Assert
        assertEquals(stored, effective)
    }

    @Test
    fun `Pascal MER composite Direito with child Esquerdo bar meets child left stub`() {
        // Arrange — same pattern as `MER-PousadaSolDaManha-uf-movido2.xml`: parent `<Orientacao Valor="2"/>`,
        // child `uf` `<Orientacao Valor="3"/>`. Bar→child X must follow the child’s stub, not the opposite bbox edge.
        val entity = SchemaElement.Entity(1, "Funcionario", ElementPosition(200, 200, 100, 66))
        val composite = SchemaElement.Attribute(
            id = 2,
            name = "endereco",
            position = ElementPosition(100, 265, 72, 16),
            ownerId = 1,
            labelSide = AttributeLabelSide.BULLET_RIGHT,
            childAttributeIds = listOf(3),
            autoSize = false,
        )
        val uf = SchemaElement.Attribute(
            id = 3,
            name = "uf",
            position = ElementPosition(101, 202, 37, 16),
            ownerId = 2,
            labelSide = AttributeLabelSide.BULLET_LEFT,
            autoSize = false,
        )
        val schema = ConceptualSchema(elements = mapOf(1 to entity, 2 to composite, 3 to uf))

        // Act
        val x = compositeChildBarConnectionX(schema, composite, uf)

        // Assert — stub on the left (Esquerdo): horizontal leg ends at Left, not Right (would cross the label)
        assertEquals(101f, x)
    }
}
