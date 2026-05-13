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
import games.polyclub.power.brmodelo.ui.EditorTabSession
import games.polyclub.power.brmodelo.ui.PickedFile
import games.polyclub.power.brmodelo.ui.showNativeFilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual object BrModeloMcpDesktopSync {
    actual fun syncBindingsFromApp(
        runtime: BrModeloMcpRuntime,
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,
        tabSessions: List<EditorTabSession>,
        selectedTabIndex: Int,
        onSelectTab: (Int) -> Unit,
        onAddBlankTab: () -> Unit,
        onForceCloseTab: (Int) -> Unit,
        onRequestCloseTab: (Int) -> Unit,
        saveTabAt: suspend (Int, Boolean) -> Boolean,
        parseAndMergePickedFile: (PickedFile) -> Unit,
        onServerRunningChanged: (Boolean) -> Unit,
    ) {
        runtime.updateBindings(
            BrModeloMcpUiBindings(
                current = { BrModeloMcpTabSnapshot(tabSessions, selectedTabIndex) },
                onSelectTab = onSelectTab,
                onAddBlankTab = onAddBlankTab,
                onForceCloseTab = onForceCloseTab,
                onRequestCloseTab = onRequestCloseTab,
                onSaveTab = { idx, saveAs -> runBlocking { saveTabAt(idx, saveAs) } },
                onOpenFile = {
                    runBlocking {
                        val picked = showNativeFilePicker() ?: return@runBlocking
                        parseAndMergePickedFile(picked)
                    }
                },
                onNotifyUser = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                },
                onServerRunningChanged = onServerRunningChanged,
            ),
        )
    }

    actual fun clearBindings(runtime: BrModeloMcpRuntime) {
        runtime.updateBindings(null)
    }
}
