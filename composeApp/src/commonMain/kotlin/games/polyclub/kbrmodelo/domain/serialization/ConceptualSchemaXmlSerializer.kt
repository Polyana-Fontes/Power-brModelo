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

import games.polyclub.kbrmodelo.domain.*
import games.polyclub.kbrmodelo.domain.xml.*

/**
 * Serializes a [ConceptualSchema] to the brModelo XML format.
 *
 * The output is structurally faithful to what the original Pascal application
 * produces (`TModelo.GeraXml` in `mer.pas`), including nested attributes,
 * embedded cardinality labels, and the `<BarraDeAtributos>` element for composite
 * attributes.
 *
 * ## ID allocation for visual-only elements
 * The original format assigns IDs to `TBarraDeAtributos` and `TCardinalidade`
 * elements that have no semantic meaning in the domain model. This serializer
 * allocates those IDs from an internal counter that starts just above the schema's
 * highest existing ID, so round-trips preserve all semantic IDs intact.
 */
object ConceptualSchemaXmlSerializer {

    /** Returns the XML string representation of [schema]. */
    fun serialize(schema: ConceptualSchema): String {
        val ctx = SerializationContext(schema)
        val root = buildRoot(schema, ctx)
        return serializeXml(root)
    }

    // ── Root ─────────────────────────────────────────────────────────────────

    private fun buildRoot(schema: ConceptualSchema, ctx: SerializationContext): XmlNode {
        val totalItens = schema.elements.size + schema.connections.size

        return xmlNode("MER") {
            add(buildInformacoes(schema, totalItens))
            add(buildEntidades(schema, ctx))
            add(buildRelacoes(schema, ctx))
            add(buildEntAssoss(schema, ctx))
            add(xmlNode("Texto"))   // annotation block (empty for now)
        }
    }

    // ── <Informacoes> ─────────────────────────────────────────────────────────

    private fun buildInformacoes(schema: ConceptualSchema, totalItens: Int): XmlNode =
        xmlNode("Informacoes") {
            add(xmlNode("Posicao", "Left" to 0, "Top" to 0))
            add(xmlNode("TotalItens", "Valor" to totalItens))
            add(xmlNode("Tipo", "Valor" to "CONCEITUAL"))
            add(xmlNode("Versao", "Valor" to schema.version))
            add(xmlNode("Autor", "Valor" to schema.author))
            add(xmlNode("Observacao", "Valor" to schema.observations))
            add(buildDefaultTemplate())
        }

    private fun buildDefaultTemplate(): XmlNode = xmlNode("Template") {
        add(xmlNode("CAMPOS"))
        add(xmlNode("TIPOS"))
        add(xmlNode("COMPLEMENTO_CAMPOS"))
        add(xmlNode("COMPLEMENTO_TABELAS"))
        add(xmlNode("DDL") {
            add(xmlNode("CTab_A", text = "CREATE TABLE *\$nome_tabela"))
            add(xmlNode("CTab_B", text = " ("))
            add(xmlNode("CTab_Compl", text = "*\$compl_tabela"))
            add(xmlNode("CTab_C", text = ")*\$separador*\$\\n"))
            add(xmlNode("CCamp", text = "*\$nome_campo *\$tipo_campo *\$compl_campo"))
            add(xmlNode("Pk_inTab", text = "-1"))
            add(xmlNode("Fk_inTab", text = "-1"))
            add(xmlNode("Const_Nomear", text = "0"))
            add(xmlNode("CConst_Nome", text = "*\$nome_tabela_*\$envol_campo"))
            add(xmlNode("Separador"))
        })
    }

    // ── <Entidades> ───────────────────────────────────────────────────────────

    private fun buildEntidades(schema: ConceptualSchema, ctx: SerializationContext): XmlNode =
        xmlNode("Entidades") {
            schema.entities.forEach { entity ->
                add(buildEntidade(entity, schema, ctx))
            }
        }

    private fun buildEntidade(
        entity: SchemaElement.Entity,
        schema: ConceptualSchema,
        ctx: SerializationContext,
    ): XmlNode = xmlNode("Entidade", "nome" to entity.name, "id" to entity.id) {
        addAll(buildBaseFields(entity))
        add(buildAtributos(schema.attributesOf(entity.id), schema, ctx))
        add(buildAtributosOcultos(entity.hiddenAttributes))
        textNode("Dicionario", entity.dictionary)
        boolValor("Nula", entity.isWeak)
        textNode("Observacao", entity.observations)
        textNode("Futuro", "")
        add(xmlNode("Anexos"))
        add(buildAutoRelacoes(entity.id, schema, ctx))
        add(buildEspecializacoes(entity.id, schema, ctx))
    }

    // ── <Relacoes> ────────────────────────────────────────────────────────────

    private fun buildRelacoes(schema: ConceptualSchema, ctx: SerializationContext): XmlNode =
        xmlNode("Relacoes") {
            schema.relationships.forEach { rel ->
                add(buildRelacao(rel, schema, ctx))
            }
        }

    private fun buildRelacao(
        rel: SchemaElement.Relationship,
        schema: ConceptualSchema,
        ctx: SerializationContext,
    ): XmlNode = xmlNode("Relacao", "nome" to rel.name, "id" to rel.id) {
        addAll(buildBaseFields(rel))
        add(buildAtributos(schema.attributesOf(rel.id), schema, ctx))
        add(buildAtributosOcultos(rel.hiddenAttributes))
        textNode("Dicionario", rel.dictionary)
        boolValor("Nula", false)
        textNode("Observacao", rel.observations)
        textNode("Futuro", "")
        add(xmlNode("Anexos"))
        valor("SetaDirecao", rel.arrowDirection.code)
        add(buildLigacoes(schema.connectionsOf(rel.id).filter { it.elementIdA == rel.id }, ctx))
    }

    // ── <EntAssoss> ───────────────────────────────────────────────────────────

    private fun buildEntAssoss(schema: ConceptualSchema, ctx: SerializationContext): XmlNode =
        xmlNode("EntAssoss") {
            schema.associativeEntities.forEach { ea ->
                add(buildEntidadeAssoss(ea, schema, ctx))
            }
        }

    private fun buildEntidadeAssoss(
        ea: SchemaElement.AssociativeEntity,
        schema: ConceptualSchema,
        ctx: SerializationContext,
    ): XmlNode = xmlNode("EntidadeAssoss", "nome" to ea.name, "id" to ea.id) {
        addAll(buildBaseFields(ea))
        add(buildAtributos(schema.attributesOf(ea.id), schema, ctx))
        add(buildAtributosOcultos(ea.hiddenAttributes))
        textNode("Dicionario", ea.dictionary)
        boolValor("Nula", false)
        textNode("Observacao", ea.observations)
        textNode("Futuro", "")
        add(xmlNode("Anexos"))
        add(buildAutoRelacoes(ea.id, schema, ctx))
        add(buildChildRelacao(ea, schema, ctx))
    }

    private fun buildChildRelacao(
        ea: SchemaElement.AssociativeEntity,
        schema: ConceptualSchema,
        ctx: SerializationContext,
    ): XmlNode {
        // We need a stable ID for ChildRelacao. We store it as (ea.id) and allocate if needed.
        val childRelId = ctx.nextId()
        return xmlNode("ChildRelacao", "nome" to ea.relationshipName, "id" to childRelId) {
            addAll(buildBaseFields(ea))    // reuse same position/font
            add(xmlNode("Atributos"))
            add(xmlNode("AtributosOcultos"))
            textNode("Dicionario", ea.relationshipDictionary)
            boolValor("Nula", false)
            textNode("Observacao", ea.relationshipObservations)
            textNode("Futuro", "")
            add(xmlNode("Anexos"))
            valor("SetaDirecao", ea.arrowDirection.code)
            add(buildLigacoes(schema.connectionsOf(ea.id).filter { it.elementIdA == ea.id }, ctx))
        }
    }

    // ── <Atributos> ───────────────────────────────────────────────────────────

    private fun buildAtributos(
        attrs: List<SchemaElement.Attribute>,
        schema: ConceptualSchema,
        ctx: SerializationContext,
    ): XmlNode = xmlNode("Atributos") {
        // Only top-level attributes (non-composite children go into BarraDeAtributos)
        attrs.filter { !isCompositeChild(it, attrs) }.forEach { attr ->
            add(buildAtributo(attr, schema, ctx))
        }
    }

    private fun buildAtributo(
        attr: SchemaElement.Attribute,
        schema: ConceptualSchema,
        ctx: SerializationContext,
    ): XmlNode {
        val barraId = ctx.nextId()
        val conn = schema.connectionsOf(attr.id).firstOrNull { it.elementIdA == attr.id }

        return xmlNode("Atributo", "nome" to attr.name, "id" to attr.id) {
            addAll(buildBaseFields(attr))

            // For composite attributes <Atributos/> is empty; children go inside <BarraDeAtributos>
            add(xmlNode("Atributos"))
            add(buildAtributosOcultos(attr.hiddenAttributes))
            textNode("Dicionario", attr.dictionary)
            boolValor("Nula", false)
            textNode("Observacao", attr.observations)
            textNode("Futuro", "")
            add(xmlNode("Anexos"))
            valor("MaxCard", attr.cardinality.maxCardinality)
            valor("MinCard", attr.cardinality.minCardinality)
            boolValor("Composto", attr.isComposite)
            boolValor("Identificador", attr.isIdentifier)
            valor("Tipo", attr.valueType)
            boolValor("Multivalorado", attr.isMultiValued)
            valor("QtdeMultivalorado", schema.canonicalQtdeMultivalorado(attr))
            valor("Orientacao", 3)           // default: OrientacaoE (left)
            boolValor("TamAuto", attr.autoSize)
            valor("Desvio", attr.deviationAngle)

            if (attr.isComposite) {
                add(xmlNode("BarraDeAtributos") {
                    val children = schema.childAttributesOf(attr.id)
                    children.forEach { child -> add(buildAtributo(child, schema, ctx)) }
                })
            } else {
                valor("BarraID", barraId)
            }

            add(buildAtributoLigacoes(attr, conn, ctx))
        }
    }

    private fun buildAtributoLigacoes(
        attr: SchemaElement.Attribute,
        conn: Connection?,
        ctx: SerializationContext,
    ): XmlNode = xmlNode("Ligacoes") {
        if (conn != null) {
            add(buildLigacao(conn, ctx))
        }
    }

    // ── <AutoRelacoes> ────────────────────────────────────────────────────────

    private fun buildAutoRelacoes(
        entityId: Int,
        schema: ConceptualSchema,
        ctx: SerializationContext,
    ): XmlNode {
        val selfRels = schema.selfRelationships.filter { it.ownerEntityId == entityId }
        return xmlNode("AutoRelacoes", "AutoRelacionado" to if (selfRels.isNotEmpty()) -1 else 0) {
            selfRels.forEach { sr -> add(buildSelfRelationship(sr, schema, ctx)) }
        }
    }

    private fun buildSelfRelationship(
        sr: SchemaElement.SelfRelationship,
        schema: ConceptualSchema,
        ctx: SerializationContext,
    ): XmlNode = xmlNode("AutoRelacao", "nome" to sr.name, "id" to sr.id) {
        addAll(buildBaseFields(sr))
        add(xmlNode("Atributos"))
        add(buildAtributosOcultos(sr.hiddenAttributes))
        textNode("Dicionario", sr.dictionary)
        boolValor("Nula", false)
        textNode("Observacao", sr.observations)
        textNode("Futuro", "")
        add(xmlNode("Anexos"))
        valor("SetaDirecao", sr.arrowDirection.code)
        add(buildLigacoes(schema.connectionsOf(sr.id).filter { it.elementIdA == sr.id }, ctx))
    }

    // ── <Especializacoes> ─────────────────────────────────────────────────────

    private fun buildEspecializacoes(
        entityId: Int,
        schema: ConceptualSchema,
        ctx: SerializationContext,
    ): XmlNode {
        val specs = schema.specializations.filter { it.baseEntityId == entityId }
        return xmlNode("Especializacoes", "ehEsp" to if (specs.isNotEmpty()) -1 else 0) {
            specs.forEach { spec -> add(buildEspecializacao(spec, schema, ctx)) }
        }
    }

    private fun buildEspecializacao(
        spec: SchemaElement.Specialization,
        schema: ConceptualSchema,
        ctx: SerializationContext,
    ): XmlNode = xmlNode("Especializacao", "nome" to spec.name, "id" to spec.id) {
        addAll(buildBaseFields(spec))
        add(xmlNode("Atributos"))
        add(buildAtributosOcultos(spec.hiddenAttributes))
        textNode("Dicionario", spec.dictionary)
        boolValor("Nula", false)
        textNode("Observacao", spec.observations)
        textNode("Futuro", "")
        add(xmlNode("Anexos"))
        boolValor("Parcial", spec.isPartial)
        add(buildLigacoes(schema.connectionsOf(spec.id).filter { it.elementIdA == spec.id }, ctx))
    }

    // ── <Ligacoes> / <Ligacao> ────────────────────────────────────────────────

    private fun buildLigacoes(
        conns: List<Connection>,
        ctx: SerializationContext,
    ): XmlNode = xmlNode("Ligacoes") {
        conns.forEach { conn -> add(buildLigacao(conn, ctx)) }
    }

    private fun buildLigacao(conn: Connection, ctx: SerializationContext): XmlNode {
        val cardId = conn.id.takeIf { it > 0 } ?: ctx.nextId()
        val showCard = conn.showCardinality
        val cardCode = conn.cardinality?.let {
            when (it) {
                Cardinality.ONE_TO_ONE  -> 1
                Cardinality.ZERO_TO_ONE -> 2
                Cardinality.ONE_TO_MANY -> 3
                Cardinality.ZERO_TO_MANY -> 4
            }
        } ?: 4

        return xmlNode("Ligacao", "Destino_ID" to conn.elementIdB) {
            add(xmlNode(
                "MostraCardinalidade",
                "Valor" to if (showCard) -1 else 0,
                "Card_id" to cardId,
            ))
            add(buildCardinalidades(cardCode, cardId, conn, showCard))
            valor("Orientacao", conn.orientation.code)
            boolValor("Fraca", conn.isWeak)
        }
    }

    private fun buildCardinalidades(
        cardCode: Int,
        cardId: Int,
        conn: Connection,
        showCard: Boolean,
    ): XmlNode = xmlNode("Cardinalidades", "Cardinalidade" to cardCode) {
        if (showCard) {
            val pos = conn.cardinalityPosition ?: ElementPosition(0, 0, 36, 20)
            add(buildCardinalidadeNode(cardId, conn.cardinality, pos, conn.cardinalityFixed, conn.cardinalityRole, conn.cardinalityAutoSize))
        }
    }

    private fun buildCardinalidadeNode(
        id: Int,
        cardinality: Cardinality?,
        position: ElementPosition,
        fixed: Boolean,
        role: String = "",
        autoSize: Boolean = true,
    ): XmlNode {
        val cardCode = when (cardinality) {
            Cardinality.ONE_TO_ONE   -> 1
            Cardinality.ZERO_TO_ONE  -> 2
            Cardinality.ONE_TO_MANY  -> 3
            Cardinality.ZERO_TO_MANY -> 4
            null -> 4
        }
        return xmlNode("Cardinalidade", "nome" to role, "id" to id) {
            valor("Left", position.x)
            valor("Top", position.y)
            valor("Width", position.width)
            valor("Height", position.height)
            add(buildDefaultFont())
            add(xmlNode("Atributos"))
            add(xmlNode("AtributosOcultos"))
            textNode("Dicionario", "")
            boolValor("Nula", false)
            textNode("Observacao", "")
            textNode("Futuro", "")
            add(xmlNode("Anexos"))
            valor("Cor", DEFAULT_CARD_COLOR)
            boolValor("TamAuto", autoSize)
            valor("Tipo", 0)
            valor("TextAlin", 0)
            valor("Card", cardCode)
            boolValor("Fixa", fixed)
            add(xmlNode("Ligacoes"))
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    /** Writes the four position children: Left, Top, Width, Height, and the Font block. */
    private fun buildBaseFields(element: SchemaElement): List<XmlNode> = buildList {
        val p = element.position
        add(xmlNode("Left", "Valor" to p.x))
        add(xmlNode("Top", "Valor" to p.y))
        add(xmlNode("Width", "Valor" to p.width))
        add(xmlNode("Height", "Valor" to p.height))
        add(buildFont(element.labelStyle))
    }

    private fun buildFont(style: LabelStyle): XmlNode = xmlNode("Fonte", "default" to 0) {
        add(xmlNode("FonteNome", "Valor" to "Tahoma"))
        add(xmlNode("FonteTamanho", "Valor" to 8))
        val styleStr = buildList {
            if (style.bold) add("fsBold")
            if (style.italic) add("fsItalic")
        }.joinToString(",").let { if (it.isEmpty()) "[]" else "[$it]" }
        add(xmlNode("FonteEstilo", "Valor" to styleStr))
        add(xmlNode("FonteCor", "Valor" to (style.color ?: 0)))
    }

    private fun buildDefaultFont(): XmlNode = buildFont(LabelStyle())

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Returns true when [attr] is a child of another [Attribute] in [siblings].
     * These children are written inside `<BarraDeAtributos>` of their parent,
     * not as direct children of `<Atributos>`.
     */
    private fun isCompositeChild(
        attr: SchemaElement.Attribute,
        siblings: List<SchemaElement.Attribute>,
    ): Boolean = siblings.any { it.childAttributeIds.contains(attr.id) }

    /**
     * Builds the `<AtributosOcultos>` node for a list of hidden attributes.
     *
     * Each [HiddenAttribute] is serialized as `<AtributoOculto Nome="...">` with its
     * properties. Composite attributes include a nested `<Atributos>` block containing
     * their children. Mirrors `TAtributoOculto.GeraXML` / `TConjPAtt.GeraXML` in `att.pas`.
     */
    private fun buildAtributosOcultos(hidden: List<HiddenAttribute>): XmlNode =
        xmlNode("AtributosOcultos") {
            hidden.forEach { add(buildAtributoOculto(it)) }
        }

    private fun buildAtributoOculto(attr: HiddenAttribute): XmlNode =
        xmlNode("AtributoOculto", "Nome" to attr.name) {
            add(xmlNode("LeftTop", "X" to attr.position.x, "Y" to attr.position.y))
            valor("MaxCard", attr.cardinality.maxCardinality)
            valor("MinCard", attr.cardinality.minCardinality)
            boolValor("Composto", attr.isComposite)
            boolValor("Identificador", attr.isIdentifier)
            valor("Tipo", attr.type)
            if (attr.isComposite) {
                add(xmlNode("Atributos") {
                    attr.children.forEach { add(buildAtributoOculto(it)) }
                })
            }
        }

    /** Default cardinality label background colour (Windows COLORREF 0xF0CAA6, light blue-grey). */
    private const val DEFAULT_CARD_COLOR = 15780518
}

// ── Serialization context ─────────────────────────────────────────────────────

/**
 * Mutable context carried through a single serialization pass.
 * Allocates IDs for visual-only elements (BarraDeAtributos, ChildRelacao) that
 * have no representation in the domain model.
 */
private class SerializationContext(schema: ConceptualSchema) {
    private var counter = schema.nextId

    fun nextId(): Int = counter++
}
