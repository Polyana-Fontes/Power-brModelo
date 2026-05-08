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

package games.polyclub.kbrmodelo.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.polyclub.kbrmodelo.domain.ConceptualSchema

// Background colour of the canvas (light grey, matching the original brModelo canvas background)
private val CANVAS_BG = Color(0xFFE8E8E8)
// Dot-grid colour (subtle)
private val GRID_DOT = Color(0xFFCCCCCC)
private const val GRID_STEP = 20f

/**
 * Interactive canvas that renders a [ConceptualSchema] using Compose [Canvas].
 *
 * Supports pan (drag to scroll) and replicates the rendering logic of the original
 * Pascal brModelo via [drawSchema]. When [schema] is null an empty canvas with a
 * placeholder message is shown.
 *
 * @param schema          The model to render, or null for an empty canvas.
 * @param modifier        Layout modifier applied to the outer Box.
 */
@Composable
internal fun SchemaCanvas(
    schema: ConceptualSchema?,
    modifier: Modifier = Modifier,
) {
    var panOffset by remember { mutableStateOf(Offset(8f, 8f)) }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .background(CANVAS_BG)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    panOffset += dragAmount
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Optional dot grid (subtle background reference grid)
            val cols = (size.width / GRID_STEP).toInt() + 2
            val rows = (size.height / GRID_STEP).toInt() + 2
            val offsetX = panOffset.x % GRID_STEP
            val offsetY = panOffset.y % GRID_STEP
            for (col in 0..cols) {
                for (row in 0..rows) {
                    drawCircle(
                        GRID_DOT,
                        radius = 1f,
                        center = Offset(offsetX + col * GRID_STEP, offsetY + row * GRID_STEP),
                    )
                }
            }

            if (schema != null) {
                // Translate the drawing context so (0,0) of the schema maps to panOffset
                translate(panOffset.x, panOffset.y) {
                    drawSchema(schema, textMeasurer)
                }
            }
        }

        if (schema == null) {
            Text(
                text = "Abra um arquivo para visualizar o modelo",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        }
    }
}
