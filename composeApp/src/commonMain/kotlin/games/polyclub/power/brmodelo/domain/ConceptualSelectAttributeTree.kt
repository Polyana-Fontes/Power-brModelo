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
 * Collects every canvas [SchemaElement.Attribute] id reachable from [seedElementIds]:
 * - For an attribute seed, the subtree (composite children) is included.
 * - For entity / relationship / associative entity / self-relationship seeds, every direct and nested attribute is included.
 */
fun collectAttributeTreeIdsFromSeeds(schema: ConceptualSchema, seedElementIds: Set<Int>): Set<Int> {
    if (seedElementIds.isEmpty()) return emptySet()
    val result = mutableSetOf<Int>()
    fun visitAttribute(attrId: Int) {
        if (attrId in result) return
        result.add(attrId)
        val a = schema.elements[attrId] as? SchemaElement.Attribute ?: return
        for (cid in a.childAttributeIds) visitAttribute(cid)
    }
    fun visitSeed(id: Int) {
        when (val el = schema.elements[id]) {
            is SchemaElement.Attribute -> visitAttribute(id)
            is SchemaElement.Entity,
            is SchemaElement.Relationship,
            is SchemaElement.AssociativeEntity,
            is SchemaElement.SelfRelationship,
            -> {
                for (a in schema.attributesOf(id)) visitAttribute(a.id)
            }
            else -> Unit
        }
    }
    for (id in seedElementIds) visitSeed(id)
    return result
}

/** Merges the current canvas element picks with every attribute in their attribute trees. Cardinality picks are preserved. */
fun expandCanvasSelectionWithAttributeTrees(schema: ConceptualSchema, selection: CanvasSelection): CanvasSelection {
    val (e, c) = selection.toMultiPickSets()
    val collected = collectAttributeTreeIdsFromSeeds(schema, e)
    return canvasSelectionFromPickSets(e + collected, c)
}

/** True when **Operações → Selecionar Atributos** would add at least one attribute to the current selection. */
fun canSelectAttributeTreeMenu(schema: ConceptualSchema, selection: CanvasSelection): Boolean {
    val (e, _) = selection.toMultiPickSets()
    if (e.isEmpty()) return false
    val collected = collectAttributeTreeIdsFromSeeds(schema, e)
    return collected.any { it !in e }
}
