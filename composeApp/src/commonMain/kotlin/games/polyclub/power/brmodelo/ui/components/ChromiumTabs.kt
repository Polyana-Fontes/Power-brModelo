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

package games.polyclub.power.brmodelo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Dimensions ────────────────────────────────────────────────────────────────

internal val CHROMIUM_TAB_STRIP_HEIGHT    = 26.dp
internal val CHROMIUM_TAB_ACTIVE_HEIGHT   = 26.dp
// Inactive tabs are 3dp shorter than active so they appear "behind" without being too sunken.
internal val CHROMIUM_TAB_INACTIVE_HEIGHT = 23.dp

private val TOP_CORNER_RADIUS   = 5.dp
private val BOTTOM_CURVE_RADIUS = 4.dp

// ── Shape ─────────────────────────────────────────────────────────────────────

/**
 * Custom shape that mimics Chromium browser tabs: rounded top corners and
 * outward-curving bottom corners that visually "rise" from the tab bar.
 *
 * Inspired by https://medium.com/@kappdev/how-to-create-chrome-inspired-custom-tabs-in-jetpack-compose-c4cb8aa33e91
 *
 * @param topCornerRadius    Radius of the rounded top-left and top-right corners (px).
 * @param bottomCurveRadius  Radius of the outward curve at the bottom corners (px).
 */
internal class ChromiumTabShape(
    private val topCornerRadius: Float,
    private val bottomCurveRadius: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val tr = topCornerRadius
        val br = bottomCurveRadius
        val w  = size.width
        val h  = size.height
        val path = Path().apply {
            moveTo(-br, h)
            quadraticTo(0f, h, 0f, h - br)
            lineTo(0f, tr)
            quadraticTo(0f, 0f, tr, 0f)
            lineTo(w - tr, 0f)
            quadraticTo(w, 0f, w, tr)
            lineTo(w, h - br)
            quadraticTo(w, h, w + br, h)
            close()
        }
        return Outline.Generic(path)
    }
}

// ── Tab composable ────────────────────────────────────────────────────────────

/**
 * A single Chromium-style tab.
 *
 * @param label         Text label shown inside the tab.
 * @param selected      Whether the tab is the active one.
 * @param tabShape      Pre-built [games.polyclub.power.brmodelo.ui.components.ChromiumTabShape] (pass a remembered instance).
 * @param activeTabBg   Background color for the active/selected state.
 * @param inactiveTabBg Background color for the inactive state.
 * @param borderColor   Color of the 3-sided border outline.
 * @param leadingIcon   Optional icon painter displayed to the left of the label.
 * @param onClose       When non-null, a × close button is shown at the right edge of the tab.
 * @param modifier      Modifier applied to the tab root box.
 * @param onClick       Callback invoked when the tab label area is clicked.
 */
@Composable
internal fun ChromiumTab(
    label: String,
    selected: Boolean,
    tabShape: Shape,
    activeTabBg: Color,
    inactiveTabBg: Color,
    borderColor: Color,
    leadingIcon: Painter? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bgColor    = if (selected) activeTabBg else inactiveTabBg
    val textColor  = if (selected) Color(0xFF1B2B3B) else Color(0xFF55667A)
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal

    Box(
        modifier = modifier
            .background(color = bgColor, shape = tabShape)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val tr = TOP_CORNER_RADIUS.toPx()
                val br = BOTTOM_CURVE_RADIUS.toPx()
                val w  = size.width
                val h  = size.height
                drawPath(
                    path = Path().apply {
                        moveTo(-br, h)
                        quadraticTo(0f, h, 0f, h - br)
                        lineTo(0f, tr)
                        quadraticTo(0f, 0f, tr, 0f)
                        lineTo(w - tr, 0f)
                        quadraticTo(w, 0f, w, tr)
                        lineTo(w, h - br)
                        quadraticTo(w, h, w + br, h)
                    },
                    color = borderColor,
                    style = Stroke(width = stroke),
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        // Icon + label — fillMaxHeight() fills the Box's constrained height (26dp) so
        // verticalAlignment = CenterVertically works, while the width comes from the
        // natural content size (icon + text) and drives the Box's measured width.
        // Using matchParentSize() here would collapse the Box to its minimum because
        // there would be no non-matchParentSize child to determine the width.
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    start = 12.dp,
                    // Reserve room on the right side for the close button when present.
                    end = if (onClose != null) 28.dp else 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIcon != null) {
                Image(
                    painter = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = label,
                // Set lineHeight = fontSize to strip Compose's default extra vertical font
                // padding, which would otherwise make the text appear lower than the icon.
                style = TextStyle(
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight = fontWeight,
                    color = textColor,
                ),
                maxLines = 1,
            )
        }

        // Close button — shown only when onClose is provided.
        if (onClose != null) {
            TabCloseButton(
                color = textColor,
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            )
        }
    }
}

// ── Close button ─────────────────────────────────────────────────────────────

@Composable
private fun TabCloseButton(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hovered by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(if (hovered) Color(0x30000000) else Color.Transparent)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        hovered = event.type == PointerEventType.Enter ||
                            event.type == PointerEventType.Move
                        if (event.type == PointerEventType.Exit) hovered = false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "×",
            style = TextStyle(
                fontSize = 13.sp,
                lineHeight = 13.sp,
                color = color,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
