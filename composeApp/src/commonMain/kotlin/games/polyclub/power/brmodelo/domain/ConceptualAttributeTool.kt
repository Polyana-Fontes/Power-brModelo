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

import androidx.compose.ui.geometry.Offset
import kotlin.math.max

private val ATRIBUTO_PATTERN = Regex("^Atributo(\\d+)$")

private fun usedAtributoIndices(schema: ConceptualSchema): Set<Int> =
    schema.attributes.mapNotNull { a ->
        ATRIBUTO_PATTERN.matchEntire(a.name)?.groupValues?.get(1)?.toIntOrNull()
    }.toSet()

private fun ConceptualSchema.nextUnusedAttributeName(): String {
    var n = 1
    val usedIdx = usedAtributoIndices(this)
    while (n in usedIdx) n++
    while (true) {
        val cand = "Atributo$n"
        if (attributes.none { it.name == cand }) return cand
        n++
    }
}

/**
 * Allocates [count] distinct names `AtributoN` using the smallest available integers (Pascal [GeraBaseNome] style).
 */
private fun ConceptualSchema.allocateConsecutiveAttributeNames(count: Int): List<String> {
    require(count > 0)
    val used = attributes.map { it.name }.toMutableSet()
    val out = ArrayList<String>(count)
    repeat(count) {
        var k = 1
        while ("Atributo$k" in used) k++
        val name = "Atributo$k"
        used.add(name)
        out.add(name)
    }
    return out
}

private fun countAttributesOnSide(
    schema: ConceptualSchema,
    ownerId: Int,
    side: ConceptualAttributeAttachPonto,
): Int {
    val owner = schema.elements[ownerId] ?: return 0
    val op = owner.position
    val code = side.pascalCode
    return schema.attributesOf(ownerId).count {
        conceptualAttributeAttachPonto(op, it.position) == code
    }
}

private fun positionSingleAttributeRect(
    owner: ElementPosition,
    side: ConceptualAttributeAttachPonto,
    attrW: Int,
    attrH: Int,
    click: Offset,
    stackIndex1Based: Int,
): ElementPosition {
    val dist = max(attrH, ConceptualPlacementDefaults.attributeStackSpacingMin)
    val gh = ConceptualPlacementDefaults.attributeHorizontalGap
    val gv = ConceptualPlacementDefaults.attributeVerticalGapBase

    val cx = click.x.toInt()
    val cy = click.y.toInt()

    return when (side) {
        ConceptualAttributeAttachPonto.RIGHT -> {
            val x = owner.x + owner.width + gh
            val y = (cy - attrH / 2).coerceIn(owner.y, (owner.y + owner.height - attrH).coerceAtLeast(owner.y))
            ElementPosition(x, y, attrW, attrH)
        }
        ConceptualAttributeAttachPonto.LEFT -> {
            val x = owner.x - attrW - gh
            val y = (cy - attrH / 2).coerceIn(owner.y, (owner.y + owner.height - attrH).coerceAtLeast(owner.y))
            ElementPosition(x, y, attrW, attrH)
        }
        ConceptualAttributeAttachPonto.TOP -> {
            val rawLeft = cx - attrW / 2
            val x = rawLeft.coerceIn(owner.x - attrW, owner.x + owner.width)
            val y = owner.y - (gv + dist * stackIndex1Based)
            ElementPosition(x, y, attrW, attrH)
        }
        ConceptualAttributeAttachPonto.BOTTOM -> {
            val rawLeft = cx - attrW / 2
            val x = rawLeft.coerceIn(owner.x - attrW, owner.x + owner.width)
            val y = owner.y + owner.height + (gv + dist * stackIndex1Based)
            ElementPosition(x, y, attrW, attrH)
        }
    }
}

/** Pascal `OrientacaoD` when `P = 1` (attribute on the LEFT of the owner) — see [TBase.OrganizeAtributos] / `TBarraDeAtributos.posicione`. */
private fun isOrientacaoD(side: ConceptualAttributeAttachPonto): Boolean =
    side == ConceptualAttributeAttachPonto.LEFT

/**
 * Positions two composite children along the bar (`TBarraDeAtributos.OrganizeAtributos` / `posicione`, mer.pas).
 */
private fun layoutTwoCompositeChildPositions(
    parent: ElementPosition,
    orientacaoD: Boolean,
    childW: Int,
    childH: Int,
): Pair<ElementPosition, ElementPosition> {
    val childCount = 2
    val barH = maxOf(parent.height * childCount + childCount * 2 - parent.height, 2)
    val wBarOff = if (!orientacaoD) parent.width - 5 else -2
    val barLeft = parent.x + wBarOff
    val barTop = parent.y + parent.height / 2 - barH / 2
    val wChild = if (orientacaoD) -(childW + 8) else 8
    val childX = barLeft + 3 + wChild
    val step = barH / (childCount - 1)
    val y0 = barTop + step * 0 - childH / 2
    val y1 = barTop + step * 1 - childH / 2
    return ElementPosition(childX, y0, childW, childH) to ElementPosition(childX, y1, childW, childH)
}

private data class AttributeVariantProps(
    val isIdentifier: Boolean = false,
    val isMultiValued: Boolean = false,
    val isOptional: Boolean = false,
    val cardinality: AttributeCardinality = AttributeCardinality(1, 1),
    val multiValuedCount: Int = 0,
    val childAttributeIds: List<Int> = emptyList(),
)

/**
 * Adds a [Connection] from [attributeId] to [ownerId] so the canvas draws the link (same as `<Ligacao>` on `<Atributo>` in XML / brM).
 * Uses [Connection.cardinality] `null` and hidden cardinality label, matching [ConceptualSchemaBrmParser].
 */
private fun ConceptualSchema.withAttributeOwnerConnection(attributeId: Int, ownerId: Int): ConceptualSchema {
    val (s, connId) = allocateId()
    return s.withConnection(
        Connection(
            id = connId,
            elementIdA = attributeId,
            elementIdB = ownerId,
            cardinality = null,
            showCardinality = false,
            orientation = LineOrientation.VERTICAL,
        ),
    )
}

private fun baseNewAttribute(
    id: Int,
    name: String,
    position: ElementPosition,
    ownerId: Int,
    variantProps: AttributeVariantProps,
): SchemaElement.Attribute =
    SchemaElement.Attribute(
        id = id,
        name = name,
        position = position,
        observations = "",
        dictionary = "",
        labelStyle = ConceptualPlacementDefaults.labelStyle,
        hiddenAttributes = emptyList(),
        ownerId = ownerId,
        isIdentifier = variantProps.isIdentifier,
        isMultiValued = variantProps.isMultiValued,
        isOptional = variantProps.isOptional,
        cardinality = variantProps.cardinality,
        multiValuedCount = variantProps.multiValuedCount,
        valueType = "VARCHAR( )",
        complement = "10",
        autoSize = true,
        deviationAngle = 10,
        childAttributeIds = variantProps.childAttributeIds,
    )

sealed class ConceptualAttributeToolResult {
    data class Ok(
        val schema: ConceptualSchema,
        val newPrimaryAttributeId: Int,
        /** Immediate owner receiving the new attribute (entity, relationship, associative, or composite attribute). */
        val ownerElementId: Int,
        /** Edge of [ownerElementId] used for placement (same as Pascal `MePonto` / organize cases). */
        val attachSide: ConceptualAttributeAttachPonto,
    ) : ConceptualAttributeToolResult()

    data class Error(val message: String) : ConceptualAttributeToolResult()
}

fun applyConceptualAttributeTool(
    schema: ConceptualSchema,
    ownerElementId: Int,
    clickSchema: Offset,
    variant: ConceptualAttributeToolVariant,
): ConceptualAttributeToolResult {
    val rawOwner = schema.elements[ownerElementId] ?: return ConceptualAttributeToolResult.Error(
        "Clique no objeto que receberá o atributo.",
    )

    return when (rawOwner) {
        is SchemaElement.Entity,
        is SchemaElement.Relationship,
        is SchemaElement.AssociativeEntity,
        ->
            placeAttributeOnOwner(schema, rawOwner.position, ownerElementId, clickSchema, variant)

        is SchemaElement.Attribute ->
            placeChildAttributeOnParent(schema, rawOwner, clickSchema, variant)

        else ->
            ConceptualAttributeToolResult.Error("Este objeto não pode possuir atributo.")
    }
}

/**
 * New attributes placed on another attribute always attach on the **right** (no edge-proximity pick),
 * then [relayoutCompositeSubtree] runs on the parent so composite bars stay aligned (same as **Organizar Atributos** bar layout).
 */
private fun placeChildAttributeOnParent(
    schema: ConceptualSchema,
    parentAttr: SchemaElement.Attribute,
    clickSchema: Offset,
    variant: ConceptualAttributeToolVariant,
): ConceptualAttributeToolResult {
    if (variant == ConceptualAttributeToolVariant.Composite) {
        return placeCompositeAttributeUnderParentAttribute(schema, parentAttr, clickSchema)
    }
    val attrW = ConceptualPlacementDefaults.attributeWidth
    val attrH = ConceptualPlacementDefaults.attributeHeight
    val side = ConceptualAttributeAttachPonto.RIGHT
    val stack = countAttributesOnSide(schema, parentAttr.id, side) + 1
    val pos = positionSingleAttributeRect(parentAttr.position, side, attrW, attrH, clickSchema, stack)
    val props = propsForSimpleVariant(variant)
    var s = schema
    val (s1, newId) = s.allocateId()
    s = s1
    val name = s.nextUnusedAttributeName()
    val newAttr = baseNewAttribute(newId, name, pos, parentAttr.id, props)
    s = s.withElement(newAttr)
    s = s.withAttributeOwnerConnection(attributeId = newId, ownerId = parentAttr.id)
    val updatedParent = parentAttr.copy(childAttributeIds = parentAttr.childAttributeIds + newId)
    s = s.withElement(updatedParent)
    s = s.withNormalizedAttributeMultiValuedCounts()
    s = relayoutCompositeSubtree(s, parentAttr.id)
    return ConceptualAttributeToolResult.Ok(s, newId, parentAttr.id, side)
}

/** Composite tool on an attribute: same as on entity/relationship, but owner is the parent attribute (always RIGHT). */
private fun placeCompositeAttributeUnderParentAttribute(
    schema: ConceptualSchema,
    parentAttr: SchemaElement.Attribute,
    clickSchema: Offset,
): ConceptualAttributeToolResult {
    val attrW = ConceptualPlacementDefaults.attributeWidth
    val attrH = ConceptualPlacementDefaults.attributeHeight
    val side = ConceptualAttributeAttachPonto.RIGHT
    val ownerPos = parentAttr.position
    val ownerId = parentAttr.id

    val stackParent = countAttributesOnSide(schema, ownerId, side) + 1
    val parentPos = positionSingleAttributeRect(ownerPos, side, attrW, attrH, clickSchema, stackParent)
    val orientD = isOrientacaoD(side)
    val (child0Pos, child1Pos) = layoutTwoCompositeChildPositions(parentPos, orientD, attrW, attrH)
    val names = schema.allocateConsecutiveAttributeNames(3)

    var s = schema
    val (s0, childId0) = s.allocateId()
    s = s0
    val (s1, childId1) = s.allocateId()
    s = s1
    val (s2, compositeParentId) = s.allocateId()
    s = s2

    val child0 = baseNewAttribute(childId0, names[1], child0Pos, compositeParentId, AttributeVariantProps())
    val child1 = baseNewAttribute(childId1, names[2], child1Pos, compositeParentId, AttributeVariantProps())
    val compositeAttr = baseNewAttribute(
        compositeParentId,
        names[0],
        parentPos,
        ownerId,
        AttributeVariantProps(
            childAttributeIds = listOf(childId0, childId1),
            multiValuedCount = 2,
        ),
    )
    s = s.withElement(child0)
    s = s.withElement(child1)
    s = s.withElement(compositeAttr)
    s = s.withAttributeOwnerConnection(attributeId = compositeParentId, ownerId = ownerId)
    s = s.withAttributeOwnerConnection(attributeId = childId0, ownerId = compositeParentId)
    s = s.withAttributeOwnerConnection(attributeId = childId1, ownerId = compositeParentId)
    val updatedParent = parentAttr.copy(childAttributeIds = parentAttr.childAttributeIds + compositeParentId)
    s = s.withElement(updatedParent)
    s = s.withNormalizedAttributeMultiValuedCounts()
    s = relayoutCompositeSubtree(s, parentAttr.id)
    return ConceptualAttributeToolResult.Ok(s, compositeParentId, ownerId, side)
}

private fun placeAttributeOnOwner(
    schema: ConceptualSchema,
    ownerPos: ElementPosition,
    ownerId: Int,
    clickSchema: Offset,
    variant: ConceptualAttributeToolVariant,
): ConceptualAttributeToolResult {
    val attrW = ConceptualPlacementDefaults.attributeWidth
    val attrH = ConceptualPlacementDefaults.attributeHeight
    val side = closestConceptualAttributeAttachPonto(ownerPos, clickSchema)

    if (variant != ConceptualAttributeToolVariant.Composite) {
        val stack = countAttributesOnSide(schema, ownerId, side) + 1
        val pos = positionSingleAttributeRect(ownerPos, side, attrW, attrH, clickSchema, stack)
        val props = propsForSimpleVariant(variant)
        var s = schema
        val (s1, newId) = s.allocateId()
        s = s1
        val name = s.nextUnusedAttributeName()
        val newAttr = baseNewAttribute(newId, name, pos, ownerId, props)
        s = s.withElement(newAttr)
        s = s.withAttributeOwnerConnection(attributeId = newId, ownerId = ownerId)
        s = s.withNormalizedAttributeMultiValuedCounts()
        return ConceptualAttributeToolResult.Ok(s, newId, ownerId, side)
    }

    val stackParent = countAttributesOnSide(schema, ownerId, side) + 1
    val parentPos = positionSingleAttributeRect(ownerPos, side, attrW, attrH, clickSchema, stackParent)
    val orientD = isOrientacaoD(side)
    val (child0Pos, child1Pos) = layoutTwoCompositeChildPositions(parentPos, orientD, attrW, attrH)
    val names = schema.allocateConsecutiveAttributeNames(3)

    var s = schema
    val (s0, childId0) = s.allocateId()
    s = s0
    val (s1, childId1) = s.allocateId()
    s = s1
    val (s2, parentId) = s.allocateId()
    s = s2

    val child0 = baseNewAttribute(childId0, names[1], child0Pos, parentId, AttributeVariantProps())
    val child1 = baseNewAttribute(childId1, names[2], child1Pos, parentId, AttributeVariantProps())
    val parentAttr = baseNewAttribute(
        parentId,
        names[0],
        parentPos,
        ownerId,
        AttributeVariantProps(
            childAttributeIds = listOf(childId0, childId1),
            multiValuedCount = 2,
        ),
    )
    s = s.withElement(child0)
    s = s.withElement(child1)
    s = s.withElement(parentAttr)
    s = s.withAttributeOwnerConnection(attributeId = parentId, ownerId = ownerId)
    s = s.withAttributeOwnerConnection(attributeId = childId0, ownerId = parentId)
    s = s.withAttributeOwnerConnection(attributeId = childId1, ownerId = parentId)
    s = s.withNormalizedAttributeMultiValuedCounts()
    return ConceptualAttributeToolResult.Ok(s, parentId, ownerId, side)
}

private fun propsForSimpleVariant(variant: ConceptualAttributeToolVariant): AttributeVariantProps =
    when (variant) {
        ConceptualAttributeToolVariant.Basic -> AttributeVariantProps()
        ConceptualAttributeToolVariant.Identifier ->
            AttributeVariantProps(isIdentifier = true)

        ConceptualAttributeToolVariant.MultiValued ->
            AttributeVariantProps(
                isMultiValued = true,
                cardinality = AttributeCardinality(1, 21),
            )

        ConceptualAttributeToolVariant.Optional ->
            AttributeVariantProps(
                isOptional = true,
                cardinality = AttributeCardinality(0, 1),
            )

        ConceptualAttributeToolVariant.Composite ->
            error("Composite handled separately")
    }
