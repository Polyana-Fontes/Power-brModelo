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

package games.polyclub.power.brmodelo.ui

import java.awt.GraphicsEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Merges logical family names with families reported by every registered [java.awt.Font] face.
 * On some platforms [GraphicsEnvironment.availableFontFamilyNames] omits faces that still appear
 * in [GraphicsEnvironment.getAllFonts]; combining both yields a list closer to the OS font picker.
 */
internal actual suspend fun platformLabelFontFamilyNames(): List<String> =
    withContext(Dispatchers.Default) {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val names = LinkedHashSet<String>()
        for (n in ge.availableFontFamilyNames) {
            val t = n.trim()
            if (t.isNotEmpty()) names.add(t)
        }
        for (f in ge.allFonts) {
            val t = f.family.trim()
            if (t.isNotEmpty()) names.add(t)
        }
        names.sorted()
    }
