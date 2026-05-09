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
import games.polyclub.kbrmodelo.domain.CanvasSelection
import games.polyclub.kbrmodelo.domain.ConceptualSchema
import games.polyclub.kbrmodelo.domain.SchemaHistory
import games.polyclub.kbrmodelo.domain.serialization.ConceptualSchemaBrmParser
import games.polyclub.kbrmodelo.domain.serialization.ConceptualSchemaXmlParser
import games.polyclub.kbrmodelo.ui.BrModeloScreen
import games.polyclub.kbrmodelo.ui.MainMenuType
import games.polyclub.kbrmodelo.ui.RibbonTab
import games.polyclub.kbrmodelo.ui.PickedFile
import games.polyclub.kbrmodelo.ui.consumeWindowDropFile
import games.polyclub.kbrmodelo.ui.isWindowDragActive
import games.polyclub.kbrmodelo.ui.setupWindowDragDrop
import games.polyclub.kbrmodelo.ui.showNativeFilePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App() {
    var isMainMenuOpen by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf<MainMenuType?>(null) }
    var selectedTab by remember { mutableStateOf(RibbonTab.EsquemaConceitual) }

    // History is the source of truth for committed schema states.
    val history = remember { SchemaHistory(null) }
    // Live schema state: updated both during drag previews (no history entry) and on commits.
    var schema by remember { mutableStateOf<ConceptualSchema?>(null) }

    // Current selection on the canvas (element id or cardinality connection id, or None).
    var selection by remember { mutableStateOf<CanvasSelection>(CanvasSelection.None) }

    // isDragOverFromPolling: set by the WASM JS polling loop (always false on Desktop).
    // isDragOverFromCallback: set by Modifier.fileDragDropTarget on Desktop (always false on WASM).
    // Keeping them separate prevents the polling from clearing the callback state and vice-versa.
    var isDragOverFromPolling  by remember { mutableStateOf(false) }
    var isDragOverFromCallback by remember { mutableStateOf(false) }
    val isDragOver = isDragOverFromPolling || isDragOverFromCallback
    val scope = rememberCoroutineScope()

    // Register window-level drag-and-drop once (no-op on Desktop)
    LaunchedEffect(Unit) {
        setupWindowDragDrop()
    }

    // Poll for drag-over state and dropped files (effective on WASM only).
    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            isDragOverFromPolling = isWindowDragActive()
            val dropped = consumeWindowDropFile()
            if (dropped != null) {
                runCatching { parseModelBytes(dropped.bytes) }
                    .onSuccess {
                        val named = it.copy(name = dropped.name)
                        history.push(named)
                        schema = named
                        selection = CanvasSelection.None
                    }
            }
        }
    }

    val openFile: () -> Unit = {
        scope.launch {
            val picked = showNativeFilePicker() ?: return@launch
            runCatching { parseModelBytes(picked.bytes) }
                .onSuccess {
                    val named = it.copy(name = picked.name)
                    history.push(named)
                    schema = named
                    selection = CanvasSelection.None
                }
        }
    }

    val loadPickedFile: (PickedFile) -> Unit = { picked ->
        runCatching { parseModelBytes(picked.bytes) }
            .onSuccess {
                val named = it.copy(name = picked.name)
                history.push(named)
                schema = named
                selection = CanvasSelection.None
            }
    }

    // Called during live interactions (drag preview) — does NOT push to undo history.
    val onSchemaPreview: (ConceptualSchema) -> Unit = { schema = it }

    // Called when an action is committed (pointer up after drag, field blur, dropdown change).
    // Pushes the new state to the undo stack.
    val onSchemaCommit: (ConceptualSchema) -> Unit = {
        history.push(it)
        schema = it
    }

    val onUndo: () -> Unit = { schema = history.undo() }
    val onRedo: () -> Unit = { schema = history.redo() }

    val onCloseTab: () -> Unit = {
        schema = null
        selection = CanvasSelection.None
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE3E3E3)) {
            BrModeloScreen(
                isMainMenuOpen = isMainMenuOpen,
                activeMenu = activeMenu,
                selectedTab = selectedTab,
                schema = schema,
                selection = selection,
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
                onDragStateChange = { isDragOverFromCallback = it },
                onFileDrop = loadPickedFile,
                onSelectionChange = { selection = it },
                onSchemaPreview = onSchemaPreview,
                onSchemaCommit = onSchemaCommit,
                onCloseTab = onCloseTab,
            )
        }
    }
}

/**
 * Detects the model format from the byte content and routes to the correct parser.
 *
 * Delphi binary DFM files start with a ShortString version prefix (e.g. `\x05 "2.0.0"`)
 * immediately followed by the 4-byte magic `"TPF0"`. All other content is treated as XML.
 */
internal fun parseModelBytes(bytes: ByteArray): ConceptualSchema {
    val isBrm = bytes.size > 10 &&
        bytes[6] == 'T'.code.toByte() &&
        bytes[7] == 'P'.code.toByte() &&
        bytes[8] == 'F'.code.toByte() &&
        bytes[9] == '0'.code.toByte()
    return if (isBrm) ConceptualSchemaBrmParser.parse(bytes)
    else ConceptualSchemaXmlParser.parse(bytes)
}

