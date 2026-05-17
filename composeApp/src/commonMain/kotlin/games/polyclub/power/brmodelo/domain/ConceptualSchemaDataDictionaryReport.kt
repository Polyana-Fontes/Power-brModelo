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
 * (selection sort on `Texto`, which holds `TBase.Nome`).
 *
 * Cardinality components (`TCardinalidade`) and inner relationship children (`TChildRelacao`) are not listed
 * in the original `GetItens` walk used by `dicFull.pas`; this port mirrors that by omitting
 * [Connection] rows and by using only the associative entity's own [SchemaElement.AssociativeEntity.dictionary],
 * not [SchemaElement.AssociativeEntity.relationshipDictionary].
 */
data class ConceptualSchemaDictionaryEntry(
    val typeLabel: String,
    val objectName: String,
    val dictionary: String,
)

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
        ConceptualSchemaDictionaryEntry(
            typeLabel = denominarMerElement(el),
            objectName = el.name,
            dictionary = dict,
        )
    }
    return rows.sortedWith(compareBy { it.objectName })
}

/**
 * Plain-text report matching the line structure produced by `dicFull.pas` `Maker`
 * (numbered header line, optional dictionary body, blank line between objects).
 */
fun formatConceptualDataDictionaryPlainText(entries: List<ConceptualSchemaDictionaryEntry>): String =
    buildString {
        entries.forEachIndexed { i, e ->
            val n = (i + 1).toString().padStart(3, '0')
            appendLine("$n - ${e.typeLabel}: ${e.objectName}")
            val body = e.dictionary.trim()
            if (body.isNotEmpty()) appendLine(body)
            appendLine()
        }
    }
