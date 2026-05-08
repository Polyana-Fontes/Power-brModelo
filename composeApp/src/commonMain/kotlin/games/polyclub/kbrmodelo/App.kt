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

package games.polyclub.kbrmodelo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import games.polyclub.kbrmodelo.domain.ConceptualSchema
import games.polyclub.kbrmodelo.domain.serialization.ConceptualSchemaXmlParser
import games.polyclub.kbrmodelo.ui.BrModeloScreen
import games.polyclub.kbrmodelo.ui.MainMenuType
import games.polyclub.kbrmodelo.ui.RibbonTab
import games.polyclub.kbrmodelo.ui.showNativeFilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun App() {
    var isMainMenuOpen by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf<MainMenuType?>(null) }
    var selectedTab by remember { mutableStateOf(RibbonTab.EsquemaConceitual) }
    var schema by remember { mutableStateOf<ConceptualSchema?>(null) }
    val scope = rememberCoroutineScope()

    val openFile: () -> Unit = {
        scope.launch {
            val bytes = withContext(Dispatchers.Default) { showNativeFilePicker() }
            if (bytes != null) {
                runCatching {
                    withContext(Dispatchers.Default) { ConceptualSchemaXmlParser.parse(bytes) }
                }.onSuccess { loaded ->
                    schema = loaded
                }
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE3E3E3)) {
            BrModeloScreen(
                isMainMenuOpen = isMainMenuOpen,
                activeMenu = activeMenu,
                selectedTab = selectedTab,
                schema = schema,
                onMainMenuToggle = {
                    isMainMenuOpen = !isMainMenuOpen
                    if (!isMainMenuOpen) activeMenu = null
                },
                onMainMenuHover = { activeMenu = it },
                onTabSelect = { selectedTab = it },
                onDismissMenu = {
                    isMainMenuOpen = false
                    activeMenu = null
                },
                onOpenFile = openFile,
            )
        }
    }
}
