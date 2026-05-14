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

import games.polyclub.power.brmodelo.ui.PickedFile
import java.io.File

internal actual fun tryLoadPickedFileFromAbsolutePath(path: String): PickedFile? {
    val t = path.trim()
    if (t.isEmpty()) return null
    val f = File(t)
    if (!f.isFile || !f.canRead()) return null
    return try {
        PickedFile(
            name = f.nameWithoutExtension.ifBlank { "modelo" },
            bytes = f.readBytes(),
            diskPath = f.absolutePath,
        )
    } catch (_: Exception) {
        null
    }
}
