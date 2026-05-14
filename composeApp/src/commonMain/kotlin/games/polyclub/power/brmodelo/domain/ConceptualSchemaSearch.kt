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

internal object ConceptualSearchConstants {
    const val DEFAULT_MAX_RESULTS: Int = 400
}

/**
 * Per-type inclusion flags for the conceptual search dialog / MCP tools.
 * When [anySelected] is false, [effective] treats the request as “search all types”.
 */
internal data class ConceptualSearchTypeFilters(
    val includeEntities: Boolean,
    val includeRelationships: Boolean,
    val includeAssociativeEntities: Boolean,
    val includeSpecializations: Boolean,
    val includeCanvasAttributes: Boolean,
    val includeHiddenAttributes: Boolean,
    val includeCardinalityLabels: Boolean,
    val includeObservationBoxes: Boolean,
) {
    fun anySelected(): Boolean =
        includeEntities || includeRelationships || includeAssociativeEntities ||
            includeSpecializations || includeCanvasAttributes || includeHiddenAttributes ||
            includeCardinalityLabels || includeObservationBoxes

    fun effective(): ConceptualSearchTypeFilters =
        if (!anySelected()) Companion.allTrue() else this

    companion object {
        fun allTrue(): ConceptualSearchTypeFilters = ConceptualSearchTypeFilters(
            includeEntities = true,
            includeRelationships = true,
            includeAssociativeEntities = true,
            includeSpecializations = true,
            includeCanvasAttributes = true,
            includeHiddenAttributes = true,
            includeCardinalityLabels = true,
            includeObservationBoxes = true,
        )
    }
}

/**
 * Optional inclusion of sidebar dictionary / observations on schema elements
 * (cardinality dictionary/observation follow the same flags on [Connection]).
 */
internal data class ConceptualSearchTextScope(
    val searchDictionary: Boolean,
    val searchObservations: Boolean,
)

internal sealed class ConceptualSearchHit {
    abstract val stableSortKey: String

    data class ElementHit(
        val elementId: Int,
        val elementKindKey: String,
        val title: String,
        val matchedIn: List<String>,
        val position: ElementPosition,
    ) : ConceptualSearchHit() {
        override val stableSortKey: String get() = "E:$elementId"
    }

    data class CardinalityHit(
        val connectionId: Int,
        val title: String,
        val matchedIn: List<String>,
        val position: ElementPosition?,
    ) : ConceptualSearchHit() {
        override val stableSortKey: String get() = "C:$connectionId"
    }

    data class HiddenHit(
        val ownerElementId: Int,
        val path: List<Int>,
        val displayName: String,
        val matchedIn: List<String>,
    ) : ConceptualSearchHit() {
        override val stableSortKey: String get() = "H:$ownerElementId:" + path.joinToString(",")
    }
}

internal data class ConceptualSearchResult(
    val hits: List<ConceptualSearchHit>,
    val truncated: Boolean,
    val totalMatched: Int,
)

internal sealed class ConceptualSearchOutcome {
    data class Ok(val result: ConceptualSearchResult) : ConceptualSearchOutcome()
    data class Err(val code: String) : ConceptualSearchOutcome()
}

private fun matchLabels(needle: String, fields: List<Pair<String, String>>): List<String> {
    val out = mutableListOf<String>()
    for ((key, raw) in fields) {
        if (raw.isNotEmpty() && containsFolded(raw, needle)) {
            out.add(key)
        }
    }
    return out
}

private fun SchemaElement.baseSearchFields(
    needle: String,
    scope: ConceptualSearchTextScope,
): List<String> {
    val pairs = buildList {
        add("name" to name)
        if (scope.searchDictionary) add("dictionary" to dictionary)
        if (scope.searchObservations) add("observations" to observations)
    }
    return matchLabels(needle, pairs)
}

private fun SchemaElement.Attribute.attributeSearchMatches(needle: String, scope: ConceptualSearchTextScope): List<String> {
    val out = baseSearchFields(needle, scope).toMutableList()
    if (containsFolded(valueType, needle)) out.add("valueType")
    if (containsFolded(complement, needle)) out.add("complement")
    return out.distinct()
}

private fun SchemaElement.AssociativeEntity.associativeSearchMatches(needle: String, scope: ConceptualSearchTextScope): List<String> {
    val out = baseSearchFields(needle, scope).toMutableList()
    val innerName = listOf("innerRelationshipName" to relationshipName)
    out.addAll(matchLabels(needle, innerName))
    val innerOptional = buildList {
        if (scope.searchDictionary) add("innerRelationshipDictionary" to relationshipDictionary)
        if (scope.searchObservations) add("innerRelationshipObservations" to relationshipObservations)
    }
    out.addAll(matchLabels(needle, innerOptional))
    return out.distinct()
}

private fun connectionCardinalityFields(conn: Connection, scope: ConceptualSearchTextScope): List<Pair<String, String>> =
    buildList {
        conn.cardinality?.let { add("cardinalityLabel" to it.label) }
        add("role" to conn.cardinalityRole)
        if (scope.searchDictionary) add("cardinalityDictionary" to conn.cardinalityDictionary)
        if (scope.searchObservations) add("cardinalityObservations" to conn.cardinalityObservations)
    }

private fun connectionIsCardinalitySearchCandidate(conn: Connection): Boolean {
    if (conn.cardinality != null) return true
    if (conn.cardinalityRole.isNotBlank()) return true
    if (conn.cardinalityDictionary.isNotBlank()) return true
    if (conn.cardinalityObservations.isNotBlank()) return true
    return false
}

private fun hiddenNodeFields(ha: HiddenAttribute, scope: ConceptualSearchTextScope): List<Pair<String, String>> =
    buildList {
        add("name" to ha.name)
        if (scope.searchDictionary) add("dictionary" to ha.dictionary)
        if (scope.searchObservations) add("observations" to ha.observations)
    }

private fun walkHiddenAttributes(
    ownerId: Int,
    nodes: List<HiddenAttribute>,
    pathPrefix: List<Int>,
    needle: String,
    scope: ConceptualSearchTextScope,
    emptyQuery: Boolean,
    out: MutableList<ConceptualSearchHit>,
) {
    nodes.forEachIndexed { index, ha ->
        val path = pathPrefix + index
        val labels = if (emptyQuery) emptyList() else matchLabels(needle, hiddenNodeFields(ha, scope))
        if (emptyQuery || labels.isNotEmpty()) {
            out.add(
                ConceptualSearchHit.HiddenHit(
                    ownerElementId = ownerId,
                    path = path,
                    displayName = ha.name,
                    matchedIn = labels,
                ),
            )
        }
        ha.children.forEachIndexed { ci, child ->
            val childPath = path + ci
            visitHiddenSubtree(ownerId, child, childPath, needle, scope, emptyQuery, out)
        }
        ha.nestedHiddenAttributes.forEachIndexed { ni, nested ->
            val nestedPath = path + ha.children.size + ni
            visitHiddenSubtree(ownerId, nested, nestedPath, needle, scope, emptyQuery, out)
        }
    }
}

private fun visitHiddenSubtree(
    ownerId: Int,
    ha: HiddenAttribute,
    path: List<Int>,
    needle: String,
    scope: ConceptualSearchTextScope,
    emptyQuery: Boolean,
    out: MutableList<ConceptualSearchHit>,
) {
    val labels = if (emptyQuery) emptyList() else matchLabels(needle, hiddenNodeFields(ha, scope))
    if (emptyQuery || labels.isNotEmpty()) {
        out.add(
            ConceptualSearchHit.HiddenHit(
                ownerElementId = ownerId,
                path = path,
                displayName = ha.name,
                matchedIn = labels,
            ),
        )
    }
    ha.children.forEachIndexed { ci, child ->
        visitHiddenSubtree(ownerId, child, path + ci, needle, scope, emptyQuery, out)
    }
    ha.nestedHiddenAttributes.forEachIndexed { ni, nested ->
        visitHiddenSubtree(ownerId, nested, path + ha.children.size + ni, needle, scope, emptyQuery, out)
    }
}

private fun elementKindKey(el: SchemaElement): String =
    when (el) {
        is SchemaElement.Entity -> "entity"
        is SchemaElement.Relationship -> "relationship"
        is SchemaElement.AssociativeEntity -> "associativeEntity"
        is SchemaElement.Specialization -> "specialization"
        is SchemaElement.Attribute -> "attribute"
        is SchemaElement.SelfRelationship -> "selfRelationship"
        is SchemaElement.Annotation -> "observationBox"
    }

private fun elementTitle(el: SchemaElement): String =
    when (el) {
        is SchemaElement.AssociativeEntity -> "${el.name} / ${el.relationshipName}"
        else -> el.name
    }

/**
 * Search over the conceptual schema (substring after accent folding when [rawQuery] is non-blank).
 *
 * When [rawQuery] is blank (after trim), every candidate in the effective type filters is listed
 * (same 400-hit cap), with empty [ConceptualSearchHit.ElementHit.matchedIn] / analogous lists — useful
 * to enumerate e.g. all entities for agents or quick UI browse.
 */
internal fun ConceptualSchema.searchConceptualModel(
    rawQuery: String,
    typeFilters: ConceptualSearchTypeFilters,
    textScope: ConceptualSearchTextScope,
    maxResults: Int = ConceptualSearchConstants.DEFAULT_MAX_RESULTS,
): ConceptualSearchOutcome {
    val trimmed = rawQuery.trim()
    val emptyQuery = trimmed.isEmpty()
    val needle = if (emptyQuery) "" else foldStringForSearch(trimmed)
    val f = typeFilters.effective()
    val hits = mutableListOf<ConceptualSearchHit>()

    val elementsSorted = elements.entries.sortedBy { it.key }
    for ((_, el) in elementsSorted) {
        when (el) {
            is SchemaElement.Entity -> {
                if (!f.includeEntities) continue
                val m = if (emptyQuery) emptyList() else el.baseSearchFields(needle, textScope)
                if (emptyQuery || m.isNotEmpty()) {
                    hits.add(
                        ConceptualSearchHit.ElementHit(
                            elementId = el.id,
                            elementKindKey = elementKindKey(el),
                            title = elementTitle(el),
                            matchedIn = m,
                            position = el.position,
                        ),
                    )
                }
            }
            is SchemaElement.Relationship -> {
                if (!f.includeRelationships) continue
                val m = if (emptyQuery) emptyList() else el.baseSearchFields(needle, textScope)
                if (emptyQuery || m.isNotEmpty()) {
                    hits.add(
                        ConceptualSearchHit.ElementHit(
                            elementId = el.id,
                            elementKindKey = elementKindKey(el),
                            title = elementTitle(el),
                            matchedIn = m,
                            position = el.position,
                        ),
                    )
                }
            }
            is SchemaElement.SelfRelationship -> {
                if (!f.includeRelationships) continue
                val m = if (emptyQuery) emptyList() else el.baseSearchFields(needle, textScope)
                if (emptyQuery || m.isNotEmpty()) {
                    hits.add(
                        ConceptualSearchHit.ElementHit(
                            elementId = el.id,
                            elementKindKey = elementKindKey(el),
                            title = elementTitle(el),
                            matchedIn = m,
                            position = el.position,
                        ),
                    )
                }
            }
            is SchemaElement.AssociativeEntity -> {
                if (!f.includeAssociativeEntities) continue
                val m = if (emptyQuery) emptyList() else el.associativeSearchMatches(needle, textScope)
                if (emptyQuery || m.isNotEmpty()) {
                    hits.add(
                        ConceptualSearchHit.ElementHit(
                            elementId = el.id,
                            elementKindKey = elementKindKey(el),
                            title = elementTitle(el),
                            matchedIn = m,
                            position = el.position,
                        ),
                    )
                }
            }
            is SchemaElement.Specialization -> {
                if (!f.includeSpecializations) continue
                val m = if (emptyQuery) emptyList() else el.baseSearchFields(needle, textScope)
                if (emptyQuery || m.isNotEmpty()) {
                    hits.add(
                        ConceptualSearchHit.ElementHit(
                            elementId = el.id,
                            elementKindKey = elementKindKey(el),
                            title = elementTitle(el),
                            matchedIn = m,
                            position = el.position,
                        ),
                    )
                }
            }
            is SchemaElement.Attribute -> {
                if (!f.includeCanvasAttributes) continue
                val m = if (emptyQuery) emptyList() else el.attributeSearchMatches(needle, textScope)
                if (emptyQuery || m.isNotEmpty()) {
                    hits.add(
                        ConceptualSearchHit.ElementHit(
                            elementId = el.id,
                            elementKindKey = elementKindKey(el),
                            title = elementTitle(el),
                            matchedIn = m,
                            position = el.position,
                        ),
                    )
                }
            }
            is SchemaElement.Annotation -> {
                if (!f.includeObservationBoxes) continue
                val m = if (emptyQuery) emptyList() else el.baseSearchFields(needle, textScope)
                if (emptyQuery || m.isNotEmpty()) {
                    hits.add(
                        ConceptualSearchHit.ElementHit(
                            elementId = el.id,
                            elementKindKey = elementKindKey(el),
                            title = elementTitle(el),
                            matchedIn = m,
                            position = el.position,
                        ),
                    )
                }
            }
        }
    }

    if (f.includeCardinalityLabels) {
        for (conn in connections.sortedBy { it.id }) {
            if (!connectionIsCardinalitySearchCandidate(conn)) continue
            val m = if (emptyQuery) {
                emptyList()
            } else {
                matchLabels(needle, connectionCardinalityFields(conn, textScope))
            }
            if (emptyQuery || m.isNotEmpty()) {
                val title = buildString {
                    append(conn.cardinality?.label ?: "")
                    if (conn.cardinalityRole.isNotBlank()) {
                        if (isNotEmpty()) append(' ')
                        append(conn.cardinalityRole)
                    }
                    if (isEmpty()) append("cardinality#${conn.id}")
                }
                hits.add(
                    ConceptualSearchHit.CardinalityHit(
                        connectionId = conn.id,
                        title = title,
                        matchedIn = m,
                        position = conn.cardinalityPosition,
                    ),
                )
            }
        }
    }

    if (f.includeHiddenAttributes) {
        for ((ownerId, el) in elementsSorted) {
            val hidden = when (el) {
                is SchemaElement.Entity -> el.hiddenAttributes
                is SchemaElement.Relationship -> el.hiddenAttributes
                is SchemaElement.AssociativeEntity -> el.hiddenAttributes
                is SchemaElement.Attribute -> el.hiddenAttributes
                is SchemaElement.SelfRelationship -> el.hiddenAttributes
                is SchemaElement.Specialization -> el.hiddenAttributes
                is SchemaElement.Annotation -> el.hiddenAttributes
            }
            if (hidden.isEmpty()) continue
            walkHiddenAttributes(ownerId, hidden, emptyList(), needle, textScope, emptyQuery, hits)
        }
    }

    hits.sortBy { it.stableSortKey }
    val total = hits.size
    val truncated = total > maxResults
    val limited = if (truncated) hits.take(maxResults) else hits
    return ConceptualSearchOutcome.Ok(
        ConceptualSearchResult(
            hits = limited,
            truncated = truncated,
            totalMatched = total,
        ),
    )
}
