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
 * An attribute that belongs to an entity or relationship but is not rendered on the canvas.
 *
 * Corresponds to `TAtributoOculto` in `att.pas`. These are attributes "hidden" from the
 * diagram because they were explicitly detached from the canvas, but they still carry
 * semantic information about the element (e.g. for logical/physical model generation).
 *
 * Composite hidden attributes use [children] for the canvas subtree (revealed as linked attributes)
 * and [nestedHiddenAttributes] for ocultos that remained on the attribute without being drawn as children.
 *
 * @param name         Attribute name. Corresponds to [TAtributoOculto.Nome].
 * @param type         Data type string (e.g. "VARCHAR"). Corresponds to [TAtributoOculto.Tipo].
 * @param isIdentifier Whether this attribute is part of the identifier/key.
 *                     Corresponds to [TAtributoOculto.Identificador].
 * @param cardinality  Min/max cardinality for multi-valued attributes.
 *                     Corresponds to [TAtributoOculto.MinCard] / [TAtributoOculto.MaxCard].
 *                     A cardinality with maxCardinality == 0 means the attribute is NOT multi-valued.
 * @param position     Canvas position stored for when the attribute is made visible again.
 *                     Corresponds to [TAtributoOculto.LeftTop].
 * @param children     Canvas subtree only: when revealed, these become [SchemaElement.Attribute.childAttributeIds].
 *                     Corresponds to composite [TAtributoOculto.Filhos] that were on the diagram.
 * @param nestedHiddenAttributes Attributes that stayed in [SchemaElement.Attribute.hiddenAttributes] on the canvas
 *                     node (not drawn as composite children). Serialized separately in XML as `<AtributosOcultosAninhados>`.
 * @param isOptional   Optional flag on the canvas attribute (Kotlin model); preserved across hide/reveal.
 * @param observations Free-text notes (XML `<Observacao>` on `<AtributoOculto>` when non-empty).
 * @param dictionary   Data dictionary text (XML `<Dicionario>` when non-empty).
 */
data class HiddenAttribute(
    val name: String,
    val type: String,
    val isIdentifier: Boolean,
    val cardinality: AttributeCardinality,
    val position: ElementPosition,
    val children: List<HiddenAttribute> = emptyList(),
    val nestedHiddenAttributes: List<HiddenAttribute> = emptyList(),
    val isOptional: Boolean = false,
    val observations: String = "",
    val dictionary: String = "",
) {
    /** True when [cardinality.maxCardinality] > 0, same logic as [TAtributoOculto.Multivalorado]. */
    val isMultiValued: Boolean get() = cardinality.maxCardinality > 0

    /** True when this node has canvas children or nested ocultos (inspector tree). */
    val isComposite: Boolean get() = children.isNotEmpty() || nestedHiddenAttributes.isNotEmpty()

    /** Branches in inspector / path order: [children] first, then [nestedHiddenAttributes]. */
    fun mergedBranchCount(): Int = children.size + nestedHiddenAttributes.size

    fun branchAt(mergedIndex: Int): HiddenAttribute? = when {
        mergedIndex < 0 -> null
        mergedIndex < children.size -> children[mergedIndex]
        mergedIndex < mergedBranchCount() -> nestedHiddenAttributes[mergedIndex - children.size]
        else -> null
    }

    /** Returns removed subtree and this node with that branch removed. */
    fun withBranchRemoved(mergedIndex: Int): Pair<HiddenAttribute, HiddenAttribute>? {
        if (mergedIndex < children.size) {
            val removed = children[mergedIndex]
            val rest = children.filterIndexed { i, _ -> i != mergedIndex }
            return removed to copy(children = rest)
        }
        val j = mergedIndex - children.size
        if (j !in nestedHiddenAttributes.indices) return null
        val removed = nestedHiddenAttributes[j]
        val rest = nestedHiddenAttributes.filterIndexed { i, _ -> i != j }
        return removed to copy(nestedHiddenAttributes = rest)
    }

    fun withBranchReplaced(mergedIndex: Int, newSubtree: HiddenAttribute): HiddenAttribute =
        if (mergedIndex < children.size) {
            copy(children = children.mapIndexed { i, c -> if (i == mergedIndex) newSubtree else c })
        } else {
            val j = mergedIndex - children.size
            copy(
                nestedHiddenAttributes = nestedHiddenAttributes.mapIndexed { i, c ->
                    if (i == j) newSubtree else c
                },
            )
        }

    /**
     * Number of leaf fields represented by this hidden subtree (used when totaling composite QtdeMultivalorado).
     * Simple oculto counts as one column; composite ocultos sum their children recursively.
     */
    fun physicalFieldLeafCount(): Int {
        if (children.isEmpty() && nestedHiddenAttributes.isEmpty()) return 1
        return children.sumOf { it.physicalFieldLeafCount() } +
            nestedHiddenAttributes.sumOf { it.physicalFieldLeafCount() }
    }

    /** Recursive copy for editor dialogs (isolated mutable draft tree). */
    fun deepCopy(): HiddenAttribute = copy(
        children = children.map { it.deepCopy() },
        nestedHiddenAttributes = nestedHiddenAttributes.map { it.deepCopy() },
    )
}
