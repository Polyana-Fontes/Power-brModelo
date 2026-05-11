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
 * Stable address for a single editable dictionary text in a conceptual schema.
 *
 * Used by **Operações → Dicionário de Dados de Objetos** to collect every dictionary slot
 * for the current selection (including nested hidden attributes and associative +Dicionário).
 */
internal sealed class ConceptualDictionarySlotKey {
    data class SchemaElementMain(val elementId: Int) : ConceptualDictionarySlotKey()

    /** Inner relationship dictionary on an associative entity (`+Dicionário` in the inspector). */
    data class AssociativeInnerRelationship(val elementId: Int) : ConceptualDictionarySlotKey()

    /** Cardinality label dictionary on a [Connection]. */
    data class ConnectionCardinality(val connectionId: Int) : ConceptualDictionarySlotKey()

    /**
     * Dictionary on a node inside [SchemaElement.hiddenAttributes].
     *
     * @param pathWithinTree Indices following [HiddenAttribute.branchAt] (children first, then nested ocultos).
     */
    data class HiddenAttributeNode(
        val ownerElementId: Int,
        val rootHiddenIndex: Int,
        val pathWithinTree: List<Int>,
    ) : ConceptualDictionarySlotKey()
}

internal data class ConceptualDictionarySlotRow(
    val key: ConceptualDictionarySlotKey,
    /** Primary line: object kind and name. */
    val title: String,
    /** Secondary line: field role and ownership chain. */
    val subtitle: String,
    val initialText: String,
)

/** True when the ribbon action **Dicionário de Dados de Objetos** should be enabled. */
internal fun canOpenBulkDataDictionaryForSelection(selection: CanvasSelection): Boolean =
    selection.selectedPickCount() >= 1

private fun readHiddenAtPath(root: HiddenAttribute, path: List<Int>): HiddenAttribute? {
    if (path.isEmpty()) return root
    var n = root
    for (i in path) {
        n = n.branchAt(i) ?: return null
    }
    return n
}

private fun updateHiddenAtPath(
    root: HiddenAttribute,
    path: List<Int>,
    f: (HiddenAttribute) -> HiddenAttribute,
): HiddenAttribute? {
    if (path.isEmpty()) return f(root)
    val idx = path[0]
    val child = root.branchAt(idx) ?: return null
    val updatedChild = updateHiddenAtPath(child, path.drop(1), f) ?: return null
    return root.withBranchReplaced(idx, updatedChild)
}

private fun SchemaElement.replaceHiddenRoot(rootIndex: Int, newRoot: HiddenAttribute): SchemaElement? {
    if (rootIndex !in hiddenAttributes.indices) return null
    val next = hiddenAttributes.toMutableList().also { it[rootIndex] = newRoot }
    return when (this) {
        is SchemaElement.Entity -> copy(hiddenAttributes = next)
        is SchemaElement.Relationship -> copy(hiddenAttributes = next)
        is SchemaElement.AssociativeEntity -> copy(hiddenAttributes = next)
        is SchemaElement.Attribute -> copy(hiddenAttributes = next)
        is SchemaElement.Specialization -> copy(hiddenAttributes = next)
        is SchemaElement.SelfRelationship -> copy(hiddenAttributes = next)
        is SchemaElement.Annotation -> copy(hiddenAttributes = next)
    }
}

private fun SchemaElement.withMainDictionary(value: String): SchemaElement =
    when (this) {
        is SchemaElement.Entity -> copy(dictionary = value)
        is SchemaElement.Relationship -> copy(dictionary = value)
        is SchemaElement.AssociativeEntity -> copy(dictionary = value)
        is SchemaElement.Attribute -> copy(dictionary = value)
        is SchemaElement.Specialization -> copy(dictionary = value)
        is SchemaElement.SelfRelationship -> copy(dictionary = value)
        is SchemaElement.Annotation -> copy(dictionary = value)
    }

private fun elementKindNoun(el: SchemaElement): String =
    when (el) {
        is SchemaElement.Entity -> "Entidade"
        is SchemaElement.Relationship -> "Relacionamento"
        is SchemaElement.AssociativeEntity -> "Entidade associativa"
        is SchemaElement.Attribute -> "Atributo"
        is SchemaElement.Specialization -> "Especialização"
        is SchemaElement.SelfRelationship -> "Auto-relacionamento"
        is SchemaElement.Annotation -> "Anotação"
    }

private fun quoteName(name: String): String = "«${name.ifBlank { "—" }}»"

private fun appendHiddenSlots(
    schema: ConceptualSchema,
    owner: SchemaElement,
    out: MutableList<ConceptualDictionarySlotRow>,
) {
    fun visit(rootIdx: Int, path: List<Int>, node: HiddenAttribute, ancestorNames: List<String>) {
        val hereTitle = "Atributo oculto ${quoteName(node.name)}"
        val inner = if (ancestorNames.isEmpty()) {
            "Dicionário · ${elementKindNoun(owner)} ${quoteName(owner.name)}"
        } else {
            val chain = ancestorNames.joinToString(" → ") { quoteName(it) }
            "Dicionário · dentro de $chain · ${elementKindNoun(owner)} ${quoteName(owner.name)}"
        }
        out.add(
            ConceptualDictionarySlotRow(
                key = ConceptualDictionarySlotKey.HiddenAttributeNode(owner.id, rootIdx, path),
                title = hereTitle,
                subtitle = inner,
                initialText = node.dictionary,
            ),
        )
        val nChildren = node.children.size
        val deeper = ancestorNames + node.name
        node.children.forEachIndexed { i, ch ->
            visit(rootIdx, path + i, ch, deeper)
        }
        node.nestedHiddenAttributes.forEachIndexed { j, ch ->
            val merged = nChildren + j
            visit(rootIdx, path + merged, ch, deeper)
        }
    }
    owner.hiddenAttributes.forEachIndexed { rootIdx, root ->
        visit(rootIdx, emptyList(), root, emptyList())
    }
}

private fun connectionCardinalityLabels(schema: ConceptualSchema, conn: Connection): Pair<String, String> {
    val a = schema.elements[conn.elementIdA]
    val b = schema.elements[conn.elementIdB]
    val na = a?.name?.ifBlank { "—" } ?: "?"
    val nb = b?.name?.ifBlank { "—" } ?: "?"
    val title = "Cardinalidade da ligação"
    val subtitle = "Dicionário do rótulo · $na ↔ $nb"
    return title to subtitle
}

/**
 * Collects every dictionary field for the current [selection] in a deterministic order.
 */
internal fun collectDictionarySlotsForSelection(
    schema: ConceptualSchema,
    selection: CanvasSelection,
): List<ConceptualDictionarySlotRow> {
    val (elemIds, cardIds) = selection.toMultiPickSets()
    val out = mutableListOf<ConceptualDictionarySlotRow>()
    for (eid in elemIds.toList().sorted()) {
        val el = schema.elements[eid] ?: continue
        val kind = elementKindNoun(el)
        out.add(
            ConceptualDictionarySlotRow(
                key = ConceptualDictionarySlotKey.SchemaElementMain(eid),
                title = "$kind ${quoteName(el.name)}",
                subtitle = "Dicionário",
                initialText = el.dictionary,
            ),
        )
        if (el is SchemaElement.AssociativeEntity) {
            val relName = el.relationshipName.ifBlank { "—" }
            out.add(
                ConceptualDictionarySlotRow(
                    key = ConceptualDictionarySlotKey.AssociativeInnerRelationship(eid),
                    title = "+Relacionamento interno ${quoteName(relName)}",
                    subtitle = "+Dicionário · ${elementKindNoun(el)} ${quoteName(el.name)}",
                    initialText = el.relationshipDictionary,
                ),
            )
        }
        appendHiddenSlots(schema, el, out)
    }
    for (cid in cardIds.toList().sorted()) {
        val conn = schema.connections.firstOrNull { it.id == cid } ?: continue
        val (title, subtitle) = connectionCardinalityLabels(schema, conn)
        out.add(
            ConceptualDictionarySlotRow(
                key = ConceptualDictionarySlotKey.ConnectionCardinality(cid),
                title = title,
                subtitle = subtitle,
                initialText = conn.cardinalityDictionary,
            ),
        )
    }
    return out
}

internal fun readDictionaryValue(schema: ConceptualSchema, key: ConceptualDictionarySlotKey): String? =
    when (key) {
        is ConceptualDictionarySlotKey.SchemaElementMain ->
            schema.elements[key.elementId]?.dictionary
        is ConceptualDictionarySlotKey.AssociativeInnerRelationship ->
            (schema.elements[key.elementId] as? SchemaElement.AssociativeEntity)?.relationshipDictionary
        is ConceptualDictionarySlotKey.ConnectionCardinality ->
            schema.connections.firstOrNull { it.id == key.connectionId }?.cardinalityDictionary
        is ConceptualDictionarySlotKey.HiddenAttributeNode -> {
            val owner = schema.elements[key.ownerElementId] ?: return null
            val root = owner.hiddenAttributes.getOrNull(key.rootHiddenIndex) ?: return null
            readHiddenAtPath(root, key.pathWithinTree)?.dictionary
        }
    }

private fun ConceptualSchema.withConnectionMapped(
    connectionId: Int,
    map: (Connection) -> Connection,
): ConceptualSchema? {
    var found = false
    val next = connections.map { c ->
        if (c.id == connectionId) {
            found = true
            map(c)
        } else {
            c
        }
    }
    return if (found) copy(connections = next) else null
}

/**
 * Applies a single dictionary write. Returns null if the address no longer exists.
 */
internal fun applyDictionarySlot(
    schema: ConceptualSchema,
    key: ConceptualDictionarySlotKey,
    text: String,
): ConceptualSchema? =
    when (key) {
        is ConceptualDictionarySlotKey.SchemaElementMain -> {
            val el = schema.elements[key.elementId] ?: return null
            schema.withElement(el.withMainDictionary(text))
        }
        is ConceptualDictionarySlotKey.AssociativeInnerRelationship -> {
            val el = schema.elements[key.elementId] as? SchemaElement.AssociativeEntity ?: return null
            schema.withElement(el.copy(relationshipDictionary = text))
        }
        is ConceptualDictionarySlotKey.ConnectionCardinality ->
            schema.withConnectionMapped(key.connectionId) { it.copy(cardinalityDictionary = text) }
        is ConceptualDictionarySlotKey.HiddenAttributeNode -> {
            val owner = schema.elements[key.ownerElementId] ?: return null
            val root = owner.hiddenAttributes.getOrNull(key.rootHiddenIndex) ?: return null
            val updatedRoot =
                updateHiddenAtPath(root, key.pathWithinTree) { it.copy(dictionary = text) } ?: return null
            val patched = owner.replaceHiddenRoot(key.rootHiddenIndex, updatedRoot) ?: return null
            schema.withElement(patched)
        }
    }

/**
 * Applies many writes in order. Returns null if any step fails.
 */
internal fun applyDictionarySlots(
    schema: ConceptualSchema,
    writes: List<Pair<ConceptualDictionarySlotKey, String>>,
): ConceptualSchema? {
    var cur: ConceptualSchema = schema
    for ((k, v) in writes) {
        cur = applyDictionarySlot(cur, k, v) ?: return null
    }
    return cur
}
