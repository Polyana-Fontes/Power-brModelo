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

package games.polyclub.kbrmodelo.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object AppColors {

    // ─── Ribbon ───────────────────────────────────────────────────────────────

    val ribbonBg         = Color(0xFFDDE4EE)
    val ribbonGroupBg    = Color(0xFFEBF0F8)
    val ribbonBorder     = Color(0xFF9AAABB)
    val ribbonSeparator  = Color(0xFFBBCCDD)
    val ribbonGroupTitle = Color(0xFF7A8FA0)
    val ribbonTabInactive = Color(0xFFC4CFDB)

    // ─── Ribbon hover (golden tint matching the brModelo original) ────────────

    val hoverBg     = Color(0xFFFFF3C0)
    val hoverBorder = Color(0xFFE8A800)
    val hoverShape  = RoundedCornerShape(3.dp)

    // ─── Ribbon dropdown ──────────────────────────────────────────────────────

    val dropdownIconStripe = Color(0xFFF3F7FB)
    val dropdownHover      = Color(0xFFCCDDEE)

    // ─── Main menu ────────────────────────────────────────────────────────────

    val menuBg        = Color(0xFFEEF2F8)
    val menuActive    = Color(0xFFFFBF00)
    val menuBorder    = Color(0xFFA0AEBC)
    val menuSubmenuBg = Color(0xFFF8F9FC)
    val menuText      = Color(0xFF1C2B3A)
    val menuSeparator = Color(0xFFC0CAD4)

    // ─── Inspector panel ──────────────────────────────────────────────────────

    val inspectorBg          = Color(0xFFF0F2F5)
    val inspectorBorder      = Color(0xFF8090A0)
    val inspectorHeaderBg    = Color(0xFFD8DDE4)
    val inspectorHeaderBorder = Color(0xFFB0BAC4)
    val inspectorTabActive   = Color(0xFFFFFFFF)
    val inspectorTabInactive = Color(0xFFC4CED8)
    val inspectorSectionBg   = Color(0xFFCFD8E3)
    val inspectorCellLabel   = Color(0xFFE8EDF2)
    val inspectorCellValue   = Color(0xFFFFFFFF)
    val inspectorCellBorder  = Color(0xFFBDC7D1)
    val inspectorLabelColor  = Color(0xFF3A4A5A)
    val inspectorValueColor  = Color(0xFF1A2535)
}
