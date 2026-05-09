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

package games.polyclub.power.brmodelo.domain.xml

import games.polyclub.power.brmodelo.domain.xml.valor

/**
 * Lightweight, immutable representation of a parsed XML element.
 *
 * Used as an intermediate tree structure during parsing and serialization of
 * `.xml` schema files. Platform-specific parsing converts raw XML bytes into
 * this common format, and the serializer converts it back to a string.
 *
 * @param name       Tag name (e.g. "Entidade", "Atributo").
 * @param attributes Map of attribute name → value (e.g. "id" → "1", "nome" → "Entidade1").
 * @param children   Ordered list of child elements.
 * @param text       Trimmed text content of this element (for nodes like `<Dicionario>text</Dicionario>`).
 */
data class XmlNode(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val children: List<XmlNode> = emptyList(),
    val text: String = "",
) {
    // ── Attribute helpers ────────────────────────────────────────────────────

    fun attr(name: String): String? = attributes[name]
    fun attrInt(name: String): Int? = attributes[name]?.toIntOrNull()

    // ── Child element helpers ────────────────────────────────────────────────

    fun child(name: String): XmlNode? = children.firstOrNull { it.name == name }
    fun children(name: String): List<XmlNode> = children.filter { it.name == name }

    // ── Value element helpers (patterns from the brModelo XML format) ────────

    /**
     * Reads the integer `Valor` attribute of a child tag.
     * e.g. `<MaxCard Valor="21"/>` → 21
     */
    fun intValor(tagName: String, default: Int = 0): Int =
        child(tagName)?.attr("Valor")?.toIntOrNull() ?: default

    /**
     * Reads a Delphi boolean stored as an integer: -1 = true, 0 = false.
     * e.g. `<Identificador Valor="-1"/>` → true
     */
    fun boolValor(tagName: String): Boolean = intValor(tagName) == -1

    /**
     * Reads the string `Valor` attribute of a child tag, returning empty string if absent.
     * e.g. `<Tipo Valor="VARCHAR( )"/>` → "VARCHAR( )"
     */
    fun strValor(tagName: String): String = child(tagName)?.attr("Valor") ?: ""

    /**
     * Reads the trimmed text content of a child tag.
     * e.g. `<Dicionario>some text</Dicionario>` → "some text"
     */
    fun textChild(tagName: String): String = child(tagName)?.text?.trim() ?: ""
}

// ── Builder DSL for serialization ────────────────────────────────────────────

/** Builds an [games.polyclub.power.brmodelo.domain.xml.XmlNode] with the given [name] and optional [block] for configuration. */
fun xmlNode(
    name: String,
    vararg attributes: Pair<String, Any>,
    text: String = "",
    block: MutableList<XmlNode>.() -> Unit = {},
): XmlNode {
    val children = mutableListOf<XmlNode>()
    children.block()
    return XmlNode(
        name = name,
        attributes = attributes.associate { (k, v) -> k to v.toString() },
        children = children,
        text = text,
    )
}

/** Adds a child `<TagName Valor="value"/>` node. */
fun MutableList<XmlNode>.valor(tagName: String, value: Any) {
    add(xmlNode(tagName, "Valor" to value))
}

/** Adds a Delphi boolean child node: -1 for true, 0 for false. */
fun MutableList<XmlNode>.boolValor(tagName: String, value: Boolean) {
    valor(tagName, if (value) -1 else 0)
}

/** Adds a child node with text content: `<TagName>text</TagName>`. */
fun MutableList<XmlNode>.textNode(tagName: String, text: String) {
    add(XmlNode(name = tagName, text = text))
}
