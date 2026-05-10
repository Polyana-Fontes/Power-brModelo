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
 * Resolves the hidden-attribute subtree at [path] in a root list (same indexing as the inspector:
 * [HiddenAttribute.children] first, then [HiddenAttribute.nestedHiddenAttributes] at each level).
 */
fun hiddenAttributeAtPath(roots: List<HiddenAttribute>, path: List<Int>): HiddenAttribute? {
    if (path.isEmpty()) return null
    var node = roots.getOrNull(path[0]) ?: return null
    for (i in 1 until path.size) {
        node = node.branchAt(path[i]) ?: return null
    }
    return node
}

private fun replaceHiddenInNode(node: HiddenAttribute, path: List<Int>, replacement: HiddenAttribute): HiddenAttribute? {
    if (path.isEmpty()) return replacement
    val idx = path[0]
    if (idx !in 0 until node.mergedBranchCount()) return null
    if (path.size == 1) {
        return node.withBranchReplaced(idx, replacement)
    }
    val child = node.branchAt(idx) ?: return null
    val updatedChild = replaceHiddenInNode(child, path.drop(1), replacement) ?: return null
    return node.withBranchReplaced(idx, updatedChild)
}

/**
 * Returns a new root list with the node at [path] replaced by [replacement], or null if [path] is invalid.
 */
fun replaceHiddenAttributeAtPath(
    attrs: List<HiddenAttribute>,
    path: List<Int>,
    replacement: HiddenAttribute,
): List<HiddenAttribute>? {
    if (path.isEmpty()) return null
    val head = path[0]
    if (head !in attrs.indices) return null
    if (path.size == 1) {
        return attrs.mapIndexed { i, h -> if (i == head) replacement else h }
    }
    val parent = attrs[head]
    val updatedParent = replaceHiddenInNode(parent, path.drop(1), replacement) ?: return null
    return attrs.mapIndexed { i, h -> if (i == head) updatedParent else h }
}

fun applyAppendHiddenAttribute(schema: ConceptualSchema, ownerElementId: Int, newAttr: HiddenAttribute): ConceptualSchema? {
    val holder = schema.elements[ownerElementId] ?: return null
    val next = holder.hiddenAttributeList() + newAttr
    return schema.withElementHiddenList(ownerElementId, next)?.withNormalizedAttributeMultiValuedCounts()
}

fun applyReplaceHiddenAttribute(
    schema: ConceptualSchema,
    ownerElementId: Int,
    path: List<Int>,
    replacement: HiddenAttribute,
): ConceptualSchema? {
    val holder = schema.elements[ownerElementId] ?: return null
    val current = holder.hiddenAttributeList()
    val updated = replaceHiddenAttributeAtPath(current, path, replacement) ?: return null
    return schema.withElementHiddenList(ownerElementId, updated)?.withNormalizedAttributeMultiValuedCounts()
}

fun applyRemoveHiddenAttribute(schema: ConceptualSchema, ownerElementId: Int, path: List<Int>): ConceptualSchema? {
    if (path.isEmpty()) return null
    val holder = schema.elements[ownerElementId] ?: return null
    val current = holder.hiddenAttributeList()
    val (_, newList) = removeHiddenAttributeAtPath(current, path) ?: return null
    return schema.withElementHiddenList(ownerElementId, newList)?.withNormalizedAttributeMultiValuedCounts()
}

private fun SchemaElement.hiddenAttributeList(): List<HiddenAttribute> =
    when (this) {
        is SchemaElement.Entity -> hiddenAttributes
        is SchemaElement.Relationship -> hiddenAttributes
        is SchemaElement.AssociativeEntity -> hiddenAttributes
        is SchemaElement.Attribute -> hiddenAttributes
        is SchemaElement.SelfRelationship -> hiddenAttributes
        is SchemaElement.Specialization -> hiddenAttributes
        is SchemaElement.Annotation -> hiddenAttributes
    }

private fun SchemaElement.withHiddenAttributeList(list: List<HiddenAttribute>): SchemaElement =
    when (this) {
        is SchemaElement.Entity -> copy(hiddenAttributes = list)
        is SchemaElement.Relationship -> copy(hiddenAttributes = list)
        is SchemaElement.AssociativeEntity -> copy(hiddenAttributes = list)
        is SchemaElement.Attribute -> copy(hiddenAttributes = list)
        is SchemaElement.SelfRelationship -> copy(hiddenAttributes = list)
        is SchemaElement.Specialization -> copy(hiddenAttributes = list)
        is SchemaElement.Annotation -> copy(hiddenAttributes = list)
    }

private fun ConceptualSchema.withElementHiddenList(elementId: Int, list: List<HiddenAttribute>): ConceptualSchema? {
    val el = elements[elementId] ?: return null
    return withElement(el.withHiddenAttributeList(list))
}

/** True if every node has a non-blank name and sibling names (merged children + nested) are unique. */
fun hiddenAttributeForestNamesValid(roots: List<HiddenAttribute>): Boolean =
    roots.all { it.name.trim().isNotEmpty() && hiddenAttributeSubtreeNamesValid(it) }

private fun hiddenAttributeSubtreeNamesValid(node: HiddenAttribute): Boolean {
    val merged = node.children + node.nestedHiddenAttributes
    val names = merged.map { it.name.trim() }
    if (names.any { it.isEmpty() }) return false
    if (names.toSet().size != names.size) return false
    return merged.all { hiddenAttributeSubtreeNamesValid(it) }
}
