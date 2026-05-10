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

private val ENTIDADE_PATTERN = Regex("^Entidade(\\d+)$")
private val RELACAO_PATTERN = Regex("^Relacao(\\d+)$")
private val ENTASSOC_PATTERN = Regex("^EntAssoc(\\d+)$")
private val AUTO_SELF_REL_PATTERN = Regex("^Auto(\\d+)$")
private val ESP_NAME_PATTERN = Regex("^Esp(\\d+)$")

private fun nextFreeIndex(used: Set<Int>): Int {
    var n = 1
    while (n in used) n++
    return n
}

private fun usedEntidadeIndices(schema: ConceptualSchema): Set<Int> =
    schema.entities.mapNotNull { e ->
        ENTIDADE_PATTERN.matchEntire(e.name)?.groupValues?.get(1)?.toIntOrNull()
    }.toSet()

private fun usedRelacaoIndices(schema: ConceptualSchema): Set<Int> {
    val names = buildSet {
        schema.relationships.forEach { add(it.name) }
        schema.associativeEntities.forEach { add(it.relationshipName) }
    }
    return names.mapNotNull { n ->
        RELACAO_PATTERN.matchEntire(n)?.groupValues?.get(1)?.toIntOrNull()
    }.toSet()
}

private fun usedEntAssocIndices(schema: ConceptualSchema): Set<Int> =
    schema.associativeEntities.mapNotNull { a ->
        ENTASSOC_PATTERN.matchEntire(a.name)?.groupValues?.get(1)?.toIntOrNull()
    }.toSet()

private fun usedAutoSelfRelIndices(schema: ConceptualSchema): Set<Int> =
    schema.selfRelationships.mapNotNull { sr ->
        AUTO_SELF_REL_PATTERN.matchEntire(sr.name)?.groupValues?.get(1)?.toIntOrNull()
    }.toSet()

private fun usedEspNameIndices(schema: ConceptualSchema): Set<Int> =
    schema.specializations.mapNotNull { s ->
        ESP_NAME_PATTERN.matchEntire(s.name)?.groupValues?.get(1)?.toIntOrNull()
    }.toSet()

private fun nextEntidadeName(schema: ConceptualSchema): String =
    "Entidade${nextFreeIndex(usedEntidadeIndices(schema))}"

private fun nextRelacaoName(schema: ConceptualSchema): String {
    val patternUsed = usedRelacaoIndices(schema)
    var n = nextFreeIndex(patternUsed)
    while (true) {
        val candidate = "Relacao$n"
        val inUse = schema.relationships.any { it.name == candidate } ||
            schema.associativeEntities.any { it.relationshipName == candidate }
        if (!inUse) return candidate
        n++
    }
}

/**
 * Next unused conceptual relationship name (`RelacaoN`), for tools that mirror Pascal [GeraBaseNome]('Relacao').
 */
internal fun ConceptualSchema.nextUnusedRelationshipName(): String = nextRelacaoName(this)

/**
 * Next unused self-relationship name (`AutoN`), matching Pascal [GeraBaseNome]('Auto') for [TAutoRelacao].
 */
internal fun ConceptualSchema.nextUnusedSelfRelationshipName(): String {
    val patternUsed = usedAutoSelfRelIndices(this)
    var n = nextFreeIndex(patternUsed)
    while (true) {
        val candidate = "Auto$n"
        if (selfRelationships.none { it.name == candidate }) return candidate
        n++
    }
}

/**
 * Next unused specialization name (`EspN`), matching Pascal [GeraBaseNome]('Esp').
 */
internal fun ConceptualSchema.nextUnusedSpecializationName(): String {
    val patternUsed = usedEspNameIndices(this)
    var n = nextFreeIndex(patternUsed)
    while (true) {
        val candidate = "Esp$n"
        if (specializations.none { it.name == candidate }) return candidate
        n++
    }
}

private fun nextEntAssocName(schema: ConceptualSchema): String =
    "EntAssoc${nextFreeIndex(usedEntAssocIndices(schema))}"

/**
 * Next unused outer name for an associative entity (`EntAssocN`), scanning pattern indices and
 * explicit name collisions (entities, relationships, associative outers).
 */
internal fun ConceptualSchema.nextUnusedAssociativeEntityOuterName(): String {
    val patternUsed = usedEntAssocIndices(this)
    var n = nextFreeIndex(patternUsed)
    while (true) {
        val candidate = "EntAssoc$n"
        val taken = associativeEntities.any { it.name == candidate } ||
            entities.any { it.name == candidate } ||
            relationships.any { it.name == candidate }
        if (!taken) return candidate
        n++
    }
}

/**
 * Places a new canvas element at the given top-left (schema coordinates) and returns the updated schema
 * plus the new element id.
 */
fun ConceptualSchema.placeConceptualItem(
    kind: ConceptualPlacementKind,
    topLeftX: Int,
    topLeftY: Int,
): Pair<ConceptualSchema, Int> {
    val posEntity = ElementPosition(
        topLeftX,
        topLeftY,
        ConceptualPlacementDefaults.entityWidth,
        ConceptualPlacementDefaults.entityHeight,
    )
    val posRel = ElementPosition(
        topLeftX,
        topLeftY,
        ConceptualPlacementDefaults.relationshipWidth,
        ConceptualPlacementDefaults.relationshipHeight,
    )
    val posAssoc = ElementPosition(
        topLeftX,
        topLeftY,
        ConceptualPlacementDefaults.associativeOuterWidth,
        ConceptualPlacementDefaults.associativeOuterHeight,
    )
    val posAnnotation = ElementPosition(
        topLeftX,
        topLeftY,
        ConceptualPlacementDefaults.annotationWidth,
        ConceptualPlacementDefaults.annotationHeight,
    )
    val style = ConceptualPlacementDefaults.labelStyle

    return when (kind) {
        ConceptualPlacementKind.PlainEntity -> {
            val name = nextEntidadeName(this)
            val (s, id) = allocateId()
            val el = SchemaElement.Entity(
                id = id,
                name = name,
                position = posEntity,
                observations = "",
                dictionary = "",
                labelStyle = style,
                hiddenAttributes = emptyList(),
            )
            s.withElement(el) to id
        }
        ConceptualPlacementKind.Relationship -> {
            val name = nextRelacaoName(this)
            val (s, id) = allocateId()
            val el = SchemaElement.Relationship(
                id = id,
                name = name,
                position = posRel,
                observations = "",
                dictionary = "",
                labelStyle = style,
                hiddenAttributes = emptyList(),
                arrowDirection = ArrowDirection.NONE,
                showName = true,
            )
            s.withElement(el) to id
        }
        ConceptualPlacementKind.AssociativeEntity -> {
            val outerName = nextEntAssocName(this)
            val innerRelName = nextRelacaoName(this)
            val (s, id) = allocateId()
            val el = SchemaElement.AssociativeEntity(
                id = id,
                name = outerName,
                position = posAssoc,
                observations = "",
                dictionary = "",
                labelStyle = style,
                hiddenAttributes = emptyList(),
                relationshipName = innerRelName,
                relationshipDictionary = "",
                relationshipObservations = "",
                arrowDirection = ArrowDirection.NONE,
            )
            s.withElement(el) to id
        }
        ConceptualPlacementKind.Annotation -> {
            val (s, id) = allocateId()
            val el = SchemaElement.Annotation(
                id = id,
                name = ConceptualPlacementDefaults.annotationDefaultName,
                position = posAnnotation,
                observations = "",
                dictionary = "",
                labelStyle = style,
                hiddenAttributes = emptyList(),
                color = ConceptualPlacementDefaults.annotationColorArgb,
                annotationType = ConceptualPlacementDefaults.annotationType,
                alignment = ConceptualPlacementDefaults.annotationTextAlignment,
                autoSize = ConceptualPlacementDefaults.annotationAutoSize,
            )
            s.withElement(el) to id
        }
    }
}
