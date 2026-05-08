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

import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

/** JVM implementation: uses the built-in `javax.xml.parsers.DocumentBuilder`. */
actual fun parseXmlBytes(bytes: ByteArray): XmlNode {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    // Suppress external entity resolution (security + offline use)
    builder.setEntityResolver { _, _ -> org.xml.sax.InputSource(java.io.StringReader("")) }
    val doc = builder.parse(bytes.inputStream())
    doc.documentElement.normalize()
    return doc.documentElement.toXmlNode()
}

private fun Element.toXmlNode(): XmlNode {
    val attrs = buildMap {
        for (i in 0 until attributes.length) {
            val attr = attributes.item(i)
            put(attr.nodeName, attr.nodeValue)
        }
    }

    val childElements = mutableListOf<XmlNode>()
    val textBuilder = StringBuilder()

    for (i in 0 until childNodes.length) {
        val child = childNodes.item(i)
        when (child.nodeType) {
            Node.ELEMENT_NODE -> childElements.add((child as Element).toXmlNode())
            Node.TEXT_NODE    -> textBuilder.append(child.nodeValue)
            Node.CDATA_SECTION_NODE -> textBuilder.append(child.nodeValue)
        }
    }

    return XmlNode(
        name = tagName,
        attributes = attrs,
        children = childElements,
        text = textBuilder.toString().trim(),
    )
}
