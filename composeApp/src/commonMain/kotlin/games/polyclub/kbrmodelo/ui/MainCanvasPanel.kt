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

package games.polyclub.kbrmodelo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import games.polyclub.kbrmodelo.domain.CanvasSelection
import games.polyclub.kbrmodelo.domain.ConceptualSchema
import games.polyclub.kbrmodelo.ui.canvas.SchemaCanvas
import games.polyclub.kbrmodelo.ui.components.CHROMIUM_TAB_ACTIVE_HEIGHT
import games.polyclub.kbrmodelo.ui.components.CHROMIUM_TAB_STRIP_HEIGHT
import games.polyclub.kbrmodelo.ui.components.ChromiumTab
import games.polyclub.kbrmodelo.ui.components.ChromiumTabShape
import kbrmodelo.composeapp.generated.resources.Res
import kbrmodelo.composeapp.generated.resources.modelo_conceitual_2s
import org.jetbrains.compose.resources.painterResource

private val DRAG_OVERLAY_BG     = Color(0x882C7BE8)
private val DRAG_OVERLAY_BORDER = Color(0xFF1E5CC7)

// Canvas tab strip colours — neutral gray to avoid blue tint
private val CANVAS_STRIP_BG        = Color(0xFFD7D7D7)  // same as canvas panel background (no dividing line)
private val CANVAS_TAB_ACTIVE_BG   = Color(0xFFEEEEEE)  // slightly lighter than strip for active tab contrast
private val CANVAS_TAB_INACTIVE_BG = Color(0xFFC8C8C8)  // slightly darker for inactive
private val CANVAS_STRIP_BORDER    = Color(0xFF888888)

@Composable
internal fun MainCanvasPanel(
    schema: ConceptualSchema? = null,
    hasUnsavedChanges: Boolean = false,
    selection: CanvasSelection = CanvasSelection.None,
    isDragOver: Boolean = false,
    onDragStateChange: (Boolean) -> Unit = {},
    onFileDrop: (PickedFile) -> Unit = {},
    onSelectionChange: (CanvasSelection) -> Unit = {},
    onSchemaPreview: (ConceptualSchema) -> Unit = {},
    onSchemaCommit: (ConceptualSchema) -> Unit = {},
    onCloseTab: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFFD7D7D7))
    ) {
        CanvasTabStrip(
            schema = schema,
            showDirtySuffix = hasUnsavedChanges,
            onClose = if (schema != null) onCloseTab else null,
        )

        // The fileDragDropTarget modifier must be on this outer Box — NOT on SchemaCanvas.
        // If it were on SchemaCanvas, the overlay Box appearing on top of it would cause the
        // Compose DnD system to report onExited (cursor is now over the overlay, not the canvas),
        // collapsing isDragOver back to false immediately and creating an infinite flicker loop.
        Box(
            modifier = Modifier
                .fillMaxSize()
                // No top padding so the canvas border touches the tab strip directly.
                .padding(start = 2.dp, end = 2.dp, bottom = 2.dp)
                .border(
                    width = if (isDragOver) 3.dp else 1.dp,
                    color = if (isDragOver) DRAG_OVERLAY_BORDER else Color(0xFF7A7A7A),
                )
                .fileDragDropTarget(
                    onDragStateChange = onDragStateChange,
                    onFileDrop = onFileDrop,
                ),
        ) {
            SchemaCanvas(
                schema = schema,
                selection = selection,
                onSelectionChange = onSelectionChange,
                onSchemaPreview = onSchemaPreview,
                onSchemaCommit = onSchemaCommit,
                modifier = Modifier.fillMaxSize(),
            )

            if (isDragOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DRAG_OVERLAY_BG),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Solte o arquivo para abrir o modelo",
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasTabStrip(
    schema: ConceptualSchema?,
    showDirtySuffix: Boolean = false,
    onClose: (() -> Unit)? = null,
) {
    val baseLabel = schema?.name?.takeIf { it.isNotBlank() } ?: "Sem título"
    val tabLabel = if (showDirtySuffix) "$baseLabel*" else baseLabel
    val modelIcon = painterResource(Res.drawable.modelo_conceitual_2s)

    val density = LocalDensity.current
    val topCornerPx   = with(density) { 5.dp.toPx() }
    val bottomCurvePx = with(density) { 4.dp.toPx() }
    val tabShape = remember(topCornerPx, bottomCurvePx) {
        ChromiumTabShape(topCornerRadius = topCornerPx, bottomCurveRadius = bottomCurvePx)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHROMIUM_TAB_STRIP_HEIGHT)
            .background(CANVAS_STRIP_BG),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                // Left/right insets give the tab's bottom-outward curves space so
                // they don't overflow outside the strip bounds.
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            ChromiumTab(
                label = tabLabel,
                selected = true,
                tabShape = tabShape,
                activeTabBg = CANVAS_TAB_ACTIVE_BG,
                inactiveTabBg = CANVAS_TAB_INACTIVE_BG,
                borderColor = CANVAS_STRIP_BORDER,
                leadingIcon = modelIcon,
                onClose = onClose,
                // Max width matches Chromium's tab cap; no weight so the tab doesn't
                // expand to fill the entire strip.
                modifier = Modifier
                    .widthIn(min = 60.dp, max = 240.dp)
                    .height(CHROMIUM_TAB_ACTIVE_HEIGHT)
                    .zIndex(2f),
                onClick = {},
            )
        }
    }
}
