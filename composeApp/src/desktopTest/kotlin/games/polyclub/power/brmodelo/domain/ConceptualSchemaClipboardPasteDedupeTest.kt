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
import kotlin.test.assertNotEquals

class ConceptualSchemaClipboardPasteDedupeTest {

    @Test
    fun mergeTranslatedFragment_dedupesOnlyPastedSubtree_originalNamesUnchanged() {
        // Arrange — same shape as: new model, one entity, three simple attributes, select-all copy, paste elsewhere.
        val entityPos = ElementPosition(0, 0, 80, 40)
        val attrPos = ElementPosition(0, 50, 60, 20)
        val entity = SchemaElement.Entity(id = 1, name = "Entidade1", position = entityPos)
        val a1 = SchemaElement.Attribute(id = 2, name = "Atributo1", position = attrPos, ownerId = 1)
        val a2 = SchemaElement.Attribute(id = 3, name = "Atributo2", position = attrPos.copy(y = 72), ownerId = 1)
        val a3 = SchemaElement.Attribute(id = 4, name = "Atributo3", position = attrPos.copy(y = 94), ownerId = 1)
        val target = ConceptualSchema(
            elements = linkedMapOf(1 to entity, 2 to a1, 3 to a2, 4 to a3),
            nextId = 5,
        )
        val fragment = extractClipboardFragment(target, setOf(1, 2, 3, 4))!!
        val translated = translateConceptualSchema(fragment, dx = 200, dy = 200)

        // Act
        val (merged, pastedIds) = mergeTranslatedFragment(target, translated)

        // Assert — originals must keep exact names (bug was: lower ids processed first and got renamed).
        assertEquals("Entidade1", (merged.elements[1] as SchemaElement.Entity).name)
        assertEquals("Atributo1", (merged.elements[2] as SchemaElement.Attribute).name)
        assertEquals("Atributo2", (merged.elements[3] as SchemaElement.Attribute).name)
        assertEquals("Atributo3", (merged.elements[4] as SchemaElement.Attribute).name)

        val pastedEntityId = pastedIds.single { merged.elements[it] is SchemaElement.Entity }
        val pastedEntity = merged.elements[pastedEntityId] as SchemaElement.Entity
        assertNotEquals("Entidade1", pastedEntity.name)
        assertEquals("Entidade12", pastedEntity.name)

        val pastedAttrs = merged.attributes.filter { it.ownerId == pastedEntityId }.sortedBy { it.name }
        assertEquals(3, pastedAttrs.size)
        assertEquals(setOf("Atributo1", "Atributo2", "Atributo3"), pastedAttrs.map { it.name }.toSet())

        val ownerNamePairs = merged.attributes.map { it.ownerId to it.name }
        assertEquals(
            ownerNamePairs.size,
            ownerNamePairs.distinct().size,
            "No duplicate (owner, attribute name) pairs after paste",
        )
    }

    @Test
    fun mergeTranslatedFragment_attributeNamesDedupedPerOwnerNotGlobally() {
        // Arrange — target entity has "email"; pasted fragment brings another entity whose attribute is also "email"
        val e1 = SchemaElement.Entity(1, "Cliente", ElementPosition(0, 0, 90, 50))
        val e1Attr = SchemaElement.Attribute(2, "email", ElementPosition(-80, 10, 70, 18), ownerId = 1)
        val target = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e1Attr),
            connections = listOf(Connection(1, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL)),
            nextId = 3,
        )
        val e2 = SchemaElement.Entity(10, "Funcionario", ElementPosition(200, 0, 100, 50))
        val e2Attr = SchemaElement.Attribute(11, "email", ElementPosition(320, 10, 70, 18), ownerId = 10)
        val fragment = ConceptualSchema(
            elements = mapOf(10 to e2, 11 to e2Attr),
            connections = listOf(Connection(2, 11, 10, null, showCardinality = false, orientation = LineOrientation.VERTICAL)),
            nextId = 12,
        )
        val translated = translateConceptualSchema(fragment, dx = 0, dy = 120)

        // Act
        val (merged, _) = mergeTranslatedFragment(target, translated)

        // Assert — two different owners may both keep "email"
        val emails = merged.attributes.filter { it.name == "email" }
        assertEquals(2, emails.size)
        assertEquals(2, emails.map { it.ownerId }.distinct().size)
    }
}
