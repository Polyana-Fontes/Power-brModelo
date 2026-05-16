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
 * Classifies the structural outcome of a successful **Ligar Objetos** / MCP `tools__link_objects` (or
 * `tools__link_existing_endpoints` when it commits the same shape) apply
 * by diffing [before] vs [after] (post-validation schema, typically after MCP patches and enrichment).
 *
 * Values are stable English snake_case strings for MCP JSON only; they are not persisted in MER XML.
 */
fun classifyMcpLinkObjectsPattern(before: ConceptualSchema, after: ConceptualSchema): String {
    val oldConnIds = before.connections.map { it.id }.toSet()
    val newElemIds = after.elements.keys.filter { it !in before.elements.keys }
    val newConns = after.connections.filter { it.id !in oldConnIds }

    if (newElemIds.any { after.elements[it] is SchemaElement.SelfRelationship }) {
        return "entity_auto_self_relationship"
    }
    if (newElemIds.any { after.elements[it] is SchemaElement.Relationship }) {
        return "entity_entity_new_relationship"
    }
    if (newConns.isEmpty()) {
        return "unknown"
    }
    if (newConns.size == 1) {
        val c = newConns[0]
        val aEl = after.elements[c.elementIdA]
        val bEl = after.elements[c.elementIdB]
        if (aEl is SchemaElement.Specialization || bEl is SchemaElement.Specialization) {
            return "specialization_entity_link"
        }
        if (c.useAssociativeOuterForEndA || c.useAssociativeOuterForEndB) {
            return "entity_associative_outer_bridge"
        }
        val relTouch =
            aEl is SchemaElement.Relationship || aEl is SchemaElement.SelfRelationship ||
                bEl is SchemaElement.Relationship || bEl is SchemaElement.SelfRelationship
        if (relTouch) {
            return "relationship_entity_leg"
        }
        return "single_connection_other"
    }
    return "multi_leg_link"
}
