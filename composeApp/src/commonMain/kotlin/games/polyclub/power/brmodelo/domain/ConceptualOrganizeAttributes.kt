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
import kotlin.math.roundToInt

// ── Divida alignment (matches SchemaRenderer.computeDividedPoints for rectangle owners) ──

private fun assocInnerDiamondPosOrganize(p: ElementPosition): ElementPosition =
    ElementPosition(
        x = p.x + 15,
        y = p.y + 15,
        width = (p.width - 30).coerceAtLeast(10),
        height = (p.height - 30).coerceAtLeast(10),
    )

private fun associativeConnectionUsesInnerDiamondOrganize(
    elem: SchemaElement,
    otherElem: SchemaElement,
    conn: Connection?,
): Boolean {
    if (elem !is SchemaElement.AssociativeEntity || otherElem is SchemaElement.Attribute) return true
    if (conn == null) return true
    return when (elem.id) {
        conn.elementIdA -> !conn.useAssociativeOuterForEndA
        conn.elementIdB -> !conn.useAssociativeOuterForEndB
        else -> true
    }
}

/**
 * Mirrors [games.polyclub.power.brmodelo.ui.canvas.SchemaRenderer]'s `computeNonAttrPonto` / `TLigacao.Ative`.
 */
private fun computeNonAttrPontoForOrganize(
    elemPos: ElementPosition,
    otherPos: ElementPosition,
    orientation: LineOrientation,
    isE1: Boolean,
): Int {
    val e1Pos = if (isE1) elemPos else otherPos
    val e2Pos = if (isE1) otherPos else elemPos

    val e1r = e1Pos.x + e1Pos.width
    val e1b = e1Pos.y + e1Pos.height
    val e2r = e2Pos.x + e2Pos.width
    val e2b = e2Pos.y + e2Pos.height

    val isH = orientation == LineOrientation.HORIZONTAL
    val DIST = 20

    val c1fwd = e1r < e2Pos.x - DIST && e1b < e2Pos.y - DIST
    val c1rev = e2r < e1Pos.x - DIST && e2b < e1Pos.y - DIST
    if (c1fwd || c1rev) {
        val swapped = c1rev
        val actualIsE1 = if (swapped) !isE1 else isE1
        return if (!isH) {
            if (actualIsE1) 4 else 1
        } else {
            if (actualIsE1) 3 else 2
        }
    }

    val c2fwd = e1r < e2Pos.x - DIST && e2b < e1Pos.y - DIST
    val c2rev = e2r < e1Pos.x - DIST && e1b < e2Pos.y - DIST
    if (c2fwd || c2rev) {
        val swapped = c2rev
        val actualIsE1 = if (swapped) !isE1 else isE1
        return if (isH) {
            if (actualIsE1) 2 else 1
        } else {
            if (actualIsE1) 3 else 4
        }
    }

    if (e1b < e2Pos.y - 4) return if (isE1) 4 else 2
    if (e2b < e1Pos.y - 4) return if (isE1) 2 else 4

    if (e1r < e2Pos.x - 4) return if (isE1) 3 else 1
    if (e2r < e1Pos.x - 4) return if (isE1) 1 else 3

    return if (isH) {
        if (isE1) { if (e1Pos.x <= e2Pos.x) 3 else 1 }
        else { if (e1Pos.y <= e2Pos.y) 2 else 4 }
    } else {
        if (isE1) { if (e1Pos.y <= e2Pos.y) 4 else 2 }
        else { if (e1Pos.x <= e2Pos.x) 1 else 3 }
    }
}

private fun connectionPontoForOrganize(
    elem: SchemaElement,
    otherElem: SchemaElement,
    schema: ConceptualSchema,
    conn: Connection,
): Int {
    if (elem is SchemaElement.Attribute) {
        val ownerPos = schema.elements[elem.ownerId]?.position
        val ellipseOnLeft = ownerPos?.let { conceptualAttributeAttachPonto(it, elem.position) != 1 } ?: false
        return if (otherElem is SchemaElement.Attribute && otherElem.ownerId == elem.id) {
            if (ellipseOnLeft) 3 else 1
        } else {
            if (ellipseOnLeft) 1 else 3
        }
    }
    if (otherElem is SchemaElement.Attribute) {
        val isE1 = conn.elementIdA == elem.id
        return computeNonAttrPontoForOrganize(
            elem.position,
            otherElem.position,
            conn.orientation,
            isE1,
        )
    }
    val isE1 = conn.elementIdA == elem.id
    val effectivePos = if (elem is SchemaElement.AssociativeEntity && otherElem !is SchemaElement.Attribute) {
        if (associativeConnectionUsesInnerDiamondOrganize(elem, otherElem, conn)) {
            assocInnerDiamondPosOrganize(elem.position)
        } else {
            elem.position
        }
    } else {
        elem.position
    }
    val effectiveOtherPos = if (otherElem is SchemaElement.AssociativeEntity && elem !is SchemaElement.Attribute) {
        if (associativeConnectionUsesInnerDiamondOrganize(otherElem, elem, conn)) {
            assocInnerDiamondPosOrganize(otherElem.position)
        } else {
            otherElem.position
        }
    } else {
        otherElem.position
    }
    return computeNonAttrPontoForOrganize(
        effectivePos,
        effectiveOtherPos,
        conn.orientation,
        isE1,
    )
}

private fun connectionIdBetweenOrganize(schema: ConceptualSchema, idA: Int, idB: Int): Int? =
    schema.connections.firstOrNull { c ->
        (c.elementIdA == idA && c.elementIdB == idB) || (c.elementIdA == idB && c.elementIdB == idA)
    }?.id

private data class OrganizeDividaSlot(val connId: Int, val otherId: Int)

private fun collectOrganizeDividaSlots(
    schema: ConceptualSchema,
    ownerId: Int,
    targetPonto: Int,
): List<OrganizeDividaSlot> {
    val owner = schema.elements[ownerId] ?: return emptyList()
    if (owner is SchemaElement.Relationship || owner is SchemaElement.SelfRelationship) return emptyList()

    val slots = ArrayList<OrganizeDividaSlot>()
    for (conn in schema.connections) {
        val isA = conn.elementIdA == ownerId
        val isB = conn.elementIdB == ownerId
        if (!isA && !isB) continue
        val otherId = if (isA) conn.elementIdB else conn.elementIdA
        val otherElem = schema.elements[otherId] ?: continue

        if (otherElem is SchemaElement.Attribute) {
            if (owner is SchemaElement.Attribute && owner.ownerId == otherId) continue
            if (owner is SchemaElement.Attribute && otherElem.ownerId == owner.id) continue
            val p = connectionPontoForOrganize(owner, otherElem, schema, conn)
            if (p != targetPonto) continue
            slots.add(OrganizeDividaSlot(conn.id, otherId))
        } else {
            if (owner is SchemaElement.Attribute) continue
            val p = connectionPontoForOrganize(owner, otherElem, schema, conn)
            if (p != targetPonto) continue
            slots.add(OrganizeDividaSlot(conn.id, otherId))
        }
    }
    return slots
}

/**
 * Maps connection id → coordinate along the owner edge (Y for ponto 1/3, X for 2/4),
 * matching [games.polyclub.power.brmodelo.ui.canvas.SchemaRenderer]'s Divida pass for
 * [SchemaElement.Entity] / [SchemaElement.AssociativeEntity].
 */
private fun dividaAlongCoordinateByConnectionForOrganize(
    schema: ConceptualSchema,
    ownerId: Int,
    targetPonto: Int,
): Map<Int, Float> {
    val owner = schema.elements[ownerId] ?: return emptyMap()
    val pos = owner.position
    val slots = collectOrganizeDividaSlots(schema, ownerId, targetPonto)
    if (slots.isEmpty()) return emptyMap()

    val (anchorStart, edgeLen) = if (targetPonto == 1 || targetPonto == 3) {
        pos.y.toFloat() to pos.height.toFloat()
    } else {
        pos.x.toFloat() to pos.width.toFloat()
    }

    val sorted = slots.sortedWith(
        compareBy(
            { slot ->
                val op = schema.elements[slot.otherId]?.position ?: ElementPosition(0, 0, 0, 0)
                when (targetPonto) {
                    1, 3 -> op.y.toFloat()
                    else -> op.x.toFloat()
                }
            },
            OrganizeDividaSlot::connId,
        ),
    )

    val nTotal = sorted.size
    val out = HashMap<Int, Float>(nTotal)
    if (nTotal == 1) {
        val coord = when (targetPonto) {
            1, 3 -> pos.y + pos.height / 2f
            else -> pos.x + pos.width / 2f
        }
        out[sorted[0].connId] = coord
        return out
    }

    val tam = (edgeLen.toInt()) / (nTotal + 1)
    for ((idx, slot) in sorted.withIndex()) {
        out[slot.connId] = anchorStart + (tam * (idx + 1)).toFloat()
    }
    return out
}

private fun rectangleOwnerUsesCanvasDivida(schema: ConceptualSchema, ownerId: Int): Boolean {
    val o = schema.elements[ownerId] ?: return false
    return o is SchemaElement.Entity || o is SchemaElement.AssociativeEntity
}

/**
 * `TBase.OrganizeAtributos` (mer.pas) walks [FLigacoes] and, for each attribute link, reads `PT`
 * from `TLigacao` — already Divida-adjusted when non-attribute legs share that edge. When **every**
 * link on a given `ponto` is attribute→owner, the same procedure still uses `Totais` + fixed
 * `Width/(n+1)` / `Height/(n+1)` steps in **connection list order**; re-sorting only attributes by
 * `Top`/`Left` would reshuffle coordinates vs Pascal.
 */
private fun ownerEdgeSharesNonAttributeOnPonto(
    schema: ConceptualSchema,
    ownerId: Int,
    targetPonto: Int,
): Boolean {
    val owner = schema.elements[ownerId] ?: return false
    if (owner !is SchemaElement.Entity && owner !is SchemaElement.AssociativeEntity) return false
    for (conn in schema.connections) {
        val isA = conn.elementIdA == ownerId
        val isB = conn.elementIdB == ownerId
        if (!isA && !isB) continue
        val otherId = if (isA) conn.elementIdB else conn.elementIdA
        val other = schema.elements[otherId] ?: continue
        if (other is SchemaElement.Attribute) continue
        if (connectionPontoForOrganize(owner, other, schema, conn) == targetPonto) return true
    }
    return false
}

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

private fun isOrganizeOwnerKind(el: SchemaElement): Boolean =
    el is SchemaElement.Entity ||
        el is SchemaElement.Relationship ||
        el is SchemaElement.AssociativeEntity ||
        el is SchemaElement.SelfRelationship

private fun isDirectOrganizeOwner(schema: ConceptualSchema, ownerId: Int): Boolean =
    schema.elements[ownerId]?.let { isOrganizeOwnerKind(it) } == true

/**
 * Walks attribute → owner until the conceptual canvas owner (entity / relationship / associative / self-rel).
 */
private fun canvasOwnerForOrganizeRoot(schema: ConceptualSchema, attributeElementId: Int): Int? {
    var cur = attributeElementId
    repeat(schema.elements.size + 2) {
        val el = schema.elements[cur] ?: return null
        when (el) {
            is SchemaElement.Attribute -> cur = el.ownerId
            is SchemaElement.Entity,
            is SchemaElement.Relationship,
            is SchemaElement.AssociativeEntity,
            is SchemaElement.SelfRelationship,
            -> return el.id
            else -> return null
        }
    }
    return null
}

private fun canvasSelectionElementIds(selection: CanvasSelection): Set<Int> = when (selection) {
    CanvasSelection.None -> emptySet()
    is CanvasSelection.Element -> setOf(selection.id)
    is CanvasSelection.Multiple -> selection.elementIds
    is CanvasSelection.Cardinality -> emptySet()
}

private data class OwnerRepositionTask(val ownerId: Int, val attributeIdFilter: Set<Int>?)

private data class CompositeBarTask(val compositeId: Int, val childIdFilter: Set<Int>?)

private data class OrganizePlan(
    val ownerTasks: List<OwnerRepositionTask>,
    val compositeTasks: List<CompositeBarTask>,
)

private fun compositeBarDepthFromCanvasOwner(schema: ConceptualSchema, compositeId: Int): Int {
    var depth = 0
    var cur = compositeId
    while (true) {
        val a = schema.elements[cur] as? SchemaElement.Attribute ?: return depth
        val owner = schema.elements[a.ownerId] ?: return depth
        when (owner) {
            is SchemaElement.Entity,
            is SchemaElement.Relationship,
            is SchemaElement.AssociativeEntity,
            is SchemaElement.SelfRelationship,
            -> return depth
            is SchemaElement.Attribute -> {
                depth++
                cur = owner.id
            }
            else -> return depth
        }
    }
}

private fun buildOrganizePlan(schema: ConceptualSchema, selected: Set<Int>): OrganizePlan {
    if (selected.isEmpty()) return OrganizePlan(emptyList(), emptyList())

    val selectedOwnerIds = selected.filter { id ->
        schema.elements[id]?.let { isOrganizeOwnerKind(it) } == true
    }.toSet()

    val fullOwners = LinkedHashSet<Int>()
    for (o in selectedOwnerIds) {
        val anyAttrUnderSelected = selected.any { sid ->
            schema.elements[sid] is SchemaElement.Attribute &&
                canvasOwnerForOrganizeRoot(schema, sid) == o
        }
        if (!anyAttrUnderSelected) {
            fullOwners.add(o)
        }
    }

    val partialFilters = mutableMapOf<Int, MutableSet<Int>>()
    for (sid in selected) {
        val a = schema.elements[sid] as? SchemaElement.Attribute ?: continue
        if (!isDirectOrganizeOwner(schema, a.ownerId)) continue
        // Composite parents are laid out only via [CompositeBarTask] (bar + nested composites), never via
        // [repositionDirectAttributesOfOwner], so "Organizar" with only a composite selected does not snap
        // the composite box back along Divida relative to the entity.
        if (a.isComposite) continue
        val o = a.ownerId
        if (o in fullOwners) continue
        partialFilters.getOrPut(o) { mutableSetOf() }.add(sid)
    }

    val ownerTasks = ArrayList<OwnerRepositionTask>()
    for (o in fullOwners) {
        ownerTasks.add(OwnerRepositionTask(o, attributeIdFilter = null))
    }
    for ((o, ids) in partialFilters) {
        if (ids.isNotEmpty()) {
            ownerTasks.add(OwnerRepositionTask(o, attributeIdFilter = ids.toSet()))
        }
    }

    val compositeTasks = ArrayList<CompositeBarTask>()
    for (el in schema.elements.values) {
        val c = el as? SchemaElement.Attribute ?: continue
        if (!c.isComposite || c.childAttributeIds.isEmpty()) continue
        val selCh = c.childAttributeIds.filter { it in selected }.toSet()
        when {
            selCh.isNotEmpty() -> compositeTasks.add(CompositeBarTask(c.id, selCh))
            c.id in selected -> compositeTasks.add(CompositeBarTask(c.id, childIdFilter = null))
            else -> Unit
        }
    }

    val dedupedComposites = compositeTasks.filterNot { task ->
        task.childIdFilter == null &&
            compositeBarAlreadyRelayoutedByOwnerTasks(schema, ownerTasks, task.compositeId)
    }

    return OrganizePlan(ownerTasks, dedupedComposites)
}

private fun compositeBarAlreadyRelayoutedByOwnerTasks(
    schema: ConceptualSchema,
    ownerTasks: List<OwnerRepositionTask>,
    compositeId: Int,
): Boolean {
    val c = schema.elements[compositeId] as? SchemaElement.Attribute ?: return false
    if (!isDirectOrganizeOwner(schema, c.ownerId)) return false
    val o = c.ownerId
    for (t in ownerTasks) {
        if (t.ownerId != o) continue
        if (t.attributeIdFilter == null) return true
        if (compositeId in t.attributeIdFilter) return true
    }
    return false
}

/**
 * Repositions direct attributes of [ownerId] following [TBase.OrganizeAtributos] in `mer.pas`
 * (Divida-style spacing on left/right, stacked gaps on top/bottom).
 *
 * For [SchemaElement.Entity] and [SchemaElement.AssociativeEntity], when a canvas edge (`ponto`
 * 1–4) also carries **non-attribute** links (relationship, specialization, another entity, …),
 * attachment coordinates along that edge follow the same Divida pass as the renderer (same
 * `ponto`, same sort, same `tam`), mirroring Pascal reading `PT` from `TLigacao`. Edges with
 * **attributes only** keep the legacy `Width/(n+1)` / `Height/(n+1)` spacing in [linked] connection
 * order, matching `TBase.OrganizeAtributos` without reshuffling by `Top`/`Left`.
 *
 * [SchemaElement.Relationship] and [SchemaElement.SelfRelationship] keep the legacy Pascal-style
 * spacing only (diamond attribute routing does not share the rectangle Divida queue).
 *
 * When [sideFilter] is non-null, only attributes whose **current** attach side matches are moved;
 * others keep their positions.
 *
 * When [attributeIdFilter] is non-null, only those direct attributes (by id) are repositioned;
 * spacing counts use that subset per side. Others keep their positions.
 */
private fun repositionDirectAttributesOfOwner(
    schema: ConceptualSchema,
    ownerId: Int,
    sideFilter: ConceptualAttributeAttachPonto?,
    attributeIdFilter: Set<Int>?,
): ConceptualSchema {
    val owner = schema.elements[ownerId] ?: return schema
    if (!isOrganizeOwnerKind(owner)) return schema
    val ownerPos = owner.position
    val linked = linkedAttributesOrdered(schema, ownerId)
    if (linked.isEmpty()) return schema

    val active = if (attributeIdFilter == null) linked else linked.filter { it.id in attributeIdFilter }
    if (active.isEmpty()) return schema

    val initialTotais = IntArray(5)
    for (a in active) {
        val p = conceptualAttributeAttachPonto(ownerPos, a.position)
        if (p in 1..4) initialTotais[p]++
    }
    if (sideFilter != null && initialTotais[sideFilter.pascalCode] == 0) return schema

    val distancia = max(16, active.maxOf { it.position.height })
    val gh = ConceptualPlacementDefaults.attributeHorizontalGap
    val gv = ConceptualPlacementDefaults.attributeVerticalGapBase

    val useRectDivida = rectangleOwnerUsesCanvasDivida(schema, ownerId)
    val edgeDiv1 = useRectDivida && ownerEdgeSharesNonAttributeOnPonto(schema, ownerId, 1)
    val edgeDiv2 = useRectDivida && ownerEdgeSharesNonAttributeOnPonto(schema, ownerId, 2)
    val edgeDiv3 = useRectDivida && ownerEdgeSharesNonAttributeOnPonto(schema, ownerId, 3)
    val edgeDiv4 = useRectDivida && ownerEdgeSharesNonAttributeOnPonto(schema, ownerId, 4)
    val divP1 = if (edgeDiv1) dividaAlongCoordinateByConnectionForOrganize(schema, ownerId, 1) else emptyMap()
    val divP2 = if (edgeDiv2) dividaAlongCoordinateByConnectionForOrganize(schema, ownerId, 2) else emptyMap()
    val divP3 = if (edgeDiv3) dividaAlongCoordinateByConnectionForOrganize(schema, ownerId, 3) else emptyMap()
    val divP4 = if (edgeDiv4) dividaAlongCoordinateByConnectionForOrganize(schema, ownerId, 4) else emptyMap()

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
        if (attributeIdFilter != null && a.id !in attributeIdFilter) continue

        val ap = a.position
        val connId = connectionIdBetweenOrganize(schema, a.id, ownerId)
        val divCoord = when (p) {
            1 -> connId?.let { divP1[it] }
            2 -> connId?.let { divP2[it] }
            3 -> connId?.let { divP3[it] }
            4 -> connId?.let { divP4[it] }
            else -> null
        }

        val newPos = when (p) {
            1 -> {
                val cy = divCoord ?: run {
                    c1++
                    val n = initialTotais[1]
                    val tam = ownerPos.height / (n + 1)
                    (ownerPos.y + tam * c1).toFloat()
                }
                val py = (cy - ap.height / 2f).roundToInt()
                ElementPosition(ownerPos.x - ap.width - gh, py, ap.width, ap.height)
            }
            3 -> {
                val cy = divCoord ?: run {
                    c3++
                    val n = initialTotais[3]
                    val tam = ownerPos.height / (n + 1)
                    (ownerPos.y + tam * c3).toFloat()
                }
                val py = (cy - ap.height / 2f).roundToInt()
                ElementPosition(ownerPos.x + ownerPos.width + gh, py, ap.width, ap.height)
            }
            2 -> {
                // mer.pas: SetBounds(PT.X, …) — PT.X is the attribute *Left*, same as Divida slot on the
                // top edge (not snap minus half-width; that shifted boxes left vs Pascal / legacy Kotlin).
                val px = when {
                    divCoord != null -> divCoord.roundToInt()
                    else -> {
                        c2h++
                        val n = initialTotais[2]
                        val tam = ownerPos.width / (n + 1)
                        ownerPos.x + tam * c2h
                    }
                }
                val y = ownerPos.y - (gv + distancia * tb2)
                tb2--
                ElementPosition(px, y, ap.width, ap.height)
            }
            4 -> {
                val px = when {
                    divCoord != null -> divCoord.roundToInt()
                    else -> {
                        c4h++
                        val n = initialTotais[4]
                        val tam = ownerPos.width / (n + 1)
                        ownerPos.x + tam * c4h
                    }
                }
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
 * When [childIdFilter] is non-null, only those children are repositioned along the bar (subset spacing).
 */
internal fun organizeCompositeBarChildren(
    schema: ConceptualSchema,
    compositeId: Int,
    childIdFilter: Set<Int>? = null,
): ConceptualSchema {
    val parent = schema.elements[compositeId] as? SchemaElement.Attribute ?: return schema
    if (!parent.isComposite || parent.childAttributeIds.isEmpty()) return schema
    val ownerElem = schema.elements[parent.ownerId] ?: return schema
    val ownerPos = ownerElem.position
    val attachPonto = conceptualAttributeAttachPonto(ownerPos, parent.position)
    val orientD = attachPonto == 1

    val p = parent.position
    val allChildren = parent.childAttributeIds.mapNotNull { schema.elements[it] as? SchemaElement.Attribute }
    val children = if (childIdFilter == null) allChildren else allChildren.filter { it.id in childIdFilter }
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
    var s = organizeCompositeBarChildren(schema, compositeId, childIdFilter = null)
    val parent = s.elements[compositeId] as? SchemaElement.Attribute ?: return s
    for (cid in parent.childAttributeIds) {
        val child = s.elements[cid] as? SchemaElement.Attribute ?: continue
        if (child.isComposite) s = relayoutCompositeSubtree(s, child.id)
    }
    return s
}

/** Full [TBase.OrganizeAtributos] for an entity / relationship / associative entity / self-relationship owner. */
fun organizeAttributesForConceptualOwner(schema: ConceptualSchema, ownerId: Int): ConceptualSchema {
    val owner = schema.elements[ownerId] ?: return schema
    if (!isOrganizeOwnerKind(owner)) {
        return schema
    }
    var s = repositionDirectAttributesOfOwner(schema, ownerId, sideFilter = null, attributeIdFilter = null)
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
    if (!isOrganizeOwnerKind(owner)) {
        return schema
    }
    var s = repositionDirectAttributesOfOwner(schema, ownerId, sideFilter = side, attributeIdFilter = null)
    val onSide = linkedAttributesOrdered(schema, ownerId).filter {
        conceptualAttributeAttachPonto(owner.position, it.position) == side.pascalCode
    }
    for (a in onSide) {
        val fresh = s.elements[a.id] as? SchemaElement.Attribute ?: continue
        if (fresh.isComposite) s = relayoutCompositeSubtree(s, fresh.id)
    }
    return s
}

private fun applyOwnerRepositionTask(schema: ConceptualSchema, task: OwnerRepositionTask): ConceptualSchema {
    val filter = task.attributeIdFilter
    var s = repositionDirectAttributesOfOwner(schema, task.ownerId, sideFilter = null, attributeIdFilter = filter)
    val compositesUnder = s.attributesOf(task.ownerId).filter { it.isComposite }
    for (c in compositesUnder) {
        val relayout = when {
            filter == null -> true
            c.id in filter -> true
            else -> false
        }
        if (relayout) s = relayoutCompositeSubtree(s, c.id)
    }
    return s
}

private fun applyCompositeBarTask(schema: ConceptualSchema, task: CompositeBarTask): ConceptualSchema {
    var s = organizeCompositeBarChildren(schema, task.compositeId, task.childIdFilter)
    val parent = s.elements[task.compositeId] as? SchemaElement.Attribute ?: return s
    val childIds: Iterable<Int> = if (task.childIdFilter == null) {
        parent.childAttributeIds
    } else {
        task.childIdFilter
    }
    for (cid in childIds) {
        val ch = s.elements[cid] as? SchemaElement.Attribute ?: continue
        if (ch.isComposite) s = relayoutCompositeSubtree(s, ch.id)
    }
    return s
}

private fun applyOrganizePlan(schema: ConceptualSchema, plan: OrganizePlan): ConceptualSchema {
    val depthSchema = schema
    var s = schema
    for (task in plan.ownerTasks) {
        s = applyOwnerRepositionTask(s, task)
    }
    val sortedComposites = plan.compositeTasks.sortedBy { compositeBarDepthFromCanvasOwner(depthSchema, it.compositeId) }
    for (task in sortedComposites) {
        s = applyCompositeBarTask(s, task)
    }
    return s
}

/** Whether **Operações → Organizar Atributos** applies to the current [CanvasSelection]. */
fun canOrganizeAttributesMenuSelection(schema: ConceptualSchema, selection: CanvasSelection): Boolean {
    val selected = canvasSelectionElementIds(selection)
    if (selected.isEmpty()) return false
    val plan = buildOrganizePlan(schema, selected)
    for (t in plan.ownerTasks) {
        if (t.attributeIdFilter == null) {
            if (schema.attributesOf(t.ownerId).isNotEmpty()) return true
        } else if (t.attributeIdFilter.isNotEmpty()) {
            return true
        }
    }
    for (t in plan.compositeTasks) {
        val c = schema.elements[t.compositeId] as? SchemaElement.Attribute ?: continue
        if (t.childIdFilter == null && c.childAttributeIds.isNotEmpty()) return true
        if (t.childIdFilter?.isNotEmpty() == true) return true
    }
    return false
}

/** Whether the ribbon "Organizar Atributos" action applies (canvas-visible attributes only). */
fun canOrganizeAttributesMenu(schema: ConceptualSchema, selectedElementId: Int): Boolean =
    canOrganizeAttributesMenuSelection(schema, CanvasSelection.Element(selectedElementId))

/** Applies **Operações → Organizar Atributos** for the current [CanvasSelection] (multi-select aware). */
fun applyOrganizeAttributesMenuAction(schema: ConceptualSchema, selection: CanvasSelection): ConceptualSchema? {
    if (!canOrganizeAttributesMenuSelection(schema, selection)) return null
    val plan = buildOrganizePlan(schema, canvasSelectionElementIds(selection))
    return applyOrganizePlan(schema, plan)
}

/** Applies the conceptual **Operações → Organizar Atributos** command for a single selected element id. */
fun applyOrganizeAttributesMenuAction(schema: ConceptualSchema, selectedElementId: Int): ConceptualSchema? =
    applyOrganizeAttributesMenuAction(schema, CanvasSelection.Element(selectedElementId))
