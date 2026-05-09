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

import games.polyclub.power.brmodelo.domain.xml.appendXmlNode

/**
 * Parses raw XML bytes (which may be encoded as ISO-8859-1 or UTF-8 per the
 * XML declaration) into an [games.polyclub.power.brmodelo.domain.xml.XmlNode] tree.
 *
 * The encoding specified in the `<?xml ... encoding="...">` declaration is
 * respected by the platform implementation.
 */
expect fun parseXmlBytes(bytes: ByteArray): XmlNode

/**
 * Serializes an [games.polyclub.power.brmodelo.domain.xml.XmlNode] tree to an XML string.
 *
 * The output always uses UTF-8 encoding with the standard XML declaration:
 * `<?xml version="1.0" encoding="UTF-8"?>`.
 */
fun serializeXml(root: XmlNode): String = buildString {
    appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
    appendXmlNode(root, indent = 0)
}

// ── Private serialization helpers ────────────────────────────────────────────

private fun StringBuilder.appendXmlNode(node: XmlNode, indent: Int) {
    val tab = "\t".repeat(indent)
    val attrStr = node.attributes.entries.joinToString(" ") { (k, v) ->
        "$k=\"${escapeXml(v)}\""
    }
    val openTag = if (attrStr.isEmpty()) "<${node.name}>" else "<${node.name} $attrStr>"

    when {
        node.children.isEmpty() && node.text.isEmpty() -> {
            // Self-closing: <Tag attr="v"/>
            val selfClose = if (attrStr.isEmpty()) "<${node.name}/>" else "<${node.name} $attrStr/>"
            append(tab)
            appendLine(selfClose)
        }
        node.children.isEmpty() -> {
            // Inline text: <Tag>text</Tag>
            append(tab)
            append(openTag)
            append(escapeXml(node.text))
            appendLine("</${node.name}>")
        }
        else -> {
            // Block with children
            append(tab)
            appendLine(openTag)
            node.children.forEach { child -> appendXmlNode(child, indent + 1) }
            append(tab)
            appendLine("</${node.name}>")
        }
    }
}

private fun escapeXml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
