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

package games.polyclub.power.brmodelo.ui.components.ribbon

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val RibbonIconInk = Color(0xFF2C3E50)
private val RibbonIconAccent = Color(0xFF3498DB)
private val RibbonIconMuted = Color(0xFF95A5A6)

/** Hub-and-spokes motif suggesting an MCP-style tool server (vector paths, no raster assets). */
@Composable
internal fun RibbonMcpHubIcon(modifier: Modifier, enabled: Boolean = true) {
    val stroke = RibbonIconInk.copy(alpha = if (enabled) 1f else 0.45f)
    val hubFill = RibbonIconAccent.copy(alpha = if (enabled) 0.35f else 0.12f)
    val spoke = RibbonIconMuted.copy(alpha = if (enabled) 0.9f else 0.35f)
    Canvas(modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val hubR = size.minDimension * 0.14f
        val nodeR = size.minDimension * 0.09f
        val orbit = size.minDimension * 0.36f
        for (i in 0 until 4) {
            val angle = PI * 0.5 * i - PI * 0.25
            val nx = cx + (cos(angle) * orbit).toFloat()
            val ny = cy + (sin(angle) * orbit).toFloat()
            drawLine(
                color = spoke,
                strokeWidth = size.minDimension * 0.055f,
                start = Offset(cx, cy),
                end = Offset(nx, ny),
            )
            drawCircle(color = hubFill, radius = nodeR, center = Offset(nx, ny))
            drawCircle(color = stroke, radius = nodeR, center = Offset(nx, ny), style = Stroke(size.minDimension * 0.06f))
        }
        drawCircle(color = hubFill, radius = hubR * 1.1f, center = Offset(cx, cy))
        drawCircle(color = stroke, radius = hubR, center = Offset(cx, cy), style = Stroke(size.minDimension * 0.07f))
    }
}

@Composable
internal fun RibbonPlayGlyphIcon(modifier: Modifier, enabled: Boolean = true) {
    val fill = RibbonIconInk.copy(alpha = if (enabled) 1f else 0.4f)
    Canvas(modifier) {
        val pad = size.minDimension * 0.22f
        val path = Path().apply {
            moveTo(pad, pad * 0.65f)
            lineTo(size.width - pad * 0.85f, size.height * 0.5f)
            lineTo(pad, size.height - pad * 0.65f)
            close()
        }
        drawPath(path, color = fill)
    }
}

@Composable
internal fun RibbonStopGlyphIcon(modifier: Modifier, enabled: Boolean = true) {
    val fill = Color(0xFFC0392B).copy(alpha = if (enabled) 1f else 0.4f)
    Canvas(modifier) {
        val pad = size.minDimension * 0.28f
        drawRoundRect(
            color = fill,
            topLeft = Offset(pad, pad),
            size = Size(size.width - 2 * pad, size.height - 2 * pad),
            cornerRadius = CornerRadius(pad * 0.35f, pad * 0.35f),
        )
    }
}
