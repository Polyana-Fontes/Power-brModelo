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

package games.polyclub.power.brmodelo

import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaXmlSerializer

internal actual suspend fun saveConceptualSchemaXml(
    schema: ConceptualSchema,
    suggestedBaseName: String,
    pickLocation: Boolean,
    explicitPath: String?,
): ConceptualSchema? {
    if (explicitPath != null) return null
    val fileStem = sanitizeBaseName(
        when {
            pickLocation -> suggestedBaseName
            schema.filePath.isNotBlank() ->
                stemFromPath(schema.filePath).trim().ifBlank { schema.name }
            else -> schema.name.ifBlank { suggestedBaseName }
        },
    )

    val updatedSchema =
        if (pickLocation) {
            schema.copy(
                name = fileStem,
                filePath = "",
                openedFromBrm = false,
            )
        } else {
            schema.copy(openedFromBrm = false)
        }

    val xml = ConceptualSchemaXmlSerializer.serialize(updatedSchema)
    triggerUtf8XmlDownload(xml, "$fileStem.xml")
    return updatedSchema
}

private fun sanitizeBaseName(raw: String): String {
    val t = raw.trim().ifBlank { "modelo" }
    return t.replace(Regex("""[\\/:*?"<>|]"""), "_")
}

private fun stemFromPath(path: String): String {
    val name = path.substringAfterLast('/').substringAfterLast('\\')
    return name.substringBeforeLast('.', name)
}

/**
 * Offers an XML file download via [Blob] + object URL (avoids large data URLs).
 */
private fun triggerUtf8XmlDownload(xmlText: String, filename: String): Unit = js(
    """
    (function(content, name) {
        var bytes = new TextEncoder().encode(content);
        var blob = new Blob([bytes], { type: 'application/xml' });
        var url = URL.createObjectURL(blob);
        var link = document.createElement('a');
        link.href = url;
        link.download = name;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    })(xmlText, filename)
    """
)
