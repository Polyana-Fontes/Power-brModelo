/*
 * KbrModelo - Kotlin port of brModelo 3.0 originally written in Pascal
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

package games.polyclub.kbrmodelo.domain.serialization

import games.polyclub.kbrmodelo.domain.*
import kotlin.test.*

/**
 * Unit tests for [ConceptualSchemaXmlParser] and [ConceptualSchemaXmlSerializer].
 *
 * Two fixture files are used:
 * - `exemplo-simples.xml` — one entity with composite attribute, one entity,
 *   one relationship. Small and easy to assert exhaustively.
 * - `MER-PousadaSolDaManha.xml` — a real-world hotel management model with
 *   specializations, auto-relationships and associative entities.
 */
class ConceptualSchemaXmlTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun loadResource(name: String): ByteArray =
        checkNotNull(javaClass.classLoader.getResourceAsStream(name)) {
            "Test resource '$name' not found on classpath"
        }.readBytes()

    private fun parseResource(name: String): ConceptualSchema =
        ConceptualSchemaXmlParser.parse(loadResource(name))

    // ─────────────────────────────────────────────────────────────────────────
    // SIMPLE XML
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse simples - metadata`() {
        // Arrange
        val bytes = loadResource("exemplo-simples.xml")

        // Act
        val schema = ConceptualSchemaXmlParser.parse(bytes)

        // Assert
        assertEquals("2.0.0", schema.version)
        assertEquals("", schema.author)
        assertEquals("", schema.observations)
    }

    @Test
    fun `parse simples - entity count`() {
        // Act
        val schema = parseResource("exemplo-simples.xml")

        // Assert
        assertEquals(2, schema.entities.size, "expected 2 entities")
        assertEquals(0, schema.associativeEntities.size)
        assertEquals(0, schema.specializations.size)
        assertEquals(0, schema.selfRelationships.size)
    }

    @Test
    fun `parse simples - relationship count`() {
        // Act
        val schema = parseResource("exemplo-simples.xml")

        // Assert
        assertEquals(1, schema.relationships.size, "expected 1 relationship")
    }

    @Test
    fun `parse simples - Entidade1 properties`() {
        // Arrange
        val schema = parseResource("exemplo-simples.xml")

        // Act
        val entity = schema.elements[1] as? SchemaElement.Entity

        // Assert
        assertNotNull(entity, "entity with id=1 must exist")
        assertEquals("Entidade1", entity.name)
        assertEquals(ElementPosition(x = 140, y = 106, width = 102, height = 66), entity.position)
        assertFalse(entity.isWeak)
    }

    @Test
    fun `parse simples - Entidade2 properties`() {
        // Arrange
        val schema = parseResource("exemplo-simples.xml")

        // Act
        val entity = schema.elements[18] as? SchemaElement.Entity

        // Assert
        assertNotNull(entity, "entity with id=18 must exist")
        assertEquals("Entidade2", entity.name)
        assertEquals(ElementPosition(x = 263, y = 268, width = 102, height = 66), entity.position)
    }

    @Test
    fun `parse simples - attribute count for Entidade1`() {
        // Arrange
        val schema = parseResource("exemplo-simples.xml")

        // Act — direct attributes (ids 2, 5, 8); composite children (11, 14) have ownerId=8
        val directAttrs = schema.attributesOf(1)

        // Assert
        assertEquals(3, directAttrs.size, "Entidade1 should have 3 direct attributes")
    }

    @Test
    fun `parse simples - Atributo1 is not identifier`() {
        // Arrange
        val schema = parseResource("exemplo-simples.xml")

        // Act
        val attr = schema.elements[2] as? SchemaElement.Attribute

        // Assert
        assertNotNull(attr, "attribute id=2 must exist")
        assertEquals("Atributo1", attr.name)
        assertFalse(attr.isIdentifier, "Atributo1 Identificador=0 → false")
        assertEquals(1, attr.ownerId)
    }

    @Test
    fun `parse simples - Atributo2 is identifier`() {
        // Arrange
        val schema = parseResource("exemplo-simples.xml")

        // Act
        val attr = schema.elements[5] as? SchemaElement.Attribute

        // Assert
        assertNotNull(attr)
        assertEquals("Atributo2", attr.name)
        assertTrue(attr.isIdentifier, "Atributo2 Identificador=-1 → true")
        assertEquals(0, attr.multiValuedCount, "non-composite QtdeMultivalorado must normalize to 0")
    }

    @Test
    fun `parse simples - Atributo3 is composite with two children`() {
        // Arrange
        val schema = parseResource("exemplo-simples.xml")

        // Act
        val attr = schema.elements[8] as? SchemaElement.Attribute

        // Assert
        assertNotNull(attr)
        assertEquals("Atributo3", attr.name)
        assertTrue(attr.isComposite, "Composto=-1 → true")
        assertEquals(listOf(11, 14), attr.childAttributeIds)
        assertEquals(2, attr.multiValuedCount, "composite QtdeMultivalorado must equal component count")
        assertEquals(2, schema.canonicalQtdeMultivalorado(attr))
    }

    @Test
    fun `parse simples - composite children have correct ownerId`() {
        // Arrange
        val schema = parseResource("exemplo-simples.xml")

        // Act
        val child4 = schema.elements[11] as? SchemaElement.Attribute
        val child5 = schema.elements[14] as? SchemaElement.Attribute

        // Assert
        assertNotNull(child4)
        assertNotNull(child5)
        assertEquals(8, child4.ownerId, "Atributo4 owner should be composite Atributo3 (id=8)")
        assertEquals(8, child5.ownerId, "Atributo5 owner should be composite Atributo3 (id=8)")
    }

    @Test
    fun `parse simples - Relacao1 properties`() {
        // Arrange
        val schema = parseResource("exemplo-simples.xml")

        // Act
        val rel = schema.elements[19] as? SchemaElement.Relationship

        // Assert
        assertNotNull(rel)
        assertEquals("Relacao1", rel.name)
        assertEquals(ArrowDirection.NONE, rel.arrowDirection)
        assertEquals(ElementPosition(x = 162, y = 221, width = 102, height = 51), rel.position)
    }

    @Test
    fun `parse simples - Relacao1 has two connections`() {
        // Arrange
        val schema = parseResource("exemplo-simples.xml")

        // Act
        val conns = schema.connectionsOf(19).filter { it.elementIdA == 19 }

        // Assert
        assertEquals(2, conns.size, "Relacao1 must connect to 2 entities")
        val destIds = conns.map { it.elementIdB }.toSet()
        assertEquals(setOf(1, 18), destIds)
    }

    @Test
    fun `parse simples - Relacao1 cardinality is ZERO_TO_MANY and is shown`() {
        // Arrange
        val schema = parseResource("exemplo-simples.xml")

        // Act — connection from Relacao1 (19) to Entidade1 (1)
        val conn = schema.connections.first { it.elementIdA == 19 && it.elementIdB == 1 }

        // Assert
        assertEquals(Cardinality.ZERO_TO_MANY, conn.cardinality)
        assertTrue(conn.showCardinality)
        assertNotNull(conn.cardinalityPosition, "cardinality label position must be stored")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPLEX XML (Pousada Sol da Manhã)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `parse pousada - has multiple entities`() {
        // Act
        val schema = parseResource("MER-PousadaSolDaManha.xml")

        // Assert — file has many entities; exact count can be validated
        assertTrue(schema.entities.size >= 10, "expected at least 10 entities")
    }

    @Test
    fun `parse pousada - has relationships`() {
        // Act
        val schema = parseResource("MER-PousadaSolDaManha.xml")

        // Assert
        assertTrue(schema.relationships.size >= 1)
    }

    @Test
    fun `parse pousada - has at least one associative entity`() {
        // Act
        val schema = parseResource("MER-PousadaSolDaManha.xml")

        // Assert
        assertTrue(schema.associativeEntities.isNotEmpty(), "expected at least one EntidadeAssoss")
    }

    @Test
    fun `parse pousada - associative entity Trabalhar exists with attributes`() {
        // Arrange
        val schema = parseResource("MER-PousadaSolDaManha.xml")

        // Act
        val trabalhar = schema.associativeEntities.firstOrNull { it.name == "Trabalhar" }

        // Assert
        assertNotNull(trabalhar, "AssociativeEntity 'Trabalhar' must exist")
        assertEquals("Vincular", trabalhar.relationshipName)
        val attrs = schema.attributesOf(trabalhar.id)
        assertTrue(attrs.any { it.name == "dataInicio" }, "must have attribute 'dataInicio'")
        assertTrue(attrs.any { it.name == "dataFim" }, "must have attribute 'dataFim'")
    }

    @Test
    fun `parse pousada - specialization exists`() {
        // Act
        val schema = parseResource("MER-PousadaSolDaManha.xml")

        // Assert
        assertTrue(schema.specializations.isNotEmpty(), "expected at least one Especializacao")
    }

    @Test
    fun `parse pousada - specialization Esp1 has correct base entity`() {
        // Arrange
        val schema = parseResource("MER-PousadaSolDaManha.xml")

        // Act
        val esp = schema.specializations.firstOrNull { it.name == "Esp1" }

        // Assert
        assertNotNull(esp, "'Esp1' specialization must exist")
        // Base entity id is 204 (Hospede) per the XML
        assertEquals(204, esp.baseEntityId)
    }

    @Test
    fun `parse pousada - specialization Esp1 links to child entities`() {
        // Arrange
        val schema = parseResource("MER-PousadaSolDaManha.xml")
        val esp = schema.specializations.first { it.name == "Esp1" }

        // Act — child connections are those NOT going to the base entity
        val childConns = schema.connectionsOf(esp.id)
            .filter { it.elementIdA == esp.id && it.elementIdB != esp.baseEntityId }

        // Assert
        assertTrue(childConns.isNotEmpty(), "Esp1 must connect to at least one child entity")
    }

    @Test
    fun `parse pousada - self-relationship exists`() {
        // Act
        val schema = parseResource("MER-PousadaSolDaManha.xml")

        // Assert
        assertTrue(schema.selfRelationships.isNotEmpty(), "expected at least one AutoRelacao")
    }

    @Test
    fun `parse pousada - self-relationship Responsabilizar exists with correct owner`() {
        // Arrange
        val schema = parseResource("MER-PousadaSolDaManha.xml")

        // Act
        val sr = schema.selfRelationships.firstOrNull { it.name == "Responsabilizar" }

        // Assert
        assertNotNull(sr, "SelfRelationship 'Responsabilizar' must exist")
        val owner = schema.elements[sr.ownerEntityId]
        assertNotNull(owner, "Owner entity of Responsabilizar must exist")
    }

    @Test
    fun `parse pousada - dataFim attribute of Trabalhar is optional`() {
        // Arrange
        val schema = parseResource("MER-PousadaSolDaManha.xml")
        val trabalhar = schema.associativeEntities.first { it.name == "Trabalhar" }

        // Act
        val dataFim = schema.attributesOf(trabalhar.id).first { it.name == "dataFim" }

        // Assert — MinCard=0 in the XML
        assertTrue(dataFim.isOptional, "dataFim must be optional (MinCard=0)")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROUND-TRIP TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `round-trip simples - entities are preserved`() {
        // Arrange
        val original = parseResource("exemplo-simples.xml")

        // Act
        val xml = ConceptualSchemaXmlSerializer.serialize(original)
        val reloaded = ConceptualSchemaXmlParser.parse(xml.toByteArray(Charsets.UTF_8))

        // Assert
        assertEquals(original.entities.size, reloaded.entities.size, "entity count")
        original.entities.forEach { e ->
            val r = reloaded.elements[e.id] as? SchemaElement.Entity
            assertNotNull(r, "entity id=${e.id} must survive round-trip")
            assertEquals(e.name, r.name)
            assertEquals(e.position, r.position)
        }
    }

    @Test
    fun `round-trip simples - relationship preserved`() {
        // Arrange
        val original = parseResource("exemplo-simples.xml")

        // Act
        val reloaded = ConceptualSchemaXmlParser.parse(
            ConceptualSchemaXmlSerializer.serialize(original).toByteArray()
        )

        // Assert
        assertEquals(original.relationships.size, reloaded.relationships.size)
        original.relationships.forEach { rel ->
            val r = reloaded.elements[rel.id] as? SchemaElement.Relationship
            assertNotNull(r)
            assertEquals(rel.name, r.name)
            assertEquals(rel.arrowDirection, r.arrowDirection)
        }
    }

    @Test
    fun `round-trip simples - attributes preserved with correct ownerId`() {
        // Arrange
        val original = parseResource("exemplo-simples.xml")

        // Act
        val reloaded = ConceptualSchemaXmlParser.parse(
            ConceptualSchemaXmlSerializer.serialize(original).toByteArray()
        )

        // Assert — check all original attributes survive
        original.attributes.forEach { attr ->
            val r = reloaded.elements[attr.id] as? SchemaElement.Attribute
            assertNotNull(r, "attribute id=${attr.id} must survive round-trip")
            assertEquals(attr.name, r.name)
            assertEquals(attr.ownerId, r.ownerId)
            assertEquals(attr.isIdentifier, r.isIdentifier)
            assertEquals(attr.isComposite, r.isComposite)
        }
    }

    @Test
    fun `round-trip simples - connections preserved`() {
        // Arrange
        val original = parseResource("exemplo-simples.xml")

        // Act
        val reloaded = ConceptualSchemaXmlParser.parse(
            ConceptualSchemaXmlSerializer.serialize(original).toByteArray()
        )

        // Assert — compare connection topology (elementIdA → elementIdB pairs)
        val originalPairs = original.connections
            .map { it.elementIdA to it.elementIdB }.toSet()
        val reloadedPairs = reloaded.connections
            .map { it.elementIdA to it.elementIdB }.toSet()
        assertEquals(originalPairs, reloadedPairs, "connection topology must be preserved")
    }

    @Test
    fun `round-trip simples - cardinality values preserved`() {
        // Arrange
        val original = parseResource("exemplo-simples.xml")

        // Act
        val reloaded = ConceptualSchemaXmlParser.parse(
            ConceptualSchemaXmlSerializer.serialize(original).toByteArray()
        )

        // Assert — cardinality of Relacao1→Entidade1 connection
        val origConn = original.connections.first { it.elementIdA == 19 && it.elementIdB == 1 }
        val reConn = reloaded.connections.first { it.elementIdA == 19 && it.elementIdB == 1 }
        assertEquals(origConn.cardinality, reConn.cardinality)
        assertEquals(origConn.showCardinality, reConn.showCardinality)
    }

    @Test
    fun `round-trip pousada - entity count preserved`() {
        // Arrange
        val original = parseResource("MER-PousadaSolDaManha.xml")

        // Act
        val reloaded = ConceptualSchemaXmlParser.parse(
            ConceptualSchemaXmlSerializer.serialize(original).toByteArray()
        )

        // Assert
        assertEquals(original.entities.size, reloaded.entities.size, "entity count")
        assertEquals(original.relationships.size, reloaded.relationships.size, "relationship count")
        assertEquals(original.associativeEntities.size, reloaded.associativeEntities.size)
        assertEquals(original.specializations.size, reloaded.specializations.size)
        assertEquals(original.selfRelationships.size, reloaded.selfRelationships.size)
    }

    @Test
    fun `round-trip pousada - specialization base entity preserved`() {
        // Arrange
        val original = parseResource("MER-PousadaSolDaManha.xml")
        val origEsp = original.specializations.first { it.name == "Esp1" }

        // Act
        val reloaded = ConceptualSchemaXmlParser.parse(
            ConceptualSchemaXmlSerializer.serialize(original).toByteArray()
        )
        val reEsp = reloaded.specializations.first { it.name == "Esp1" }

        // Assert
        assertEquals(origEsp.baseEntityId, reEsp.baseEntityId)
        assertEquals(origEsp.isPartial, reEsp.isPartial)
    }

    @Test
    fun `round-trip pousada - self-relationship owner preserved`() {
        // Arrange
        val original = parseResource("MER-PousadaSolDaManha.xml")
        val origSr = original.selfRelationships.first { it.name == "Responsabilizar" }

        // Act
        val reloaded = ConceptualSchemaXmlParser.parse(
            ConceptualSchemaXmlSerializer.serialize(original).toByteArray()
        )
        val reSr = reloaded.selfRelationships.first { it.name == "Responsabilizar" }

        // Assert
        assertEquals(origSr.ownerEntityId, reSr.ownerEntityId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HIDDEN ATTRIBUTES — teste-varios-componentes
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `varios XML - MinhaEntidade has 7 hidden attributes`() {
        // Arrange
        val schema = parseResource("teste-varios-componentes.xml")

        // Act
        val entity = schema.entities.first { it.name == "MinhaEntidade" }

        // Assert
        assertEquals(7, entity.hiddenAttributes.size,
            "Expected 7 hidden attributes, got: ${entity.hiddenAttributes.map { it.name }}")
    }

    @Test
    fun `varios XML - hidden attribute names are preserved including duplicates and spaces`() {
        // Arrange
        val schema = parseResource("teste-varios-componentes.xml")
        val entity = schema.entities.first { it.name == "MinhaEntidade" }

        // Act
        val names = entity.hiddenAttributes.map { it.name }

        // Assert
        assertTrue("umAtributoOculto" in names)
        assertTrue("atributo2" in names)
        assertTrue(names.any { it.contains("espaço") || it.contains("espa") },
            "Expected an attribute with spaces in name")
        assertEquals(2, names.count { it == "vouDuplicarOnomeDesse" },
            "Expected duplicate name to appear twice")
        assertTrue("vixeEleDeixouDuplicar" in names)
        assertTrue("tipo inventado" in names)
    }

    @Test
    fun `varios XML - umAtributoOculto is identifier and multivalued`() {
        // Arrange
        val schema = parseResource("teste-varios-componentes.xml")
        val entity = schema.entities.first { it.name == "MinhaEntidade" }

        // Act
        val attr = entity.hiddenAttributes.first { it.name == "umAtributoOculto" }

        // Assert
        assertTrue(attr.isIdentifier, "umAtributoOculto should be identifier")
        assertTrue(attr.isMultiValued, "umAtributoOculto MaxCard=20 should be multivalued")
        assertEquals(20, attr.cardinality.maxCardinality)
        assertEquals(1, attr.cardinality.minCardinality)
        assertEquals("Texto(1)", attr.type)
    }

    @Test
    fun `varios XML - non-identifier non-multivalued attribute parsed correctly`() {
        // Arrange
        val schema = parseResource("teste-varios-componentes.xml")
        val entity = schema.entities.first { it.name == "MinhaEntidade" }

        // Act
        val attr = entity.hiddenAttributes.first { it.name == "vouDuplicarOnomeDesse" && it.type == "Moeda" }

        // Assert
        assertFalse(attr.isIdentifier)
        assertFalse(attr.isMultiValued)
        assertEquals("Moeda", attr.type)
    }

    @Test
    fun `varios XML - invented type is preserved verbatim`() {
        // Arrange
        val schema = parseResource("teste-varios-componentes.xml")
        val entity = schema.entities.first { it.name == "MinhaEntidade" }

        // Act
        val attr = entity.hiddenAttributes.first { it.name == "tipo inventado" }

        // Assert
        assertEquals("bolhaDeSabão", attr.type)
    }

    @Test
    fun `varios XML - round-trip preserves all hidden attributes`() {
        // Arrange
        val original = parseResource("teste-varios-componentes.xml")
        val originalEntity = original.entities.first { it.name == "MinhaEntidade" }

        // Act
        val reloaded = ConceptualSchemaXmlParser.parse(
            ConceptualSchemaXmlSerializer.serialize(original).toByteArray()
        )
        val reloadedEntity = reloaded.entities.first { it.name == "MinhaEntidade" }

        // Assert
        assertEquals(originalEntity.hiddenAttributes.size, reloadedEntity.hiddenAttributes.size)
        val origNames = originalEntity.hiddenAttributes.map { it.name }
        val reNames = reloadedEntity.hiddenAttributes.map { it.name }
        assertEquals(origNames, reNames)
    }
}
