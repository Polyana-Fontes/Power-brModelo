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
 * Ordered list of canvas attributes linked to [ownerId] via [Connection] (attribute → owner),
 * in the same order as [ConceptualSchema.connections] (mirrors Pascal `FLigacoes` walk on the owner).
 */
private fun linkedAttributesOrdered(schema: ConceptualSchema, ownerId: Int): List<SchemaElement.Attribute> {
    val out = ArrayList<SchemaElement.Attribute>()
    for (c in schema.connections) {
        if (c.elementIdB != ownerId) continue
        val a = schema.elements[c.elementIdA] as? SchemaElement.Attribute ?: continue
        if (a.ownerId != ownerId) continue
        out.add(a)
    }
    return out
}

/**
 * Repositions direct attributes of [ownerId] following [TBase.OrganizeAtributos] in `mer.pas`
 * (Divida-style spacing on left/right, stacked gaps on top/bottom).
 *
 * When [sideFilter] is non-null, only attributes whose **current** attach side matches are moved;
 * others keep their positions.
 */
private fun repositionDirectAttributesOfOwner(
    schema: ConceptualSchema,
    ownerId: Int,
    sideFilter: ConceptualAttributeAttachPonto?,
): ConceptualSchema {
    val owner = schema.elements[ownerId] ?: return schema
    val ownerPos = owner.position
    val linked = linkedAttributesOrdered(schema, ownerId)
    if (linked.isEmpty()) return schema

    val initialTotais = IntArray(5)
    for (a in linked) {
        val p = conceptualAttributeAttachPonto(ownerPos, a.position)
        if (p in 1..4) initialTotais[p]++
    }
    if (sideFilter != null && initialTotais[sideFilter.pascalCode] == 0) return schema

    val distancia = max(16, linked.maxOf { it.position.height })
    val gh = ConceptualPlacementDefaults.attributeHorizontalGap
    val gv = ConceptualPlacementDefaults.attributeVerticalGapBase

    var c1 = 0
    var c3 = 0
    var c2h = 0
    var c4h = 0
    var tb2 = initialTotais[2]
    var tb4 = initialTotais[4]

    var s = schema
    for (a in linked) {
        val p = conceptualAttributeAttachPonto(ownerPos, a.position)
        if (sideFilter != null && p != sideFilter.pascalCode) continue

        val ap = a.position
        val newPos = when (p) {
            1 -> {
                c1++
                val n = initialTotais[1]
                val tam = ownerPos.height / (n + 1)
                val py = ownerPos.y + tam * c1
                ElementPosition(ownerPos.x - ap.width - gh, py - ap.height / 2, ap.width, ap.height)
            }
            3 -> {
                c3++
                val n = initialTotais[3]
                val tam = ownerPos.height / (n + 1)
                val py = ownerPos.y + tam * c3
                ElementPosition(ownerPos.x + ownerPos.width + gh, py - ap.height / 2, ap.width, ap.height)
            }
            2 -> {
                c2h++
                val n = initialTotais[2]
                val tam = ownerPos.width / (n + 1)
                val px = ownerPos.x + tam * c2h
                val y = ownerPos.y - (gv + distancia * tb2)
                tb2--
                ElementPosition(px, y, ap.width, ap.height)
            }
            4 -> {
                c4h++
                val n = initialTotais[4]
                val tam = ownerPos.width / (n + 1)
                val px = ownerPos.x + tam * c4h
                val y = ownerPos.y + ownerPos.height + (gv + distancia * tb4)
                tb4--
                ElementPosition(px, y, ap.width, ap.height)
            }
            else -> continue
        }
        s = s.withElement(a.copy(position = newPos))
    }
    return s
}

/**
 * Lays out composite child attributes along [TBarraDeAtributos] (`mer.pas`), generalized to N children.
 */
internal fun organizeCompositeBarChildren(schema: ConceptualSchema, compositeId: Int): ConceptualSchema {
    val parent = schema.elements[compositeId] as? SchemaElement.Attribute ?: return schema
    if (!parent.isComposite || parent.childAttributeIds.isEmpty()) return schema
    val ownerElem = schema.elements[parent.ownerId] ?: return schema
    val ownerPos = ownerElem.position
    val attachPonto = conceptualAttributeAttachPonto(ownerPos, parent.position)
    val orientD = attachPonto == 1

    val p = parent.position
    val children = parent.childAttributeIds.mapNotNull { schema.elements[it] as? SchemaElement.Attribute }
    if (children.isEmpty()) return schema

    val n = children.size
    var barH = p.height * n + n * 2 - p.height
    if (barH < 2) barH = 2
    val wBarOff = if (!orientD) p.width - 5 else -2
    val barLeft = p.x + wBarOff
    val barTop = p.y + p.height / 2 - barH / 2
    val barW = 6

    var s = schema
    children.forEachIndexed { i, child ->
        val ch = child.position
        val attrW = ch.width
        val attrH = ch.height
        val wChild = if (orientD) -(attrW + 8) else 8
        val childX = barLeft + barW / 2 + wChild
        val childY = if (n > 1) {
            barTop + (barH / (n - 1)) * i - attrH / 2
        } else {
            barTop - attrH / 2 + 1
        }
        s = s.withElement(child.copy(position = ElementPosition(childX, childY, attrW, attrH)))
    }
    return s
}

/** Recursively reorganizes composite bars (Pascal `TAtributo.OrganizeAtributos` → `TBarraDeAtributos.OrganizeAtributos`). */
internal fun relayoutCompositeSubtree(schema: ConceptualSchema, compositeId: Int): ConceptualSchema {
    var s = organizeCompositeBarChildren(schema, compositeId)
    val parent = s.elements[compositeId] as? SchemaElement.Attribute ?: return s
    for (cid in parent.childAttributeIds) {
        val child = s.elements[cid] as? SchemaElement.Attribute ?: continue
        if (child.isComposite) s = relayoutCompositeSubtree(s, child.id)
    }
    return s
}

/** Full [TBase.OrganizeAtributos] for an entity / relationship / associative entity owner. */
fun organizeAttributesForConceptualOwner(schema: ConceptualSchema, ownerId: Int): ConceptualSchema {
    val owner = schema.elements[ownerId] ?: return schema
    if (owner !is SchemaElement.Entity &&
        owner !is SchemaElement.Relationship &&
        owner !is SchemaElement.AssociativeEntity
    ) {
        return schema
    }
    var s = repositionDirectAttributesOfOwner(schema, ownerId, sideFilter = null)
    for (a in s.attributesOf(ownerId)) {
        if (a.isComposite) s = relayoutCompositeSubtree(s, a.id)
    }
    return s
}

/** Reorganizes only attributes on [side] (used after placing a new attribute on that side). */
fun organizeAttributesOnOwnerSide(
    schema: ConceptualSchema,
    ownerId: Int,
    side: ConceptualAttributeAttachPonto,
): ConceptualSchema {
    val owner = schema.elements[ownerId] ?: return schema
    if (owner !is SchemaElement.Entity &&
        owner !is SchemaElement.Relationship &&
        owner !is SchemaElement.AssociativeEntity &&
        owner !is SchemaElement.Attribute
    ) {
        return schema
    }
    var s = repositionDirectAttributesOfOwner(schema, ownerId, sideFilter = side)
    val onSide = linkedAttributesOrdered(schema, ownerId).filter {
        conceptualAttributeAttachPonto(owner.position, it.position) == side.pascalCode
    }
    for (a in onSide) {
        val fresh = s.elements[a.id] as? SchemaElement.Attribute ?: continue
        if (fresh.isComposite) s = relayoutCompositeSubtree(s, fresh.id)
    }
    return s
}

/** Whether the ribbon "Organizar Atributos" action applies (canvas-visible attributes only). */
fun canOrganizeAttributesMenu(schema: ConceptualSchema, selectedElementId: Int): Boolean {
    return when (val e = schema.elements[selectedElementId]) {
        is SchemaElement.Entity,
        is SchemaElement.Relationship,
        is SchemaElement.AssociativeEntity,
        -> schema.attributesOf(selectedElementId).isNotEmpty()
        is SchemaElement.Attribute -> e.isComposite && schema.childAttributesOf(selectedElementId).isNotEmpty()
        else -> false
    }
}

/** Applies the conceptual **Operações → Organizar Atributos** command for the current selection. */
fun applyOrganizeAttributesMenuAction(schema: ConceptualSchema, selectedElementId: Int): ConceptualSchema? {
    if (!canOrganizeAttributesMenu(schema, selectedElementId)) return null
    return when (val e = schema.elements[selectedElementId]) {
        is SchemaElement.Entity,
        is SchemaElement.Relationship,
        is SchemaElement.AssociativeEntity,
        -> organizeAttributesForConceptualOwner(schema, selectedElementId)
        is SchemaElement.Attribute -> {
            var s = organizeCompositeBarChildren(schema, selectedElementId)
            val parent = s.elements[selectedElementId] as? SchemaElement.Attribute ?: return null
            for (cid in parent.childAttributeIds) {
                val child = s.elements[cid] as? SchemaElement.Attribute ?: continue
                if (child.isComposite) s = relayoutCompositeSubtree(s, child.id)
            }
            s
        }
        else -> null
    }
}
