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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConceptualSchemaDataDictionaryReportTest {

    @Test
    fun `collect entries sorts by object name and maps Pascal type labels`() {
        // Arrange
        val z = SchemaElement.Entity(
            id = 1,
            name = "Zebra",
            position = ElementPosition(0, 0, 10, 10),
            dictionary = "dz",
        )
        val a = SchemaElement.Entity(
            id = 2,
            name = "Anta",
            position = ElementPosition(0, 0, 10, 10),
            dictionary = "da",
        )
        val schema = ConceptualSchema(
            name = "t",
            elements = mapOf(1 to z, 2 to a),
        )

        // Act
        val rows = collectConceptualSchemaDictionaryReportEntries(schema)

        // Assert
        assertEquals(listOf("Anta", "Zebra"), rows.map { it.objectName })
        assertEquals("Entidade", rows[0].typeLabel)
        assertEquals("da", rows[0].dictionary)
        assertEquals("Entidade", rows[1].typeLabel)
        assertEquals("dz", rows[1].dictionary)
    }

    @Test
    fun `associative entity uses only entity dictionary for report body`() {
        // Arrange
        val ea = SchemaElement.AssociativeEntity(
            id = 1,
            name = "EA1",
            position = ElementPosition(0, 0, 10, 10),
            dictionary = "outer",
            relationshipDictionary = "inner",
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ea))

        // Act
        val row = collectConceptualSchemaDictionaryReportEntries(schema).single()

        // Assert
        assertEquals("Entidade associativa", row.typeLabel)
        assertEquals("outer", row.dictionary)
        assertTrue(!formatConceptualDataDictionaryMarkdown(listOf(row)).contains("inner"))
    }

    @Test
    fun `attribute entries use qualified owner dot path in objectName`() {
        // Arrange — same local name on different entities
        val pos = ElementPosition(0, 0, 10, 10)
        val entF = SchemaElement.Entity(id = 1, name = "Funcionario", position = pos)
        val entP = SchemaElement.Entity(id = 2, name = "Projeto", position = pos)
        val a1 = SchemaElement.Attribute(id = 3, name = "nome", position = pos, ownerId = 1)
        val a2 = SchemaElement.Attribute(id = 4, name = "nome", position = pos, ownerId = 2)
        val schema = ConceptualSchema(
            elements = mapOf(1 to entF, 2 to entP, 3 to a1, 4 to a2),
        )

        // Act
        val rows = collectConceptualSchemaDictionaryReportEntries(schema).associateBy { it.objectName }

        // Assert
        assertEquals("Atributo", rows["Funcionario.nome"]!!.typeLabel)
        assertEquals("Atributo", rows["Projeto.nome"]!!.typeLabel)
    }

    @Test
    fun `composite attribute builds nested dot path`() {
        // Arrange — composite parent on entity, leaf on composite
        val pos = ElementPosition(0, 0, 10, 10)
        val pessoa = SchemaElement.Entity(id = 1, name = "Pessoa", position = pos)
        val endereco = SchemaElement.Attribute(
            id = 2,
            name = "endereco",
            position = pos,
            ownerId = 1,
            childAttributeIds = listOf(3),
            compostoPersisted = true,
        )
        val uf = SchemaElement.Attribute(id = 3, name = "uf", position = pos, ownerId = 2)
        val schema = ConceptualSchema(elements = mapOf(1 to pessoa, 2 to endereco, 3 to uf))

        // Act
        val rows = collectConceptualSchemaDictionaryReportEntries(schema).associateBy { it.objectName }

        // Assert
        assertNotNull(rows["Pessoa.endereco"])
        assertNotNull(rows["Pessoa.endereco.uf"])
    }

    @Test
    fun `self-relationship owner yields entity then rel then attribute`() {
        // Arrange — attribute owned by auto-relationship diamond
        val pos = ElementPosition(0, 0, 10, 10)
        val emp = SchemaElement.Entity(id = 1, name = "Empregado", position = pos)
        val selfRel = SchemaElement.SelfRelationship(
            id = 2,
            name = "supervisiona",
            position = pos,
            ownerEntityId = 1,
        )
        val papel = SchemaElement.Attribute(id = 3, name = "papel", position = pos, ownerId = 2)
        val schema = ConceptualSchema(elements = mapOf(1 to emp, 2 to selfRel, 3 to papel))

        // Act
        val row = collectConceptualSchemaDictionaryReportEntries(schema).first { it.objectName.contains("papel") }

        // Assert
        assertEquals("Empregado.supervisiona.papel", row.objectName)
    }

    @Test
    fun `relationship-owned attribute uses relationship name as prefix`() {
        // Arrange — relationship attribute (n-ary / diamond)
        val pos = ElementPosition(0, 0, 10, 10)
        val r = SchemaElement.Relationship(id = 1, name = "trabalha_em", position = pos)
        val desde = SchemaElement.Attribute(id = 2, name = "desde", position = pos, ownerId = 1)
        val schema = ConceptualSchema(elements = mapOf(1 to r, 2 to desde))

        // Act
        val row = collectConceptualSchemaDictionaryReportEntries(schema).single { it.typeLabel == "Atributo" }

        // Assert
        assertEquals("trabalha_em.desde", row.objectName)
    }

    @Test
    fun `associative entity-owned attribute uses EA rectangle name as prefix`() {
        // Arrange — attribute owned by associative entity (outer rectangle name)
        val pos = ElementPosition(0, 0, 10, 10)
        val ea = SchemaElement.AssociativeEntity(
            id = 1,
            name = "Matricula",
            position = pos,
            relationshipName = "inscreve",
        )
        val nota = SchemaElement.Attribute(id = 2, name = "nota", position = pos, ownerId = 1)
        val schema = ConceptualSchema(elements = mapOf(1 to ea, 2 to nota))

        // Act
        val row = collectConceptualSchemaDictionaryReportEntries(schema).single { it.objectName.contains("nota") }

        // Assert
        assertEquals("Matricula.nota", row.objectName)
    }

    @Test
    fun `markdown export has title optional schema line and numbered headings`() {
        // Arrange
        val row = ConceptualSchemaDictionaryEntry(
            typeLabel = "Entidade",
            objectName = "Anta",
            dictionary = "Texto do dicionário.",
        )

        // Act
        val md = formatConceptualDataDictionaryMarkdown(listOf(row), schemaName = "MER Pousada")

        // Assert
        assertTrue(md.startsWith("# Dicionário de dados"))
        assertTrue(md.contains("- **Esquema:** MER Pousada"))
        assertTrue(md.contains("---"))
        assertTrue(md.contains("## 001 — **Entidade:** Anta"))
        assertTrue(md.contains("Texto do dicionário."))
    }

    @Test
    fun `plain text export mirrors markdown semantics without markup`() {
        // Arrange
        val row = ConceptualSchemaDictionaryEntry(
            typeLabel = "Entidade",
            objectName = "Anta",
            dictionary = "Texto do dicionário.",
        )

        // Act
        val plain = formatConceptualDataDictionaryPlainText(listOf(row), schemaName = "MER Pousada")

        // Assert
        assertTrue(plain.startsWith("Dicionário de dados"))
        assertTrue(plain.contains("Esquema: MER Pousada"))
        assertTrue(plain.contains("---"))
        assertTrue(plain.contains("001 — Entidade: Anta"))
        assertTrue(plain.contains("Texto do dicionário."))
        assertTrue(!plain.contains("#"))
        assertTrue(!plain.contains("**"))
    }

    @Test
    fun `markdown export omits schema line when schema name blank`() {
        // Arrange
        val row = ConceptualSchemaDictionaryEntry("Entidade", "X", "")

        // Act
        val md = formatConceptualDataDictionaryMarkdown(listOf(row), schemaName = "   ")

        // Assert
        assertTrue(!md.contains("**Esquema:**"))
        assertTrue(md.contains("# Dicionário de dados"))
    }
}
