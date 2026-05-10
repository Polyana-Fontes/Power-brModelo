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
    data class Ok(
        val connection: Connection,
    ) : ConceptualLinkValidationResult()

    data class Error(
        val message: String,
    ) : ConceptualLinkValidationResult()
}

/**
 * Validates [first] and [second] as ends of a new conceptual link and builds a [Connection]
 * with [elementIdA] on the relationship side and [elementIdB] on the entity side (Ponta),
 * matching [Connection] documentation and the XML / brM conventions.
 */
fun validateAndBuildConceptualLink(
    schema: ConceptualSchema,
    first: ConceptualLinkPick,
    second: ConceptualLinkPick,
    newConnectionId: Int,
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

    if (kindA == kindB) {
        return ConceptualLinkValidationResult.Error(
            "Uma ligação conceitual deve ser entre uma entidade e um relacionamento.",
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

    // Relationship-side picks always target the inner diamond of an associative entity; entity-side may be outer.
    val useOuterB = entElem is SchemaElement.AssociativeEntity && entPick.isAssociativeOuterEntitySide

    val conn = Connection(
        id = newConnectionId,
        elementIdA = relPick.elementId,
        elementIdB = entPick.elementId,
        cardinality = Cardinality.ZERO_TO_MANY,
        showCardinality = true,
        orientation = LineOrientation.HORIZONTAL,
        useAssociativeOuterForEndA = false,
        useAssociativeOuterForEndB = useOuterB,
    )
    return ConceptualLinkValidationResult.Ok(conn)
}
