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

import games.polyclub.kbrmodelo.domain.Cardinality
import games.polyclub.kbrmodelo.domain.ConceptualSchema
import games.polyclub.kbrmodelo.domain.SchemaElement
import kotlin.test.*

/**
 * Unit tests for [ConceptualSchemaBrmParser].
 *
 * Two fixture files are used (same models as the XML test suite):
 * - `exemplo-simples.brM` — small model saved in Delphi DFM binary format.
 * - `MER-PousadaSolDaManha.brm` — real-world hotel management schema.
 *
 * Where possible, results are cross-validated against the XML parser output so
 * that any discrepancy between the two save formats surfaces as a test failure.
 */
class ConceptualSchemaBrmTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun loadResource(name: String): ByteArray =
        checkNotNull(javaClass.classLoader.getResourceAsStream(name)) {
            "Test resource '$name' not found"
        }.readBytes()

    private fun parseBrm(name: String): ConceptualSchema =
        ConceptualSchemaBrmParser.parse(loadResource(name))

    private fun parseXml(name: String): ConceptualSchema =
        ConceptualSchemaXmlParser.parse(loadResource(name))

    // ─────────────────────────────────────────────────────────────────────────
    // SIMPLE brM
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `simples brM - parses without error`() {
        // Act & Assert
        assertDoesNotThrow { parseBrm("exemplo-simples.brM") }
    }

    @Test
    fun `simples brM - has same entity count as XML`() {
        // Arrange
        val brm = parseBrm("exemplo-simples.brM")
        val xml = parseXml("exemplo-simples.xml")

        // Act
        val brmEntities = brm.entities.size
        val xmlEntities = xml.entities.size

        // Assert
        assertEquals(xmlEntities, brmEntities,
            "Entity count differs: XML=$xmlEntities, brM=$brmEntities")
    }

    @Test
    fun `simples brM - has same attribute count as XML`() {
        // Arrange
        val brm = parseBrm("exemplo-simples.brM")
        val xml = parseXml("exemplo-simples.xml")

        // Act & Assert
        assertEquals(xml.attributes.size, brm.attributes.size,
            "Attribute count differs: XML=${xml.attributes.size}, brM=${brm.attributes.size}")
    }

    @Test
    fun `simples brM - has same relationship count as XML`() {
        // Arrange
        val brm = parseBrm("exemplo-simples.brM")
        val xml = parseXml("exemplo-simples.xml")

        // Act & Assert
        assertEquals(xml.relationships.size, brm.relationships.size,
            "Relationship count differs: XML=${xml.relationships.size}, brM=${brm.relationships.size}")
    }

    @Test
    fun `simples brM - entity names match XML`() {
        // Arrange
        val brm = parseBrm("exemplo-simples.brM")
        val xml = parseXml("exemplo-simples.xml")

        // Act
        val brmNames = brm.entities.map { it.name }.toSet()
        val xmlNames = xml.entities.map { it.name }.toSet()

        // Assert
        assertEquals(xmlNames, brmNames, "Entity names differ between XML and brM")
    }

    @Test
    fun `simples brM - attribute names match XML`() {
        // Arrange
        val brm = parseBrm("exemplo-simples.brM")
        val xml = parseXml("exemplo-simples.xml")

        // Act
        val brmNames = brm.attributes.map { it.name }.toSet()
        val xmlNames = xml.attributes.map { it.name }.toSet()

        // Assert
        assertEquals(xmlNames, brmNames, "Attribute names differ between XML and brM")
    }

    @Test
    fun `simples brM - identifier attribute detected correctly`() {
        // Arrange
        val schema = parseBrm("exemplo-simples.brM")

        // Act
        val identifiers = schema.attributes.filter { it.isIdentifier }

        // Assert
        assertTrue(identifiers.isNotEmpty(), "No identifier attributes found in brM model")
    }

    @Test
    fun `simples brM - attribute owner IDs resolve to actual elements`() {
        // Arrange
        val schema = parseBrm("exemplo-simples.brM")

        // Act & Assert
        for (attr in schema.attributes) {
            val owner = schema.elements[attr.ownerId]
            assertNotNull(owner,
                "Attribute '${attr.name}' (id=${attr.id}) has ownerId=${attr.ownerId} " +
                "which does not resolve to any element")
        }
    }

    @Test
    fun `simples brM - element positions are positive`() {
        // Arrange
        val schema = parseBrm("exemplo-simples.brM")

        // Act & Assert
        for (element in schema.elements.values) {
            val p = element.position
            assertTrue(p.width > 0 && p.height > 0,
                "${element::class.simpleName} '${element.name}' has non-positive dimensions: ${p.width}x${p.height}")
        }
    }

    @Test
    fun `simples brM - connections reference existing elements`() {
        // Arrange
        val schema = parseBrm("exemplo-simples.brM")

        // Act & Assert
        for (conn in schema.connections) {
            assertNotNull(schema.elements[conn.elementIdA],
                "Connection ${conn.id}: elementIdA=${conn.elementIdA} not found")
            assertNotNull(schema.elements[conn.elementIdB],
                "Connection ${conn.id}: elementIdB=${conn.elementIdB} not found")
        }
    }

    @Test
    fun `simples brM - connection count is non-zero`() {
        // Arrange
        val schema = parseBrm("exemplo-simples.brM")

        // Assert
        assertTrue(schema.connections.isNotEmpty(), "Expected at least one connection in brM model")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPLEX brM — MER-PousadaSolDaManha
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `pousada brM - parses without error`() {
        // Act & Assert
        assertDoesNotThrow { parseBrm("MER-PousadaSolDaManha.brm") }
    }

    @Test
    fun `pousada brM - has same entity count as XML`() {
        // Arrange
        val brm = parseBrm("MER-PousadaSolDaManha.brm")
        val xml = parseXml("MER-PousadaSolDaManha.xml")

        // Assert
        assertEquals(xml.entities.size, brm.entities.size,
            "Pousada entity count differs: XML=${xml.entities.size}, brM=${brm.entities.size}")
    }

    @Test
    fun `pousada brM - has same relationship count as XML`() {
        // Arrange
        val brm = parseBrm("MER-PousadaSolDaManha.brm")
        val xml = parseXml("MER-PousadaSolDaManha.xml")

        // Assert
        assertEquals(xml.relationships.size, brm.relationships.size,
            "Pousada relationship count: XML=${xml.relationships.size}, brM=${brm.relationships.size}")
    }

    @Test
    fun `pousada brM - entity names match XML`() {
        // Arrange
        val brm = parseBrm("MER-PousadaSolDaManha.brm")
        val xml = parseXml("MER-PousadaSolDaManha.xml")

        // Act
        val brmNames = brm.entities.map { it.name }.toSet()
        val xmlNames = xml.entities.map { it.name }.toSet()

        // Assert
        assertEquals(xmlNames, brmNames,
            "Entity names differ.\nOnly in XML: ${xmlNames - brmNames}\nOnly in brM: ${brmNames - xmlNames}")
    }

    @Test
    fun `pousada brM - relationship names match XML`() {
        // Arrange
        val brm = parseBrm("MER-PousadaSolDaManha.brm")
        val xml = parseXml("MER-PousadaSolDaManha.xml")

        // Act
        val brmNames = brm.relationships.map { it.name }.toSet()
        val xmlNames = xml.relationships.map { it.name }.toSet()

        // Assert
        assertEquals(xmlNames, brmNames,
            "Relationship names differ.\nOnly in XML: ${xmlNames - brmNames}\nOnly in brM: ${brmNames - xmlNames}")
    }

    @Test
    fun `pousada brM - attribute count matches XML`() {
        // Arrange
        val brm = parseBrm("MER-PousadaSolDaManha.brm")
        val xml = parseXml("MER-PousadaSolDaManha.xml")

        // Assert
        assertEquals(xml.attributes.size, brm.attributes.size,
            "Pousada attribute count: XML=${xml.attributes.size}, brM=${brm.attributes.size}")
    }

    @Test
    fun `pousada brM - specialization count matches XML`() {
        // Arrange
        val brm = parseBrm("MER-PousadaSolDaManha.brm")
        val xml = parseXml("MER-PousadaSolDaManha.xml")

        // Assert
        assertEquals(xml.specializations.size, brm.specializations.size,
            "Pousada specialization count: XML=${xml.specializations.size}, brM=${brm.specializations.size}")
    }

    @Test
    fun `pousada brM - all attribute owners resolve`() {
        // Arrange
        val schema = parseBrm("MER-PousadaSolDaManha.brm")

        // Act & Assert
        for (attr in schema.attributes) {
            val owner = schema.elements[attr.ownerId]
            assertNotNull(owner,
                "Attribute '${attr.name}' (id=${attr.id}) ownerId=${attr.ownerId} unresolved")
        }
    }

    @Test
    fun `pousada brM - connections reference valid elements`() {
        // Arrange
        val schema = parseBrm("MER-PousadaSolDaManha.brm")

        // Act & Assert
        for (conn in schema.connections) {
            assertNotNull(schema.elements[conn.elementIdA],
                "Connection ${conn.id}: elementIdA=${conn.elementIdA} not found")
            assertNotNull(schema.elements[conn.elementIdB],
                "Connection ${conn.id}: elementIdB=${conn.elementIdB} not found")
        }
    }

    @Test
    fun `pousada brM - cardinalities are valid enum values`() {
        // Arrange
        val schema = parseBrm("MER-PousadaSolDaManha.brm")

        // Act
        val invalid = schema.connections.filter { conn ->
            conn.cardinality != null && conn.cardinality !in Cardinality.entries
        }

        // Assert
        assertTrue(invalid.isEmpty(),
            "Connections with invalid cardinality: $invalid")
    }

    @Test
    fun `pousada brM - has more elements than simple model`() {
        // Arrange
        val simples = parseBrm("exemplo-simples.brM")
        val pousada = parseBrm("MER-PousadaSolDaManha.brm")

        // Assert
        assertTrue(
            pousada.elements.size > simples.elements.size,
            "Pousada (${pousada.elements.size}) should have more elements than simples (${simples.elements.size})",
        )
    }
}

// Convenience alias so the test class does not depend on JUnit 4 directly
private fun assertDoesNotThrow(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        fail("Expected no exception but got: ${e::class.simpleName}: ${e.message}")
    }
}
