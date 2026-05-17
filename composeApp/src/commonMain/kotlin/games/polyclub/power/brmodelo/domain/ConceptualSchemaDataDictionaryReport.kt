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

/**
 * One row in the conceptual **data dictionary** report (Pascal `TbrFmDicFull.Maker` / `TModelo.GetItens`).
 *
 * Objects are sorted by [objectName] only, matching `TGeralList.Ordene` in `uAux.pas`
 * (selection sort on `Texto`, which holds `TBase.Nome`). **Exception:** canvas [SchemaElement.Attribute]
 * rows use a qualified [ConceptualSchemaDictionaryEntry.objectName] (see [conceptualDictionaryAttributeDisplayName])
 * so the text export disambiguates homonyms; sorting therefore follows that qualified string.
 *
 * Cardinality components (`TCardinalidade`) and inner relationship children (`TChildRelacao`) are not listed
 * in the original `GetItens` walk used by `dicFull.pas`; this port mirrors that by omitting
 * [Connection] rows and by using only the associative entity's own [SchemaElement.AssociativeEntity.dictionary],
 * not [SchemaElement.AssociativeEntity.relationshipDictionary].
 */
data class ConceptualSchemaDictionaryEntry(
    val typeLabel: String,
    /**
     * Display name in the report header (`dicFull.pas` line `Texto` = `TBase.Nome`).
     * For [SchemaElement.Attribute], this is a **qualified** name (see [conceptualDictionaryAttributeDisplayName])
     * so homonymous attributes stay distinguishable in the text export.
     */
    val objectName: String,
    val dictionary: String,
)

/**
 * Builds the `Entidade.atributo` / `Entidade.composto.folha` style header for a canvas attribute in the
 * full-schema data dictionary export.
 *
 * **Format:** dot-separated “MER qualified attribute” path — each segment is the element’s display
 * [SchemaElement.name], outermost holder first (entity, relationship, associative entity, or
 * `Entidade.autoRel` for [SchemaElement.SelfRelationship]), then composite parents, then the leaf name.
 * This matches common ER / relational disambiguation (`Funcionario.nome` vs `Projeto.nome`) without
 * adding extra punctuation that would read oddly in a plain-text dictionary.
 *
 * Hidden attributes ([SchemaElement.hiddenAttributes]) are **not** separate canvas elements and are
 * still edited via **Operações → Dicionário de Dados de Objetos** ([collectDictionarySlotsForSelection]);
 * they are outside this report’s `GetItens`-style walk.
 */
fun conceptualDictionaryAttributeDisplayName(schema: ConceptualSchema, attribute: SchemaElement.Attribute): String {
    val leaf = attribute.name.ifBlank { "«atributo»" }
    return (ownerPathPrefixSegments(schema, attribute.ownerId) + leaf).joinToString(".")
}

private fun ownerPathPrefixSegments(schema: ConceptualSchema, ownerId: Int, visited: MutableSet<Int> = mutableSetOf()): List<String> {
    if (ownerId in visited) return listOf("«ciclo»")
    visited.add(ownerId)
    val el = schema.elements[ownerId] ?: return listOf("?")
    return when (el) {
        is SchemaElement.Entity -> listOf(el.name.ifBlank { "«entidade»" })
        is SchemaElement.Relationship -> listOf(el.name.ifBlank { "«relação»" })
        is SchemaElement.AssociativeEntity -> listOf(el.name.ifBlank { "«entidade associativa»" })
        is SchemaElement.SelfRelationship -> {
            val ent = schema.elements[el.ownerEntityId] as? SchemaElement.Entity
            listOf(
                ent?.name?.ifBlank { null } ?: "?",
                el.name.ifBlank { "«auto-relacionamento»" },
            )
        }
        is SchemaElement.Attribute ->
            ownerPathPrefixSegments(schema, el.ownerId, visited) + el.name.ifBlank { "«composto»" }
        else -> listOf(el.name.ifBlank { "?" })
    }
}

/** Maps a canvas element to the same Portuguese labels as `Denominar` in `uAux.pas`. */
fun denominarMerElement(element: SchemaElement): String =
    when (element) {
        is SchemaElement.Attribute -> "Atributo"
        is SchemaElement.SelfRelationship -> "Auto relacionamento"
        is SchemaElement.AssociativeEntity -> "Entidade associativa"
        is SchemaElement.Entity -> "Entidade"
        is SchemaElement.Specialization -> "Especialização"
        is SchemaElement.Annotation -> "Observação"
        is SchemaElement.Relationship -> "Relação"
    }

/**
 * Collects every conceptual canvas element that participates in `TModelo.GetItens` semantics
 * (top-level `TBase` descendants, excluding cardinality bars and nested child-relationship objects).
 */
fun collectConceptualSchemaDictionaryReportEntries(schema: ConceptualSchema): List<ConceptualSchemaDictionaryEntry> {
    val rows = schema.elements.values.map { el ->
        val dict =
            when (el) {
                is SchemaElement.AssociativeEntity -> el.dictionary
                else -> el.dictionary
            }
        val objectName =
            when (el) {
                is SchemaElement.Attribute -> conceptualDictionaryAttributeDisplayName(schema, el)
                else -> el.name
            }
        ConceptualSchemaDictionaryEntry(
            typeLabel = denominarMerElement(el),
            objectName = objectName,
            dictionary = dict,
        )
    }
    return rows.sortedWith(compareBy { it.objectName })
}

/**
 * Markdown report for the full-schema data dictionary (save / preview / print).
 *
 * Structure: document title, optional schema metadata line, horizontal rule, then for each object
 * a level-2 heading with the legacy ordinal and Portuguese type label, followed by the free-text body.
 */
fun formatConceptualDataDictionaryMarkdown(
    entries: List<ConceptualSchemaDictionaryEntry>,
    schemaName: String = "",
): String =
    buildString {
        appendLine("# Dicionário de dados")
        appendLine()
        val title = schemaName.trim()
        if (title.isNotEmpty()) {
            appendLine("- **Esquema:** $title")
            appendLine()
        }
        appendLine("---")
        appendLine()
        entries.forEachIndexed { i, e ->
            val n = (i + 1).toString().padStart(3, '0')
            appendLine("## $n — **${e.typeLabel}:** ${e.objectName}")
            appendLine()
            val body = e.dictionary.trim()
            if (body.isNotEmpty()) {
                appendLine(body)
                appendLine()
            }
            appendLine()
        }
    }.trimEnd()

/**
 * Plain-text variant of [formatConceptualDataDictionaryMarkdown] (same schema line, ordinals, type labels,
 * [ConceptualSchemaDictionaryEntry.objectName] including owner-qualified attribute paths, and dictionary bodies).
 * Used for PDF export where Markdown syntax is not needed.
 */
fun formatConceptualDataDictionaryPlainText(
    entries: List<ConceptualSchemaDictionaryEntry>,
    schemaName: String = "",
): String =
    buildString {
        appendLine("Dicionário de dados")
        appendLine()
        val title = schemaName.trim()
        if (title.isNotEmpty()) {
            appendLine("Esquema: $title")
            appendLine()
        }
        appendLine("---")
        appendLine()
        entries.forEachIndexed { i, e ->
            val n = (i + 1).toString().padStart(3, '0')
            appendLine("$n — ${e.typeLabel}: ${e.objectName}")
            appendLine()
            val body = e.dictionary.trim()
            if (body.isNotEmpty()) {
                appendLine(body)
                appendLine()
            }
            appendLine()
        }
    }.trimEnd()
