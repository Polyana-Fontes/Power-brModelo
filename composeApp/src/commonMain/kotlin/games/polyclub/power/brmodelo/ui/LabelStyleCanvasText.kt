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
import androidx.compose.ui.unit.sp
import games.polyclub.power.brmodelo.domain.LabelStyle

/** Default canvas label size (sp) when [LabelStyle.fontSizePoints] is unset — matches legacy Kotlin canvas. */
internal const val CANVAS_LABEL_FALLBACK_FONT_SP = 11f

/**
 * Builds a [TextStyle] for diagram labels from [LabelStyle] and a [base] (typically [CANVAS_TEXT_STYLE]).
 *
 * [LabelStyle.fontFamilyName] maps to a platform-specific [androidx.compose.ui.text.font.FontFamily].
 * [LabelStyle.fontSizePoints] maps to the same integer in **sp** (MER stores point size like Pascal `TFont.Size`).
 */
internal fun LabelStyle.mergeOntoCanvasTextStyle(base: TextStyle): TextStyle {
    val sizeSp = (fontSizePoints ?: CANVAS_LABEL_FALLBACK_FONT_SP.toInt()).coerceIn(4, 144).sp
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
    )
}
