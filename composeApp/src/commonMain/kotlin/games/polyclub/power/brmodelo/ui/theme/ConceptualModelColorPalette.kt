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

package games.polyclub.power.brmodelo.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * Colours for the conceptual schema **canvas**: grid, diagram drawing, rubber-band overlays,
 * export bitmap background, and in-canvas UI (balloon, empty-state) that are tied to the model view.
 *
 * Provide [LocalConceptualModelColorPalette] at the boundary where the canvas is composed (e.g. per editor tab)
 * so each tab can switch diagram theming independently from application chrome.
 * Use [Companion.light] and [Companion.dark] as defaults when persisting per-model theme later.
 */
data class ConceptualModelColorPalette(
    /** Main canvas area behind the diagram ([SchemaCanvas]). */
    val canvasBackground: Color,
    /** Dot grid on the canvas. */
    val gridDot: Color,
    /** Rectangle multi-select band in **view** coordinates ([SchemaCanvas] overlay). */
    val viewRubberBandSelectionFill: Color,
    val viewRubberBandSelectionStroke: Color,
    /** Bulk-delete rubber band in **view** coordinates. */
    val viewRubberBandBulkDeleteFill: Color,
    val viewRubberBandBulkDeleteStroke: Color,
    /** Full-surface tint when a file is dragged over the canvas drop target. */
    val fileDropOverlayFill: Color,
    val fileDropOverlayBorder: Color,
    val fileDropOverlayPrompt: Color,
    /** Placeholder text when no schema is open. */
    val emptySchemaMessage: Color,
    /** Card behind the hidden-attributes balloon. */
    val hiddenAttributeBalloonCard: Color,
    val hiddenAttributeBalloonText: Color,
    /** Default fill for entity / relationship bodies ([SchemaRenderer]). */
    val diagramEntityFill: Color,
    /** Default ink for lines and primary diagram text. */
    val diagramPrimaryInk: Color,
    val diagramEntityShadowDark: Color,
    val diagramEntityShadowLight: Color,
    val diagramIdentifierAttributeFill: Color,
    val diagramSpecializationPartialInk: Color,
    val diagramAnnotationFrameInk: Color,
    val diagramCompositeIndicatorInk: Color,
    /** Selection outline, handles, and highlighted connection polylines. */
    val diagramSelectionAccent: Color,
    /** Scrim over the associative inner diamond when the outer body is link-hover highlighted. */
    val diagramAssociativeInnerMuteScrim: Color,
    /** Link / attribute / specialization tool hover and first-pick highlight. */
    val diagramToolTargetOrange: Color,
    /** Red threat tint on elements during bulk-delete preview ([SchemaRenderer] element overlay). */
    val diagramElementBulkDeleteThreatFill: Color,
    val diagramElementBulkDeleteThreatStroke: Color,
    /** Blue preview tint on elements during rectangle multi-select ([SchemaRenderer] element overlay). */
    val diagramElementSelectionBandFill: Color,
    val diagramElementSelectionBandStroke: Color,
    /** Background when raster export requests an opaque bitmap ([SchemaExporter]). */
    val exportCanvasBackground: Color,
) {
    /** Same as [diagramSelectionAccent]; used for connection line highlight where the code kept a separate name. */
    val selectionConnectionHighlight: Color get() = diagramSelectionAccent

    /** Base text style for on-diagram labels (font size matches legacy `mer.pas` canvas text). */
    fun canvasLabelBaseTextStyle(): TextStyle =
        TextStyle(fontSize = 11.sp, color = diagramPrimaryInk)

    companion object {
        /** Default light palette matching the previous hard-coded values. */
        fun light(): ConceptualModelColorPalette = ConceptualModelColorPalette(
            canvasBackground = Color(0xFFE8E8E8),
            gridDot = Color(0xFFCCCCCC),
            viewRubberBandSelectionFill = Color(0x402E7DFF),
            viewRubberBandSelectionStroke = Color(0xFF0060C0),
            viewRubberBandBulkDeleteFill = Color(0x40FF3B3B),
            viewRubberBandBulkDeleteStroke = Color(0xFFCC0000),
            fileDropOverlayFill = Color(0x882C7BE8),
            fileDropOverlayBorder = Color(0xFF1E5CC7),
            fileDropOverlayPrompt = Color.White,
            emptySchemaMessage = Color(0xFF888888),
            hiddenAttributeBalloonCard = Color(0xFFFFF9C4),
            hiddenAttributeBalloonText = Color(0xFF1A1A1A),
            diagramEntityFill = Color.White,
            diagramPrimaryInk = Color.Black,
            diagramEntityShadowDark = Color(0xFF707070),
            diagramEntityShadowLight = Color(0xFFB3B3B3),
            diagramIdentifierAttributeFill = Color(0xFF963636),
            diagramSpecializationPartialInk = Color(0xFF008080),
            diagramAnnotationFrameInk = Color(0xFF363636),
            diagramCompositeIndicatorInk = Color.Blue,
            diagramSelectionAccent = Color(0xFF0060C0),
            diagramAssociativeInnerMuteScrim = Color.White.copy(alpha = 0.78f),
            diagramToolTargetOrange = Color(0xFFFF6600),
            diagramElementBulkDeleteThreatFill = Color(0x66FF2D2D),
            diagramElementBulkDeleteThreatStroke = Color(0xFFAA0000),
            diagramElementSelectionBandFill = Color(0x662E7DFF),
            diagramElementSelectionBandStroke = Color(0xFF0060C0),
            exportCanvasBackground = Color(0xFFE8E8E8),
        )

        /**
         * Dark diagram palette: light ink on dark canvas, adjusted semantic hues, and scrims that read on dark fills.
         */
        fun dark(): ConceptualModelColorPalette = ConceptualModelColorPalette(
            canvasBackground = Color(0xFF1E1E22),
            gridDot = Color(0xFF404048),
            viewRubberBandSelectionFill = Color(0x404DA3FF),
            viewRubberBandSelectionStroke = Color(0xFF6BA3FF),
            viewRubberBandBulkDeleteFill = Color(0x40FF5555),
            viewRubberBandBulkDeleteStroke = Color(0xFFFF6B6B),
            fileDropOverlayFill = Color(0x884A6FE8),
            fileDropOverlayBorder = Color(0xFF7B9DFF),
            fileDropOverlayPrompt = Color.White,
            emptySchemaMessage = Color(0xFF707078),
            hiddenAttributeBalloonCard = Color(0xFF3D3830),
            hiddenAttributeBalloonText = Color(0xFFE8E4DC),
            diagramEntityFill = Color(0xFF2A2A32),
            diagramPrimaryInk = Color(0xFFE8E8EC),
            diagramEntityShadowDark = Color(0xFF101014),
            diagramEntityShadowLight = Color(0xFF484850),
            diagramIdentifierAttributeFill = Color(0xFFB84D4D),
            diagramSpecializationPartialInk = Color(0xFF4DD4D4),
            diagramAnnotationFrameInk = Color(0xFFB0B0B8),
            diagramCompositeIndicatorInk = Color(0xFF6BA3FF),
            diagramSelectionAccent = Color(0xFF4DA3FF),
            diagramAssociativeInnerMuteScrim = Color.Black.copy(alpha = 0.78f),
            diagramToolTargetOrange = Color(0xFFFF8833),
            diagramElementBulkDeleteThreatFill = Color(0x66FF5555),
            diagramElementBulkDeleteThreatStroke = Color(0xFFFF5555),
            diagramElementSelectionBandFill = Color(0x664DA3FF),
            diagramElementSelectionBandStroke = Color(0xFF4DA3FF),
            exportCanvasBackground = Color(0xFF1E1E22),
        )
    }
}

val LocalConceptualModelColorPalette =
    staticCompositionLocalOf { ConceptualModelColorPalette.light() }
