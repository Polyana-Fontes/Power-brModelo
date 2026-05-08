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

package games.polyclub.kbrmodelo.ui.components.ribbon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.polyclub.kbrmodelo.ui.RibbonTab
import games.polyclub.kbrmodelo.ui.components.AppColors

@Composable
internal fun RibbonTopBar(
    selectedTab: RibbonTab,
    onMainMenuClick: () -> Unit,
    onTabSelect: (RibbonTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(AppColors.ribbonBg)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Hamburger menu button — drawn as 3 solid lines (avoids WASM glyph issues)
        Box(
            modifier = Modifier
                .size(width = 26.dp, height = 22.dp)
                .background(Color(0xFF3E5A7E), RoundedCornerShape(3.dp))
                .clickable(onClick = onMainMenuClick)
                .align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(Color.White, RoundedCornerShape(1.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        RibbonTabButton(
            text = "Esquema Conceitual",
            selected = selectedTab == RibbonTab.EsquemaConceitual,
            onClick = { onTabSelect(RibbonTab.EsquemaConceitual) }
        )
        Spacer(modifier = Modifier.width(2.dp))
        RibbonTabButton(
            text = "Opções",
            selected = selectedTab == RibbonTab.Opcoes,
            onClick = { onTabSelect(RibbonTab.Opcoes) }
        )
    }
}

@Composable
private fun RibbonTabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg        = if (selected) AppColors.ribbonBg else AppColors.ribbonTabInactive
    val textColor = if (selected) Color(0xFF1B365D) else Color(0xFF445566)
    Box(
        modifier = Modifier
            .height(22.dp)
            .background(bg, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            .then(
                if (selected) {
                    Modifier.drawBehind {
                        val w = size.width; val h = size.height; val s = 1.dp.toPx()
                        val c = AppColors.ribbonBorder
                        drawLine(c, Offset(0f, h), Offset(0f, s / 2))
                        drawLine(c, Offset(0f, s / 2), Offset(w, s / 2))
                        drawLine(c, Offset(w, s / 2), Offset(w, h))
                        // No bottom line — tab merges with ribbon below
                    }
                } else {
                    Modifier.drawBehind {
                        val w = size.width; val h = size.height; val s = 1.dp.toPx()
                        val c = AppColors.ribbonBorder
                        drawLine(c, Offset(0f, h), Offset(0f, s / 2))
                        drawLine(c, Offset(0f, s / 2), Offset(w, s / 2))
                        drawLine(c, Offset(w, s / 2), Offset(w, h))
                        drawLine(c, Offset(0f, h), Offset(w, h))
                    }
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, color = textColor)
    }
}
