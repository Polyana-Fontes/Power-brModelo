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
 * Ribbon **Operações → Converter Esp. para Restrita** / **Converter Esp. para Opcional**.
 *
 * Mirrors Pascal [TEspecializacao.ConverteEspToRestrita], [TEntidade.ConverteEspToRestrita],
 * and [TEspecializacao.OpcionalizeEsp] in `mer.pas` (~8745–8784, ~9414–9446, ~8535–8567).
 */

private fun ConceptualSchema.withBaseEntitySpecializationIdsSyncedForConvert(baseEntityId: Int): ConceptualSchema {
    val ent = elements[baseEntityId] as? SchemaElement.Entity ?: return this
    val ids = specializations.filter { it.baseEntityId == baseEntityId }.map { it.id }
    return copy(
        elements = elements + (baseEntityId to ent.copy(
            specializationId = ids.firstOrNull(),
            parentSpecializationIds = ids,
        )),
    )
}

/** Incident connections involving [specId], in [ConceptualSchema.connections] list order (Pascal [FLigacoes] order). */
private fun specConnectionsInSchemaOrder(schema: ConceptualSchema, specId: Int): List<Connection> =
    schema.connections.filter { it.elementIdA == specId || it.elementIdB == specId }

private fun connectionOtherEnd(conn: Connection, specId: Int): Int =
    if (conn.elementIdA == specId) conn.elementIdB else conn.elementIdA

private fun singleSelectedSpecializationId(schema: ConceptualSchema, selection: CanvasSelection): Int? {
    val (elementIds, cardinalityIds) = selection.toMultiPickSets()
    if (cardinalityIds.isNotEmpty()) return null
    if (elementIds.size != 1) return null
    val id = elementIds.single()
    return if (schema.elements[id] is SchemaElement.Specialization) id else null
}

/** Pascal [TEntidade.HaRestrita]: any specialization on the base is restricted. */
private fun entityHasRestrictedSpecializationOnBase(schema: ConceptualSchema, baseEntityId: Int): Boolean =
    schema.specializations.any { it.baseEntityId == baseEntityId && it.type == SpecializationType.RESTRICTED }

/**
 * Pascal [TEntidade.ConverteEspToRestrita]: for every specialization on the base other than [pedinteId],
 * collect linked ends that are not the base entity (subtype entities).
 */
private fun subtypePlainEntityIdsFromOtherSpecializations(
    schema: ConceptualSchema,
    baseEntityId: Int,
    pedinteId: Int,
): List<Int> {
    val out = mutableListOf<Int>()
    for (esp in schema.specializations) {
        if (esp.baseEntityId != baseEntityId || esp.id == pedinteId) continue
        for (conn in schema.connectionsOf(esp.id)) {
            val other = connectionOtherEnd(conn, esp.id)
            if (other == baseEntityId) continue
            if (schema.elements[other] is SchemaElement.Entity) {
                out.add(other)
            }
        }
    }
    return out
}

/** True when **Converter Esp. para Restrita** applies (one optional triangle selected, base has no restricted spec, and there is something to merge). */
fun canConvertOptionalSpecializationsToRestrictedMenu(schema: ConceptualSchema, selection: CanvasSelection): Boolean {
    val sid = singleSelectedSpecializationId(schema, selection) ?: return false
    val ped = schema.elements[sid] as? SchemaElement.Specialization ?: return false
    if (ped.type != SpecializationType.OPTIONAL) return false
    if (entityHasRestrictedSpecializationOnBase(schema, ped.baseEntityId)) return false
    return subtypePlainEntityIdsFromOtherSpecializations(schema, ped.baseEntityId, ped.id).isNotEmpty()
}

/**
 * Merges sibling optional specializations into the selected triangle (Pascal [ConverteEspToRestrita]),
 * then links collected subtype entities via [validateAndBuildConceptualLink] (Pascal [Adicione]).
 */
fun applyConvertOptionalSpecializationsToRestricted(
    schema: ConceptualSchema,
    selection: CanvasSelection,
): ConceptualSchema? {
    val sid = singleSelectedSpecializationId(schema, selection) ?: return null
    val ped = schema.elements[sid] as? SchemaElement.Specialization ?: return null
    if (ped.type != SpecializationType.OPTIONAL) return null
    if (entityHasRestrictedSpecializationOnBase(schema, ped.baseEntityId)) return null
    val baseId = ped.baseEntityId
    val toCollect = subtypePlainEntityIdsFromOtherSpecializations(schema, baseId, ped.id).distinct()
    if (toCollect.isEmpty()) return null
    val otherSpecIds = schema.specializations
        .filter { it.baseEntityId == baseId && it.id != ped.id }
        .map { it.id }
        .toSet()
    var work = schema.withoutElements(otherSpecIds)
    val specNow = work.elements[ped.id] as? SchemaElement.Specialization ?: return null
    for (eid in toCollect) {
        val ent = work.elements[eid] as? SchemaElement.Entity ?: return null
        when (
            val res = validateAndBuildConceptualLink(
                work,
                ConceptualLinkPick(specNow.id),
                ConceptualLinkPick(ent.id),
            )
        ) {
            is ConceptualLinkValidationResult.Ok -> work = res.schema
            is ConceptualLinkValidationResult.Error -> return null
        }
    }
    return work.withBaseEntitySpecializationIdsSyncedForConvert(baseId)
}

/** True when **Converter Esp. para Opcional** applies (restricted triangle with at least three links). */
fun canConvertRestrictedSpecializationToOptionalsMenu(schema: ConceptualSchema, selection: CanvasSelection): Boolean {
    val sid = singleSelectedSpecializationId(schema, selection) ?: return false
    val spec = schema.elements[sid] as? SchemaElement.Specialization ?: return false
    if (spec.type != SpecializationType.RESTRICTED) return false
    return specConnectionsInSchemaOrder(schema, sid).size >= 3
}

private fun connectionSpecializationToBase(connectionId: Int, specializationId: Int, baseEntityId: Int): Connection =
    Connection(
        id = connectionId,
        elementIdA = specializationId,
        elementIdB = baseEntityId,
        cardinality = Cardinality.ZERO_TO_MANY,
        showCardinality = false,
        orientation = LineOrientation.VERTICAL,
    )

/**
 * Keeps the first two incident links on the selected specialization (Pascal indices 0 and 1), sets it optional,
 * and for each removed subtype leg creates a new optional specialization ([TEntidade.CriarEsp] + [Adicione]).
 */
fun applyConvertRestrictedSpecializationToOptionals(
    schema: ConceptualSchema,
    selection: CanvasSelection,
): ConceptualSchema? {
    val sid = singleSelectedSpecializationId(schema, selection) ?: return null
    val selfBefore = schema.elements[sid] as? SchemaElement.Specialization ?: return null
    if (selfBefore.type != SpecializationType.RESTRICTED) return null
    val incident = specConnectionsInSchemaOrder(schema, sid)
    if (incident.size < 3) return null
    val baseId = selfBefore.baseEntityId
    val toStrip = incident.drop(2)
    val splitEntityIds = mutableListOf<Int>()
    for (conn in toStrip) {
        val other = connectionOtherEnd(conn, sid)
        if (other == baseId) return null
        if (schema.elements[other] !is SchemaElement.Entity) return null
        splitEntityIds.add(other)
    }
    var work = toStrip.fold(schema) { s, c -> s.withoutConnection(c.id) }
    val selfMid = work.elements[sid] as? SchemaElement.Specialization ?: return null
    work = work.withElement(selfMid.copy(type = SpecializationType.OPTIONAL))
    val geom = selfMid.position

    for (eid in splitEntityIds.distinct()) {
        val ent = work.elements[eid] as? SchemaElement.Entity ?: return null
        val name = work.nextUnusedSpecializationName()
        val left = ent.position.x + ent.position.width / 2 - geom.width / 2
        val top = geom.y
        val pos = ElementPosition(x = left, y = top, width = geom.width, height = geom.height)

        val (w1, newSpecId) = work.allocateId()
        work = w1
        val newSpec = SchemaElement.Specialization(
            id = newSpecId,
            name = name,
            position = pos,
            observations = "",
            dictionary = "",
            labelStyle = selfMid.labelStyle,
            hiddenAttributes = emptyList(),
            baseEntityId = baseId,
            type = SpecializationType.OPTIONAL,
            isPartial = false,
        )
        work = work.withElement(newSpec)

        val (w2, connBaseId) = work.allocateId()
        work = w2
        work = work.withConnection(connectionSpecializationToBase(connBaseId, newSpecId, baseId))

        when (
            val res = validateAndBuildConceptualLink(
                work,
                ConceptualLinkPick(newSpecId),
                ConceptualLinkPick(eid),
            )
        ) {
            is ConceptualLinkValidationResult.Ok -> work = res.schema
            is ConceptualLinkValidationResult.Error -> return null
        }
    }
    return work.withBaseEntitySpecializationIdsSyncedForConvert(baseId)
}
