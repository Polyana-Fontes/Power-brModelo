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
 * Variants of the conceptual "Especialização" ribbon tool.
 *
 * Maps to Pascal `Tool_Especializacao`, `Tool_EspecializacaoA`, `Tool_EspecializacaoB` in `mer.pas`.
 */
enum class ConceptualSpecializationToolVariant {
    /** Pascal [Tool_Especializacao] — [TEntidade.CriarEsp] (triangle only, optional). */
    Basic,

    /** Pascal [Tool_EspecializacaoA] — [TEntidade.Especialise](EspRestrita). */
    ExclusiveWithEntityCreation,

    /** Pascal [Tool_EspecializacaoB] — [TEntidade.Especialise](EspOpicional). */
    NonExclusiveWithEntityCreation,
}

sealed class ConceptualSpecializationToolResult {
    data class Ok(
        val schema: ConceptualSchema,
        val newSpecializationId: Int,
    ) : ConceptualSpecializationToolResult()

    data class Error(
        val message: String,
    ) : ConceptualSpecializationToolResult()
}

/** Pascal `distancia = 105` in [TEntidade.CriarEsp] / [TEntidade.Especialise]. */
private const val SPECIALIZATION_DISTANCE_PX = 105

/** Default [TEspecializacao.Create] size before `Width := Width div 2` in [TEntidade.CriarEsp]. */
private const val SPEC_FULL_WIDTH_PX = 51

private const val SPEC_HEIGHT_PX = 31

/**
 * Triangle position after [TEntidade.CriarEsp]: halved width, below the base entity.
 * Matches `valores-padroes.xml` sample (25×31).
 */
internal fun specializationTriangleBelowBase(base: ElementPosition): ElementPosition {
    val specW = SPEC_FULL_WIDTH_PX / 2
    val specH = SPEC_HEIGHT_PX
    val cx = base.x + base.width / 2
    val top = base.y + base.height + (SPECIALIZATION_DISTANCE_PX - specH - 3) / 2
    val left = cx - (specW - 3) / 2
    return ElementPosition(left, top, specW, specH)
}

private fun specializationWidenedBelowBase(base: ElementPosition): ElementPosition {
    val specW = SPEC_FULL_WIDTH_PX
    val specH = SPEC_HEIGHT_PX
    val cx = base.x + base.width / 2
    val top = base.y + base.height + (SPECIALIZATION_DISTANCE_PX - specH - 3) / 2
    val left = cx - (specW - 3) / 2
    return ElementPosition(left, top, specW, specH)
}

private fun ConceptualSchema.specializationsForBase(entityId: Int): List<SchemaElement.Specialization> =
    specializations.filter { it.baseEntityId == entityId }

private fun ConceptualSchema.entityHasRestrictedSpecialization(entityId: Int): Boolean =
    specializationsForBase(entityId).any { it.type == SpecializationType.RESTRICTED }

private fun ConceptualSchema.entityHasAnySpecialization(entityId: Int): Boolean =
    specializationsForBase(entityId).isNotEmpty()

private fun connectionSpecializationToEntity(
    connectionId: Int,
    specializationId: Int,
    entityId: Int,
): Connection =
    Connection(
        id = connectionId,
        elementIdA = specializationId,
        elementIdB = entityId,
        cardinality = Cardinality.ZERO_TO_MANY,
        showCardinality = false,
        orientation = LineOrientation.VERTICAL,
    )

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

private fun ConceptualSchema.uniqueEntityNamePreferred(preferred: String): String {
    var candidate = preferred
    var n = 2
    while (entities.any { it.name == candidate }) {
        candidate = "$preferred$n"
        n++
    }
    return candidate
}

/**
 * Applies the specialization tool to a **plain** [SchemaElement.Entity] (Pascal [TEntidade] only).
 *
 * See [TEntidade.CriarEsp] and [TEntidade.Especialise] in `mer.pas` (~9455–9536, ~2280–2292).
 */
fun applyConceptualSpecializationTool(
    schema: ConceptualSchema,
    baseEntityId: Int,
    variant: ConceptualSpecializationToolVariant,
): ConceptualSpecializationToolResult {
    val baseEl = schema.elements[baseEntityId]
    if (baseEl !is SchemaElement.Entity) {
        return ConceptualSpecializationToolResult.Error(
            "Clique na entidade que será especializada.",
        )
    }

    when (variant) {
        ConceptualSpecializationToolVariant.Basic -> {
            if (schema.entityHasRestrictedSpecialization(baseEntityId)) {
                return ConceptualSpecializationToolResult.Error(
                    "Entidade já especializada de forma obrigatória. " +
                        "Não foi possível criar uma nova especialização para a entidade selecionada.",
                )
            }
            return placeBasicSpecialization(schema, baseEl)
        }
        ConceptualSpecializationToolVariant.ExclusiveWithEntityCreation -> {
            if (schema.entityHasAnySpecialization(baseEntityId)) {
                return ConceptualSpecializationToolResult.Error(
                    "Entidade já especializada. " +
                        "Não foi possível criar uma nova especialização para a entidade selecionada.",
                )
            }
            return placeExclusiveSpecializationWithChildren(schema, baseEl)
        }
        ConceptualSpecializationToolVariant.NonExclusiveWithEntityCreation -> {
            if (schema.entityHasRestrictedSpecialization(baseEntityId)) {
                return ConceptualSpecializationToolResult.Error(
                    "Entidade já especializada de forma obrigatória. " +
                        "Não foi possível criar uma nova especialização para a entidade selecionada.",
                )
            }
            return placeNonExclusiveSpecializationWithChild(schema, baseEl)
        }
    }
}

private fun placeBasicSpecialization(
    schema: ConceptualSchema,
    base: SchemaElement.Entity,
): ConceptualSpecializationToolResult {
    val name = schema.nextUnusedSpecializationName()
    val pos = specializationTriangleBelowBase(base.position)
    val style = ConceptualPlacementDefaults.specializationLabelStyle

    var work = schema
    val (w1, specId) = work.allocateId()
    work = w1
    val spec = SchemaElement.Specialization(
        id = specId,
        name = name,
        position = pos,
        observations = "",
        dictionary = "",
        labelStyle = style,
        hiddenAttributes = emptyList(),
        baseEntityId = base.id,
        type = SpecializationType.OPTIONAL,
        isPartial = false,
    )
    work = work.withElement(spec)

    val (w2, connId) = work.allocateId()
    work = w2
    work = work.withConnection(
        connectionSpecializationToEntity(connId, specId, base.id),
    )
    work = work.withBaseEntitySpecializationIdsSynced(base.id)
    return ConceptualSpecializationToolResult.Ok(work, specId)
}

private fun placeExclusiveSpecializationWithChildren(
    schema: ConceptualSchema,
    base: SchemaElement.Entity,
): ConceptualSpecializationToolResult {
    val basePos = base.position
    val name = schema.nextUnusedSpecializationName()
    val style = ConceptualPlacementDefaults.specializationLabelStyle
    val widePos = specializationWidenedBelowBase(basePos)

    val childTop = basePos.y + basePos.height + SPECIALIZATION_DISTANCE_PX
    val halfW = basePos.width / 2
    val halfDist = SPECIALIZATION_DISTANCE_PX / 2
    val eLeft = (basePos.x - halfW - halfDist).coerceAtLeast(0)
    val fLeft = basePos.x + halfW + halfDist

    val childNameA = schema.uniqueEntityNamePreferred("${base.name}_A")
    val childNameB = schema.uniqueEntityNamePreferred("${base.name}_B")

    var work = schema
    val (w1, specId) = work.allocateId()
    work = w1
    val (w2, connBaseId) = work.allocateId()
    work = w2
    val (w3, entAId) = work.allocateId()
    work = w3
    val (w4, entBId) = work.allocateId()
    work = w4
    val (w5, connAId) = work.allocateId()
    work = w5
    val (w6, connBId) = work.allocateId()
    work = w6

    val spec = SchemaElement.Specialization(
        id = specId,
        name = name,
        position = widePos,
        observations = "",
        dictionary = "",
        labelStyle = style,
        hiddenAttributes = emptyList(),
        baseEntityId = base.id,
        type = SpecializationType.RESTRICTED,
        isPartial = false,
    )

    val entStyle = ConceptualPlacementDefaults.labelStyle
    val entA = SchemaElement.Entity(
        id = entAId,
        name = childNameA,
        position = ElementPosition(
            x = eLeft,
            y = childTop,
            width = ConceptualPlacementDefaults.entityWidth,
            height = ConceptualPlacementDefaults.entityHeight,
        ),
        observations = "",
        dictionary = "",
        labelStyle = entStyle,
        hiddenAttributes = emptyList(),
        parentSpecializationIds = listOf(specId),
    )
    val entB = SchemaElement.Entity(
        id = entBId,
        name = childNameB,
        position = ElementPosition(
            x = fLeft,
            y = childTop,
            width = ConceptualPlacementDefaults.entityWidth,
            height = ConceptualPlacementDefaults.entityHeight,
        ),
        observations = "",
        dictionary = "",
        labelStyle = entStyle,
        hiddenAttributes = emptyList(),
        parentSpecializationIds = listOf(specId),
    )

    work = work
        .withElement(spec)
        .withElement(entA)
        .withElement(entB)
        .withConnection(connectionSpecializationToEntity(connBaseId, specId, base.id))
        .withConnection(connectionSpecializationToEntity(connAId, specId, entAId))
        .withConnection(connectionSpecializationToEntity(connBId, specId, entBId))

    work = work.withBaseEntitySpecializationIdsSynced(base.id)
    return ConceptualSpecializationToolResult.Ok(work, specId)
}

private fun placeNonExclusiveSpecializationWithChild(
    schema: ConceptualSchema,
    base: SchemaElement.Entity,
): ConceptualSpecializationToolResult {
    val basePos = base.position
    val name = schema.nextUnusedSpecializationName()
    val pos = specializationTriangleBelowBase(basePos)
    val style = ConceptualPlacementDefaults.specializationLabelStyle

    val childTop = basePos.y + basePos.height + SPECIALIZATION_DISTANCE_PX
    val childLeft = basePos.x
    val childName = schema.uniqueEntityNamePreferred("${base.name}_A")

    var work = schema
    val (w1, specId) = work.allocateId()
    work = w1
    val (w2, connBaseId) = work.allocateId()
    work = w2
    val (w3, entAId) = work.allocateId()
    work = w3
    val (w4, connAId) = work.allocateId()
    work = w4

    val spec = SchemaElement.Specialization(
        id = specId,
        name = name,
        position = pos,
        observations = "",
        dictionary = "",
        labelStyle = style,
        hiddenAttributes = emptyList(),
        baseEntityId = base.id,
        type = SpecializationType.OPTIONAL,
        isPartial = false,
    )
    val entStyle = ConceptualPlacementDefaults.labelStyle
    val child = SchemaElement.Entity(
        id = entAId,
        name = childName,
        position = ElementPosition(
            x = childLeft,
            y = childTop,
            width = ConceptualPlacementDefaults.entityWidth,
            height = ConceptualPlacementDefaults.entityHeight,
        ),
        observations = "",
        dictionary = "",
        labelStyle = entStyle,
        hiddenAttributes = emptyList(),
        parentSpecializationIds = listOf(specId),
    )

    work = work
        .withElement(spec)
        .withElement(child)
        .withConnection(connectionSpecializationToEntity(connBaseId, specId, base.id))
        .withConnection(connectionSpecializationToEntity(connAId, specId, entAId))

    work = work.withBaseEntitySpecializationIdsSynced(base.id)
    return ConceptualSpecializationToolResult.Ok(work, specId)
}
