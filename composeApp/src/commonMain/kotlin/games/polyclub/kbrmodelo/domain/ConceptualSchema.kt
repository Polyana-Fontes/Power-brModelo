/*
 * KbrModelo - Kotlin port of brModelo 3.0 originally written in Pascal
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

package games.polyclub.kbrmodelo.domain

/**
 * The complete in-memory representation of a conceptual ER schema.
 *
 * Corresponds to a [TModelo] instance with `TipoDeModelo == tpModeloConceitual` (0)
 * in the original Pascal source (`mer.pas`).
 *
 * All cross-element references use [Int] IDs that resolve against [elements].
 * Connections are stored separately in [connections], mirroring the way
 * `TLigacao` components live as children of `TModelo`.
 *
 * @param name        Human-readable name of this schema.
 *                    Corresponds to `TModelo.Nome`.
 * @param filePath    Path to the `.brM` file on disk, or empty if unsaved.
 *                    Corresponds to `TModelo.Arquivo`.
 * @param author      Author metadata.
 *                    Corresponds to `TModelo.Autor`.
 * @param observations Free-text observations about the whole model.
 *                     Corresponds to `TModelo.Observacao`.
 * @param version     Format version string read from the saved file.
 *                    Corresponds to `TModelo.Versao`.
 * @param elements    Flat map from element ID → [SchemaElement].
 *                    Includes entities, relationships, attributes, specializations,
 *                    associative entities, self-relationships and annotations.
 *                    Preserves insertion order for deterministic serialization.
 * @param connections Ordered list of all [Connection]s in this schema.
 *                    Corresponds to the `TLigacao` children of `TModelo`.
 * @param nextId      Counter for assigning unique IDs to new elements.
 *                    Corresponds to `TModelo.FIDs` (the internal OID counter).
 * @param openedFromBrm When true, the model was loaded from a Delphi `.brM` binary file.
 *                      Saving as XML uses "Save As" semantics until written to an XML path.
 */
data class ConceptualSchema(
    val name: String = "",
    val filePath: String = "",
    val author: String = "",
    val observations: String = "",
    val version: String = "2.0.0",
    val elements: Map<Int, SchemaElement> = emptyMap(),
    val connections: List<Connection> = emptyList(),
    val nextId: Int = 1,
    val openedFromBrm: Boolean = false,
) {
    // ── Typed element accessors ──────────────────────────────────────────────

    val entities: List<SchemaElement.Entity>
        get() = elements.values.filterIsInstance<SchemaElement.Entity>()

    val relationships: List<SchemaElement.Relationship>
        get() = elements.values.filterIsInstance<SchemaElement.Relationship>()

    val associativeEntities: List<SchemaElement.AssociativeEntity>
        get() = elements.values.filterIsInstance<SchemaElement.AssociativeEntity>()

    val attributes: List<SchemaElement.Attribute>
        get() = elements.values.filterIsInstance<SchemaElement.Attribute>()

    val specializations: List<SchemaElement.Specialization>
        get() = elements.values.filterIsInstance<SchemaElement.Specialization>()

    val selfRelationships: List<SchemaElement.SelfRelationship>
        get() = elements.values.filterIsInstance<SchemaElement.SelfRelationship>()

    val annotations: List<SchemaElement.Annotation>
        get() = elements.values.filterIsInstance<SchemaElement.Annotation>()

    // ── Structural queries ───────────────────────────────────────────────────

    /** Returns the direct attributes owned by [elementId] (immediate children only). */
    fun attributesOf(elementId: Int): List<SchemaElement.Attribute> =
        attributes.filter { it.ownerId == elementId }

    /** Returns the connections that involve [elementId] on either end. */
    fun connectionsOf(elementId: Int): List<Connection> =
        connections.filter { it.elementIdA == elementId || it.elementIdB == elementId }

    /** Returns the IDs of all elements directly connected to [elementId]. */
    fun neighborIds(elementId: Int): List<Int> =
        connectionsOf(elementId).map { conn ->
            if (conn.elementIdA == elementId) conn.elementIdB else conn.elementIdA
        }

    /** Returns child [SchemaElement.Attribute]s of a composite attribute. */
    fun childAttributesOf(attributeId: Int): List<SchemaElement.Attribute> =
        attributes.filter { it.ownerId == attributeId }

    // ── Mutation helpers (return a new schema — immutable style) ─────────────

    /** Adds or replaces an element, auto-incrementing [nextId] when needed. */
    fun withElement(element: SchemaElement): ConceptualSchema {
        val newNextId = maxOf(nextId, element.id + 1)
        return copy(
            elements = elements + (element.id to element),
            nextId = newNextId,
        )
    }

    /** Removes an element and all connections that reference it. */
    fun withoutElement(elementId: Int): ConceptualSchema = copy(
        elements = elements - elementId,
        connections = connections.filter {
            it.elementIdA != elementId && it.elementIdB != elementId
        },
    )

    /** Adds a connection to the schema. */
    fun withConnection(connection: Connection): ConceptualSchema = copy(
        connections = connections + connection,
    )

    /** Removes a connection by ID. */
    fun withoutConnection(connectionId: Int): ConceptualSchema = copy(
        connections = connections.filter { it.id != connectionId },
    )

    /**
     * Leaf columns contributed by the subtree rooted at this canvas attribute ([attributeId]):
     * **1** if simple; if composite, recursive sum over [SchemaElement.Attribute.childAttributeIds]
     * plus [SchemaElement.Attribute.hiddenAttributes] leaves on each composite node.
     */
    fun attributeSubtreePhysicalFieldCount(attributeId: Int): Int {
        val attr = elements[attributeId] as? SchemaElement.Attribute ?: return 0
        if (!attr.isComposite) return 1
        var n = attr.childAttributeIds.sumOf { attributeSubtreePhysicalFieldCount(it) }
        n += attr.hiddenAttributes.sumOf { it.physicalFieldLeafCount() }
        return n
    }

    /**
     * Canonical QtdeMultivalorado: **0** if [attr] is not composite; otherwise totals leaf columns from every
     * visible composite child subtree **and** from [SchemaElement.Attribute.hiddenAttributes] on [attr] only.
     */
    fun canonicalQtdeMultivalorado(attr: SchemaElement.Attribute): Int {
        if (!attr.isComposite) return 0
        var total = attr.childAttributeIds.sumOf { attributeSubtreePhysicalFieldCount(it) }
        total += attr.hiddenAttributes.sumOf { it.physicalFieldLeafCount() }
        return total
    }

    /**
     * Rewrites each attribute's [SchemaElement.Attribute.multiValuedCount] to [canonicalQtdeMultivalorado]
     * (fixes bogus QtdeMultivalorado from legacy saves).
     */
    fun withNormalizedAttributeMultiValuedCounts(): ConceptualSchema =
        copy(
            elements = elements.mapValues { (_, el) ->
                if (el is SchemaElement.Attribute) {
                    val n = canonicalQtdeMultivalorado(el)
                    if (el.multiValuedCount != n) el.copy(multiValuedCount = n) else el
                } else {
                    el
                }
            },
        )

    /** Returns the next available ID and increments the counter. */
    fun allocateId(): Pair<ConceptualSchema, Int> =
        copy(nextId = nextId + 1) to nextId

    companion object {
        /** An empty schema ready to be populated. */
        val EMPTY = ConceptualSchema()
    }
}
