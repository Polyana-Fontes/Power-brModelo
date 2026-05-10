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

import games.polyclub.power.brmodelo.domain.*
import games.polyclub.power.brmodelo.domain.xml.XmlNode
import games.polyclub.power.brmodelo.domain.xml.parseXmlBytes

/**
 * Deserializes a brModelo `.xml` file into a [games.polyclub.power.brmodelo.domain.ConceptualSchema].
 *
 * The XML format is the one produced by the original Pascal brModelo application.
 * Relevant Pascal source: `mer.pas` → `TModelo.GeraXml` / `TModelo.LoadFromXML`.
 *
 * ## Key format notes
 * - **Booleans**: Delphi stores them as integers: `-1` = true, `0` = false.
 * - **Nested structure**: Attributes are nested inside their owner element.
 *   Child attributes of composite attributes are nested inside `<BarraDeAtributos>`.
 * - **Connections** (`<Ligacao>`): stored inside the element that originates the
 *   connection. The `Destino_ID` attribute is the target element's ID.
 * - **Cardinality labels**: when `MostraCardinalidade Valor="-1"`, the full
 *   `<Cardinalidade>` element (with position) is embedded inside `<Cardinalidades>`.
 * - **IDs**: all elements have a numeric `id` attribute. Connections reference
 *   elements via `Destino_ID` and cardinality labels via `Card_id`.
 * - **Encoding**: original files use ISO-8859-1; the Java XML parser handles this
 *   automatically from the `<?xml ... encoding="ISO-8859-1"?>` declaration.
 */
object ConceptualSchemaXmlParser {

    /** Parses raw bytes of a brModelo `.xml` file. */
    fun parse(bytes: ByteArray): ConceptualSchema = parse(parseXmlBytes(bytes))

    // ── Root parsing ─────────────────────────────────────────────────────────

    private fun parse(root: XmlNode): ConceptualSchema {
        val info = root.child("Informacoes")

        val version = info?.strValor("Versao") ?: "2.0.0"
        val author = info?.child("Autor")?.attr("Valor") ?: ""
        val observations = info?.child("Observacao")?.attr("Valor") ?: ""

        val elements = mutableMapOf<Int, SchemaElement>()
        val connections = mutableListOf<Connection>()

        root.child("Entidades")?.children("Entidade")?.forEach { node ->
            parseEntidade(node, elements, connections)
        }

        root.child("Relacoes")?.children("Relacao")?.forEach { node ->
            parseRelacao(node, elements, connections)
        }

        root.child("EntAssoss")?.children("EntidadeAssoss")?.forEach { node ->
            parseEntAssoss(node, elements, connections)
        }

        root.child("Texto")?.children("Texto")?.forEach { node ->
            parseTexto(node, elements)
        }

        val maxId = (elements.keys.maxOrNull() ?: 0)
            .coerceAtLeast(connections.maxOfOrNull { it.id } ?: 0)

        return ConceptualSchema(
            version = version,
            author = author,
            observations = observations,
            elements = elements,
            connections = connections,
            nextId = maxId + 1,
        ).withNormalizedAttributeMultiValuedCounts().withCoercedMinimumDimensions()
    }

    // ── Entity ───────────────────────────────────────────────────────────────

    private fun parseEntidade(
        node: XmlNode,
        elements: MutableMap<Int, SchemaElement>,
        connections: MutableList<Connection>,
    ) {
        val id = node.attrInt("id") ?: return
        val name = node.attr("nome") ?: ""
        val position = parsePosition(node)
        val labelStyle = parseFont(node)
        val dictionary = node.textChild("Dicionario")
        val observations = node.textChild("Observacao")
        // <Nula Valor="0|-1"/> — used as a generic base flag; maps to isWeak here.
        val isWeak = node.boolValor("Nula")

        // Nested attributes (direct children or composite children via BarraDeAtributos)
        node.child("Atributos")?.children("Atributo")?.forEach { attrNode ->
            parseAtributo(attrNode, ownerId = id, elements, connections)
        }

        // Auto-relationships (self-references)
        val autoRelNode = node.child("AutoRelacoes")
        if (autoRelNode?.attr("AutoRelacionado") == "-1") {
            autoRelNode.children("AutoRelacao").forEach { arNode ->
                parseSelfRelationship(arNode, ownerEntityId = id, elements, connections)
            }
        }

        // Specializations where this entity is the BASE
        val espNode = node.child("Especializacoes")
        val specializationIds = mutableListOf<Int>()
        if (espNode?.attr("ehEsp") == "-1") {
            espNode.children("Especializacao").forEach { eNode ->
                val specId = parseEspecializacao(eNode, baseEntityId = id, elements, connections)
                if (specId != null) specializationIds.add(specId)
            }
        }

        val hiddenAttributes = parseHiddenAttributes(node)

        elements[id] = SchemaElement.Entity(
            id = id,
            name = name,
            position = position,
            observations = observations,
            dictionary = dictionary,
            labelStyle = labelStyle,
            hiddenAttributes = hiddenAttributes,
            isWeak = isWeak,
            specializationId = specializationIds.firstOrNull(),
            parentSpecializationIds = specializationIds,
        )
    }

    // ── Relationship ─────────────────────────────────────────────────────────

    private fun parseRelacao(
        node: XmlNode,
        elements: MutableMap<Int, SchemaElement>,
        connections: MutableList<Connection>,
    ) {
        val id = node.attrInt("id") ?: return
        val name = node.attr("nome") ?: ""
        val position = parsePosition(node)
        val labelStyle = parseFont(node)
        val dictionary = node.textChild("Dicionario")
        val observations = node.textChild("Observacao")
        val arrowDirection = ArrowDirection.fromCode(node.intValor("SetaDirecao"))

        node.child("Atributos")?.children("Atributo")?.forEach { attrNode ->
            parseAtributo(attrNode, ownerId = id, elements, connections)
        }

        node.child("Ligacoes")?.children("Ligacao")?.forEach { ligNode ->
            parseLigacao(ligNode, elementIdA = id, connections)
        }

        elements[id] = SchemaElement.Relationship(
            id = id,
            name = name,
            position = position,
            observations = observations,
            dictionary = dictionary,
            labelStyle = labelStyle,
            hiddenAttributes = parseHiddenAttributes(node),
            arrowDirection = arrowDirection,
            showName = true,
        )
    }

    // ── Associative Entity ────────────────────────────────────────────────────

    private fun parseEntAssoss(
        node: XmlNode,
        elements: MutableMap<Int, SchemaElement>,
        connections: MutableList<Connection>,
    ) {
        val id = node.attrInt("id") ?: return
        val name = node.attr("nome") ?: ""
        val position = parsePosition(node)
        val labelStyle = parseFont(node)
        val dictionary = node.textChild("Dicionario")
        val observations = node.textChild("Observacao")

        // Attributes of the associative entity itself
        node.child("Atributos")?.children("Atributo")?.forEach { attrNode ->
            parseAtributo(attrNode, ownerId = id, elements, connections)
        }

        // The embedded inner relationship (ChildRelacao)
        val childRelNode = node.child("ChildRelacao")
        val relName = childRelNode?.attr("nome") ?: ""
        val relDictionary = childRelNode?.textChild("Dicionario") ?: ""
        val relObservations = childRelNode?.textChild("Observacao") ?: ""
        val arrowDirection = ArrowDirection.fromCode(
            childRelNode?.intValor("SetaDirecao") ?: 0
        )

        // Connections from the inner ChildRelacao to participating entities
        childRelNode?.child("Ligacoes")?.children("Ligacao")?.forEach { ligNode ->
            parseLigacao(ligNode, elementIdA = id, connections)
        }

        elements[id] = SchemaElement.AssociativeEntity(
            id = id,
            name = name,
            position = position,
            observations = observations,
            dictionary = dictionary,
            labelStyle = labelStyle,
            hiddenAttributes = parseHiddenAttributes(node),
            relationshipName = relName,
            relationshipDictionary = relDictionary,
            relationshipObservations = relObservations,
            arrowDirection = arrowDirection,
        )
    }

    // ── Attribute ─────────────────────────────────────────────────────────────

    /**
     * Parses an `<Atributo>` element and adds it (and any composite children) to
     * [elements] plus their connections to [connections].
     *
     * @param ownerId The ID of the owning entity, relationship, or parent attribute.
     */
    private fun parseAtributo(
        node: XmlNode,
        ownerId: Int,
        elements: MutableMap<Int, SchemaElement>,
        connections: MutableList<Connection>,
    ) {
        val id = node.attrInt("id") ?: return
        val name = node.attr("nome") ?: ""
        val position = parsePosition(node)
        val labelStyle = parseFont(node)
        val dictionary = node.textChild("Dicionario")
        val observations = node.textChild("Observacao")

        val maxCard = node.intValor("MaxCard", default = 1)
        val minCard = node.intValor("MinCard", default = 1)
        val isIdentifier = node.boolValor("Identificador")
        val isMultiValued = maxCard > 1
        // An attribute is optional when MinCard is 0 (applies both to the attribute itself
        // and to its participation in its owner, consistent with TAtributo.Opcional)
        val isOptional = minCard == 0
        val valueType = node.strValor("Tipo")
        val autoSize = node.boolValor("TamAuto")
        val deviationAngle = node.intValor("Desvio", default = 10)

        // Parse composite children from <BarraDeAtributos>
        val childIds = mutableListOf<Int>()
        node.child("BarraDeAtributos")?.children("Atributo")?.forEach { childNode ->
            val childId = childNode.attrInt("id")
            if (childId != null) {
                childIds.add(childId)
                // Child attributes' owner is this composite attribute
                parseAtributo(childNode, ownerId = id, elements, connections)
            }
        }

        // Connection from this attribute to its owner
        node.child("Ligacoes")?.children("Ligacao")?.forEach { ligNode ->
            // For composite child attributes, Destino_ID points to BarraDeAtributos
            // (a visual element), but we remap the connection to go to the composite
            // attribute (ownerId) instead. For non-composite attributes the Destino_ID
            // already matches the real owner.
            parseLigacao(ligNode, elementIdA = id, connections, remapDestino = ownerId)
        }

        elements[id] = SchemaElement.Attribute(
            id = id,
            name = name,
            position = position,
            observations = observations,
            dictionary = dictionary,
            labelStyle = labelStyle,
            hiddenAttributes = parseHiddenAttributes(node),
            ownerId = ownerId,
            isIdentifier = isIdentifier,
            isMultiValued = isMultiValued,
            isOptional = isOptional,
            cardinality = AttributeCardinality(
                minCardinality = minCard,
                maxCardinality = maxCard
            ),
            valueType = valueType,
            autoSize = autoSize,
            deviationAngle = deviationAngle,
            childAttributeIds = childIds,
        )
    }

    // ── Specialization ────────────────────────────────────────────────────────

    /**
     * Parses a `<Especializacao>` element nested inside a `<Entidade>`.
     *
     * @param baseEntityId The ID of the entity that contains this specialization.
     * @return The specialization's ID, or null on parse failure.
     */
    private fun parseEspecializacao(
        node: XmlNode,
        baseEntityId: Int,
        elements: MutableMap<Int, SchemaElement>,
        connections: MutableList<Connection>,
    ): Int? {
        val id = node.attrInt("id") ?: return null
        val name = node.attr("nome") ?: ""
        val position = parsePosition(node)
        val labelStyle = parseFont(node)
        val dictionary = node.textChild("Dicionario")
        val observations = node.textChild("Observacao")
        val isPartial = node.boolValor("Parcial")
        // <Tipo Valor="0|1"/> — 0=restricted, 1=optional. Defaults to OPTIONAL when absent.
        val type = SpecializationType.fromCode(node.intValor("Tipo", default = 1))

        node.child("Ligacoes")?.children("Ligacao")?.forEach { ligNode ->
            parseLigacao(ligNode, elementIdA = id, connections)
        }

        elements[id] = SchemaElement.Specialization(
            id = id,
            name = name,
            position = position,
            observations = observations,
            dictionary = dictionary,
            labelStyle = labelStyle,
            hiddenAttributes = parseHiddenAttributes(node),
            baseEntityId = baseEntityId,
            type = type,
            isPartial = isPartial,
        )
        return id
    }

    // ── Self-Relationship ─────────────────────────────────────────────────────

    private fun parseSelfRelationship(
        node: XmlNode,
        ownerEntityId: Int,
        elements: MutableMap<Int, SchemaElement>,
        connections: MutableList<Connection>,
    ) {
        val id = node.attrInt("id") ?: return
        val name = node.attr("nome") ?: ""
        val position = parsePosition(node)
        val labelStyle = parseFont(node)
        val arrowDirection = ArrowDirection.fromCode(node.intValor("SetaDirecao"))

        node.child("Ligacoes")?.children("Ligacao")?.forEach { ligNode ->
            parseLigacao(ligNode, elementIdA = id, connections)
        }

        elements[id] = SchemaElement.SelfRelationship(
            id = id,
            name = name,
            position = position,
            observations = node.textChild("Observacao"),
            dictionary = node.textChild("Dicionario"),
            labelStyle = labelStyle,
            hiddenAttributes = parseHiddenAttributes(node),
            ownerEntityId = ownerEntityId,
            arrowDirection = arrowDirection,
        )
    }

    // ── Annotation (Texto) ────────────────────────────────────────────────────

    private fun parseTexto(
        node: XmlNode,
        elements: MutableMap<Int, SchemaElement>,
    ) {
        val id = node.attrInt("id") ?: return
        val name = node.attr("nome") ?: ""
        val position = parsePosition(node)
        val labelStyle = parseFont(node)
        val color = node.child("Cor")?.attr("Valor")?.toIntOrNull()
        val annotationType = AnnotationType.fromCode(node.intValor("Tipo"))
        val alignment = TextAlignment.fromCode(node.intValor("TextAlin"))
        val autoSize = node.boolValor("TamAuto")

        elements[id] = SchemaElement.Annotation(
            id = id,
            name = name,
            position = position,
            observations = node.textChild("Observacao"),
            dictionary = node.textChild("Dicionario"),
            labelStyle = labelStyle,
            color = color,
            annotationType = annotationType,
            alignment = alignment,
            autoSize = autoSize,
        )
    }

    // ── Connection (Ligacao) ──────────────────────────────────────────────────

    /**
     * Parses a `<Ligacao>` element and appends a [games.polyclub.power.brmodelo.domain.Connection] to [connections].
     *
     * @param elementIdA The ID of the element that contains this `<Ligacao>`.
     * @param remapDestino When non-null, overrides the `Destino_ID` value. Used for
     *   composite attribute children whose connections point to a BarraDeAtributos
     *   (a visual-only element) instead of the semantic owner.
     */
    private fun parseLigacao(
        node: XmlNode,
        elementIdA: Int,
        connections: MutableList<Connection>,
        remapDestino: Int? = null,
    ) {
        val destinoId = remapDestino ?: node.attrInt("Destino_ID") ?: return
        val mostraCard = node.child("MostraCardinalidade")
        val cardId = mostraCard?.attrInt("Card_id") ?: return
        val showCardinality = mostraCard.attr("Valor") == "-1"

        val cardinalidadesNode = node.child("Cardinalidades") ?: return
        val cardCode = cardinalidadesNode.attr("Cardinalidade")?.toIntOrNull() ?: 4
        val cardinality = Cardinality.fromCode(cardCode)

        val isWeak = node.boolValor("Fraca")
        val orientation = LineOrientation.fromCode(node.intValor("Orientacao"))

        val useOuterA = node.intValor("AssocOuterA", default = 0) != 0
        val useOuterB = node.intValor("AssocOuterB", default = 0) != 0

        // Cardinality label position, role name, and fixed flag (only when label is shown)
        val cardNode = if (showCardinality) cardinalidadesNode.child("Cardinalidade") else null
        val cardPos = cardNode?.let { parsePosition(it) }
        val cardFixed = cardNode?.boolValor("Fixa") ?: false
        val cardRole = cardNode?.attr("nome")?.trim() ?: ""
        val cardAutoSize = cardNode?.boolValor("TamAuto") ?: true
        val cardDict = cardNode?.textChild("Dicionario") ?: ""
        val cardObs = cardNode?.textChild("Observacao") ?: ""

        connections.add(
            Connection(
                id = cardId,
                elementIdA = elementIdA,
                elementIdB = destinoId,
                cardinality = cardinality,
                showCardinality = showCardinality,
                cardinalityFixed = cardFixed,
                isWeak = isWeak,
                orientation = orientation,
                cardinalityRole = cardRole,
                cardinalityObservations = cardObs,
                cardinalityDictionary = cardDict,
                cardinalityPosition = cardPos,
                cardinalityAutoSize = cardAutoSize,
                useAssociativeOuterForEndA = useOuterA,
                useAssociativeOuterForEndB = useOuterB,
            )
        )
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private fun parsePosition(node: XmlNode) =
        ElementPosition(
            x = node.intValor("Left"),
            y = node.intValor("Top"),
            width = node.intValor("Width"),
            height = node.intValor("Height"),
        )

    private fun parseFont(node: XmlNode): LabelStyle {
        val fonte = node.child("Fonte") ?: return LabelStyle()
        val style = fonte.strValor("FonteEstilo")
        return LabelStyle(
            color = fonte.child("FonteCor")?.attr("Valor")?.toIntOrNull(),
            bold = style.contains("fsBold"),
            italic = style.contains("fsItalic"),
        )
    }

    /**
     * Parses `<AtributosOcultos>` — hidden attributes not shown on the canvas.
     *
     * Each `<AtributoOculto Nome="...">` child may contain a nested `<Atributos>` block
     * with further `<AtributoOculto>` elements when `<Composto Valor="-1"/>`.
     * Mirrors `TAtributoOculto.LoadByXML` / `GeraXML` in `att.pas`.
     */
    private fun parseHiddenAttributes(node: XmlNode): List<HiddenAttribute> {
        val aocultos = node.child("AtributosOcultos") ?: return emptyList()
        return aocultos.children("AtributoOculto").map { parseHiddenAttribute(it) }
    }

    private fun parseHiddenAttribute(node: XmlNode): HiddenAttribute {
        val name = node.attr("Nome") ?: ""
        val leftTop = node.child("LeftTop")
        val position = ElementPosition(
            x = leftTop?.attr("X")?.toIntOrNull() ?: -1,
            y = leftTop?.attr("Y")?.toIntOrNull() ?: -1,
            width = 0,
            height = 0,
        )
        val maxCard = node.intValor("MaxCard")
        val minCard = node.intValor("MinCard")
        val isIdentifier = node.boolValor("Identificador")
        val type = node.strValor("Tipo")
        val isComposite = node.boolValor("Composto")
        val children = if (isComposite) {
            node.child("Atributos")?.children("AtributoOculto")?.map { parseHiddenAttribute(it) } ?: emptyList()
        } else {
            emptyList()
        }
        val nestedHiddenAttributes =
            node.child("AtributosOcultosAninhados")?.children("AtributoOculto")?.map { parseHiddenAttribute(it) }
                ?: emptyList()
        val isOptional = node.boolValor("Opcional")
        return HiddenAttribute(
            name = name,
            type = type,
            isIdentifier = isIdentifier,
            cardinality = AttributeCardinality(
                minCardinality = minCard,
                maxCardinality = maxCard
            ),
            position = position,
            children = children,
            nestedHiddenAttributes = nestedHiddenAttributes,
            isOptional = isOptional,
        )
    }
}
