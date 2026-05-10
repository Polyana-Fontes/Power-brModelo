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

package games.polyclub.power.brmodelo.domain

/**
 * Delphi / VCL [TColor] identifiers and numeric values, aligned with Lazarus `lcl/graphics.pp`
 * (`Colors` array + `GetColorValues`) and default [TColorBox] styles (standard + extended + system,
 * without `clNone` / `clDefault` unless explicitly included).
 *
 * Absolute RGB colours use Windows **COLORREF** byte order `0x00BBGGRR` in the lower 3 bytes.
 * System colours use `0x80000000` OR the Win32 colour index (see `COLOR_*` in Lazarus `lcltype.pp`).
 */
object VclTColorTable {

    data class NamedColor(val constant: String, val colorRef: Int)

    private val SYS = 0x80000000.toInt()

    /** Win32 indices from Lazarus `lcltype.pp` / `graphics.pp`. */
    private object Idx {
        const val SCROLLBAR = 0
        const val BACKGROUND = 1
        const val ACTIVECAPTION = 2
        const val INACTIVECAPTION = 3
        const val MENU = 4
        const val WINDOW = 5
        const val WINDOWFRAME = 6
        const val MENUTEXT = 7
        const val WINDOWTEXT = 8
        const val CAPTIONTEXT = 9
        const val ACTIVEBORDER = 10
        const val INACTIVEBORDER = 11
        const val APPWORKSPACE = 12
        const val HIGHLIGHT = 13
        const val HIGHLIGHTTEXT = 14
        const val BTNFACE = 15
        const val BTNSHADOW = 16
        const val GRAYTEXT = 17
        const val BTNTEXT = 18
        const val INACTIVECAPTIONTEXT = 19
        const val BTNHIGHLIGHT = 20
        const val DDKSHADOW = 21
        const val LIGHT3D = 22
        const val INFOTEXT = 23
        const val INFOBK = 24
        const val HOTLIGHT = 26
        const val GRADIENTACTIVECAPTION = 27
        const val GRADIENTINACTIVECAPTION = 28
        const val MENUHILIGHT = 29
        const val MENUBAR = 30
        const val FORM = 31
    }

    private fun sys(index: Int): Int = SYS or index

    /**
     * Default items produced by Lazarus/VCL-style colour pickers (`GetColorValues` iteration order,
     * skipping `clNone` and `clDefault` when those styles are off — the usual `TColorBox` default).
     */
    val defaultColorBoxPresets: List<NamedColor> = buildList {
        // Standard 16 (graphics.pp)
        add(NamedColor("clBlack", 0x000000))
        add(NamedColor("clMaroon", 0x000080))
        add(NamedColor("clGreen", 0x008000))
        add(NamedColor("clOlive", 0x008080))
        add(NamedColor("clNavy", 0x800000))
        add(NamedColor("clPurple", 0x800080))
        add(NamedColor("clTeal", 0x808000))
        add(NamedColor("clGray", 0x808080))
        add(NamedColor("clSilver", 0xC0C0C0))
        add(NamedColor("clRed", 0x0000FF))
        add(NamedColor("clLime", 0x00FF00))
        add(NamedColor("clYellow", 0x00FFFF))
        add(NamedColor("clBlue", 0xFF0000))
        add(NamedColor("clFuchsia", 0xFF00FF))
        add(NamedColor("clAqua", 0xFFFF00))
        add(NamedColor("clWhite", 0xFFFFFF))
        // Extended 4
        add(NamedColor("clMoneyGreen", 0xC0DCC0))
        add(NamedColor("clSkyBlue", 0xF0CAA6))
        add(NamedColor("clCream", 0xF0FBFF))
        add(NamedColor("clMedGray", 0xA4A0A0))
        // System — same order as Lazarus `Colors` array after special entries (see graphics.pp)
        add(NamedColor("clScrollBar", sys(Idx.SCROLLBAR)))
        add(NamedColor("clBackground", sys(Idx.BACKGROUND)))
        add(NamedColor("clActiveCaption", sys(Idx.ACTIVECAPTION)))
        add(NamedColor("clInactiveCaption", sys(Idx.INACTIVECAPTION)))
        add(NamedColor("clMenu", sys(Idx.MENU)))
        add(NamedColor("clMenuBar", sys(Idx.MENUBAR)))
        add(NamedColor("clMenuHighlight", sys(Idx.MENUHILIGHT)))
        add(NamedColor("clMenuText", sys(Idx.MENUTEXT)))
        add(NamedColor("clWindow", sys(Idx.WINDOW)))
        add(NamedColor("clWindowFrame", sys(Idx.WINDOWFRAME)))
        add(NamedColor("clWindowText", sys(Idx.WINDOWTEXT)))
        add(NamedColor("clCaptionText", sys(Idx.CAPTIONTEXT)))
        add(NamedColor("clActiveBorder", sys(Idx.ACTIVEBORDER)))
        add(NamedColor("clInactiveBorder", sys(Idx.INACTIVEBORDER)))
        add(NamedColor("clAppWorkspace", sys(Idx.APPWORKSPACE)))
        add(NamedColor("clHighlight", sys(Idx.HIGHLIGHT)))
        add(NamedColor("clHighlightText", sys(Idx.HIGHLIGHTTEXT)))
        add(NamedColor("clBtnFace", sys(Idx.BTNFACE)))
        add(NamedColor("clBtnShadow", sys(Idx.BTNSHADOW)))
        add(NamedColor("clGrayText", sys(Idx.GRAYTEXT)))
        add(NamedColor("clBtnText", sys(Idx.BTNTEXT)))
        add(NamedColor("clInactiveCaptionText", sys(Idx.INACTIVECAPTIONTEXT)))
        add(NamedColor("clBtnHighlight", sys(Idx.BTNHIGHLIGHT)))
        add(NamedColor("cl3DDkShadow", sys(Idx.DDKSHADOW)))
        add(NamedColor("cl3DLight", sys(Idx.LIGHT3D)))
        add(NamedColor("clInfoText", sys(Idx.INFOTEXT)))
        add(NamedColor("clInfoBk", sys(Idx.INFOBK)))
        add(NamedColor("clHotLight", sys(Idx.HOTLIGHT)))
        add(NamedColor("clGradientActiveCaption", sys(Idx.GRADIENTACTIVECAPTION)))
        add(NamedColor("clGradientInactiveCaption", sys(Idx.GRADIENTINACTIVECAPTION)))
        add(NamedColor("clForm", sys(Idx.FORM)))
    }

    /**
     * Lowercased `cl*` → numeric [TColor] / COLORREF / system index encoding, for DFM/XML `vaIdent` parsing.
     */
    val namedColorRefsLowercase: Map<String, Int> = buildMap {
        defaultColorBoxPresets.forEach { put(it.constant.lowercase(), it.colorRef) }
        // Special (present in Lazarus `Colors` array; often omitted from combo unless style flags on)
        put("clnone", 0x1FFFFFFF)
        put("cldefault", 0x20000000)
        // Common aliases from Delphi Graphics / XML
        put("clltgray", 0xC0C0C0)
        put("cldkgray", 0x808080)
        put("clcolordesktop", sys(Idx.BACKGROUND))
        put("cl3dface", sys(Idx.BTNFACE))
        put("cl3dshadow", sys(Idx.BTNSHADOW))
        put("cl3dhilight", sys(Idx.BTNHIGHLIGHT))
        put("clbtnhilight", sys(Idx.BTNHIGHLIGHT))
    }
}
