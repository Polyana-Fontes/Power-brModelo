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
}

/**
 * Returns the [ConceptualLinkEndpointKind] for [pick], or `null` if the element cannot participate
 * in conceptual entity–relationship links.
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
        else -> null
    }
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
 * - **Entity + entity** (including two outers of associative entities): mirrors Pascal `Tool_Ligacao` when
 *   both ends are [TBaseEntidade]: temporarily switches to `Tool_Relacionamento`, places a new [TRelacao]
 *   at the midpoint of the two bases' `Left`/`Top`, then calls `Relacione` for each entity (`mer.pas` ~2961–2967,
 *   `Tool_Relacionamento` block ~2297–2317).
 */
fun validateAndBuildConceptualLink(
    schema: ConceptualSchema,
    first: ConceptualLinkPick,
    second: ConceptualLinkPick,
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
            "Só é possível ligar entidades e relacionamentos.",
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
        return ConceptualLinkValidationResult.Error("Não é possível ligar um objeto a si mesmo.")
    }

    if (kindA == ConceptualLinkEndpointKind.ENTITY_SIDE &&
        kindB == ConceptualLinkEndpointKind.ENTITY_SIDE
    ) {
        return buildEntityEntityLinkThroughRelationship(schema, first, second, elA, elB)
    }

    if (kindA == kindB) {
        return ConceptualLinkValidationResult.Error(
            "Uma ligação conceitual deve ser entre uma entidade e um relacionamento.",
        )
    }

    val duplicate = schema.connections.any { conn ->
        (conn.elementIdA == first.elementId && conn.elementIdB == second.elementId) ||
            (conn.elementIdA == second.elementId && conn.elementIdB == first.elementId)
    }
    if (duplicate) {
        return ConceptualLinkValidationResult.Error("Já existe uma ligação entre estes objetos.")
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
    return ConceptualLinkValidationResult.Ok(work.withConnection(conn))
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
