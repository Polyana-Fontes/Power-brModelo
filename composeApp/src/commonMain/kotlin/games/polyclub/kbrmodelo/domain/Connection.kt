/*
 * KbrModelo - Kotlin port of brModelo 3.0 originally written in Pascal
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

package games.polyclub.kbrmodelo.domain

/**
 * Orientation of a connection line segment.
 *
 * Corresponds to the Pascal constants `OrientacaoV = 0`, `OrientacaoH = 1`,
 * `OrientacaoD = 2`, `OrientacaoE = 3` used by `TLinha` and `TLigacao`.
 */
enum class LineOrientation(val code: Int) {
    VERTICAL(0),
    HORIZONTAL(1),
    DIAGONAL(2),
    LEFT(3);

    companion object {
        fun fromCode(code: Int): LineOrientation =
            entries.firstOrNull { it.code == code } ?: HORIZONTAL
    }
}

/**
 * A directed connection between two [SchemaElement]s.
 *
 * Corresponds to `TLigacao` in `mer.pas`. In the original Pascal code a
 * `TLigacao` always connects exactly two elements (`E1` and `E2`). The
 * cardinality label is displayed near [elementIdB] (the "end" side).
 *
 * Connections are used for:
 * - Entity ↔ Relationship (or AssociativeEntity / SelfRelationship)
 * - Entity ↔ Specialization (base entity or child entity)
 * - Attribute ↔ its owner entity/relationship
 * - Attribute ↔ its child attributes (composite)
 *
 * @param id                  Unique identifier within the schema.
 * @param elementIdA          ID of the first connected element. Corresponds to `TLigacao.E1.OID`.
 * @param elementIdB          ID of the second connected element. Corresponds to `TLigacao.E2.OID`.
 * @param cardinality         Cardinality label shown near [elementIdB].
 *                            Corresponds to `TLigacao.Cardinalidade` (1–4) mapped via [Cardinality].
 *                            Null when no cardinality applies (e.g. attribute connections).
 * @param showCardinality     Whether the cardinality label is visible on the canvas.
 *                            Corresponds to `TLigacao.MostraCardinalidade`.
 * @param cardinalityFixed    Whether the cardinality label position is locked by the user.
 *                            Corresponds to `TCardinalidade.Fixa`.
 * @param isWeak              Whether this connection represents a weak-entity participation
 *                            (drawn as a double line). Corresponds to `TLigacao.Fraca`.
 * @param orientation         Primary orientation of the line segments.
 *                            Corresponds to `TLigacao.Orientacao`.
 * @param cardinalityRole     Optional role name displayed after the cardinality label
 *                            (e.g. "Responsável" → "(1,1) Responsável").
 *                            Corresponds to `TCardinalidade.nome` in the original Pascal source.
 * @param cardinalityPosition Position of the floating cardinality label on the canvas.
 *                            Corresponds to `TCardinalidade` Left/Top (a `TBase` subclass).
 */
data class Connection(
    val id: Int,
    val elementIdA: Int,
    val elementIdB: Int,
    val cardinality: Cardinality? = null,
    val showCardinality: Boolean = true,
    val cardinalityFixed: Boolean = false,
    val isWeak: Boolean = false,
    val orientation: LineOrientation = LineOrientation.HORIZONTAL,
    val cardinalityRole: String = "",
    val cardinalityPosition: ElementPosition? = null,
)
