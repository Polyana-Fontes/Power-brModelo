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

package games.polyclub.power.brmodelo.mcp

import games.polyclub.power.brmodelo.domain.ConceptualSchema
import java.io.File

/**
 * Normalizes an MCP-supplied absolute path for conceptual MER XML export.
 * Appends `.xml` when the final name has no extension.
 */
internal fun normalizeAbsoluteXmlSavePath(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val file = File(trimmed)
    if (!file.isAbsolute) return null
    return if (!file.name.lowercase().endsWith(".xml")) {
        File(file.parentFile ?: File("."), "${file.name}.xml").absolutePath
    } else {
        file.absolutePath
    }
}

/** Disk path already associated with the tab, when safe to overwrite without Save-As. */
internal fun knownConceptualSchemaDiskPath(schema: ConceptualSchema): String? {
    val path = schema.filePath.trim()
    if (path.isEmpty() || schema.openedFromBrm) return null
    return path
}
