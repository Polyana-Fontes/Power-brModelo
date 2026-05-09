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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import games.polyclub.power.brmodelo.ui.RibbonTab
import games.polyclub.power.brmodelo.ui.components.AppColors
import games.polyclub.power.brmodelo.ui.components.CHROMIUM_TAB_STRIP_HEIGHT
import games.polyclub.power.brmodelo.ui.components.ChromiumTab
import games.polyclub.power.brmodelo.ui.components.ChromiumTabShape

// Ribbon tabs are slightly shorter than the full strip height to leave a small
// breathing space at the top, distinguishing them from the canvas/inspector tabs.
private val RIBBON_TAB_ACTIVE_HEIGHT   = 22.dp
private val RIBBON_TAB_INACTIVE_HEIGHT = 19.dp

@Composable
internal fun RibbonTopBar(
    selectedTab: RibbonTab,
    onMainMenuClick: () -> Unit,
    onTabSelect: (RibbonTab) -> Unit,
) {
    val density = LocalDensity.current
    val topCornerPx   = with(density) { 5.dp.toPx() }
    val bottomCurvePx = with(density) { 4.dp.toPx() }
    val tabShape = remember(topCornerPx, bottomCurvePx) {
        ChromiumTabShape(
            topCornerRadius = topCornerPx,
            bottomCurveRadius = bottomCurvePx
        )
    }

    // Outer Box so we can position the HorizontalDivider at the bottom independently
    // from the Row content (hamburger + tabs).
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHROMIUM_TAB_STRIP_HEIGHT)
            .background(AppColors.ribbonBg),
    ) {
        // Strip separator — active tab (zIndex 2f) renders on top of it, making the
        // active tab appear to rise seamlessly into the ribbon content below.
        HorizontalDivider(
            color = AppColors.ribbonBorder,
            thickness = 1.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1.5f),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Hamburger menu button
            Box(
                modifier = Modifier
                    .size(width = 26.dp, height = 22.dp)
                    .background(Color(0xFF3E5A7E), RoundedCornerShape(3.dp))
                    .clickable(onClick = onMainMenuClick)
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
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

            ChromiumTab(
                label = "Esquema Conceitual",
                selected = selectedTab == RibbonTab.EsquemaConceitual,
                tabShape = tabShape,
                activeTabBg = AppColors.ribbonBg,
                inactiveTabBg = AppColors.ribbonTabInactive,
                borderColor = AppColors.ribbonBorder,
                modifier = Modifier
                    .height(
                        if (selectedTab == RibbonTab.EsquemaConceitual)
                            RIBBON_TAB_ACTIVE_HEIGHT
                        else
                            RIBBON_TAB_INACTIVE_HEIGHT
                    )
                    .zIndex(if (selectedTab == RibbonTab.EsquemaConceitual) 2f else 1f),
                onClick = { onTabSelect(RibbonTab.EsquemaConceitual) },
            )

            ChromiumTab(
                label = "Opções",
                selected = selectedTab == RibbonTab.Opcoes,
                tabShape = tabShape,
                activeTabBg = AppColors.ribbonBg,
                inactiveTabBg = AppColors.ribbonTabInactive,
                borderColor = AppColors.ribbonBorder,
                modifier = Modifier
                    .height(
                        if (selectedTab == RibbonTab.Opcoes)
                            RIBBON_TAB_ACTIVE_HEIGHT
                        else
                            RIBBON_TAB_INACTIVE_HEIGHT
                    )
                    .zIndex(if (selectedTab == RibbonTab.Opcoes) 2f else 1f),
                onClick = { onTabSelect(RibbonTab.Opcoes) },
            )
        }
    }
}
