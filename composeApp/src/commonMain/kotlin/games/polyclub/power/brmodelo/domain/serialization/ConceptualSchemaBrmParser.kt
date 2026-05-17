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

package games.polyclub.power.brmodelo.domain.serialization

import games.polyclub.power.brmodelo.domain.AnnotationType
import games.polyclub.power.brmodelo.domain.ArrowDirection
import games.polyclub.power.brmodelo.domain.AttributeCardinality
import games.polyclub.power.brmodelo.domain.Cardinality
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.Connection
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.HiddenAttribute
import games.polyclub.power.brmodelo.domain.LabelStyle
import games.polyclub.power.brmodelo.domain.LineOrientation
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.domain.SpecializationType
import games.polyclub.power.brmodelo.domain.TextAlignment
import games.polyclub.power.brmodelo.domain.VclTColorTable
import games.polyclub.power.brmodelo.domain.brm.DfmNode
import games.polyclub.power.brmodelo.domain.brm.DfmValue
import games.polyclub.power.brmodelo.domain.brm.parseDfmBytes
import kotlin.math.roundToInt

/**
 * Deserializes a brModelo `.brM` file into a [games.polyclub.power.brmodelo.domain.ConceptualSchema].
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
 * associative entity, specialization) become explicit [games.polyclub.power.brmodelo.domain.Connection] objects.
 * Attribute-to-owner connections are implicit via [games.polyclub.power.brmodelo.domain.SchemaElement.Attribute.ownerId].
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
                barraToOwner = barraToOwner,
                childRelacaoToAssoss = childRelacaoToAssoss,
            ) ?: continue
            connections.add(conn)
        }

        // Pass 3: ensure every sub-attribute of a composite attribute has an
        // explicit connection to its composite parent.
        //
        // In brM files, TBarraDeAtributos is the intermediary between a composite
        // TAtributo and its sub-attributes.  The TCardinalidade._Comando for these
        // connections contains the TBarraDeAtributos OID instead of the composite
        // attribute OID.  Pass 2 already resolves this via barraToOwner, but as
        // a defensive measure (and for models where the TCardinalidade was not
        // stored or could not be parsed) we synthesise any missing connections here.
        val connectedPairs = connections
            .map { it.elementIdA to it.elementIdB }
            .toMutableSet()
            .also { set -> connections.forEach { set.add(it.elementIdB to it.elementIdA) } }

        // Populate childAttributeIds for composite attributes (derived from ownerId).
        val childIdsByOwner = mutableMapOf<Int, MutableList<Int>>()
        for (elem in elements.values) {
            if (elem is SchemaElement.Attribute) {
                val owner = elements[elem.ownerId]
                if (owner is SchemaElement.Attribute) {
                    childIdsByOwner.getOrPut(owner.id) { mutableListOf() }.add(elem.id)
                }
            }
        }

        // Replace composite attributes with updated childAttributeIds lists.
        for ((compositeId, childIds) in childIdsByOwner) {
            val composite = elements[compositeId] as? SchemaElement.Attribute ?: continue
            elements[compositeId] = composite.copy(childAttributeIds = childIds)

            // Add any missing sub-attribute connections.
            for (childId in childIds) {
                if ((compositeId to childId) !in connectedPairs &&
                    (childId to compositeId) !in connectedPairs) {
                    connections.add(
                        Connection(
                            id = connIdCounter++,
                            elementIdA = childId,
                            elementIdB = compositeId,
                            cardinality = null,
                            showCardinality = false,
                            isWeak = false,
                        ),
                    )
                    connectedPairs.add(childId to compositeId)
                    connectedPairs.add(compositeId to childId)
                }
            }
        }

        // Pass 3b: Delphi Composto with no canvas children (only ocultos / empty bar in file).
        for ((oid, el) in elements.toList()) {
            if (el !is SchemaElement.Attribute) continue
            if (el.childAttributeIds.isNotEmpty()) continue
            val node = byOid[oid] ?: continue
            if (node.className != "TAtributo") continue
            if (node.boolProp("Composto")) {
                elements[oid] = el.copy(compostoPersisted = true)
            }
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
        ).withNormalizedAttributeMultiValuedCounts().withCoercedMinimumDimensions()
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
            cardinality = AttributeCardinality(
                minCard,
                maxCard
            ),
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
     * Converts a `TCardinalidade` DfmNode into a [games.polyclub.power.brmodelo.domain.Connection].
     *
     * The `_Comando` property is a pipe-separated string of 36 integers encoding
     * the full ligação geometry and entity OID references (see [TLigacao.Get_Comando]
     * in `mer.pas`).
     *
     * `_Comando[1]` and `_Comando[2]` are E1/E2 OIDs; `_Comando[7]` is the OID of
     * the **Ponta** element (the "arrowhead" end, per Pascal's `if Value[7]=Value[2]`
     * logic). The renderer always reads `elementIdB` as Ponta for cardinality-label
     * placement, so we swap A↔B when necessary.
     *
     * When E1 or E2 references a `TBarraDeAtributos` (the visual attribute-bar), we
     * resolve it through [barraToOwner] to the actual parent attribute/entity instead
     * of discarding the connection. This is how attribute-to-owner and sub-attribute
     * connections are encoded in the brM format.
     *
     * @return null if the connection should not be included in the domain model.
     */
    private fun cardinalidadeToConnection(
        node: DfmNode,
        connId: Int,
        elements: Map<Int, SchemaElement>,
        barraToOwner: Map<Int, Int>,
        childRelacaoToAssoss: Map<Int, Int> = emptyMap(),
    ): Connection? {
        val comando = node.strProp("_Comando").ifBlank { return null }
        val parts = comando.split("|").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size < 36) return null

        val rawE1 = parts[1]
        val rawE2 = parts[2]
        val rawPonta = parts[7]  // OID of the Ponta (arrowhead) end

        // Resolve TBarraDeAtributos → actual parent attribute/entity, then
        // resolve TChildRelacao → parent TEntidadeAssoss.
        val resolvedE1 = barraToOwner[rawE1]?.let { childRelacaoToAssoss[it] ?: it }
            ?: childRelacaoToAssoss[rawE1]
            ?: rawE1
        val resolvedE2 = barraToOwner[rawE2]?.let { childRelacaoToAssoss[it] ?: it }
            ?: childRelacaoToAssoss[rawE2]
            ?: rawE2

        // Skip connections that still cannot be mapped to any domain element
        // (e.g. purely visual TPonto or TLinha components).
        if (resolvedE1 !in elements && resolvedE2 !in elements) return null

        val showCard = parts[3] != 0
        val isWeak = parts[4] != 0
        val orientation = LineOrientation.fromCode(parts[0])

        // Ensure elementIdB == Ponta.  Pascal: "if Value[7]=Value[2] then Ponta:=E2 else Ponta:=E1"
        // The renderer uses conn.elementIdB as Ponta for cardinality-label side detection.
        val pontaIsRawE2 = rawPonta == rawE2
        val (elemIdA, elemIdB) = if (pontaIsRawE2) resolvedE1 to resolvedE2
                                 else resolvedE2 to resolvedE1

        val e1 = elements[elemIdA]
        val e2 = elements[elemIdB]

        // Attribute-to-owner connections: include them as Connection objects with no cardinality
        // so that the renderer can draw the connecting lines (computeDividedPoints uses them).
        if (e1 is SchemaElement.Attribute || e2 is SchemaElement.Attribute) {
            return Connection(
                id = connId,
                elementIdA = elemIdA,
                elementIdB = elemIdB,
                cardinality = null,
                showCardinality = false,
                isWeak = false,
                orientation = orientation,
            )
        }

        // Entity/Relationship connection: full cardinality data
        val cardCode = node.intProp("Cardinalidade")
        val cardinality = Cardinality.fromCode(cardCode)
        val fixa = node.boolProp("Fixa")

        // TCardinalidade's Left/Top is always the current label position (whether Fixa or auto).
        // Use it unconditionally so that brM and XML produce the same positioned labels.
        val labelPos = node.position()

        return Connection(
            id = connId,
            elementIdA = elemIdA,
            elementIdB = elemIdB,
            cardinality = cardinality,
            showCardinality = showCard,
            cardinalityFixed = fixa,
            isWeak = node.boolProp("Fraca") || isWeak,
            orientation = orientation,
            cardinalityRole = node.strProp("Nome"),
            cardinalityObservations = node.strProp("Observacao"),
            cardinalityDictionary = node.strProp("Dicionario"),
            cardinalityPosition = labelPos,
            cardinalityAutoSize = node.boolProp("TamAuto", true),
        )
    }

    // ── Common property helpers ───────────────────────────────────────────────

    private fun parseLabelStyle(node: DfmNode): LabelStyle {
        val fontColorStr = node.strProp("FontColor")
        val parentFontStyles = (node.properties["FontStyles"] as? DfmValue.SetVal)?.identifiers ?: emptyList()
        val flatFontStyles = (node.properties["Font.Style"] as? DfmValue.SetVal)?.identifiers ?: emptyList()
        val fontChild = node.children.firstOrNull { it.className.equals("TFont", ignoreCase = true) }
        val childFontStyles = (fontChild?.properties["Style"] as? DfmValue.SetVal)?.identifiers ?: emptyList()
        val fontStyles = when {
            flatFontStyles.isNotEmpty() -> flatFontStyles
            childFontStyles.isNotEmpty() -> childFontStyles
            else -> parentFontStyles
        }
        val fontName = node.strProp("Font.Name").trim().takeIf { it.isNotEmpty() }
            ?: fontChild?.strProp("Name")?.trim()?.takeIf { it.isNotEmpty() }
        val fontSizePoints = brmFlatOrNestedFontSizePoints(node, fontChild)
        val fontScript = brmReadDfmRawProperty(node, "Font.Charset")
            ?: fontChild?.let { brmReadFontCharsetRaw(it) }
        return LabelStyle(
            color = parseColorRef(fontColorStr),
            bold = "fsBold" in fontStyles,
            italic = "fsItalic" in fontStyles,
            underline = "fsUnderline" in fontStyles,
            strikeThrough = "fsStrikeOut" in fontStyles,
            fontFamilyName = fontName,
            fontSizePoints = fontSizePoints,
            fontScript = fontScript,
        )
    }

    private fun brmFlatOrNestedFontSizePoints(owner: DfmNode, fontChild: DfmNode?): Int? {
        val szFlat = owner.intProp("Font.Size", 0)
        if (szFlat > 0) return szFlat.coerceIn(1, 144)
        val hFlat = owner.intProp("Font.Height", 0)
        if (hFlat < 0) {
            return ((-hFlat) * 72.0 / 96.0).roundToInt().coerceIn(1, 144)
        }
        return fontChild?.let { brmInferFontSizePoints(it) }
    }

    /** Reads a DFM property as a raw string (identifiers or decimal ints). */
    private fun brmReadDfmRawProperty(node: DfmNode, key: String): String? {
        val raw = node.properties[key] ?: return null
        return when (raw) {
            is DfmValue.StrVal -> raw.value.trim().takeIf { it.isNotEmpty() }
            is DfmValue.IntVal -> raw.value.toString()
            else -> null
        }
    }
    private fun brmInferFontSizePoints(font: DfmNode): Int? {
        val sz = font.intProp("Size", 0)
        if (sz > 0) return sz.coerceIn(1, 144)
        val h = font.intProp("Height", 0)
        if (h < 0) {
            return ((-h) * 72.0 / 96.0).roundToInt().coerceIn(1, 144)
        }
        return null
    }

    /** Raw `TFont.Charset` as stored in DFM (identifier like `TURKISH_CHARSET` or a decimal). */
    private fun brmReadFontCharsetRaw(font: DfmNode): String? {
        val raw = font.properties["Charset"] ?: return null
        return when (raw) {
            is DfmValue.StrVal -> raw.value.trim().takeIf { it.isNotEmpty() }
            is DfmValue.IntVal -> raw.value.toString()
            else -> null
        }
    }

    /**
     * Parses a Delphi TColor integer or identifier string to an Int.
     *
     * Delphi stores TColor values in DFM/XML as either a decimal integer or a named
     * constant (`clRed`, `clBtnFace`, …). Absolute colours use COLORREF `0x00BBGGRR`;
     * system colours use `0x80000000 or COLOR_*` (see [VclTColorTable]).
     */
    private fun parseColorRef(value: String): Int? {
        if (value.isBlank()) return null
        return when {
            value.startsWith("cl", ignoreCase = true) ->
                VclTColorTable.namedColorRefsLowercase[value.lowercase()]
            value.startsWith("#") -> value.drop(1).toIntOrNull(16)
            else -> value.toIntOrNull()
        }
    }

    /**
     * Parses the `_AOcultos` string property — the serialized list of hidden attributes.
     *
     * The value is a `\r\n`-separated sequence of pipe-delimited records:
     * ```
     * {nivel}|{X}|{Y}|{MaxCard}|{MinCard}|{Identificador}|{Tipo}|{Nome}|
     * ```
     * - `nivel` determines depth (0 = root, 1 = child of the last level-0 attribute, etc.)
     * - `Identificador` uses Delphi's `BoolToStr`: `-1` = true, `0` = false
     *
     * Mirrors `TAtributoOculto.Serialize` / `unSerialize` in `att.pas`.
     */
    private fun parseHiddenAttributes(node: DfmNode): List<HiddenAttribute> {
        val raw = node.strProp("_AOcultos")
        if (raw.isBlank()) return emptyList()
        val lines = raw.split("\r\n", "\n").filter { it.isNotBlank() }
        return buildHiddenTree(lines, 0, 0).first
    }

    private fun buildHiddenTree(
        lines: List<String>,
        startIndex: Int,
        expectedLevel: Int,
    ): Pair<List<HiddenAttribute>, Int> {
        val result = mutableListOf<HiddenAttribute>()
        var i = startIndex
        while (i < lines.size) {
            val parts = lines[i].split("|")
            if (parts.size < 8) { i++; continue }
            val nivel = parts[0].toIntOrNull() ?: break
            if (nivel < expectedLevel) break          // back to parent
            if (nivel > expectedLevel) { i++; continue } // skip orphan lines

            val x        = parts[1].toIntOrNull() ?: -1
            val y        = parts[2].toIntOrNull() ?: -1
            val maxCard  = parts[3].toIntOrNull() ?: 0
            val minCard  = parts[4].toIntOrNull() ?: 0
            val isIdentifier = parts[5].trim() == "-1"
            val tipo     = parts[6]
            val nome     = parts[7]
            i++

            // Collect children at the next level
            val (children, nextIdx) = buildHiddenTree(lines, i, expectedLevel + 1)
            i = nextIdx

            result.add(
                HiddenAttribute(
                    name = nome,
                    type = tipo,
                    isIdentifier = isIdentifier,
                    cardinality = AttributeCardinality(
                        minCardinality = minCard,
                        maxCardinality = maxCard
                    ),
                    position = ElementPosition(
                        x = x,
                        y = y,
                        width = 0,
                        height = 0
                    ),
                    children = children,
                )
            )
        }
        return result to i
    }
}
