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
 * Hide / show conceptual attributes on the canvas (ribbon **Operações**).
 *
 * Mirrors Pascal [TModelo.OcultarAtributo] and [TModelo.MostraAtributoOculto] in `mer.pas` (~3289–3367).
 */

/** Same link shape as [applyConceptualAttributeTool] / XML `<Ligacao>` on `<Atributo>`. */
private fun ConceptualSchema.withAttributeCanvasLink(attributeId: Int, ownerId: Int): ConceptualSchema {
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

/**
 * Pascal [TModelo.OcultarAtributo]: `ma := a.MaxCard; if not a.Multivalorado then Ma := 0` — max cardinality
 * on [HiddenAttribute] must be **0** when the canvas attribute is not multi-valued, otherwise
 * [HiddenAttribute.isMultiValued] (which uses `maxCardinality > 0`) misclassifies simple (1,1) attributes.
 */
private fun cardinalityForHiddenStorage(a: SchemaElement.Attribute): AttributeCardinality =
    if (a.isMultiValued) a.cardinality
    else AttributeCardinality(a.cardinality.minCardinality, 0)

private fun leafCanvasAttributeToHidden(a: SchemaElement.Attribute): HiddenAttribute =
    HiddenAttribute(
        name = a.name,
        type = a.valueType,
        isIdentifier = a.isIdentifier,
        cardinality = cardinalityForHiddenStorage(a),
        position = a.position,
        children = emptyList(),
        nestedHiddenAttributes = a.hiddenAttributes,
        isOptional = a.isOptional,
    )

/**
 * Builds a [HiddenAttribute] tree for the canvas subtree rooted at [attrId] and the set of canvas
 * attribute ids to remove (including [attrId] and composite descendants).
 */
private fun canvasAttributeSubtreeToHidden(schema: ConceptualSchema, attrId: Int): Pair<HiddenAttribute, Set<Int>> {
    val a = schema.elements[attrId] as SchemaElement.Attribute
    val removed = mutableSetOf(attrId)
    val canvasChildHiddens = a.childAttributeIds.map { cid ->
        val (h, sub) = canvasAttributeSubtreeToHidden(schema, cid)
        removed += sub
        h
    }
    if (canvasChildHiddens.isEmpty()) {
        return leafCanvasAttributeToHidden(a) to removed
    }
    return HiddenAttribute(
        name = a.name,
        type = a.valueType,
        isIdentifier = a.isIdentifier,
        cardinality = cardinalityForHiddenStorage(a),
        position = a.position,
        children = canvasChildHiddens,
        nestedHiddenAttributes = a.hiddenAttributes,
        isOptional = a.isOptional,
    ) to removed
}

/** Where to append a new hidden attribute: composite parent holds its children's ocultos; otherwise the direct owner. */
internal fun hiddenAttributeStorageOwnerId(schema: ConceptualSchema, attr: SchemaElement.Attribute): Int {
    val owner = schema.elements[attr.ownerId] ?: return attr.ownerId
    return if (owner is SchemaElement.Attribute) owner.id else attr.ownerId
}

internal fun ultimateNonAttributeOwner(schema: ConceptualSchema, startOwnerId: Int): Int {
    var cur = startOwnerId
    while (schema.elements[cur] is SchemaElement.Attribute) {
        cur = (schema.elements[cur] as SchemaElement.Attribute).ownerId
    }
    return cur
}

private fun defaultRevealAttributePosition(schema: ConceptualSchema, canvasOwnerId: Int): ElementPosition {
    val owner = schema.elements[canvasOwnerId]?.position
    return if (owner != null) {
        ElementPosition(
            x = owner.x + owner.width + 8,
            y = owner.y,
            width = ConceptualPlacementDefaults.attributeWidth,
            height = ConceptualPlacementDefaults.attributeHeight,
        )
    } else {
        ElementPosition(0, 0, ConceptualPlacementDefaults.attributeWidth, ConceptualPlacementDefaults.attributeHeight)
    }
}

/** Materializes [h] and every [HiddenAttribute.children] subtree onto the canvas; [HiddenAttribute.nestedHiddenAttributes] stay ocultos on the new attribute. */
private fun materializeHiddenSubtree(
    schema: ConceptualSchema,
    canvasOwnerId: Int,
    h: HiddenAttribute,
): Pair<ConceptualSchema, Int> {
    val (s0, aid) = schema.allocateId()
    var s = s0
    val hasStoredPosition = h.position.x >= 0 && h.position.y >= 0
    val pos = if (hasStoredPosition) h.position else defaultRevealAttributePosition(schema, canvasOwnerId)
    val attr = SchemaElement.Attribute(
        id = aid,
        name = h.name,
        position = pos,
        observations = "",
        dictionary = "",
        labelStyle = ConceptualPlacementDefaults.labelStyle,
        hiddenAttributes = h.nestedHiddenAttributes,
        ownerId = canvasOwnerId,
        isIdentifier = h.isIdentifier,
        isMultiValued = h.isMultiValued,
        isOptional = h.isOptional,
        cardinality = h.cardinality,
        multiValuedCount = 0,
        valueType = h.type.ifBlank { "VARCHAR( )" },
        complement = "10",
        autoSize = true,
        deviationAngle = 10,
        childAttributeIds = emptyList(),
    )
    s = s.withElement(attr)
    s = s.withAttributeCanvasLink(aid, canvasOwnerId)
    val ownerEl = s.elements[canvasOwnerId]
    if (ownerEl is SchemaElement.Attribute) {
        s = s.withElement(ownerEl.copy(childAttributeIds = ownerEl.childAttributeIds + aid))
        s = relayoutCompositeSubtree(s, canvasOwnerId)
    }
    val childIds = mutableListOf<Int>()
    for (ch in h.children) {
        val (s2, cid) = materializeHiddenSubtree(s, aid, ch)
        s = s2
        childIds.add(cid)
    }
    if (childIds.isNotEmpty()) {
        val cur = s.elements[aid] as SchemaElement.Attribute
        s = s.withElement(cur.copy(childAttributeIds = childIds))
        s = relayoutCompositeSubtree(s, aid)
    }
    return s to aid
}

private fun removeHiddenBranchFromNode(
    node: HiddenAttribute,
    path: List<Int>,
): Pair<HiddenAttribute, HiddenAttribute>? {
    if (path.isEmpty()) return null
    val idx = path.first()
    if (path.size == 1) {
        return node.withBranchRemoved(idx)
    }
    val child = node.branchAt(idx) ?: return null
    val (removed, updatedChild) = removeHiddenBranchFromNode(child, path.drop(1)) ?: return null
    return removed to node.withBranchReplaced(idx, updatedChild)
}

/** Removes one node from a hidden-attribute forest; returns removed node and new root list. */
internal fun removeHiddenAttributeAtPath(
    attrs: List<HiddenAttribute>,
    path: List<Int>,
): Pair<HiddenAttribute, List<HiddenAttribute>>? {
    if (path.isEmpty()) return null
    val head = path.first()
    if (head !in attrs.indices) return null
    if (path.size == 1) {
        val removed = attrs[head]
        val rest = attrs.filterIndexed { i, _ -> i != head }
        return removed to rest
    }
    val parent = attrs[head]
    val (removed, updatedParent) = removeHiddenBranchFromNode(parent, path.drop(1)) ?: return null
    val out = attrs.toMutableList()
    out[head] = updatedParent
    return removed to out
}

fun hiddenAttributePathExists(schema: ConceptualSchema, storageOwnerId: Int, path: List<Int>): Boolean {
    val roots = schema.elements[storageOwnerId]?.hiddenAttributeList() ?: return false
    if (path.isEmpty()) return false
    if (path.size == 1) return path[0] in roots.indices
    if (path[0] !in roots.indices) return false
    var node = roots[path[0]]
    for (i in 1 until path.size) {
        val idx = path[i]
        if (idx !in 0 until node.mergedBranchCount()) return false
        if (i == path.lastIndex) return true
        node = node.branchAt(idx) ?: return false
    }
    return false
}

fun canHideCanvasAttributeMenu(schema: ConceptualSchema, selection: CanvasSelection): Boolean {
    val id = (selection as? CanvasSelection.Element)?.id ?: return false
    return schema.elements[id] is SchemaElement.Attribute
}

/**
 * Moves the selected canvas attribute (and any composite subtree) into the owner's [SchemaElement.hiddenAttributes]
 * list and removes it from the canvas.
 */
fun applyHideCanvasAttribute(schema: ConceptualSchema, selection: CanvasSelection): ConceptualSchema? {
    val id = (selection as? CanvasSelection.Element)?.id ?: return null
    val attr = schema.elements[id] as? SchemaElement.Attribute ?: return null
    val storageId = hiddenAttributeStorageOwnerId(schema, attr)
    val (hidden, removeIds) = canvasAttributeSubtreeToHidden(schema, id)
    var work = schema.withoutElements(removeIds, clearCompostoPersistedWhenEmptyCompositeParents = false)
    val holder = work.elements[storageId] ?: return null
    val nextHidden = holder.hiddenAttributeList() + hidden
    work = work.withElementHiddenList(storageId, nextHidden) ?: return null
    val hidAttr = schema.elements[id] as SchemaElement.Attribute
    val compositeParentId = hidAttr.ownerId.takeIf { ow -> schema.elements[ow] is SchemaElement.Attribute }
    if (compositeParentId != null) {
        val p = work.elements[compositeParentId] as? SchemaElement.Attribute
        if (p != null) {
            work = work.withElement(p.copy(compostoPersisted = true))
        }
    }
    work = organizeAttributesForConceptualOwner(work, ultimateNonAttributeOwner(work, storageId))
    return work.withNormalizedAttributeMultiValuedCounts()
}

fun canRevealHiddenAttributeMenu(
    schema: ConceptualSchema,
    selection: CanvasSelection,
    hiddenPath: List<Int>?,
): Boolean {
    if (hiddenPath == null || hiddenPath.size != 1) return false
    val ownerId = (selection as? CanvasSelection.Element)?.id ?: return false
    val el = schema.elements[ownerId] ?: return false
    if (el.hiddenAttributeList().isEmpty()) return false
    return hiddenAttributePathExists(schema, ownerId, hiddenPath)
}

/**
 * Materializes the hidden attribute at [path] under [storageOwnerId] (the selected element carrying [SchemaElement.hiddenAttributes]).
 * Only a **single** index into that list is allowed: composite ocultos must be revealed as a whole, not subtree-by-subtree.
 * Returns the updated schema and the new canvas attribute id.
 */
fun applyRevealHiddenAttribute(
    schema: ConceptualSchema,
    storageOwnerId: Int,
    path: List<Int>,
): Pair<ConceptualSchema, Int>? {
    if (path.size != 1) return null
    val holder = schema.elements[storageOwnerId] ?: return null
    val current = holder.hiddenAttributeList()
    val (removed, newList) = removeHiddenAttributeAtPath(current, path) ?: return null
    var work = schema.withElementHiddenList(storageOwnerId, newList) ?: return null
    val (out, newAttrId) = materializeHiddenSubtree(work, storageOwnerId, removed)
    val ultimate = ultimateNonAttributeOwner(out, storageOwnerId)
    val final = out.withNormalizedAttributeMultiValuedCounts()
        .let { organizeAttributesForConceptualOwner(it, ultimate) }
    return final to newAttrId
}
