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

package games.polyclub.kbrmodelo.domain.serialization

import games.polyclub.kbrmodelo.domain.AnnotationType
import games.polyclub.kbrmodelo.domain.ArrowDirection
import games.polyclub.kbrmodelo.domain.AttributeCardinality
import games.polyclub.kbrmodelo.domain.Cardinality
import games.polyclub.kbrmodelo.domain.ConceptualSchema
import games.polyclub.kbrmodelo.domain.Connection
import games.polyclub.kbrmodelo.domain.ElementPosition
import games.polyclub.kbrmodelo.domain.HiddenAttribute
import games.polyclub.kbrmodelo.domain.LabelStyle
import games.polyclub.kbrmodelo.domain.LineOrientation
import games.polyclub.kbrmodelo.domain.SchemaElement
import games.polyclub.kbrmodelo.domain.SpecializationType
import games.polyclub.kbrmodelo.domain.TextAlignment
import games.polyclub.kbrmodelo.domain.brm.DfmNode
import games.polyclub.kbrmodelo.domain.brm.parseDfmBytes

/**
 * Deserializes a brModelo `.brM` file into a [ConceptualSchema].
 *
 * The `.brM` format is a Delphi binary DFM stream (magic "TPF0") with a
 * version ShortString prefix prepended by brModelo. The root component is
 * [TModelo]; all model elements are stored as flat children of TModelo and
 * cross-referenced by integer OID values.
 *
 * ## Key mappings
 *
 * | DFM class          | Domain type                          |
 * |--------------------|--------------------------------------|
 * | TEntidade          | SchemaElement.Entity                 |
 * | TRelacao / TMaxRelacao | SchemaElement.Relationship       |
 * | TEntidadeAssoss    | SchemaElement.AssociativeEntity      |
 * | TAtributo          | SchemaElement.Attribute              |
 * | TEspecializacao    | SchemaElement.Specialization         |
 * | TAutoRelacao       | SchemaElement.SelfRelationship       |
 * | TTexto             | SchemaElement.Annotation             |
 * | TCardinalidade     | Connection (via `_Comando` string)   |
 * | TBarraDeAtributos  | (visual only — OID noted, skipped)   |
 * | TPonto, TSeta, TLinha | (visual only — skipped)           |
 *
 * ## Connection reconstruction
 *
 * Connections are encoded in `TCardinalidade._Comando` as 36 pipe-separated
 * integers. Indices 1 and 2 are the OIDs of the two connected elements (E1, E2).
 * Connections where both endpoints are semantic elements (entity, relationship,
 * associative entity, specialization) become explicit [Connection] objects.
 * Attribute-to-owner connections are implicit via [SchemaElement.Attribute.ownerId].
 */
object ConceptualSchemaBrmParser {

    /** Parses raw bytes of a brModelo `.brM` file. */
    fun parse(bytes: ByteArray): ConceptualSchema {
        val root = parseDfmBytes(bytes)
        return convert(root)
    }

    // ── Root conversion ───────────────────────────────────────────────────────

    private fun convert(root: DfmNode): ConceptualSchema {
        val author = root.strProp("Autor")
        val observations = root.strProp("Observacao")

        // Collect ALL children of TModelo into an OID map
        val byOid = mutableMapOf<Int, DfmNode>()
        val allNodes = mutableListOf<DfmNode>()
        collectAll(root, byOid, allNodes)

        // Build: barraOid → ownerOid (what TAtributo/entity the barra belongs to).
        // When a sub-attribute has _Dono pointing to a TBarraDeAtributos, we resolve
        // through this map to find the actual parent attribute or entity.
        val barraToOwner = byOid.values
            .filter { it.className == "TBarraDeAtributos" }
            .associate { barra ->
                val barraOid = barra.intProp("OID")
                val ownerOid = barra.intProp("_Dono")
                barraOid to ownerOid
            }
        val barraOids = barraToOwner.keys.toSet()

        // Build: childRelacaoOid → assossOid.
        // TEntidadeAssoss stores the OID of its inner relationship diamond via the
        // published property `_ChildRelacao`. TCardinalidade connections reference
        // TChildRelacao OIDs; we resolve them to the parent associative entity so
        // they can be found in `elements`.
        val childRelacaoToAssoss = byOid.values
            .filter { it.className == "TEntidadeAssoss" }
            .mapNotNull { assoss ->
                val childOid = assoss.intProp("_ChildRelacao", -1)
                val assossOid = assoss.intProp("OID")
                if (childOid > 0) childOid to assossOid else null
            }
            .toMap()

        // Pass 1: build SchemaElements from component nodes
        val elements = mutableMapOf<Int, SchemaElement>()
        for (node in allNodes) {
            val element = nodeToElement(node, byOid, barraToOwner) ?: continue
            elements[element.id] = element
        }

        // Pass 2: build Connections from TCardinalidade nodes
        var connIdCounter = (byOid.keys.maxOrNull() ?: 0) + 1
        val connections = mutableListOf<Connection>()
        for (node in allNodes) {
            if (node.className != "TCardinalidade") continue
            val conn = cardinalidadeToConnection(
                node = node,
                connId = connIdCounter++,
                elements = elements,
                barraOids = barraOids,
                childRelacaoToAssoss = childRelacaoToAssoss,
            ) ?: continue
            connections.add(conn)
        }

        val maxId = maxOf(
            elements.keys.maxOrNull() ?: 0,
            connections.maxOfOrNull { it.id } ?: 0,
        )

        return ConceptualSchema(
            author = author,
            observations = observations,
            elements = elements,
            connections = connections,
            nextId = maxId + 1,
        )
    }

    // ── Collect all children recursively ──────────────────────────────────────

    /**
     * Recursively collects all DfmNodes under [parent], indexing by OID.
     * Only nodes with a positive OID are included.
     */
    private fun collectAll(
        parent: DfmNode,
        byOid: MutableMap<Int, DfmNode>,
        all: MutableList<DfmNode>,
    ) {
        for (child in parent.children) {
            val oid = child.intProp("OID")
            if (oid > 0) {
                byOid[oid] = child
                all.add(child)
            }
            collectAll(child, byOid, all)
        }
    }

    // ── Element conversion ────────────────────────────────────────────────────

    private fun nodeToElement(
        node: DfmNode,
        byOid: Map<Int, DfmNode>,
        barraToOwner: Map<Int, Int>,
    ): SchemaElement? {
        val oid = node.intProp("OID")
        if (oid <= 0) return null

        return when (node.className) {
            "TEntidade" -> parseEntidade(node, oid)
            "TRelacao", "TMaxRelacao", "TRelacaoMaxima" -> parseRelacao(node, oid)
            "TEntidadeAssoss" -> parseEntAssoss(node, oid, byOid)
            "TAtributo" -> parseAtributo(node, oid, barraToOwner)
            "TEspecializacao" -> parseEspecializacao(node, oid)
            "TAutoRelacao" -> parseAutoRelacao(node, oid)
            "TTexto", "TBaseTexto" -> parseTexto(node, oid)
            else -> null // TBarraDeAtributos, TPonto, TSeta, TLinha, TCardinalidade, etc.
        }
    }

    // ── Entity ────────────────────────────────────────────────────────────────

    private fun parseEntidade(node: DfmNode, oid: Int): SchemaElement.Entity {
        return SchemaElement.Entity(
            id = oid,
            name = node.strProp("Nome"),
            position = node.position(),
            observations = node.strProp("Observacoes"),
            dictionary = node.strProp("Dicionario"),
            labelStyle = parseLabelStyle(node),
            hiddenAttributes = parseHiddenAttributes(node),
            isWeak = node.boolProp("Nula"),
            specializationId = null, // resolved after all elements are built
            parentSpecializationIds = emptyList(),
        )
    }

    // ── Relationship ──────────────────────────────────────────────────────────

    private fun parseRelacao(node: DfmNode, oid: Int): SchemaElement.Relationship {
        return SchemaElement.Relationship(
            id = oid,
            name = node.strProp("Nome"),
            position = node.position(),
            observations = node.strProp("Observacoes"),
            dictionary = node.strProp("Dicionario"),
            labelStyle = parseLabelStyle(node),
            hiddenAttributes = parseHiddenAttributes(node),
            arrowDirection = ArrowDirection.fromCode(node.intProp("SetaDirecao")),
            showName = !node.boolProp("NaoPinteNome"),
        )
    }

    // ── Associative Entity ────────────────────────────────────────────────────

    private fun parseEntAssoss(node: DfmNode, oid: Int, @Suppress("UNUSED_PARAMETER") byOid: Map<Int, DfmNode>): SchemaElement.AssociativeEntity {
        return SchemaElement.AssociativeEntity(
            id = oid,
            name = node.strProp("Nome"),
            position = node.position(),
            observations = node.strProp("Observacoes"),
            dictionary = node.strProp("Dicionario"),
            labelStyle = parseLabelStyle(node),
            hiddenAttributes = parseHiddenAttributes(node),
            relationshipName = node.strProp("RelacaoNome"),
            relationshipObservations = node.strProp("RelecaoObservacao"),
            relationshipDictionary = node.strProp("RelecaoDicionario"),
            arrowDirection = ArrowDirection.fromCode(node.intProp("SetaDirecao")),
        )
    }

    // ── Attribute ─────────────────────────────────────────────────────────────

    private fun parseAtributo(
        node: DfmNode,
        oid: Int,
        barraToOwner: Map<Int, Int>,
    ): SchemaElement.Attribute {
        val maxCard = node.intProp("MaxCard", 1)
        val minCard = node.intProp("MinCard", 1)

        // _Dono can point to the TBarraDeAtributos of the parent composite attribute.
        // Resolve through the barra chain to reach the actual owner (TAtributo or entity).
        val rawDono = node.intProp("_Dono")
        val ownerId = barraToOwner[rawDono] ?: rawDono

        return SchemaElement.Attribute(
            id = oid,
            name = node.strProp("Nome"),
            position = node.position(),
            observations = node.strProp("Observacoes"),
            dictionary = node.strProp("Dicionario"),
            labelStyle = parseLabelStyle(node),
            hiddenAttributes = parseHiddenAttributes(node),
            ownerId = ownerId,
            isIdentifier = node.boolProp("Identificador"),
            isMultiValued = maxCard > 1,
            isOptional = minCard == 0,
            cardinality = AttributeCardinality(minCard, maxCard),
            multiValuedCount = node.intProp("QtdeMultivalorado", 1),
            valueType = node.strProp("TipoDoValor"),
            complement = node.strProp("Complemento"),
            autoSize = node.boolProp("TamAuto", true),
            deviationAngle = node.intProp("Desvio", 10),
        )
    }

    // ── Specialization ────────────────────────────────────────────────────────

    private fun parseEspecializacao(node: DfmNode, oid: Int): SchemaElement.Specialization {
        return SchemaElement.Specialization(
            id = oid,
            position = node.position(),
            observations = node.strProp("Observacoes"),
            dictionary = node.strProp("Dicionario"),
            labelStyle = parseLabelStyle(node),
            baseEntityId = node.intProp("_EntBase"),
            type = SpecializationType.fromCode(node.intProp("Tipo", 1)),
            isPartial = node.boolProp("Parcial"),
        )
    }

    // ── Self-Relationship ─────────────────────────────────────────────────────

    private fun parseAutoRelacao(node: DfmNode, oid: Int): SchemaElement.SelfRelationship {
        return SchemaElement.SelfRelationship(
            id = oid,
            name = node.strProp("Nome"),
            position = node.position(),
            observations = node.strProp("Observacoes"),
            dictionary = node.strProp("Dicionario"),
            labelStyle = parseLabelStyle(node),
            ownerEntityId = -1, // resolved via TBaseEntidade._AutoRelacao
            arrowDirection = ArrowDirection.fromCode(node.intProp("SetaDirecao")),
        )
    }

    // ── Annotation ────────────────────────────────────────────────────────────

    private fun parseTexto(node: DfmNode, oid: Int): SchemaElement.Annotation {
        return SchemaElement.Annotation(
            id = oid,
            name = node.strProp("Nome"),
            position = node.position(),
            observations = node.strProp("Observacoes"),
            dictionary = node.strProp("Dicionario"),
            labelStyle = parseLabelStyle(node),
            color = parseColorRef(node.strProp("Cor")),
            annotationType = AnnotationType.fromCode(node.intProp("Tipo")),
            alignment = TextAlignment.fromCode(node.intProp("TextAlin")),
            autoSize = node.boolProp("TamAuto", true),
        )
    }

    // ── Connection from TCardinalidade ────────────────────────────────────────

    /**
     * Converts a `TCardinalidade` DfmNode into a [Connection].
     *
     * The `_Comando` property is a pipe-separated string of 36 integers encoding
     * the full ligação geometry and entity OID references (see [TLigacao.Get_Comando]
     * in `mer.pas`).
     *
     * Connections where one endpoint is a TBarraDeAtributos (visual-only) are
     * skipped — attribute ownership is encoded in [SchemaElement.Attribute.ownerId].
     *
     * @return null if the connection should not be included in the domain model.
     */
    private fun cardinalidadeToConnection(
        node: DfmNode,
        connId: Int,
        elements: Map<Int, SchemaElement>,
        barraOids: Set<Int>,
        childRelacaoToAssoss: Map<Int, Int> = emptyMap(),
    ): Connection? {
        val comando = node.strProp("_Comando").ifBlank { return null }
        val parts = comando.split("|").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size < 36) return null

        // Resolve TChildRelacao OIDs to their parent TEntidadeAssoss
        val e1Oid = childRelacaoToAssoss[parts[1]] ?: parts[1]
        val e2Oid = childRelacaoToAssoss[parts[2]] ?: parts[2]
        val showCard = parts[3] != 0
        val isWeak = parts[4] != 0
        val orientation = LineOrientation.fromCode(parts[0])

        // Skip connections involving visual-only TBarraDeAtributos components
        if (e1Oid in barraOids || e2Oid in barraOids) return null

        // Skip connections where one endpoint is an attribute (implicit via ownerId)
        val e1 = elements[e1Oid]
        val e2 = elements[e2Oid]
        if (e1 is SchemaElement.Attribute || e2 is SchemaElement.Attribute) return null

        // The cardinality of the TCardinalidade component itself
        val cardCode = node.intProp("Cardinalidade")
        val cardinality = Cardinality.fromCode(cardCode)

        // Position of the cardinality label
        val fixa = node.boolProp("Fixa")
        val labelPos = if (fixa) node.position() else null

        return Connection(
            id = connId,
            elementIdA = e1Oid,
            elementIdB = e2Oid,
            cardinality = cardinality,
            showCardinality = showCard,
            cardinalityFixed = fixa,
            isWeak = node.boolProp("Fraca") || isWeak,
            orientation = orientation,
            cardinalityPosition = labelPos,
        )
    }

    // ── Common property helpers ───────────────────────────────────────────────

    private fun parseLabelStyle(node: DfmNode): LabelStyle {
        val fontColorStr = node.strProp("FontColor")
        val fontStyles = (node.properties["FontStyles"] as? games.polyclub.kbrmodelo.domain.brm.DfmValue.SetVal)
            ?.identifiers ?: emptyList()
        return LabelStyle(
            color = parseColorRef(fontColorStr),
            bold = "fsBold" in fontStyles,
            italic = "fsItalic" in fontStyles,
        )
    }

    /** Parses a Delphi COLORREF integer or identifier string to an ARGB Int. */
    private fun parseColorRef(value: String): Int? {
        if (value.isBlank()) return null
        return when {
            value.startsWith("cl", ignoreCase = true) -> null // system colour name → default
            value.startsWith("#") -> value.drop(1).toIntOrNull(16)
            else -> value.toIntOrNull()
        }
    }

    /** Hidden-attribute parsing is not yet implemented; deferred to a future iteration. */
    @Suppress("UNUSED_PARAMETER")
    private fun parseHiddenAttributes(node: DfmNode): List<HiddenAttribute> = emptyList()
}
