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

package games.polyclub.kbrmodelo.domain.xml

// ── External DOM type stubs ───────────────────────────────────────────────────
// Minimal external declarations so that regular Kotlin code (loops, when, etc.)
// can work with browser DOM nodes without calling js() inside control structures.
// Kotlin/Wasm restricts js() to be the single expression of a top-level function.

private external interface DomNode : JsAny {
    val nodeName: String
    val nodeType: Int
    val nodeValue: String?
    val attributes: DomAttrMap?
    val childNodes: DomNodeList
}

private external interface DomAttrMap : JsAny {
    val length: Int
}

private external interface DomNodeList : JsAny {
    val length: Int
}

private external interface DomAttr : JsAny {
    val name: String
    val value: String
}

// ── Single-expression js() helpers ───────────────────────────────────────────
// Each function is a single js() expression — the only form allowed in Wasm.

/** Parses [xmlText] with DOMParser and returns the root element, or null on error. */
private fun parseWithDomParser(xmlText: String): DomNode? = js(
    """
    (function() {
        try {
            var p = new DOMParser();
            var doc = p.parseFromString(xmlText, 'application/xml');
            if (doc.getElementsByTagName('parsererror').length > 0) return null;
            return doc.documentElement;
        } catch(e) { return null; }
    })()
    """
)

private fun attrAt(map: DomAttrMap, i: Int): DomAttr = js("map[i]")
private fun childAt(list: DomNodeList, i: Int): DomNode = js("list[i]")

// ── Public actual ─────────────────────────────────────────────────────────────

/**
 * Parses a brModelo XML file using the browser's built-in DOMParser.
 *
 * Encoding is handled in pure Kotlin before parsing:
 * - ISO-8859-1 / Latin-1: each byte maps to its Unicode code point.
 * - UTF-8 (default): standard [ByteArray.decodeToString].
 */
actual fun parseXmlBytes(bytes: ByteArray): XmlNode {
    val xmlText = decodeBytesToString(bytes)
    val root = parseWithDomParser(xmlText)
        ?: throw IllegalArgumentException("DOMParser returned a parse error.")
    return domToXmlNode(root)
        ?: throw IllegalArgumentException("Root element not found.")
}

// ── Encoding ──────────────────────────────────────────────────────────────────

private fun decodeBytesToString(bytes: ByteArray): String {
    val peek = bytes.take(200).map { (it.toInt() and 0x7F).toChar() }.joinToString("")
    val enc = Regex("""encoding=["']([^"']+)["']""")
        .find(peek)?.groupValues?.get(1)?.lowercase() ?: "utf-8"
    return if (enc == "iso-8859-1" || enc == "latin-1" || enc == "iso8859-1") {
        bytes.map { (it.toInt() and 0xFF).toChar() }.joinToString("")
    } else {
        bytes.decodeToString()
    }
}

// ── DOM traversal (pure Kotlin — no js() inside loops or conditionals) ────────

private fun domToXmlNode(node: DomNode): XmlNode? {
    if (node.nodeType != 1) return null  // ELEMENT_NODE only

    val tagName = node.nodeName

    val attrs = mutableMapOf<String, String>()
    val attrMap = node.attributes
    if (attrMap != null) {
        for (i in 0 until attrMap.length) {
            val a: DomAttr = attrAt(attrMap, i)
            attrs[a.name] = a.value
        }
    }

    val children = mutableListOf<XmlNode>()
    val text = StringBuilder()
    val kids = node.childNodes
    for (i in 0 until kids.length) {
        val child: DomNode = childAt(kids, i)
        when (child.nodeType) {
            1    -> domToXmlNode(child)?.let { children.add(it) }
            3, 4 -> text.append(child.nodeValue ?: "")  // TEXT_NODE / CDATA
        }
    }

    return XmlNode(
        name       = tagName,
        attributes = attrs,
        children   = children,
        text       = text.toString().trim(),
    )
}
