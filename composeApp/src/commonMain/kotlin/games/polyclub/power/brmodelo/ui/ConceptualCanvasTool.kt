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

package games.polyclub.power.brmodelo.ui

import games.polyclub.power.brmodelo.domain.ConceptualAttributeToolVariant
import games.polyclub.power.brmodelo.domain.ConceptualLinkPick
import games.polyclub.power.brmodelo.domain.ConceptualPlacementKind
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolVariant

/**
 * Active conceptual-schema canvas mode (placement / manipulation tool).
 * [None] means no tool is armed; pointer and ribbon use default behaviour.
 */
internal sealed class ConceptualCanvasTool {
    internal data object None : ConceptualCanvasTool()

    /** Free-form observation / text box tool (ribbon button separate from entity split). */
    internal data object Observation : ConceptualCanvasTool()

    /**
     * "Ligar objetos" — first click picks an endpoint, second click completes the link.
     * [AwaitingFirst] uses `cursor_ligacao`; [AwaitingSecond] uses `cursor_ligacao2`.
     */
    internal sealed class LinkObjects : ConceptualCanvasTool() {
        internal data object AwaitingFirst : LinkObjects()
        internal data class AwaitingSecond(val first: ConceptualLinkPick) : LinkObjects()
    }

    /**
     * "Auto relacionar" — single click on an entity (or associative outer) creates a self-relationship
     * with two legs (Pascal `Tool_AutoRel` / `TBaseEntidade.AutoRelacionar`). Cursor: `cursor_autorel`.
     */
    internal data object AutoSelfRelationship : ConceptualCanvasTool()

    /**
     * Especialização — click a plain entity to place a specialization (Pascal `Tool_Especializacao*`).
     * Cursor depends on [variant] (`cursor_especializacao`, `cursor_especializacaoa`, `cursor_especializacaob`).
     */
    internal data class Specialization(val variant: ConceptualSpecializationToolVariant) : ConceptualCanvasTool()

    /**
     * Atributo — click an entity, relationship, associative entity, or attribute to add an attribute
     * (Pascal `Tool_Atributo*`). All variants share cursor `cursor_atributo`.
     */
    internal data class Attribute(val variant: ConceptualAttributeToolVariant) : ConceptualCanvasTool()

    internal sealed class Entity : ConceptualCanvasTool() {
        internal data object Plain : Entity()
        internal data object Relation : Entity()
        internal data object Associative : Entity()
    }
}

/** Variants offered by the Entidade split button and its dropdown. */
internal enum class EntityToolVariant {
    Plain,
    Relation,
    Associative,
}

internal fun EntityToolVariant.toConceptualTool(): ConceptualCanvasTool.Entity =
    when (this) {
        EntityToolVariant.Plain -> ConceptualCanvasTool.Entity.Plain
        EntityToolVariant.Relation -> ConceptualCanvasTool.Entity.Relation
        EntityToolVariant.Associative -> ConceptualCanvasTool.Entity.Associative
    }

internal fun ConceptualCanvasTool.matchesEntityVariant(variant: EntityToolVariant): Boolean =
    when (this) {
        is ConceptualCanvasTool.Entity.Plain -> variant == EntityToolVariant.Plain
        is ConceptualCanvasTool.Entity.Relation -> variant == EntityToolVariant.Relation
        is ConceptualCanvasTool.Entity.Associative -> variant == EntityToolVariant.Associative
        else -> false
    }

internal fun ConceptualSpecializationToolVariant.toConceptualTool(): ConceptualCanvasTool.Specialization =
    ConceptualCanvasTool.Specialization(this)

internal fun ConceptualCanvasTool.matchesSpecializationVariant(variant: ConceptualSpecializationToolVariant): Boolean =
    this is ConceptualCanvasTool.Specialization && this.variant == variant

internal fun ConceptualAttributeToolVariant.toConceptualTool(): ConceptualCanvasTool.Attribute =
    ConceptualCanvasTool.Attribute(this)

internal fun ConceptualCanvasTool.matchesAttributeVariant(variant: ConceptualAttributeToolVariant): Boolean =
    this is ConceptualCanvasTool.Attribute && this.variant == variant

internal fun ConceptualCanvasTool.toPlacementKindOrNull(): ConceptualPlacementKind? =
    when (this) {
        is ConceptualCanvasTool.Entity.Plain -> ConceptualPlacementKind.PlainEntity
        is ConceptualCanvasTool.Entity.Relation -> ConceptualPlacementKind.Relationship
        is ConceptualCanvasTool.Entity.Associative -> ConceptualPlacementKind.AssociativeEntity
        is ConceptualCanvasTool.Observation -> ConceptualPlacementKind.Annotation
        else -> null
    }
