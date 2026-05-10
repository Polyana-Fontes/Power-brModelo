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

package games.polyclub.power.brmodelo.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool
import org.jetbrains.compose.resources.ExperimentalResourceApi

// All js() calls in Kotlin/Wasm must be the single expression of a top-level function.

private fun wasmSetBodyCursorCss(css: String): Unit = js("document.body.style.cursor = css")

private fun wasmClearBodyCursorCss(): Unit = js("document.body.style.cursor = ''")

@OptIn(ExperimentalResourceApi::class)
@Composable
internal actual fun rememberConceptualCanvasToolCursorModifier(tool: ConceptualCanvasTool): Modifier {
    val path = when (tool) {
        is ConceptualCanvasTool.Entity.Plain ->
            "files/brmodelo_cursors/png/cursor_entidade.png"
        is ConceptualCanvasTool.Entity.Relation ->
            "files/brmodelo_cursors/png/cursor_relacao.png"
        is ConceptualCanvasTool.Entity.Associative ->
            "files/brmodelo_cursors/png/cursor_entassoss.png"
        is ConceptualCanvasTool.Observation ->
            "files/brmodelo_cursors/png/cursor_textoii.png"
        is ConceptualCanvasTool.LinkObjects.AwaitingFirst ->
            "files/brmodelo_cursors/png/cursor_ligacao.png"
        is ConceptualCanvasTool.LinkObjects.AwaitingSecond ->
            "files/brmodelo_cursors/png/cursor_ligacao2.png"
        else -> null
    }
    if (path == null) {
        SideEffect { wasmClearBodyCursorCss() }
        return Modifier
    }

    var pointerInside by remember { mutableStateOf(false) }
    val cursorCss = remember(path) {
        val uri = Res.getUri(path)
        "url(\"$uri\") 0 0, auto"
    }

    val toolState by rememberUpdatedState(tool)
    val cursorCssState by rememberUpdatedState(cursorCss)
    val inside = pointerInside

    SideEffect {
        when (toolState) {
            is ConceptualCanvasTool.None -> wasmClearBodyCursorCss()
            else ->
                if (!inside) {
                    wasmClearBodyCursorCss()
                } else {
                    wasmSetBodyCursorCss(cursorCssState)
                }
        }
    }

    return Modifier.pointerInput(tool) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Enter -> pointerInside = true
                    PointerEventType.Exit -> pointerInside = false
                    else -> {}
                }
            }
        }
    }
}
