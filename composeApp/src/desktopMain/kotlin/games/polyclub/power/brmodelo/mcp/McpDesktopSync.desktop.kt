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

package games.polyclub.power.brmodelo.mcp

import androidx.compose.material3.SnackbarHostState
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolKind
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolOverrides
import games.polyclub.power.brmodelo.ui.EditorTabSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual object McpDesktopSync {
    actual fun syncBindingsFromApp(
        runtime: McpRuntime,
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,
        tabSessions: List<EditorTabSession>,
        selectedTabIndex: Int,
        onSelectTab: (Int) -> Unit,
        onAddBlankTab: () -> Unit,
        onForceCloseTab: (Int) -> Unit,
        onRequestCloseTab: (Int) -> Unit,
        saveTabAt: suspend (Int, Boolean) -> Boolean,
        onOpenModelFileAtPath: (String) -> String?,
        onOpenXmlAsUnsavedTab: (String, String) -> String?,
        onReplaceModelXmlAtTab: (Int, String) -> String?,
        onPatchModelXmlAtTab: (Int, String, String, Boolean) -> String?,
        onPlaceProceduralConceptualToolAtTab: (
            Int,
            ConceptualProceduralToolKind,
            Int,
            Int,
            ConceptualProceduralToolOverrides,
        ) -> McpProceduralToolApplyOutcome,
        onServerRunningChanged: (Boolean) -> Unit,
    ) {
        runtime.updateBindings(
            McpUiBindings(
                current = { McpTabSnapshot(tabSessions, selectedTabIndex) },
                onSelectTab = onSelectTab,
                onAddBlankTab = onAddBlankTab,
                onForceCloseTab = onForceCloseTab,
                onRequestCloseTab = onRequestCloseTab,
                onSaveTab = { idx, saveAs -> runBlocking { saveTabAt(idx, saveAs) } },
                onOpenModelFileAtPath = onOpenModelFileAtPath,
                onOpenXmlAsUnsavedTab = onOpenXmlAsUnsavedTab,
                onReplaceModelXmlAtTab = onReplaceModelXmlAtTab,
                onPatchModelXmlAtTab = onPatchModelXmlAtTab,
                onPlaceProceduralConceptualToolAtTab = onPlaceProceduralConceptualToolAtTab,
                onNotifyUser = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                },
                onServerRunningChanged = onServerRunningChanged,
            ),
        )
    }

    actual fun clearBindings(runtime: McpRuntime) {
        runtime.updateBindings(null)
    }
}
