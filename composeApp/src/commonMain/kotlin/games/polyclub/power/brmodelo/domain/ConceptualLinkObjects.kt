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

import androidx.compose.ui.geometry.Offset

/**
 * Pick target for the conceptual "Ligar objetos" tool.
 *
 * For an [SchemaElement.AssociativeEntity], [isAssociativeOuterEntitySide] distinguishes a click on the
 * outer rectangle (entity side, `true`) from the inner diamond (relationship side, `false`).
 * For any other element type the flag is ignored and should be `false`.
 */
data class ConceptualLinkPick(
    val elementId: Int,
    val isAssociativeOuterEntitySide: Boolean = false,
)

/** Semantic role of a link endpoint in the conceptual ER model. */
enum class ConceptualLinkEndpointKind {
    /** Plain entity or the outer rectangle of an associative entity. */
    ENTITY_SIDE,

    /** Relationship, self-relationship, or inner diamond of an associative entity. */
    RELATIONSHIP_SIDE,

    /** Specialization triangle ([SchemaElement.Specialization]). Pascal [TEspecializacao] in `Tool_Ligacao`. */
    SPECIALIZATION_SIDE,
}

/**
 * Returns the [ConceptualLinkEndpointKind] for [pick], or `null` if the element cannot participate
 * in the conceptual "Ligar objetos" tool.
 */
fun conceptualLinkEndpointKind(
    element: SchemaElement,
    pick: ConceptualLinkPick,
): ConceptualLinkEndpointKind? {
    if (pick.elementId != element.id) return null
    return when (element) {
        is SchemaElement.Entity ->
            ConceptualLinkEndpointKind.ENTITY_SIDE
        is SchemaElement.Relationship, is SchemaElement.SelfRelationship ->
            ConceptualLinkEndpointKind.RELATIONSHIP_SIDE
        is SchemaElement.AssociativeEntity ->
            if (pick.isAssociativeOuterEntitySide) ConceptualLinkEndpointKind.ENTITY_SIDE
            else ConceptualLinkEndpointKind.RELATIONSHIP_SIDE
        is SchemaElement.Specialization ->
            ConceptualLinkEndpointKind.SPECIALIZATION_SIDE
        else -> null
    }
}

private fun conceptualRelToEntityLegCount(schema: ConceptualSchema, relId: Int, entityId: Int): Int =
    schema.connections.count { it.elementIdA == relId && it.elementIdB == entityId }

/**
 * Whether another [Connection] with `elementIdA == relId` and `elementIdB == entityId` would be invalid.
 * - [SchemaElement.SelfRelationship]: up to two legs to the same entity (Pascal `Relacione(Self)` × 2).
 * - [SchemaElement.Relationship]: normally one leg per entity; a **second** leg to the **same** entity is allowed
 *   only when that diamond still links exclusively to that entity (manual auto-rel: E–R then R–E).
 */
internal fun isDuplicateConceptualRelEntityConnection(schema: ConceptualSchema, relId: Int, entityId: Int): Boolean {
    val n = conceptualRelToEntityLegCount(schema, relId, entityId)
    if (n == 0) return false
    val rel = schema.elements[relId] ?: return true
    when (rel) {
        is SchemaElement.SelfRelationship -> return n >= 2
        is SchemaElement.Relationship -> {
            if (n >= 2) return true
            // n == 1: allow second (rel, entityId) only if this rel still has a single distinct entity end
            if (schema.selfRelationships.any { it.ownerEntityId == entityId }) return true
            val distinctEntityEnds =
                schema.connections
                    .filter { it.elementIdA == relId }
                    .map { it.elementIdB }
                    .distinct()
            return distinctEntityEnds.size != 1 || distinctEntityEnds.single() != entityId
        }
        else -> return true
    }
}

/**
 * When a loose [SchemaElement.Relationship] has exactly two legs to the same entity, promote it to
 * [SchemaElement.SelfRelationship] so the model matches Pascal `TAutoRelacao` / XML auto-rel metadata.
 */
private fun upgradeRelationshipToSelfIfBinaryAutoPattern(schema: ConceptualSchema, relId: Int): ConceptualSchema {
    val rel = schema.elements[relId] as? SchemaElement.Relationship ?: return schema
    val legs = schema.connections.filter { it.elementIdA == relId }
    if (legs.size != 2) return schema
    val distinctEnds = legs.map { it.elementIdB }.distinct()
    if (distinctEnds.size != 1) return schema
    val ownerEntityId = distinctEnds.single()
    val selfRel =
        SchemaElement.SelfRelationship(
            id = rel.id,
            name = rel.name,
            position = rel.position,
            observations = rel.observations,
            dictionary = rel.dictionary,
            labelStyle = rel.labelStyle,
            hiddenAttributes = rel.hiddenAttributes,
            ownerEntityId = ownerEntityId,
            arrowDirection = rel.arrowDirection,
        )
    return schema.copy(elements = schema.elements + (relId to selfRel))
}

/**
 * Pascal [TBaseEntidade.AutoRelacionar]: `SetBounds(Left + Width + 30, Top + Height div 6,
 * 2 * (Height - Height div 3), Height - Height div 3)`.
 */
private fun selfRelationshipPositionFromOwningEntity(entityPosition: ElementPosition): ElementPosition {
    val h = entityPosition.height
    val third = h / 3
    val diamondW = 2 * (h - third)
    val diamondH = h - third
    return ElementPosition(
        x = entityPosition.x + entityPosition.width + 30,
        y = entityPosition.y + h / 6,
        width = diamondW,
        height = diamondH,
    )
}

private fun selfRelationshipDiamondMetrics(entityPosition: ElementPosition): Pair<Int, Int> {
    val h = entityPosition.height
    val third = h / 3
    val diamondW = 2 * (h - third)
    val diamondH = h - third
    return diamondW to diamondH
}

/**
 * Places the self-relationship diamond from a schema-space [clickSchema], using the same closest-edge
 * rule as attribute tools ([closestConceptualAttributeAttachPonto]) and attribute-like insets along that edge.
 */
internal fun selfRelationshipPositionFromClickOnOwner(
    entityPosition: ElementPosition,
    clickSchema: Offset,
): ElementPosition {
    val ep = entityPosition
    val (diamondW, diamondH) = selfRelationshipDiamondMetrics(ep)
    val gap = 30
    val side = closestConceptualAttributeAttachPonto(ep, clickSchema)
    val cx = clickSchema.x.toInt()
    val cy = clickSchema.y.toInt()
    return when (side) {
        ConceptualAttributeAttachPonto.RIGHT -> ElementPosition(
            x = ep.x + ep.width + gap,
            y = (cy - diamondH / 2).coerceIn(ep.y, (ep.y + ep.height - diamondH).coerceAtLeast(ep.y)),
            width = diamondW,
            height = diamondH,
        )
        ConceptualAttributeAttachPonto.LEFT -> ElementPosition(
            x = ep.x - diamondW - gap,
            y = (cy - diamondH / 2).coerceIn(ep.y, (ep.y + ep.height - diamondH).coerceAtLeast(ep.y)),
            width = diamondW,
            height = diamondH,
        )
        ConceptualAttributeAttachPonto.TOP -> ElementPosition(
            x = (cx - diamondW / 2).coerceIn(ep.x - diamondW, ep.x + ep.width),
            y = ep.y - diamondH - gap,
            width = diamondW,
            height = diamondH,
        )
        ConceptualAttributeAttachPonto.BOTTOM -> ElementPosition(
            x = (cx - diamondW / 2).coerceIn(ep.x - diamondW, ep.x + ep.width),
            y = ep.y + ep.height + gap,
            width = diamondW,
            height = diamondH,
        )
    }
}

/**
 * Pascal: [TBaseEntidade.AutoRelacionar] — [TAutoRelacao], `Relacione(Self)` twice, name from [GeraBaseNome]('Auto').
 */
private fun buildEntityAutoSelfRelationship(
    schema: ConceptualSchema,
    ownerElement: SchemaElement,
    entityPick: ConceptualLinkPick,
    autoSelfRelationshipClickSchema: Offset?,
): ConceptualLinkValidationResult {
    require(entityPick.elementId == ownerElement.id)
    if (schema.selfRelationships.any { it.ownerEntityId == ownerElement.id }) {
        return ConceptualLinkValidationResult.Error(
            "Esta entidade já possui um auto-relacionamento.",
        )
    }
    val name = schema.nextUnusedSelfRelationshipName()
    val pos = when (autoSelfRelationshipClickSchema) {
        null -> selfRelationshipPositionFromOwningEntity(ownerElement.position)
        else -> selfRelationshipPositionFromClickOnOwner(ownerElement.position, autoSelfRelationshipClickSchema)
    }
    val style = ConceptualPlacementDefaults.labelStyle

    var work = schema
    val (w1, selfRelId) = work.allocateId()
    work = w1
    val selfRel = SchemaElement.SelfRelationship(
        id = selfRelId,
        name = name,
        position = pos,
        observations = "",
        dictionary = "",
        labelStyle = style,
        hiddenAttributes = emptyList(),
        ownerEntityId = ownerElement.id,
        arrowDirection = ArrowDirection.NONE,
    )
    work = work.withElement(selfRel)

    val (w2, connId1) = work.allocateId()
    work = w2
    work = work.withConnection(
        connectionFromRelationshipToEntity(selfRelId, connId1, entityPick, ownerElement),
    )

    val (w3, connId2) = work.allocateId()
    work = w3
    work = work.withConnection(
        connectionFromRelationshipToEntity(selfRelId, connId2, entityPick, ownerElement),
    )

    return ConceptualLinkValidationResult.Ok(work)
}

/**
 * Pascal [TModelo.Add] / [TEspecializacao.Adicione] + [TEspecializacao.CanLiga] (`mer.pas` ~2364–2390, ~8535–8597).
 * Keeps the base entity’s specialization list in sync like [applyConceptualSpecializationTool].
 */
private fun ConceptualSchema.withBaseEntitySpecializationIdsSynced(baseEntityId: Int): ConceptualSchema {
    val ent = elements[baseEntityId] as? SchemaElement.Entity ?: return this
    val ids = specializations.filter { it.baseEntityId == baseEntityId }.map { it.id }
    return copy(
        elements = elements + (baseEntityId to ent.copy(
            specializationId = ids.firstOrNull(),
            parentSpecializationIds = ids,
        )),
    )
}

/**
 * Specializations that link [entityId] as a **subtype** (Pascal [TEntidade.Origem]), excluding the
 * link from a specialization to its own [SchemaElement.Specialization.baseEntityId].
 */
private fun incomingIsaSpecializations(schema: ConceptualSchema, entityId: Int): List<SchemaElement.Specialization> =
    schema.specializations.filter { sp ->
        sp.baseEntityId != entityId && hasSpecializationEntityConnection(schema, sp.id, entityId)
    }

/**
 * Walks from [SchemaElement.Specialization.baseEntityId] following Pascal [TEntidade.Origem] chains
 * (only true subtype links — not the base-entity sync list on [SchemaElement.Entity.parentSpecializationIds]).
 */
private fun specializationLinkWouldBeCircular(
    schema: ConceptualSchema,
    spec: SchemaElement.Specialization,
    candidateChild: SchemaElement.Entity,
): Boolean {
    var currentEntityId = spec.baseEntityId
    val visited = mutableSetOf<Int>()
    while (true) {
        val parents = incomingIsaSpecializations(schema, currentEntityId)
        val parentSpec = parents.firstOrNull() ?: return false
        currentEntityId = parentSpec.baseEntityId
        if (currentEntityId == candidateChild.id) return true
        if (!visited.add(currentEntityId)) return true
    }
}

/**
 * Pascal: `Assigned(EntidadeBase) and (EntidadeBase.Especializacoes.Count > 1) and (FLigacoes.Count > 1)`.
 */
private fun blocksAdicioneBecauseMultipleSpecsOnBase(
    schema: ConceptualSchema,
    spec: SchemaElement.Specialization,
): Boolean {
    val specsOnBase = schema.specializations.count { it.baseEntityId == spec.baseEntityId }
    val linksOnSpec = schema.connectionsOf(spec.id).size
    return specsOnBase > 1 && linksOnSpec > 1
}

private fun hasSpecializationEntityConnection(schema: ConceptualSchema, specId: Int, entityId: Int): Boolean =
    schema.connections.any {
        (it.elementIdA == specId && it.elementIdB == entityId) ||
            (it.elementIdB == specId && it.elementIdA == entityId)
    }

private fun buildSpecializationToPlainEntityLink(
    schema: ConceptualSchema,
    spec: SchemaElement.Specialization,
    entitySideElement: SchemaElement,
): ConceptualLinkValidationResult {
    if (entitySideElement !is SchemaElement.Entity) {
        return ConceptualLinkValidationResult.Error(
            "Somente entidades simples podem ser ligadas a uma especialização.",
        )
    }
    if (hasSpecializationEntityConnection(schema, spec.id, entitySideElement.id)) {
        return ConceptualLinkValidationResult.Error("Já existe uma ligação entre estes objetos.")
    }
    if (blocksAdicioneBecauseMultipleSpecsOnBase(schema, spec)) {
        return ConceptualLinkValidationResult.Error("Operação não realizável.")
    }
    if (specializationLinkWouldBeCircular(schema, spec, entitySideElement)) {
        return ConceptualLinkValidationResult.Error(
            "Referência circular na construção da especialização/generalização.",
        )
    }

    var work = schema
    val (w1, newConnId) = work.allocateId()
    work = w1
    work = work.withConnection(
        Connection(
            id = newConnId,
            elementIdA = spec.id,
            elementIdB = entitySideElement.id,
            cardinality = Cardinality.ZERO_TO_MANY,
            showCardinality = false,
            orientation = LineOrientation.HORIZONTAL,
        ),
    )

    val linksOnSpec = work.connectionsOf(spec.id).size
    val specNow = work.elements[spec.id] as? SchemaElement.Specialization ?: spec
    if (linksOnSpec > 2 && specNow.type == SpecializationType.OPTIONAL) {
        work = work.withElement(specNow.copy(type = SpecializationType.RESTRICTED))
    }

    work = work.withBaseEntitySpecializationIdsSynced(spec.baseEntityId)
    return ConceptualLinkValidationResult.Ok(work)
}

sealed class ConceptualLinkValidationResult {
    /**
     * Schema after applying the link tool: new [Connection]s (and optionally a new [SchemaElement.Relationship])
     * are already merged; IDs and [ConceptualSchema.nextId] are updated.
     */
    data class Ok(
        val schema: ConceptualSchema,
    ) : ConceptualLinkValidationResult()

    data class Error(
        val message: String,
    ) : ConceptualLinkValidationResult()
}

/**
 * Validates [first] and [second] as ends of a new conceptual link and returns an updated [ConceptualSchema].
 *
 * - **Entity + relationship** (in any order): one new [Connection] with [Connection.elementIdA] on the
 *   relationship side and [Connection.elementIdB] on the entity side (Ponta), as in XML / brM.
 *   A second leg between the **same** [SchemaElement.Relationship] and **same** entity (E–R then R–E) completes
 *   a manual auto-relationship: the diamond is replaced by [SchemaElement.SelfRelationship].
 * - **Entity + entity** (including two outers of associative entities): mirrors Pascal `Tool_Ligacao` when
 *   both ends are [TBaseEntidade]: temporarily switches to `Tool_Relacionamento`, places a new [TRelacao]
 *   at the midpoint of the two bases' `Left`/`Top`, then calls `Relacione` for each entity (`mer.pas` ~2961–2967,
 *   `Tool_Relacionamento` block ~2297–2317).
 * - **Specialization + plain entity** (either order): Pascal [Tool_Ligacao] branch in [TModelo.Add] —
 *   [TEspecializacao.Adicione] (`mer.pas` ~2378–2387). Only [SchemaElement.Entity] (Pascal [TEntidade]), not
 *   associative outers.
 */
fun validateAndBuildConceptualLink(
    schema: ConceptualSchema,
    first: ConceptualLinkPick,
    second: ConceptualLinkPick,
    /**
     * When the same entity is linked to itself to create an auto-relationship, optional schema-space click
     * (same coordinates as the canvas) chooses the owner side and aligns the diamond like attribute tools.
     * `null` keeps the legacy Pascal placement (diamond to the right of the entity at a fixed vertical offset).
     */
    autoSelfRelationshipClickSchema: Offset? = null,
): ConceptualLinkValidationResult {
    val elA = schema.elements[first.elementId]
    val elB = schema.elements[second.elementId]
    if (elA == null || elB == null) {
        return ConceptualLinkValidationResult.Error("Elemento não encontrado.")
    }

    val kindA = conceptualLinkEndpointKind(elA, first)
    val kindB = conceptualLinkEndpointKind(elB, second)
    if (kindA == null || kindB == null) {
        return ConceptualLinkValidationResult.Error(
            "Só é possível ligar entidades, relacionamentos e especializações.",
        )
    }

    if (first.elementId == second.elementId) {
        if (elA is SchemaElement.AssociativeEntity &&
            first.isAssociativeOuterEntitySide != second.isAssociativeOuterEntitySide
        ) {
            return ConceptualLinkValidationResult.Error(
                "O relacionamento interno de uma entidade associativa não pode se ligar à própria entidade associativa.",
            )
        }
        // Pascal: linking the same entity twice (Tool_Ligacao) or Tool_AutoRel — TAutoRelacao + two Relacione(Self).
        if (first == second && kindA == ConceptualLinkEndpointKind.ENTITY_SIDE) {
            return when (elA) {
                is SchemaElement.Entity,
                is SchemaElement.AssociativeEntity,
                -> buildEntityAutoSelfRelationship(schema, elA, first, autoSelfRelationshipClickSchema)
                else -> ConceptualLinkValidationResult.Error("Não é possível ligar um objeto a si mesmo.")
            }
        }
        return ConceptualLinkValidationResult.Error("Não é possível ligar um objeto a si mesmo.")
    }

    if (kindA == ConceptualLinkEndpointKind.SPECIALIZATION_SIDE &&
        kindB == ConceptualLinkEndpointKind.ENTITY_SIDE
    ) {
        return buildSpecializationToPlainEntityLink(
            schema,
            elA as SchemaElement.Specialization,
            elB,
        )
    }
    if (kindB == ConceptualLinkEndpointKind.SPECIALIZATION_SIDE &&
        kindA == ConceptualLinkEndpointKind.ENTITY_SIDE
    ) {
        return buildSpecializationToPlainEntityLink(
            schema,
            elB as SchemaElement.Specialization,
            elA,
        )
    }

    if (kindA == ConceptualLinkEndpointKind.ENTITY_SIDE &&
        kindB == ConceptualLinkEndpointKind.ENTITY_SIDE
    ) {
        return buildEntityEntityLinkThroughRelationship(schema, first, second, elA, elB)
    }

    if (kindA == kindB) {
        return ConceptualLinkValidationResult.Error(
            "Seleções incorretas para esta ligação.",
        )
    }

    val relPick: ConceptualLinkPick
    val entPick: ConceptualLinkPick
    if (kindA == ConceptualLinkEndpointKind.RELATIONSHIP_SIDE) {
        relPick = first
        entPick = second
    } else {
        relPick = second
        entPick = first
    }

    if (isDuplicateConceptualRelEntityConnection(schema, relPick.elementId, entPick.elementId)) {
        return ConceptualLinkValidationResult.Error("Já existe uma ligação entre estes objetos.")
    }

    val entElem = schema.elements[entPick.elementId]!!

    val useOuterB = entElem is SchemaElement.AssociativeEntity && entPick.isAssociativeOuterEntitySide

    var work = schema
    val (w1, newConnId) = work.allocateId()
    work = w1
    val conn = Connection(
        id = newConnId,
        elementIdA = relPick.elementId,
        elementIdB = entPick.elementId,
        cardinality = Cardinality.ZERO_TO_MANY,
        showCardinality = true,
        orientation = LineOrientation.HORIZONTAL,
        useAssociativeOuterForEndA = false,
        useAssociativeOuterForEndB = useOuterB,
    )
    work = work.withConnection(conn)
    work = upgradeRelationshipToSelfIfBinaryAutoPattern(work, relPick.elementId)
    return ConceptualLinkValidationResult.Ok(work)
}

/**
 * Pascal: `Clicado := Point(UsrSelA.Left - ((UsrSelA.Left - UsrSelb.Left) div 2), ...)`.
 */
private fun relationshipTopLeftBetweenEntities(posA: ElementPosition, posB: ElementPosition): ElementPosition {
    val x = posA.x - (posA.x - posB.x) / 2
    val y = posA.y - (posA.y - posB.y) / 2
    return ElementPosition(
        x = x,
        y = y,
        width = ConceptualPlacementDefaults.relationshipWidth,
        height = ConceptualPlacementDefaults.relationshipHeight,
    )
}

private fun connectionFromRelationshipToEntity(
    relId: Int,
    newConnectionId: Int,
    entityPick: ConceptualLinkPick,
    entityElement: SchemaElement,
): Connection {
    val useOuterB =
        entityElement is SchemaElement.AssociativeEntity && entityPick.isAssociativeOuterEntitySide
    return Connection(
        id = newConnectionId,
        elementIdA = relId,
        elementIdB = entityPick.elementId,
        cardinality = Cardinality.ZERO_TO_MANY,
        showCardinality = true,
        orientation = LineOrientation.HORIZONTAL,
        useAssociativeOuterForEndA = false,
        useAssociativeOuterForEndB = useOuterB,
    )
}

private fun buildEntityEntityLinkThroughRelationship(
    schema: ConceptualSchema,
    first: ConceptualLinkPick,
    second: ConceptualLinkPick,
    elA: SchemaElement,
    elB: SchemaElement,
): ConceptualLinkValidationResult {
    val name = schema.nextUnusedRelationshipName()
    val pos = relationshipTopLeftBetweenEntities(elA.position, elB.position)
    val style = ConceptualPlacementDefaults.labelStyle

    var work = schema
    val (w1, relId) = work.allocateId()
    work = w1
    val rel = SchemaElement.Relationship(
        id = relId,
        name = name,
        position = pos,
        observations = "",
        dictionary = "",
        labelStyle = style,
        hiddenAttributes = emptyList(),
        arrowDirection = ArrowDirection.NONE,
        showName = true,
    )
    work = work.withElement(rel)

    val (w2, connId1) = work.allocateId()
    work = w2
    work = work.withConnection(
        connectionFromRelationshipToEntity(relId, connId1, first, elA),
    )

    val (w3, connId2) = work.allocateId()
    work = w3
    work = work.withConnection(
        connectionFromRelationshipToEntity(relId, connId2, second, elB),
    )

    return ConceptualLinkValidationResult.Ok(work)
}
