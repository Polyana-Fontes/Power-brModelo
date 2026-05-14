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
import games.polyclub.power.brmodelo.domain.ConceptualSearchHit
import games.polyclub.power.brmodelo.domain.ConceptualSearchOutcome
import games.polyclub.power.brmodelo.domain.ConceptualSearchTextScope
import games.polyclub.power.brmodelo.domain.ConceptualSearchTypeFilters
import games.polyclub.power.brmodelo.ui.EditorTabSession
import kotlinx.coroutines.CoroutineScope

/**
 * Desktop wires MCP tool handlers onto the UI using blocking bridges; WASM is a no-op.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object McpDesktopSync {
    fun syncBindingsFromApp(
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
        onConceptualSearchFind: (
            Int,
            String,
            ConceptualSearchTypeFilters,
            ConceptualSearchTextScope,
        ) -> ConceptualSearchOutcome,
        onConceptualSearchApplyHit: (Int, ConceptualSearchHit) -> String?,
        onServerRunningChanged: (Boolean) -> Unit,
    )

    fun clearBindings(runtime: McpRuntime)
}
