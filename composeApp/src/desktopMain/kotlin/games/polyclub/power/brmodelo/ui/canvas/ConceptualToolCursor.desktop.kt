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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool
import java.awt.Point
import java.awt.Toolkit
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
internal actual fun rememberConceptualCanvasToolCursorModifier(tool: ConceptualCanvasTool): Modifier {
    // Key by tool so the previous AWT cursor is dropped immediately when switching variants
    // (otherwise the old PointerIcon stays until the new async load finishes).
    var icon by remember(tool) { mutableStateOf<PointerIcon?>(null) }
    LaunchedEffect(tool) {
        icon = loadAwtPointerIcon(tool)
    }
    return icon?.let { Modifier.pointerHoverIcon(it) } ?: Modifier
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadAwtPointerIcon(tool: ConceptualCanvasTool): PointerIcon? {
    val path = when (tool) {
        is ConceptualCanvasTool.Entity.Plain ->
            "files/brmodelo_cursors/png/cursor_entidade.png"
        is ConceptualCanvasTool.Entity.Relation ->
            "files/brmodelo_cursors/png/cursor_relacao.png"
        is ConceptualCanvasTool.Entity.Associative ->
            "files/brmodelo_cursors/png/cursor_entassoss.png"
        is ConceptualCanvasTool.Observation ->
            "files/brmodelo_cursors/png/cursor_textoii.png"
        else -> return null
    }
    return withContext(Dispatchers.IO) {
        runCatching {
            val bytes = Res.readBytes(path)
            val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@runCatching null
            val tk = Toolkit.getDefaultToolkit()
            val cursor = tk.createCustomCursor(image, Point(0, 0), "brmodelo_entity_tool")
            PointerIcon(cursor)
        }.getOrNull()
    }
}
