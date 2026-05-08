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

package games.polyclub.kbrmodelo.domain.brm

import games.polyclub.kbrmodelo.domain.ElementPosition

/**
 * A parsed Delphi binary DFM component node.
 *
 * Corresponds to one component in the Delphi VCL form stream (the `.brM` file).
 * All `TBase`-derived brModelo elements (TEntidade, TRelacao, TAtributo, etc.)
 * map to one DfmNode each.
 *
 * The property values are stored as [DfmValue] and helper accessors are provided
 * via [intProp], [boolProp], and [strProp].
 */
data class DfmNode(
    val className: String,
    val instanceName: String,
    val properties: Map<String, DfmValue>,
    val children: List<DfmNode>,
) {
    // ── Typed property accessors ──────────────────────────────────────────────

    fun intProp(name: String, default: Int = 0): Int =
        when (val v = properties[name]) {
            is DfmValue.IntVal -> v.value
            is DfmValue.BoolVal -> if (v.value) 1 else 0
            else -> default
        }

    fun boolProp(name: String, default: Boolean = false): Boolean =
        when (val v = properties[name]) {
            is DfmValue.BoolVal -> v.value
            is DfmValue.IntVal -> v.value != 0
            else -> default
        }

    fun strProp(name: String, default: String = ""): String =
        when (val v = properties[name]) {
            is DfmValue.StrVal -> v.value
            is DfmValue.IntVal -> v.value.toString()
            else -> default
        }

    /** Resolves the [ElementPosition] from the standard VCL Left/Top/Width/Height properties. */
    fun position(): ElementPosition = ElementPosition(
        x = intProp("Left"),
        y = intProp("Top"),
        width = intProp("Width"),
        height = intProp("Height"),
    )
}

// ── DfmValue type hierarchy ───────────────────────────────────────────────────

/**
 * A typed value read from a Delphi binary DFM property stream.
 *
 * Each variant corresponds to one of the Delphi `TValueType` constants used
 * in [Classes.TReader]:
 * - [IntVal]: `vaInt8` (0x02), `vaInt16` (0x03), `vaInt32` (0x04)
 * - [BoolVal]: `vaFalse` (0x08), `vaTrue` (0x09)
 * - [StrVal]: `vaString` (0x06), `vaIdent` (0x07), `vaLString` (0x0c), `vaWString` (0x0f)
 * - [SetVal]: `vaSet` (0x0b)
 * - [ListVal]: `vaList` (0x01)
 * - [BinVal]: `vaBinary` (0x0a)
 * - [NilVal]: `vaNil` (0x0d)
 */
sealed class DfmValue {
    data class IntVal(val value: Int) : DfmValue()
    data class BoolVal(val value: Boolean) : DfmValue()
    data class StrVal(val value: String) : DfmValue()
    data class SetVal(val identifiers: List<String>) : DfmValue()
    data class ListVal(val items: List<DfmValue>) : DfmValue()
    data class BinVal(val bytes: ByteArray) : DfmValue() {
        override fun equals(other: Any?) = other is BinVal && bytes.contentEquals(other.bytes)
        override fun hashCode() = bytes.contentHashCode()
    }
    data object NilVal : DfmValue()
}
