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

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Used when Local Font Access API is missing, denied, or fails. */
private val WASM_FONT_FAMILY_FALLBACK: List<String> = listOf(
    "Tahoma",
    "Arial",
    "Helvetica",
    "Verdana",
    "Segoe UI",
    "Calibri",
    "Cambria",
    "Times New Roman",
    "Times",
    "Georgia",
    "Garamond",
    "Palatino Linotype",
    "Book Antiqua",
    "Courier New",
    "Courier",
    "Consolas",
    "Lucida Console",
    "Monaco",
    "Impact",
    "Comic Sans MS",
    "Trebuchet MS",
    "Franklin Gothic Medium",
    "Arial Black",
    "Lucida Sans Unicode",
    "MS Sans Serif",
    "MS Serif",
)

/**
 * Enumerates local font families via `window.queryLocalFonts()` when supported
 * (Local Font Access API). Requires a user permission grant in supporting browsers.
 */
internal actual suspend fun platformLabelFontFamilyNames(): List<String> =
    suspendCancellableCoroutine { cont ->
        queryLocalFontsJs(
            onSuccess = { payload ->
                val parsed = payload.split('\u0001').map { it.trim() }.filter { it.isNotEmpty() }
                if (cont.isActive) {
                    cont.resume(parsed.ifEmpty { WASM_FONT_FAMILY_FALLBACK })
                }
            },
            onFailure = {
                if (cont.isActive) {
                    cont.resume(WASM_FONT_FAMILY_FALLBACK)
                }
            },
        )
    }

/**
 * Calls `window.queryLocalFonts()`, deduplicates by [FontData.family], sorts, and delivers
 * family names joined with U+0001. Invokes [onFailure] if the API is missing or the promise rejects.
 */
private fun queryLocalFontsJs(onSuccess: (String) -> Unit, onFailure: () -> Unit): Unit = js(
    """
    (function(ok, fail) {
        if (typeof window.queryLocalFonts !== 'function') { fail(); return; }
        window.queryLocalFonts().then(function(fonts) {
            var seen = {};
            var out = [];
            for (var i = 0; i < fonts.length; i++) {
                var fam = fonts[i].family;
                if (fam && !seen[fam]) { seen[fam] = true; out.push(fam); }
            }
            out.sort(function(a, b) { return a.localeCompare(b); });
            ok(out.join('\u0001'));
        }).catch(function() { fail(); });
    })(onSuccess, onFailure)
    """
)
