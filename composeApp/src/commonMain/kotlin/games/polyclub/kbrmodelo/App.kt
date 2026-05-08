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
import androidx.compose.runtime.LaunchedEffect
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
import games.polyclub.kbrmodelo.ui.consumeWindowDropDataUrl
import games.polyclub.kbrmodelo.ui.isWindowDragActive
import games.polyclub.kbrmodelo.ui.setupWindowDragDrop
import games.polyclub.kbrmodelo.ui.showNativeFilePicker
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App() {
    var isMainMenuOpen by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf<MainMenuType?>(null) }
    var selectedTab by remember { mutableStateOf(RibbonTab.EsquemaConceitual) }
    var schema by remember { mutableStateOf<ConceptualSchema?>(null) }
    var isDragOver by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Register window-level drag-and-drop once (no-op on Desktop)
    LaunchedEffect(Unit) {
        setupWindowDragDrop()
    }

    // Poll for drag-over state and dropped files (effective on WASM only)
    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            isDragOver = isWindowDragActive()
            val dataUrl = consumeWindowDropDataUrl()
            if (dataUrl != null) {
                loadFromDataUrl(dataUrl)?.let { schema = it }
            }
        }
    }

    val openFile: () -> Unit = {
        scope.launch {
            val bytes = showNativeFilePicker() ?: return@launch
            runCatching { ConceptualSchemaXmlParser.parse(bytes) }
                .onSuccess { schema = it }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE3E3E3)) {
            BrModeloScreen(
                isMainMenuOpen = isMainMenuOpen,
                activeMenu = activeMenu,
                selectedTab = selectedTab,
                schema = schema,
                isDragOver = isDragOver,
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

@OptIn(ExperimentalEncodingApi::class)
private fun loadFromDataUrl(dataUrl: String): ConceptualSchema? =
    runCatching {
        val bytes = Base64.Default.decode(dataUrl.substringAfter(","))
        ConceptualSchemaXmlParser.parse(bytes)
    }.getOrNull()
