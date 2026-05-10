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

import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaXmlParser
import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaXmlSerializer
import kotlin.math.roundToInt

internal const val BRMODELO_CLIPBOARD_MAGIC_V1 = "BRMODELO_CLIPBOARD_V1"

private const val PASTE_SAME_TAB_NUDGE_PX = 24

/** Bounding box in model coordinates (used for paste anchoring). */
internal data class ClipboardBounds(
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
) {
    val centerX: Int get() = (minX + maxX) / 2
    val centerY: Int get() = (minY + maxY) / 2
}

/** Serialized next to the XML body for anchoring and cross-tab behaviour. */
internal data class ClipboardMeta(
    val sourceEditorTabId: Long,
    val bounds: ClipboardBounds,
)

internal data class ConceptualPasteContext(
    val targetSchema: ConceptualSchema,
    val targetEditorTabId: Long,
    val layoutWidthPx: Float,
    val layoutHeightPx: Float,
    val panX: Float,
    val panY: Float,
    /** Model-space point under the pointer when [isPointerOverCanvas] is true. */
    val pointerModelX: Float?,
    val pointerModelY: Float?,
    val isPointerOverCanvas: Boolean,
)

internal data class ConceptualPasteResult(
    val schema: ConceptualSchema,
    val pastedElementIds: Set<Int>,
    val selection: CanvasSelection,
)

/** Element ids to include when copying the current selection (includes attribute trees and cardinality endpoints). */
fun elementIdsForClipboard(schema: ConceptualSchema, selection: CanvasSelection): Set<Int> {
    val (e, c) = selection.toMultiPickSets()
    val seeds = e.toMutableSet()
    for (cid in c) {
        val conn = schema.connections.firstOrNull { it.id == cid } ?: continue
        seeds.add(conn.elementIdA)
        seeds.add(conn.elementIdB)
    }
    if (seeds.isEmpty()) return emptySet()
    return seeds + collectAttributeTreeIdsFromSeeds(schema, seeds)
}

fun connectionsForClipboard(schema: ConceptualSchema, elementIds: Set<Int>): List<Connection> =
    schema.connections.filter { it.elementIdA in elementIds && it.elementIdB in elementIds }
        .sortedBy { it.id }

/** Subgraph as a standalone [ConceptualSchema] suitable for XML serialization. */
fun extractClipboardFragment(schema: ConceptualSchema, elementIds: Set<Int>): ConceptualSchema? {
    if (elementIds.isEmpty()) return null
    val ordered = LinkedHashMap<Int, SchemaElement>()
    for (id in elementIds.toList().sorted()) {
        schema.elements[id]?.let { ordered[id] = it }
    }
    if (ordered.isEmpty()) return null
    val conns = connectionsForClipboard(schema, elementIds)
    val maxId = (ordered.keys + conns.map { it.id }).maxOrNull() ?: 0
    return ConceptualSchema(
        name = "",
        filePath = "",
        author = "",
        observations = "",
        version = schema.version,
        elements = ordered,
        connections = conns,
        nextId = maxId + 1,
        openedFromBrm = false,
    )
}

internal fun ConceptualSchema.clipboardBounds(): ClipboardBounds? {
    var minX = Int.MAX_VALUE
    var minY = Int.MAX_VALUE
    var maxX = Int.MIN_VALUE
    var maxY = Int.MIN_VALUE
    fun acc(p: ElementPosition) {
        minX = minOf(minX, p.x)
        minY = minOf(minY, p.y)
        maxX = maxOf(maxX, p.x + p.width)
        maxY = maxOf(maxY, p.y + p.height)
    }
    fun walkHidden(h: HiddenAttribute) {
        acc(h.position)
        h.children.forEach { walkHidden(it) }
        h.nestedHiddenAttributes.forEach { walkHidden(it) }
    }
    for (el in elements.values) {
        acc(el.position)
        for (h in el.hiddenAttributes) walkHidden(h)
    }
    for (c in connections) c.cardinalityPosition?.let { acc(it) }
    if (minX == Int.MAX_VALUE) return null
    return ClipboardBounds(minX, minY, maxX, maxY)
}

private fun translateHiddenAttributes(attrs: List<HiddenAttribute>, dx: Int, dy: Int): List<HiddenAttribute> =
    attrs.map { h ->
        h.copy(
            position = h.position.copy(x = h.position.x + dx, y = h.position.y + dy),
            children = translateHiddenAttributes(h.children, dx, dy),
            nestedHiddenAttributes = translateHiddenAttributes(h.nestedHiddenAttributes, dx, dy),
        )
    }

private fun translateElement(el: SchemaElement, dx: Int, dy: Int): SchemaElement {
    val hid = translateHiddenAttributes(el.hiddenAttributes, dx, dy)
    val p = el.position.copy(x = el.position.x + dx, y = el.position.y + dy)
    return when (el) {
        is SchemaElement.Entity -> el.copy(position = p, hiddenAttributes = hid)
        is SchemaElement.Relationship -> el.copy(position = p, hiddenAttributes = hid)
        is SchemaElement.AssociativeEntity -> el.copy(position = p, hiddenAttributes = hid)
        is SchemaElement.Attribute -> el.copy(position = p, hiddenAttributes = hid)
        is SchemaElement.Specialization -> el.copy(position = p, hiddenAttributes = hid)
        is SchemaElement.SelfRelationship -> el.copy(position = p, hiddenAttributes = hid)
        is SchemaElement.Annotation -> el.copy(position = p, hiddenAttributes = hid)
    }
}

private fun translateConnection(c: Connection, dx: Int, dy: Int): Connection {
    val card = c.cardinalityPosition?.copy(
        x = c.cardinalityPosition.x + dx,
        y = c.cardinalityPosition.y + dy,
    )
    return c.copy(cardinalityPosition = card)
}

internal fun translateConceptualSchema(schema: ConceptualSchema, dx: Int, dy: Int): ConceptualSchema =
    schema.copy(
        elements = schema.elements.mapValues { (_, v) -> translateElement(v, dx, dy) },
        connections = schema.connections.map { translateConnection(it, dx, dy) },
    )

internal fun encodeClipboardPayload(meta: ClipboardMeta, fragment: ConceptualSchema): String =
    buildString {
        appendLine(BRMODELO_CLIPBOARD_MAGIC_V1)
        append("META:sourceTab=")
        append(meta.sourceEditorTabId)
        append(";minX=")
        append(meta.bounds.minX)
        append(";minY=")
        append(meta.bounds.minY)
        append(";maxX=")
        append(meta.bounds.maxX)
        append(";maxY=")
        append(meta.bounds.maxY)
        appendLine()
        append(ConceptualSchemaXmlSerializer.serialize(fragment))
    }

internal fun decodeClipboardPayload(text: String): Pair<ClipboardMeta?, ConceptualSchema>? {
    val t = text.trim()
    if (!t.startsWith(BRMODELO_CLIPBOARD_MAGIC_V1)) return null
    val afterMagic = t.removePrefix(BRMODELO_CLIPBOARD_MAGIC_V1).trimStart()
    val xmlStart = afterMagic.indexOf('<')
    if (xmlStart < 0) return null
    val header = afterMagic.substring(0, xmlStart).trim()
    val xml = afterMagic.substring(xmlStart)
    val metaLine = header.lineSequence().firstOrNull { it.startsWith("META:") }
    val meta = metaLine?.let(::parseClipboardMetaLine)
    val schema = try {
        ConceptualSchemaXmlParser.parse(xml.encodeToByteArray())
    } catch (_: Exception) {
        return null
    }
    return meta to schema
}

private fun parseClipboardMetaLine(line: String): ClipboardMeta? {
    val body = line.removePrefix("META:").trim()
    val parts = body.split(';').associate { kv ->
        val idx = kv.indexOf('=')
        if (idx <= 0) "" to ""
        else kv.substring(0, idx).trim() to kv.substring(idx + 1).trim()
    }
    val tab = parts["sourceTab"]?.toLongOrNull() ?: return null
    val minX = parts["minX"]?.toIntOrNull() ?: return null
    val minY = parts["minY"]?.toIntOrNull() ?: return null
    val maxX = parts["maxX"]?.toIntOrNull() ?: return null
    val maxY = parts["maxY"]?.toIntOrNull() ?: return null
    return ClipboardMeta(
        sourceEditorTabId = tab,
        bounds = ClipboardBounds(minX, minY, maxX, maxY),
    )
}

private fun pasteTranslationPx(
    ctx: ConceptualPasteContext,
    sourceEditorTabId: Long,
    fragmentBounds: ClipboardBounds,
): Pair<Int, Int> {
    val px = ctx.pointerModelX
    val py = ctx.pointerModelY
    if (ctx.isPointerOverCanvas && px != null && py != null) {
        return Pair(
            px.roundToInt() - fragmentBounds.minX,
            py.roundToInt() - fragmentBounds.minY,
        )
    }
    if (sourceEditorTabId >= 0L && ctx.targetEditorTabId == sourceEditorTabId) {
        return Pair(PASTE_SAME_TAB_NUDGE_PX, PASTE_SAME_TAB_NUDGE_PX)
    }
    val modelCx = (ctx.layoutWidthPx / 2f - ctx.panX).roundToInt()
    val modelCy = (ctx.layoutHeightPx / 2f - ctx.panY).roundToInt()
    return Pair(
        modelCx - fragmentBounds.centerX,
        modelCy - fragmentBounds.centerY,
    )
}

/** Remap ids from [fragment] (already translated) onto [target], then dedupe names on pasted ids. */
internal fun mergeTranslatedFragment(target: ConceptualSchema, fragment: ConceptualSchema): Pair<ConceptualSchema, Set<Int>> {
    val oldElementIds = fragment.elements.keys.sorted()
    if (oldElementIds.isEmpty()) return target to emptySet()

    var work = target
    val idMap = LinkedHashMap<Int, Int>()
    for (oid in oldElementIds) {
        val (w, nid) = work.allocateId()
        work = w
        idMap[oid] = nid
    }
    val pastedIds = idMap.values.toSet()

    for (oid in oldElementIds) {
        val el = fragment.elements.getValue(oid)
        val remapped = remapSchemaElementIds(el, idMap).withCoercedMinimumDimensions()
        work = work.withElement(remapped)
    }

    for (c in fragment.connections.sortedBy { it.id }) {
        val (w, newConnId) = work.allocateId()
        work = w
        val nc = Connection(
            id = newConnId,
            elementIdA = idMap.getValue(c.elementIdA),
            elementIdB = idMap.getValue(c.elementIdB),
            cardinality = c.cardinality,
            showCardinality = c.showCardinality,
            cardinalityFixed = c.cardinalityFixed,
            isWeak = c.isWeak,
            orientation = c.orientation,
            cardinalityRole = c.cardinalityRole,
            cardinalityObservations = c.cardinalityObservations,
            cardinalityDictionary = c.cardinalityDictionary,
            cardinalityPosition = c.cardinalityPosition?.coercedToMinimumDimensions(),
            cardinalityAutoSize = c.cardinalityAutoSize,
            useAssociativeOuterForEndA = c.useAssociativeOuterForEndA,
            useAssociativeOuterForEndB = c.useAssociativeOuterForEndB,
        )
        work = work.withConnection(nc)
    }

    work = work.withNormalizedAttributeMultiValuedCounts()
    work = work.withDedupedConceptualSchemaElementNames()
    return work to pastedIds
}

internal fun remapSchemaElementIds(el: SchemaElement, idMap: Map<Int, Int>): SchemaElement {
    fun m(i: Int): Int = idMap[i] ?: i
    return when (el) {
        is SchemaElement.Entity -> el.copy(
            id = m(el.id),
            specializationId = el.specializationId?.let { oid -> idMap[oid] },
            parentSpecializationIds = el.parentSpecializationIds.mapNotNull { oid -> idMap[oid] },
        )
        is SchemaElement.Relationship -> el.copy(id = m(el.id))
        is SchemaElement.AssociativeEntity -> el.copy(id = m(el.id))
        is SchemaElement.Attribute -> el.copy(
            id = m(el.id),
            ownerId = m(el.ownerId),
            childAttributeIds = el.childAttributeIds.map { m(it) },
        )
        is SchemaElement.Specialization -> el.copy(
            id = m(el.id),
            baseEntityId = m(el.baseEntityId),
        )
        is SchemaElement.SelfRelationship -> el.copy(
            id = m(el.id),
            ownerEntityId = m(el.ownerEntityId),
        )
        is SchemaElement.Annotation -> el.copy(id = m(el.id))
    }
}

private fun disambiguateName(base: String, isTaken: (String) -> Boolean): String {
    if (!isTaken(base)) return base
    var n = 2
    while (true) {
        val c = "$base$n"
        if (!isTaken(c)) return c
        n++
    }
}

/** Ensures unique names across the entire schema (canvas elements and hidden attribute trees). */
private fun ConceptualSchema.withDedupedConceptualSchemaElementNames(): ConceptualSchema {
    val allIds = elements.keys.sorted()
    if (allIds.isEmpty()) return this
    var s = this
    for (id in allIds) {
        val el = s.elements[id] ?: continue
        val next = when (el) {
            is SchemaElement.Entity -> {
                val nn = disambiguateName(el.name) { cand ->
                    s.entities.any { it.name == cand && it.id != id } ||
                        s.associativeEntities.any { it.name == cand }
                }
                el.copy(name = nn)
            }
            is SchemaElement.AssociativeEntity -> {
                val nEnt = disambiguateName(el.name) { cand ->
                    s.entities.any { it.name == cand } ||
                        s.associativeEntities.any { it.name == cand && it.id != id }
                }
                val nRel = disambiguateName(el.relationshipName) { cand ->
                    s.relationships.any { it.name == cand } ||
                        s.associativeEntities.any { it.relationshipName == cand && it.id != id } ||
                        s.selfRelationships.any { it.name == cand }
                }
                el.copy(name = nEnt, relationshipName = nRel)
            }
            is SchemaElement.Relationship -> {
                val nn = disambiguateName(el.name) { cand ->
                    s.relationships.any { it.name == cand && it.id != id } ||
                        s.associativeEntities.any { it.relationshipName == cand }
                }
                el.copy(name = nn)
            }
            is SchemaElement.SelfRelationship -> {
                val nn = disambiguateName(el.name) { cand ->
                    s.selfRelationships.any { it.name == cand && it.id != id } ||
                        s.relationships.any { it.name == cand } ||
                        s.associativeEntities.any { it.relationshipName == cand }
                }
                el.copy(name = nn)
            }
            is SchemaElement.Specialization -> {
                val nn = disambiguateName(el.name) { cand ->
                    s.specializations.any { it.name == cand && it.id != id }
                }
                el.copy(name = nn)
            }
            is SchemaElement.Annotation -> {
                val nn = disambiguateName(el.name) { cand ->
                    s.annotations.any { it.name == cand && it.id != id }
                }
                el.copy(name = nn)
            }
            is SchemaElement.Attribute -> {
                val nn = disambiguateName(el.name) { cand ->
                    s.attributes.any {
                        it.ownerId == el.ownerId && it.name == cand && it.id != id
                    }
                }
                el.copy(
                    name = nn,
                    hiddenAttributes = dedupeHiddenAttributeTreeNames(el.hiddenAttributes),
                )
            }
        }
        if (next != el) {
            s = s.copy(elements = s.elements + (id to next))
        }
    }
    // Second pass: entity-likes may have been updated; fix hidden trees on non-attributes too.
    for (id in allIds) {
        val el = s.elements[id] ?: continue
        if (el is SchemaElement.Attribute) continue
        val hid = dedupeHiddenAttributeTreeNames(el.hiddenAttributes)
        if (hid != el.hiddenAttributes) {
            val u = when (el) {
                is SchemaElement.Entity -> el.copy(hiddenAttributes = hid)
                is SchemaElement.Relationship -> el.copy(hiddenAttributes = hid)
                is SchemaElement.AssociativeEntity -> el.copy(hiddenAttributes = hid)
                is SchemaElement.Specialization -> el.copy(hiddenAttributes = hid)
                is SchemaElement.SelfRelationship -> el.copy(hiddenAttributes = hid)
                is SchemaElement.Annotation -> el.copy(hiddenAttributes = hid)
                is SchemaElement.Attribute -> el
            }
            s = s.copy(elements = s.elements + (id to u))
        }
    }
    return s
}

private fun dedupeHiddenAttributeTreeNames(attrs: List<HiddenAttribute>): List<HiddenAttribute> =
    dedupeHiddenSiblings(attrs)

private fun dedupeHiddenSiblings(siblings: List<HiddenAttribute>): List<HiddenAttribute> {
    val used = mutableSetOf<String>()
    return siblings.map { h ->
        val nn = disambiguateName(h.name) { it in used }.also { used.add(it) }
        h.copy(
            name = nn,
            children = dedupeHiddenSiblings(h.children),
            nestedHiddenAttributes = dedupeHiddenSiblings(h.nestedHiddenAttributes),
        )
    }
}

/** Build clipboard text, or null when there is nothing to copy. */
internal fun buildConceptualClipboardPayload(
    schema: ConceptualSchema,
    selection: CanvasSelection,
    sourceEditorTabId: Long,
): String? {
    val ids = elementIdsForClipboard(schema, selection)
    val frag = extractClipboardFragment(schema, ids) ?: return null
    val bounds = frag.clipboardBounds() ?: return null
    val meta = ClipboardMeta(sourceEditorTabId = sourceEditorTabId, bounds = bounds)
    return encodeClipboardPayload(meta, frag)
}

internal fun pasteConceptualClipboard(
    ctx: ConceptualPasteContext,
    payload: String,
): ConceptualPasteResult? {
    val (meta, fragmentIn) = decodeClipboardPayload(payload) ?: return null
    if (fragmentIn.elements.isEmpty()) return null
    val bounds = meta?.bounds ?: fragmentIn.clipboardBounds() ?: return null
    val sourceTab = meta?.sourceEditorTabId ?: -1L
    val (dx, dy) = pasteTranslationPx(ctx, sourceTab, bounds)
    val translated = translateConceptualSchema(fragmentIn, dx, dy)
    val (merged, pasted) = mergeTranslatedFragment(ctx.targetSchema, translated)
    val selection = canvasSelectionForPastedIds(merged, pasted)
    return ConceptualPasteResult(schema = merged, pastedElementIds = pasted, selection = selection)
}

private fun canvasSelectionForPastedIds(schema: ConceptualSchema, pastedIds: Set<Int>): CanvasSelection {
    if (pastedIds.isEmpty()) return CanvasSelection.None
    val roots = pastedIds.filter { oid ->
        val el = schema.elements[oid] ?: return@filter false
        when (el) {
            is SchemaElement.Attribute -> el.ownerId !in pastedIds
            else -> true
        }
    }.toSet()
    val pick = if (roots.isNotEmpty()) roots else pastedIds
    return canvasSelectionFromPickSets(pick, emptySet())
}
