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

import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.ui.EditorTabSession

/**
 * Snapshot of open editor tabs exposed to the MCP server (read on the UI thread).
 */
internal data class BrModeloMcpTabSnapshot(
    val sessions: List<EditorTabSession>,
    val selectedIndex: Int,
)

/**
 * UI-thread actions invoked from MCP tool handlers (desktop only).
 */
internal class BrModeloMcpUiBindings(
    val current: () -> BrModeloMcpTabSnapshot,
    val onSelectTab: (Int) -> Unit,
    val onAddBlankTab: () -> Unit,
    val onForceCloseTab: (Int) -> Unit,
    val onRequestCloseTab: (Int) -> Unit,
    val onSaveTab: (Int, Boolean) -> Boolean,
    val onOpenFile: () -> Unit,
    val onNotifyUser: (String) -> Unit,
    val onServerRunningChanged: (Boolean) -> Unit,
)

/**
 * Desktop MCP server + settings; WASM [actual] is a no-op stub.
 */
internal expect class BrModeloMcpRuntime() {
    fun setSettingsDialogOpener(opener: () -> Unit)

    fun updateBindings(bindings: BrModeloMcpUiBindings?)

    fun openSettingsDialog()

    fun startServer(): Boolean

    fun stopServer()

    fun isServerRunning(): Boolean

    fun onTabsChanged()

    fun shutdown()
}

internal fun BrModeloMcpTabSnapshot.schemaForTab(index: Int): ConceptualSchema? =
    sessions.getOrNull(index)?.schema
