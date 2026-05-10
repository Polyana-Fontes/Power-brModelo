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

import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaXmlParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ConceptualPlacementTest {

    private fun loadResource(name: String): ByteArray =
        checkNotNull(javaClass.classLoader.getResourceAsStream(name)) {
            "Test resource '$name' not found on classpath"
        }.readBytes()

    @Test
    fun `defaults match valores-padroes xml fixture`() {
        // Arrange
        val bytes = loadResource("valores-padroes.xml")

        // Act
        val schema = ConceptualSchemaXmlParser.parse(bytes)
        val entidade = schema.entities.first { it.name == "Entidade1" }
        val relacao = schema.relationships.first { it.name == "Relacao1" }
        val assoc = schema.associativeEntities.first()
        val texto = schema.annotations.first()

        // Assert
        assertEquals(ConceptualPlacementDefaults.entityWidth, entidade.position.width)
        assertEquals(ConceptualPlacementDefaults.entityHeight, entidade.position.height)
        assertEquals(ConceptualPlacementDefaults.relationshipWidth, relacao.position.width)
        assertEquals(ConceptualPlacementDefaults.relationshipHeight, relacao.position.height)
        assertEquals(ConceptualPlacementDefaults.associativeOuterWidth, assoc.position.width)
        assertEquals(ConceptualPlacementDefaults.associativeOuterHeight, assoc.position.height)
        assertEquals(ConceptualPlacementDefaults.labelStyle.color, entidade.labelStyle.color)
        assertEquals(ConceptualPlacementDefaults.annotationWidth, texto.position.width)
        assertEquals(ConceptualPlacementDefaults.annotationHeight, texto.position.height)
        assertEquals(ConceptualPlacementDefaults.annotationColorArgb, texto.color)
        assertEquals(ConceptualPlacementDefaults.annotationType, texto.annotationType)
        assertEquals(ConceptualPlacementDefaults.annotationTextAlignment, texto.alignment)
        assertEquals(ConceptualPlacementDefaults.annotationAutoSize, texto.autoSize)
        assertEquals(ConceptualPlacementDefaults.annotationDefaultName, texto.name)
    }

    @Test
    fun `placeConceptualItem assigns incremental names and shared Relacao pool`() {
        // Arrange
        val empty = ConceptualSchema()

        // Act — two plain entities
        val (s1, id1) = empty.placeConceptualItem(ConceptualPlacementKind.PlainEntity, 10, 20)
        val (s2, id2) = s1.placeConceptualItem(ConceptualPlacementKind.PlainEntity, 30, 40)

        // Assert
        assertEquals("Entidade1", (s1.elements[id1] as SchemaElement.Entity).name)
        assertEquals("Entidade2", (s2.elements[id2] as SchemaElement.Entity).name)

        // Act — top-level relationship
        val (s3, id3) = s2.placeConceptualItem(ConceptualPlacementKind.Relationship, 0, 0)

        // Assert
        assertEquals("Relacao1", (s3.elements[id3] as SchemaElement.Relationship).name)

        // Act — associative uses EntAssoc1 and next free Relacao (2)
        val (s4, id4) = s3.placeConceptualItem(ConceptualPlacementKind.AssociativeEntity, 100, 100)
        val assoc = s4.elements[id4] as SchemaElement.AssociativeEntity

        // Assert
        assertEquals("EntAssoc1", assoc.name)
        assertEquals("Relacao2", assoc.relationshipName)

        // Act — another top-level relationship after inner name took Relacao2
        val (s5, id5) = s4.placeConceptualItem(ConceptualPlacementKind.Relationship, 5, 5)

        // Assert
        assertEquals("Relacao3", (s5.elements[id5] as SchemaElement.Relationship).name)
    }

    @Test
    fun `placeConceptualItem annotation always uses default name at click position`() {
        // Arrange
        val empty = ConceptualSchema()

        // Act
        val (s1, id1) = empty.placeConceptualItem(ConceptualPlacementKind.Annotation, 70, 192)
        val (s2, id2) = s1.placeConceptualItem(ConceptualPlacementKind.Annotation, 80, 200)
        val a1 = s2.elements[id1] as SchemaElement.Annotation
        val a2 = s2.elements[id2] as SchemaElement.Annotation

        // Assert
        assertEquals(ConceptualPlacementDefaults.annotationDefaultName, a1.name)
        assertEquals(ConceptualPlacementDefaults.annotationDefaultName, a2.name)
        assertEquals(70, a1.position.x)
        assertEquals(192, a1.position.y)
        assertEquals(80, a2.position.x)
        assertEquals(200, a2.position.y)
    }
}
