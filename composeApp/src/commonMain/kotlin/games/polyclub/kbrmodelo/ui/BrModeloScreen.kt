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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import games.polyclub.kbrmodelo.ui.components.ribbon.HeaderRibbon

// TopBar height (30dp) + ribbon content (90dp) = 120dp to place menu just below the TopBar button
private val MENU_TOP_OFFSET = 30.dp

@Composable
internal fun BrModeloScreen(
    isMainMenuOpen: Boolean,
    activeMenu: MainMenuType?,
    selectedTab: RibbonTab,
    onMainMenuToggle: () -> Unit,
    onMainMenuHover: (MainMenuType) -> Unit,
    onTabSelect: (RibbonTab) -> Unit,
    onDismissMenu: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderRibbon(
                selectedTab = selectedTab,
                onMainMenuClick = onMainMenuToggle,
                onTabSelect = onTabSelect
            )
            WorkspaceArea()
        }

        // Dismiss overlay behind the menu
        if (isMainMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismissMenu() }
            )
            // Menu floats just below the TopBar (button height = 30dp), left edge aligned with button
            FunctionalMainMenu(
                modifier = Modifier
                    .padding(start = 4.dp, top = MENU_TOP_OFFSET)
                    .height(340.dp),
                activeMenu = activeMenu,
                onMenuHover = onMainMenuHover
            )
        }
    }
}
