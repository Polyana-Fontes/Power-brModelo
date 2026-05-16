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

import androidx.compose.ui.graphics.Color

/**
 * Converts a Delphi **TColor** / Windows COLORREF integer to a Compose [Color].
 *
 * - Absolute colours: lower 24 bits are BGR (`0x00BBGGRR`), matching [colorRefBgrToCompose].
 * - System colours: high byte `0x80` and index in the low byte (Win32 `COLOR_*`); mapped to a
 *   fixed light-theme approximation (Wine `sysparams.c` defaults, plus Lazarus `COLOR_FORM`).
 */
internal fun vclColorRefToCompose(colorRef: Int): Color {
    val u = colorRef.toLong() and 0xFFFFFFFFL
    return if ((u and 0xFF000000L) == 0x80000000L) {
        val idx = (u and 0xFFL).toInt().coerceIn(0, 31)
        colorRefBgrToCompose(defaultSystemColorRefBgr(idx))
    } else {
        colorRefBgrToCompose(colorRef)
    }
}

/** Windows COLORREF BGR (lower 3 bytes) → sRGB [Color]. */
internal fun colorRefBgrToCompose(colorRef: Int): Color {
    val r = (colorRef and 0xFF).toFloat() / 255f
    val g = ((colorRef shr 8) and 0xFF).toFloat() / 255f
    val b = ((colorRef shr 16) and 0xFF).toFloat() / 255f
    return Color(red = r, green = g, blue = b)
}

private fun rgbToColorRefBgr(r: Int, g: Int, b: Int): Int =
    ((b and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (r and 0xFF)

/** Compose sRGB [Color] → absolute Windows COLORREF (lower 24 bits BGR), for MER `FonteCor` and similar. */
internal fun composeColorToVclAbsoluteRef(color: Color): Int {
    val r = (color.red * 255f).toInt().coerceIn(0, 255)
    val g = (color.green * 255f).toInt().coerceIn(0, 255)
    val b = (color.blue * 255f).toInt().coerceIn(0, 255)
    return rgbToColorRefBgr(r, g, b)
}

/**
 * Default RGB values from Wine `dlls/win32u/sysparams.c` (`system_colors[]`), converted to COLORREF BGR.
 * Index 25 is reserved on Windows; [COLOR_FORM] (31) uses a neutral form background.
 */
private fun defaultSystemColorRefBgr(index: Int): Int = when (index) {
    0 -> rgbToColorRefBgr(212, 208, 200) // SCROLLBAR
    1 -> rgbToColorRefBgr(58, 110, 165) // BACKGROUND
    2 -> rgbToColorRefBgr(10, 36, 106) // ACTIVECAPTION
    3 -> rgbToColorRefBgr(128, 128, 128) // INACTIVECAPTION
    4 -> rgbToColorRefBgr(212, 208, 200) // MENU
    5 -> rgbToColorRefBgr(255, 255, 255) // WINDOW
    6 -> rgbToColorRefBgr(0, 0, 0) // WINDOWFRAME
    7 -> rgbToColorRefBgr(0, 0, 0) // MENUTEXT
    8 -> rgbToColorRefBgr(0, 0, 0) // WINDOWTEXT
    9 -> rgbToColorRefBgr(255, 255, 255) // CAPTIONTEXT
    10 -> rgbToColorRefBgr(212, 208, 200) // ACTIVEBORDER
    11 -> rgbToColorRefBgr(212, 208, 200) // INACTIVEBORDER
    12 -> rgbToColorRefBgr(128, 128, 128) // APPWORKSPACE
    13 -> rgbToColorRefBgr(10, 36, 106) // HIGHLIGHT
    14 -> rgbToColorRefBgr(255, 255, 255) // HIGHLIGHTTEXT
    15 -> rgbToColorRefBgr(212, 208, 200) // BTNFACE
    16 -> rgbToColorRefBgr(128, 128, 128) // BTNSHADOW
    17 -> rgbToColorRefBgr(128, 128, 128) // GRAYTEXT
    18 -> rgbToColorRefBgr(0, 0, 0) // BTNTEXT
    19 -> rgbToColorRefBgr(212, 208, 200) // INACTIVECAPTIONTEXT
    20 -> rgbToColorRefBgr(255, 255, 255) // BTNHIGHLIGHT
    21 -> rgbToColorRefBgr(64, 64, 64) // 3DDKSHADOW
    22 -> rgbToColorRefBgr(212, 208, 200) // 3DLIGHT
    23 -> rgbToColorRefBgr(0, 0, 0) // INFOTEXT
    24 -> rgbToColorRefBgr(255, 255, 225) // INFOBK
    25 -> rgbToColorRefBgr(181, 181, 181) // reserved / ALTERNATEBTNFACE (Wine)
    26 -> rgbToColorRefBgr(0, 0, 200) // HOTLIGHT
    27 -> rgbToColorRefBgr(166, 202, 240) // GRADIENTACTIVECAPTION
    28 -> rgbToColorRefBgr(192, 192, 192) // GRADIENTINACTIVECAPTION
    29 -> rgbToColorRefBgr(10, 36, 106) // MENUHILIGHT
    30 -> rgbToColorRefBgr(212, 208, 200) // MENUBAR
    31 -> rgbToColorRefBgr(240, 240, 240) // FORM (Lazarus; neutral client area)
    else -> rgbToColorRefBgr(192, 192, 192)
}
