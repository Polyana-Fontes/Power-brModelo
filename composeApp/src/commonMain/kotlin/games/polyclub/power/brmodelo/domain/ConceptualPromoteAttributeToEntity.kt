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
 * The attribute id to promote when [selection] matches Pascal **single** [TAtributo] selection
 * with a valid owner ([TBaseEntidade], [TAutoRelacao], or [TMaxRelacao] — not another attribute).
 */
fun attributeIdSelectedForPromoteToEntity(schema: ConceptualSchema, selection: CanvasSelection): Int? {
    val (elementIds, cardinalityIds) = selection.toMultiPickSets()
    if (cardinalityIds.isNotEmpty()) return null
    if (elementIds.size != 1) return null
    val id = elementIds.single()
    val attr = schema.elements[id] as? SchemaElement.Attribute ?: return null
    val owner = schema.elements[attr.ownerId] ?: return null
    return when (owner) {
        is SchemaElement.Entity,
        is SchemaElement.SelfRelationship,
        is SchemaElement.Relationship,
        is SchemaElement.AssociativeEntity,
        -> id
        else -> null
    }
}

/** True when **Operações → Promover à Entidade** applies (Pascal [promo_entidade]). */
fun canPromoteAttributeToEntityMenu(schema: ConceptualSchema, selection: CanvasSelection): Boolean =
    attributeIdSelectedForPromoteToEntity(schema, selection) != null

/**
 * Pascal [TModelo.PromoverAEntidade]: turns the selected attribute into a new [SchemaElement.Entity],
 * linked via a new or existing relationship hub. Composite children become attributes of the new entity.
 */
fun applyPromoteAttributeToEntity(schema: ConceptualSchema, selection: CanvasSelection): ConceptualSchema? {
    val attrId = attributeIdSelectedForPromoteToEntity(schema, selection) ?: return null
    val attr = schema.elements[attrId] as SchemaElement.Attribute
    val owner = schema.elements[attr.ownerId] ?: return null

    var work = schema
    val relId: Int
    val createdNewRel: Boolean
    var baseEntityId: Int? = null

    val ax = attr.position.x
    val ay = attr.position.y

    when (owner) {
        is SchemaElement.Entity -> {
            createdNewRel = true
            baseEntityId = owner.id
            val (w1, rId) = work.allocateId()
            work = w1
            relId = rId
            val relPos = ElementPosition(
                ax,
                ay,
                ConceptualPlacementDefaults.relationshipWidth,
                ConceptualPlacementDefaults.relationshipHeight,
            )
            work = work.withElement(
                SchemaElement.Relationship(
                    id = relId,
                    name = work.nextUnusedRelationshipName(),
                    position = relPos,
                    observations = "",
                    dictionary = "",
                    labelStyle = ConceptualPlacementDefaults.labelStyle,
                    hiddenAttributes = emptyList(),
                    arrowDirection = ArrowDirection.NONE,
                    showName = true,
                ),
            )
        }
        is SchemaElement.SelfRelationship -> {
            createdNewRel = true
            baseEntityId = owner.ownerEntityId
            val (w1, rId) = work.allocateId()
            work = w1
            relId = rId
            val relPos = ElementPosition(
                ax,
                ay,
                ConceptualPlacementDefaults.relationshipWidth,
                ConceptualPlacementDefaults.relationshipHeight,
            )
            work = work.withElement(
                SchemaElement.Relationship(
                    id = relId,
                    name = work.nextUnusedRelationshipName(),
                    position = relPos,
                    observations = "",
                    dictionary = "",
                    labelStyle = ConceptualPlacementDefaults.labelStyle,
                    hiddenAttributes = emptyList(),
                    arrowDirection = ArrowDirection.NONE,
                    showName = true,
                ),
            )
        }
        is SchemaElement.Relationship -> {
            createdNewRel = false
            relId = owner.id
        }
        is SchemaElement.AssociativeEntity -> {
            createdNewRel = false
            relId = owner.id
        }
        else -> return null
    }

    val relWidth = when (val hub = work.elements[relId]) {
        is SchemaElement.Relationship -> hub.position.width
        is SchemaElement.AssociativeEntity -> hub.position.width
        else -> ConceptualPlacementDefaults.relationshipWidth
    }

    val entityLeft = if (createdNewRel) ax + relWidth + 50 else ax
    val entityTop = ay

    val (wEnt, newEntityId) = work.allocateId()
    work = wEnt
    work = work.withElement(
        SchemaElement.Entity(
            id = newEntityId,
            name = attr.name,
            position = ElementPosition(
                entityLeft,
                entityTop,
                ConceptualPlacementDefaults.entityWidth,
                ConceptualPlacementDefaults.entityHeight,
            ),
            observations = attr.observations,
            dictionary = attr.dictionary,
            labelStyle = attr.labelStyle,
            hiddenAttributes = attr.hiddenAttributes,
        ),
    )

    if (isDuplicateConceptualRelEntityConnection(work, relId, newEntityId)) return null
    val (wC0, connNew) = work.allocateId()
    work = wC0
    work = work.withConnection(relationshipToEntityConnection(connNew, relId, newEntityId))

    if (baseEntityId != null) {
        if (isDuplicateConceptualRelEntityConnection(work, relId, baseEntityId)) return null
        val (wC1, connBase) = work.allocateId()
        work = wC1
        work = work.withConnection(relationshipToEntityConnection(connBase, relId, baseEntityId))
    }

    val childIds = work.attributes.filter { it.ownerId == attrId }.map { it.id }
    for (cid in childIds) {
        val ch = work.elements[cid] as SchemaElement.Attribute
        work = work.withElement(ch.copy(ownerId = newEntityId))
    }

    work = work.withoutElement(attrId)

    for (cid in childIds) {
        val (wCh, cAttr) = work.allocateId()
        work = wCh
        work = work.withConnection(
            Connection(
                id = cAttr,
                elementIdA = cid,
                elementIdB = newEntityId,
                cardinality = null,
                showCardinality = false,
                orientation = LineOrientation.VERTICAL,
            ),
        )
    }

    return work
}

private fun relationshipToEntityConnection(connectionId: Int, relId: Int, entityId: Int): Connection =
    Connection(
        id = connectionId,
        elementIdA = relId,
        elementIdB = entityId,
        cardinality = Cardinality.ZERO_TO_MANY,
        showCardinality = true,
        orientation = LineOrientation.VERTICAL,
        useAssociativeOuterForEndA = false,
        useAssociativeOuterForEndB = false,
    )
