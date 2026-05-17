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

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import games.polyclub.power.brmodelo.domain.LabelStyle
import kotlin.math.roundToInt

/** Default canvas label size (sp) when [LabelStyle.fontSizePoints] is unset — matches legacy Kotlin canvas. */
internal const val CANVAS_LABEL_FALLBACK_FONT_SP = 11f

/**
 * Pascal / VCL builds fonts with `MulDiv(PointSize, LOGPIXELSY, 72)` (see `mer.pas` / Windows `CreateFont`).
 * At the traditional **96 DPI** reference, one stored **point** corresponds to **96/72** “logical px” of height
 * before hinting — using the same factor for Compose **`sp`** makes diagram text match brModelo 3.x more closely
 * than mapping `pt` → `sp` 1:1 (which rendered noticeably smaller while geometry stayed correct at 100% zoom).
 */
private const val MER_POINTS_TO_COMPOSE_SP_SCALE = 96f / 72f

/**
 * Negative [androidx.compose.ui.text.TextStyle.letterSpacing] in **em** (fraction of font size) so diagram
 * labels use slightly less horizontal width while keeping the same [fontSize] — closer to legacy VCL kerning feel.
 */
private const val MER_CANVAS_LETTER_TRACKING_EM = -0.0475f

/**
 * Converts MER / Pascal [LabelStyle.fontSizePoints] to an integer used as Compose **`N.sp`** on the canvas.
 * [fontSizePoints] null → legacy Kotlin default (unscaled); non-null → scaled by [MER_POINTS_TO_COMPOSE_SP_SCALE].
 */
internal fun merFontSizePointsToCanvasSpInt(fontSizePoints: Int?): Int =
    when (fontSizePoints) {
        null -> CANVAS_LABEL_FALLBACK_FONT_SP.toInt().coerceIn(4, 144)
        else -> (fontSizePoints * MER_POINTS_TO_COMPOSE_SP_SCALE).roundToInt().coerceIn(4, 144)
    }

/**
 * Builds a [TextStyle] for diagram labels from [LabelStyle] and a [base] (typically [CANVAS_TEXT_STYLE]).
 *
 * [LabelStyle.fontFamilyName] maps to a platform-specific [androidx.compose.ui.text.font.FontFamily].
 * [LabelStyle.fontSizePoints] is stored like Pascal `TFont.Size` / MER `FonteTamanho` (points); rendering uses
 * [merFontSizePointsToCanvasSpInt] so **on-screen** size tracks legacy VCL ~96dpi behaviour.
 * A small negative [androidx.compose.ui.text.TextStyle.letterSpacing] in **em** tightens horizontal extent
 * without changing body size.
 */
internal fun LabelStyle.mergeOntoCanvasTextStyle(base: TextStyle): TextStyle {
    val sizeSp = merFontSizePointsToCanvasSpInt(fontSizePoints).sp
    val family = fontFamilyName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { name ->
            try {
                platformFontFamilyFromLogicalName(name)
            } catch (_: Throwable) {
                null
            }
        }
    val fg = color?.let { vclColorRefToCompose(it) } ?: base.color
    val decoration = when {
        underline && strikeThrough ->
            TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        underline -> TextDecoration.Underline
        strikeThrough -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }
    return base.copy(
        fontFamily = family ?: base.fontFamily,
        fontSize = sizeSp,
        fontWeight = if (bold) FontWeight.Black else base.fontWeight,
        fontStyle = if (italic) FontStyle.Italic else base.fontStyle,
        color = fg,
        textDecoration = decoration,
        letterSpacing = MER_CANVAS_LETTER_TRACKING_EM.em,
    )
}
