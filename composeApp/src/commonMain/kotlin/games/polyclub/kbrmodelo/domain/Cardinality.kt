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
 * Cardinality of a connection between a relationship and an entity.
 *
 * Corresponds to the Pascal constant array `aCardinalidade [1..4]` and the
 * `Cardinalidade` property on `TLigacao` / `TCardinalidade`.
 *
 * Values match the original integer codes: 1→(1,1), 2→(0,1), 3→(1,n), 4→(0,n).
 */
enum class Cardinality(val label: String) {
    ONE_TO_ONE("(1,1)"),
    ZERO_TO_ONE("(0,1)"),
    ONE_TO_MANY("(1,n)"),
    ZERO_TO_MANY("(0,n)");

    companion object {
        /** Returns the [Cardinality] that maps to the given Pascal integer code, or null if invalid. */
        fun fromCode(code: Int): Cardinality? = when (code) {
            1 -> ONE_TO_ONE
            2 -> ZERO_TO_ONE
            3 -> ONE_TO_MANY
            4 -> ZERO_TO_MANY
            else -> null
        }
    }
}

/**
 * Cardinality of a multi-valued attribute expressed as explicit min/max integers.
 *
 * Corresponds to [TAtributo.MinCard] / [TAtributo.MaxCard] in the original Pascal source.
 * [maxCardinality] == 21 represents unbounded (N).
 */
data class AttributeCardinality(
    val minCardinality: Int,
    val maxCardinality: Int,
) {
    /** True when [maxCardinality] represents an unbounded (N) value. */
    val isUnbounded: Boolean get() = maxCardinality > 20

    /** Human-readable cardinality string, e.g. "(0,n)" or "(1,2)". */
    fun toLabel(): String {
        if (maxCardinality == 0) return ""
        val maxStr = if (isUnbounded) "n" else maxCardinality.toString()
        return "($minCardinality,$maxStr)"
    }
}
