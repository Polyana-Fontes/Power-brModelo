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

import kotlin.math.max

/**
 * Leaf field definitions for a new composite attribute (no nested composite canvas children in one step).
 */
data class ConceptualCompositeLeafSpec(
    val name: String? = null,
    val observations: String? = null,
    val dictionary: String? = null,
    val valueType: String? = null,
    val complement: String? = null,
)

private fun SchemaElement.Attribute.withLeafSpec(spec: ConceptualCompositeLeafSpec): SchemaElement.Attribute {
    var a = this
    spec.name?.trim()?.takeIf { it.isNotEmpty() }?.let { a = a.copy(name = it) }
    spec.observations?.let { a = a.copy(observations = it) }
    spec.dictionary?.let { a = a.copy(dictionary = it) }
    spec.valueType?.trim()?.takeIf { it.isNotEmpty() }?.let { a = a.copy(valueType = it) }
    spec.complement?.let { a = a.copy(complement = it) }
    return a
}

/**
 * Lays out [n] composite child attribute boxes along the composite bar (same geometry as [layoutTwoCompositeChildPositions]).
 */
internal fun layoutNCompositeChildPositions(
    parent: ElementPosition,
    orientacaoD: Boolean,
    childW: Int,
    childH: Int,
    n: Int,
): List<ElementPosition> {
    require(n >= 1)
    val childCount = n
    val barH = max(parent.height * childCount + childCount * 2 - parent.height, 2)
    val wBarOff = if (!orientacaoD) parent.width - 5 else -2
    val barLeft = parent.x + wBarOff
    val barTop = parent.y + parent.height / 2 - barH / 2
    val wChild = if (orientacaoD) -(childW + 8) else 8
    val childX = barLeft + 3 + wChild
    if (n == 1) {
        return listOf(ElementPosition(childX, barTop - childH / 2 + 1, childW, childH))
    }
    val step = barH / (n - 1)
    return (0 until n).map { i ->
        ElementPosition(childX, barTop + step * i - childH / 2, childW, childH)
    }
}

private fun ConceptualSchema.validateNewCanvasAndHiddenNamesForComposite(
    canvasExplicitNames: List<String>,
    nestedHidden: List<HiddenAttribute>,
): ConceptualAttributeToolResult.Error? {
    val hiddenFlat = nestedHidden.collectAllDeclaredNamesDepthFirst()
    if (hiddenFlat.any { it.isEmpty() }) {
        return ConceptualAttributeToolResult.Error("hidden_attribute_name_required")
    }
    val all = canvasExplicitNames + hiddenFlat
    if (all.size != all.toSet().size) {
        return ConceptualAttributeToolResult.Error("Duplicate attribute or hidden names in the request.")
    }
    for (n in all) {
        if (nameCollidesWithExistingAttributeOrHidden(n)) {
            return ConceptualAttributeToolResult.Error("Já existe um atributo com este nome.")
        }
    }
    return null
}

private fun placeCompositeOnBigOwner(
    schema: ConceptualSchema,
    ownerPos: ElementPosition,
    ownerId: Int,
    attachSide: ConceptualAttributeAttachPonto?,
    leafSpecs: List<ConceptualCompositeLeafSpec>,
    nestedHiddenAttributes: List<HiddenAttribute>,
): ConceptualAttributeToolResult {
    val attrW = ConceptualPlacementDefaults.attributeWidth
    val attrH = ConceptualPlacementDefaults.attributeHeight
    val side = attachSide ?: preferredAttachSideForConceptualOwner(schema, ownerId)
    val click = syntheticClickOnOwnerSideCenter(ownerPos, side)
    val stackParent = countAttributesOnSide(schema, ownerId, side) + 1
    val parentPos = positionSingleAttributeRect(ownerPos, side, attrW, attrH, click, stackParent)
    val orientD = isOrientacaoD(side)
    val n = leafSpecs.size
    val childPositions = layoutNCompositeChildPositions(parentPos, orientD, attrW, attrH, n)
    val autoNames = schema.allocateConsecutiveAttributeNames(n + 1)
    val explicitNames = buildList {
        add(autoNames[0])
        leafSpecs.forEachIndexed { i, spec ->
            add(spec.name?.trim()?.takeIf { it.isNotEmpty() } ?: autoNames[i + 1])
        }
    }
    schema.validateNewCanvasAndHiddenNamesForComposite(explicitNames, nestedHiddenAttributes)?.let { return it }
    var s = schema
    val childIds = ArrayList<Int>(n)
    repeat(n) {
        val (s0, cid) = s.allocateId()
        s = s0
        childIds.add(cid)
    }
    val (s1, compositeParentId) = s.allocateId()
    s = s1
    val childElements = childIds.mapIndexed { idx, cid ->
        val pos = childPositions[idx]
        val props = AttributeVariantProps()
        baseNewAttribute(cid, explicitNames[idx + 1], pos, compositeParentId, props)
            .withLeafSpec(leafSpecs[idx])
    }
    val parentAttr = baseNewAttribute(
        compositeParentId,
        explicitNames[0],
        parentPos,
        ownerId,
        AttributeVariantProps(
            childAttributeIds = childIds,
            multiValuedCount = n,
        ),
    ).copy(hiddenAttributes = nestedHiddenAttributes)
    for (ch in childElements) {
        s = s.withElement(ch)
    }
    s = s.withElement(parentAttr)
    s = s.withAttributeOwnerConnection(attributeId = compositeParentId, ownerId = ownerId)
    for (cid in childIds) {
        s = s.withAttributeOwnerConnection(attributeId = cid, ownerId = compositeParentId)
    }
    s = s.withNormalizedAttributeMultiValuedCounts()
    s = relayoutCompositeSubtree(s, compositeParentId)
    return ConceptualAttributeToolResult.Ok(s, compositeParentId, ownerId, side)
}

private fun placeCompositeOnParentAttribute(
    schema: ConceptualSchema,
    parentAttr: SchemaElement.Attribute,
    leafSpecs: List<ConceptualCompositeLeafSpec>,
    nestedHiddenAttributes: List<HiddenAttribute>,
): ConceptualAttributeToolResult {
    val attrW = ConceptualPlacementDefaults.attributeWidth
    val attrH = ConceptualPlacementDefaults.attributeHeight
    val side = ConceptualAttributeAttachPonto.RIGHT
    val ownerPos = parentAttr.position
    val ownerId = parentAttr.id
    val click = syntheticClickOnOwnerSideCenter(ownerPos, side)
    val stackParent = countAttributesOnSide(schema, ownerId, side) + 1
    val parentPos = positionSingleAttributeRect(ownerPos, side, attrW, attrH, click, stackParent)
    val orientD = isOrientacaoD(side)
    val n = leafSpecs.size
    val childPositions = layoutNCompositeChildPositions(parentPos, orientD, attrW, attrH, n)
    val autoNames = schema.allocateConsecutiveAttributeNames(n + 1)
    val explicitNames = buildList {
        add(autoNames[0])
        leafSpecs.forEachIndexed { i, spec ->
            add(spec.name?.trim()?.takeIf { it.isNotEmpty() } ?: autoNames[i + 1])
        }
    }
    schema.validateNewCanvasAndHiddenNamesForComposite(explicitNames, nestedHiddenAttributes)?.let { return it }
    var s = schema
    val childIds = ArrayList<Int>(n)
    repeat(n) {
        val (s0, cid) = s.allocateId()
        s = s0
        childIds.add(cid)
    }
    val (s1, compositeParentId) = s.allocateId()
    s = s1
    val childElements = childIds.mapIndexed { idx, cid ->
        val pos = childPositions[idx]
        baseNewAttribute(cid, explicitNames[idx + 1], pos, compositeParentId, AttributeVariantProps())
            .withLeafSpec(leafSpecs[idx])
    }
    val parentAttrNew = baseNewAttribute(
        compositeParentId,
        explicitNames[0],
        parentPos,
        ownerId,
        AttributeVariantProps(
            childAttributeIds = childIds,
            multiValuedCount = n,
        ),
    ).copy(hiddenAttributes = nestedHiddenAttributes)
    for (ch in childElements) {
        s = s.withElement(ch)
    }
    s = s.withElement(parentAttrNew)
    s = s.withAttributeOwnerConnection(attributeId = compositeParentId, ownerId = ownerId)
    for (cid in childIds) {
        s = s.withAttributeOwnerConnection(attributeId = cid, ownerId = compositeParentId)
    }
    val updatedParent = parentAttr.copy(childAttributeIds = parentAttr.childAttributeIds + compositeParentId)
    s = s.withElement(updatedParent)
    s = s.withNormalizedAttributeMultiValuedCounts()
    s = relayoutCompositeSubtree(s, parentAttr.id)
    return ConceptualAttributeToolResult.Ok(s, compositeParentId, ownerId, side)
}

/**
 * Creates a composite attribute with [leafSpecs.size] simple canvas children (no nested composite in this call).
 * [nestedHiddenAttributes] are stored on the new composite parent (same as inspector / XML nested ocultos).
 */
fun applyConceptualCompositeAttributeWithLeafChildren(
    schema: ConceptualSchema,
    ownerElementId: Int,
    attachSide: ConceptualAttributeAttachPonto?,
    leafSpecs: List<ConceptualCompositeLeafSpec>,
    nestedHiddenAttributes: List<HiddenAttribute>,
): ConceptualAttributeToolResult {
    if (leafSpecs.isEmpty()) {
        return ConceptualAttributeToolResult.Error("At least one leaf child is required.")
    }
    val rawOwner = schema.elements[ownerElementId] ?: return ConceptualAttributeToolResult.Error(
        "Clique no objeto que receberá o atributo.",
    )
    return when (rawOwner) {
        is SchemaElement.Entity,
        is SchemaElement.Relationship,
        is SchemaElement.AssociativeEntity,
        -> placeCompositeOnBigOwner(
            schema,
            rawOwner.position,
            ownerElementId,
            attachSide,
            leafSpecs,
            nestedHiddenAttributes,
        )
        is SchemaElement.Attribute ->
            placeCompositeOnParentAttribute(schema, rawOwner, leafSpecs, nestedHiddenAttributes)
        else ->
            ConceptualAttributeToolResult.Error("Este objeto não pode possuir atributo.")
    }
}
