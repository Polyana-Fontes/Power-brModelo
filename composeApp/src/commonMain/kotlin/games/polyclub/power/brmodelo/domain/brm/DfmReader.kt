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

package games.polyclub.power.brmodelo.domain.brm

import games.polyclub.power.brmodelo.domain.brm.decodeToStringLatin1

/**
 * Parses a brModelo `.brM` file (Delphi binary DFM stream) into a [games.polyclub.power.brmodelo.domain.brm.DfmNode] tree.
 *
 * ## File structure
 * ```
 * ShortString version   — brModelo-specific version prefix, e.g. "2.0.0"
 * "TPF0"                — 4-byte Delphi binary DFM signature
 * DFM component tree    — root is TModelo; all model elements are its flat children
 * ```
 *
 * ## Binary DFM format
 * Each component is encoded as:
 * ```
 * classNameLen(1) className(N)
 * instanceNameLen(1) instanceName(N)
 * [propNameLen(1) propName(N) valueType(1) value(…)]*
 * 0x00                  — end of property list
 * [child component]*
 * 0x00                  — end of children list
 * ```
 * Property value types correspond to Delphi's `TValueType` enum (see [games.polyclub.power.brmodelo.domain.brm.DfmValue]).
 *
 * This parser is written in pure Kotlin/commonMain and has no platform dependencies.
 */
fun parseDfmBytes(bytes: ByteArray): DfmNode {
    val reader = DfmByteReader(bytes)

    // brModelo-specific: version ShortString before the standard DFM magic bytes
    reader.readShortString() // e.g. "2.0.0" — ignored for now

    val magic = reader.readRaw(4).decodeToStringLatin1()
    require(magic == "TPF0") { "Not a brM/DFM file — expected 'TPF0', got '$magic'" }

    return reader.readComponent()
}

// ── Internal byte reader ──────────────────────────────────────────────────────

private class DfmByteReader(private val data: ByteArray) {
    var pos: Int = 0

    // ── Primitive reads ───────────────────────────────────────────────────────

    private fun readUByte(): Int {
        check(pos < data.size) { "Unexpected end of stream at position $pos" }
        return data[pos++].toInt() and 0xFF
    }

    private fun readSByte(): Int {
        check(pos < data.size) { "Unexpected end of stream at position $pos" }
        return data[pos++].toInt() // Kotlin `Byte` is signed, extension to Int is sign-preserving
    }

    private fun readUShortLE(): Int {
        val lo = readUByte()
        val hi = readUByte()
        return (hi shl 8) or lo
    }

    private fun readSShortLE(): Int {
        val v = readUShortLE()
        return if (v >= 0x8000) v - 0x10000 else v
    }

    private fun readSIntLE(): Int {
        val b0 = readUByte()
        val b1 = readUByte()
        val b2 = readUByte()
        val b3 = readUByte()
        return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    }

    private fun peekUByte(): Int {
        check(pos < data.size) { "Unexpected end of stream at position $pos" }
        return data[pos].toInt() and 0xFF
    }

    fun readRaw(n: Int): ByteArray {
        check(pos + n <= data.size) { "Unexpected end of stream reading $n bytes at position $pos" }
        val result = data.copyOfRange(pos, pos + n)
        pos += n
        return result
    }

    /** Reads a Delphi ShortString: 1-byte length + that many ISO-8859-1 bytes. */
    fun readShortString(): String {
        val len = readUByte()
        return if (len == 0) "" else readRaw(len).decodeToStringLatin1()
    }

    // ── Component reading ─────────────────────────────────────────────────────

    fun readComponent(): DfmNode {
        val className = readShortString()
        val instanceName = readShortString()

        val props = mutableMapOf<String, DfmValue>()
        while (true) {
            val propName = readShortString()
            if (propName.isEmpty()) break
            props[propName] = readValue()
        }

        val children = mutableListOf<DfmNode>()
        while (peekUByte() != 0) {
            children.add(readComponent())
        }
        readUByte() // consume end-of-children marker (0x00)

        return DfmNode(
            className,
            instanceName,
            props,
            children
        )
    }

    // ── Value reading ─────────────────────────────────────────────────────────

    /**
     * Reads a property value according to its leading type byte.
     *
     * Types correspond to the Delphi `TValueType` enum in `Classes.pas`:
     * 0x01=vaList, 0x02=vaInt8, 0x03=vaInt16, 0x04=vaInt32, 0x05=vaExtended,
     * 0x06=vaString, 0x07=vaIdent, 0x08=vaFalse, 0x09=vaTrue, 0x0a=vaBinary,
     * 0x0b=vaSet, 0x0c=vaLString, 0x0d=vaNil, 0x0e=vaCollection, 0x0f=vaWString.
     */
    fun readValue(): DfmValue {
        return when (val type = readUByte()) {
            // vaInt8 — signed 8-bit integer
            0x02 -> DfmValue.IntVal(readSByte())

            // vaInt16 — signed 16-bit LE integer
            0x03 -> DfmValue.IntVal(readSShortLE())

            // vaInt32 — signed 32-bit LE integer
            0x04 -> DfmValue.IntVal(readSIntLE())

            // vaExtended — 10-byte Delphi 80-bit extended float; parsed as approximate Double
            0x05 -> {
                val raw = readRaw(10)
                DfmValue.IntVal(extendedToInt(raw)) // approximate conversion for integer-valued floats
            }

            // vaString — ShortString (1-byte length + chars)
            0x06 -> DfmValue.StrVal(readShortString())

            // vaIdent — identifier stored as ShortString
            0x07 -> DfmValue.StrVal(readShortString())

            // vaFalse / vaTrue — no following bytes
            0x08 -> DfmValue.BoolVal(false)
            0x09 -> DfmValue.BoolVal(true)

            // vaBinary — 4-byte LE length + raw bytes
            0x0a -> {
                val len = readSIntLE()
                DfmValue.BinVal(readRaw(len))
            }

            // vaSet — list of ShortString identifiers terminated by empty identifier
            0x0b -> {
                val items = mutableListOf<String>()
                while (true) {
                    val s = readShortString()
                    if (s.isEmpty()) break
                    items.add(s)
                }
                DfmValue.SetVal(items)
            }

            // vaLString — long string: 4-byte LE length + chars (ISO-8859-1)
            0x0c -> {
                val len = readSIntLE()
                DfmValue.StrVal(if (len <= 0) "" else readRaw(len).decodeToStringLatin1())
            }

            // vaNil
            0x0d -> DfmValue.NilVal

            // vaCollection — list of items, each with properties, terminated by vaNull
            0x0e -> {
                val items = mutableListOf<DfmValue>()
                while (peekUByte() != 0x00) {
                    // Skip the leading 0x01 (vaList start) for each item
                    if (peekUByte() == 0x01) readUByte()
                    val itemProps = mutableMapOf<String, DfmValue>()
                    while (true) {
                        val n = readShortString()
                        if (n.isEmpty()) break
                        itemProps[n] = readValue()
                    }
                    items.add(DfmValue.ListVal(itemProps.values.toList()))
                }
                readUByte() // consume vaNull end marker
                DfmValue.ListVal(items)
            }

            // vaWString — 4-byte char count + WideChar pairs (UTF-16 LE)
            0x0f -> {
                val charCount = readSIntLE()
                if (charCount <= 0) {
                    DfmValue.StrVal("")
                } else {
                    val raw = readRaw(charCount * 2)
                    DfmValue.StrVal(
                        decodeUtf16LE(
                            raw
                        )
                    )
                }
            }

            // vaList — list of values terminated by vaNull (0x00)
            0x01 -> {
                val items = mutableListOf<DfmValue>()
                while (peekUByte() != 0x00) {
                    items.add(readValue())
                }
                readUByte() // consume vaNull
                DfmValue.ListVal(items)
            }

            // vaInt64 (added in newer Delphi versions)
            0x10 -> {
                val lo = readSIntLE().toLong() and 0xFFFFFFFFL
                val hi = readSIntLE().toLong()
                DfmValue.IntVal(((hi shl 32) or lo).toInt())
            }

            // vaUTF8String (type code varies across Delphi versions; observed as 0x14 in
            // brModelo binaries compiled with Delphi XE+ Unicode). Format: 4-byte LE length
            // + that many UTF-8 bytes (no null terminator).
            0x12, 0x14 -> {
                val len = readSIntLE()
                DfmValue.StrVal(if (len <= 0) "" else readRaw(len).decodeToString())
            }

            else -> throw IllegalStateException(
                "Unknown DFM value type 0x${type.toString(16).padStart(2, '0')} at pos ${pos - 1}"
            )
        }
    }

    // ── Helper: approximate 80-bit Delphi Extended → Int ─────────────────────

    /**
     * Converts a Delphi 80-bit extended float to an Int approximation.
     *
     * The 80-bit extended format (x87 FPU):
     * - bits 79: sign
     * - bits 78-64: 15-bit biased exponent (bias=16383)
     * - bits 63-0: 64-bit mantissa with explicit integer bit
     *
     * For integer-valued floats (like TForm.Left/Top/Width/Height when stored as
     * Extended), this gives the exact value. For non-integer values, we truncate.
     */
    private fun extendedToInt(raw: ByteArray): Int {
        // Delphi stores in little-endian: bytes 0-7 = mantissa, bytes 8-9 = sign+exponent
        val signExp = ((raw[9].toInt() and 0xFF) shl 8) or (raw[8].toInt() and 0xFF)
        val sign = if (signExp and 0x8000 != 0) -1 else 1
        val exp = (signExp and 0x7FFF) - 16383

        // Reconstruct mantissa as 64-bit integer (explicit integer bit in bit 63)
        var mantissa = 0L
        for (i in 7 downTo 0) {
            mantissa = (mantissa shl 8) or (raw[i].toLong() and 0xFF)
        }

        if (exp < 0 || exp > 62) return 0 // out of Int range or fractional
        val shifted = mantissa shr (63 - exp)
        return (sign * (shifted and 0x7FFFFFFF)).toInt()
    }
}

// ── Encoding helpers ──────────────────────────────────────────────────────────

/**
 * Decodes a [ByteArray] as ISO-8859-1 (Latin-1), mapping each byte directly to
 * a Unicode code point. Works identically across JVM and Kotlin/Wasm.
 */
internal fun ByteArray.decodeToStringLatin1(): String = buildString(size) {
    for (b in this@decodeToStringLatin1) {
        append((b.toInt() and 0xFF).toChar())
    }
}

/**
 * Decodes a [ByteArray] as UTF-16 LE (Windows WideString).
 */
private fun decodeUtf16LE(bytes: ByteArray): String = buildString(bytes.size / 2) {
    var i = 0
    while (i + 1 < bytes.size) {
        val lo = bytes[i].toInt() and 0xFF
        val hi = bytes[i + 1].toInt() and 0xFF
        append(((hi shl 8) or lo).toChar())
        i += 2
    }
}
