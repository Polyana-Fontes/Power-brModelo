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

package games.polyclub.power.brmodelo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaBrmParser
import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaXmlParser
import games.polyclub.power.brmodelo.ui.BrModeloScreen
import games.polyclub.power.brmodelo.ui.CloseTabUnsavedDialog
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool
import games.polyclub.power.brmodelo.ui.EditorTabSession
import games.polyclub.power.brmodelo.ui.EntityToolRibbonBinding
import games.polyclub.power.brmodelo.ui.ObservationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.EntityToolVariant
import games.polyclub.power.brmodelo.ui.MainMenuType
import games.polyclub.power.brmodelo.ui.PickedFile
import games.polyclub.power.brmodelo.ui.QuitApplicationUnsavedDialog
import games.polyclub.power.brmodelo.ui.RibbonTab
import games.polyclub.power.brmodelo.ui.consumeWindowDropFile
import games.polyclub.power.brmodelo.ui.isDesktopTarget
import games.polyclub.power.brmodelo.ui.isWindowDragActive
import games.polyclub.power.brmodelo.ui.setupWindowDragDrop
import games.polyclub.power.brmodelo.ui.showNativeFilePicker
import games.polyclub.power.brmodelo.ui.components.ribbon.entityVariantRibbonPresentation
import games.polyclub.power.brmodelo.ui.matchesEntityVariant
import games.polyclub.power.brmodelo.ui.toConceptualTool
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App(onApplicationTitleChange: (String) -> Unit = {}) {
    var isMainMenuOpen by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf<MainMenuType?>(null) }
    var selectedRibbonTab by remember { mutableStateOf(RibbonTab.EsquemaConceitual) }

    var conceptualCanvasTool by remember { mutableStateOf<ConceptualCanvasTool>(ConceptualCanvasTool.None) }
    var entityToolVariant by remember { mutableStateOf(EntityToolVariant.Plain) }

    val initialTabId = 1L
    var nextTabId by remember { mutableLongStateOf(initialTabId + 1) }
    var tabSessions by remember {
        mutableStateOf(listOf(EditorTabSession.blank(initialTabId)))
    }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var pendingCloseTabIndex by remember { mutableStateOf<Int?>(null) }
    var pendingApplicationQuit by remember { mutableStateOf(false) }

    fun selectedSession(): EditorTabSession = tabSessions[selectedTabIndex]

    fun replaceTabAt(index: Int, session: EditorTabSession) {
        tabSessions = tabSessions.toMutableList().also { it[index] = session }
    }

    fun mutateSelectedTab(mutator: (EditorTabSession) -> EditorTabSession) {
        replaceTabAt(selectedTabIndex, mutator(selectedSession()))
    }

    /** Each tab owns its [EditorTabSession.history]; mutations always target the selected tab. */
    fun pushCommitOnSelected(normalized: ConceptualSchema) {
        val idx = selectedTabIndex
        val tab = tabSessions[idx]
        tab.history.push(normalized)
        replaceTabAt(
            idx,
            tab.copy(
                schema = normalized,
                inspectorCommittedSchema = tab.history.current,
            ),
        )
    }

    fun performRemoveTab(index: Int) {
        if (tabSessions.size == 1) {
            tabSessions = listOf(EditorTabSession.blank(nextTabId++))
            selectedTabIndex = 0
            return
        }
        val oldSel = selectedTabIndex
        val newList = tabSessions.filterIndexed { i, _ -> i != index }
        tabSessions = newList
        selectedTabIndex = when {
            index < oldSel -> oldSel - 1
            index == oldSel -> minOf(oldSel, newList.lastIndex)
            else -> oldSel
        }
    }

    fun requestCloseTab(index: Int) {
        if (index !in tabSessions.indices) return
        if (tabSessions[index].needsCloseConfirmation()) {
            pendingCloseTabIndex = index
        } else {
            performRemoveTab(index)
        }
    }

    fun openLoadedModel(model: ConceptualSchema) {
        val incomingPath = model.filePath.trim()
        if (incomingPath.isNotEmpty()) {
            val dupIdx = tabSessions.indexOfFirst { tab ->
                val existing = tab.schema.filePath.trim()
                existing.isNotEmpty() && diskPathsReferToSameFile(existing, incomingPath)
            }
            if (dupIdx >= 0) {
                selectedTabIndex = dupIdx
                return
            }
        }

        if (tabSessions.size == 1 && tabSessions[0].isReplaceableBlankStarter()) {
            val soleId = tabSessions[0].id
            tabSessions = listOf(EditorTabSession.fromLoadedModel(soleId, model))
            selectedTabIndex = 0
            return
        }

        val session = EditorTabSession.fromLoadedModel(nextTabId++, model)
        tabSessions = tabSessions + session
        selectedTabIndex = tabSessions.lastIndex
    }

    fun addBlankTab() {
        val session = EditorTabSession.blank(nextTabId++)
        tabSessions = tabSessions + session
        selectedTabIndex = tabSessions.lastIndex
    }

    var isDragOverFromPolling by remember { mutableStateOf(false) }
    var isDragOverFromCallback by remember { mutableStateOf(false) }
    val isDragOver = isDragOverFromPolling || isDragOverFromCallback
    val scope = rememberCoroutineScope()

    suspend fun saveTabAt(index: Int, saveAs: Boolean): Boolean {
        val tab = tabSessions.getOrNull(index) ?: return true
        val s = tab.schema
        val pickLocation = saveAs || s.filePath.isBlank() || s.openedFromBrm
        val updated = saveConceptualSchemaXml(
            schema = s,
            suggestedBaseName = s.name.ifBlank { "modelo" },
            pickLocation = pickLocation,
        ) ?: return false
        tab.history.syncCurrent(updated)
        replaceTabAt(
            index,
            tab.copy(
                schema = updated,
                inspectorCommittedSchema = updated,
                savedDiskBaseline = updated,
            ),
        )
        return true
    }

    LaunchedEffect(Unit) {
        setupWindowDragDrop()
    }

    LaunchedEffect(tabSessions) {
        val warn = tabSessions.any { it.needsCloseConfirmation() }
        setBrowserUnloadWarningEnabled(warn)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            isDragOverFromPolling = isWindowDragActive()
            val dropped = consumeWindowDropFile()
            if (dropped != null) {
                runCatching { parseModelBytesWithSource(dropped.bytes) }
                    .onSuccess { (parsed, fromBrm) ->
                        openLoadedModel(mergeLoadedModel(parsed, fromBrm, dropped))
                    }
            }
        }
    }

    val openFile: () -> Unit = {
        scope.launch {
            val picked = showNativeFilePicker() ?: return@launch
            runCatching { parseModelBytesWithSource(picked.bytes) }
                .onSuccess { (parsed, fromBrm) ->
                    openLoadedModel(mergeLoadedModel(parsed, fromBrm, picked))
                }
        }
    }

    val loadPickedFile: (PickedFile) -> Unit = { picked ->
        runCatching { parseModelBytesWithSource(picked.bytes) }
            .onSuccess { (parsed, fromBrm) ->
                openLoadedModel(mergeLoadedModel(parsed, fromBrm, picked))
            }
    }

    val onSchemaPreview: (ConceptualSchema) -> Unit = { preview ->
        mutateSelectedTab { it.copy(schema = preview) }
    }

    val onSchemaCommit: (ConceptualSchema) -> Unit = {
        pushCommitOnSelected(it.withNormalizedAttributeMultiValuedCounts())
    }

    val onRevertSchemaPreview: () -> Unit = {
        mutateSelectedTab { tab ->
            val committed = tab.history.current ?: tab.schema
            tab.copy(schema = committed)
        }
    }

    fun enqueueSave(saveAs: Boolean) {
        scope.launch { saveTabAt(selectedTabIndex, saveAs) }
    }

    DisposableEffect(selectedTabIndex, tabSessions) {
        bindDesktopSaveShortcut { enqueueSave(saveAs = false) }
        onDispose { bindDesktopSaveShortcut(null) }
    }

    val tabsState = rememberUpdatedState(tabSessions)
    DisposableEffect(scope) {
        registerDesktopMainWindowCloseHandler {
            scope.launch {
                if (tabsState.value.any { it.needsCloseConfirmation() }) {
                    pendingApplicationQuit = true
                } else {
                    quitApplicationCompletely()
                }
            }
        }
        onDispose { registerDesktopMainWindowCloseHandler(null) }
    }

    val currentName = selectedSession().schema.name
    LaunchedEffect(currentName, selectedTabIndex) {
        onApplicationTitleChange(formatApplicationWindowTitle(currentName))
    }

    val sel = selectedSession()

    val (entityTitle, entityIcon) = entityVariantRibbonPresentation(entityToolVariant)
    val entityToolBinding = EntityToolRibbonBinding(
        variant = entityToolVariant,
        isArmed = conceptualCanvasTool.matchesEntityVariant(entityToolVariant),
        displayTitle = entityTitle,
        displayIcon = entityIcon,
        onMainClick = {
            if (conceptualCanvasTool.matchesEntityVariant(entityToolVariant)) {
                conceptualCanvasTool = ConceptualCanvasTool.None
            } else {
                conceptualCanvasTool = entityToolVariant.toConceptualTool()
            }
        },
        onDropdownVariant = { v ->
            entityToolVariant = v
            conceptualCanvasTool = v.toConceptualTool()
        },
    )

    val observationToolBinding = ObservationToolRibbonBinding(
        isArmed = conceptualCanvasTool is ConceptualCanvasTool.Observation,
        onClick = {
            conceptualCanvasTool =
                if (conceptualCanvasTool is ConceptualCanvasTool.Observation) {
                    ConceptualCanvasTool.None
                } else {
                    ConceptualCanvasTool.Observation
                }
        },
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE3E3E3)) {
            Box(modifier = Modifier.fillMaxSize()) {
                BrModeloScreen(
                    isMainMenuOpen = isMainMenuOpen,
                    activeMenu = activeMenu,
                    selectedTab = selectedRibbonTab,
                    canvasTabs = tabSessions,
                    selectedCanvasTabIndex = selectedTabIndex,
                    onSelectCanvasTab = { selectedTabIndex = it },
                    onRequestCloseCanvasTab = { requestCloseTab(it) },
                    schema = sel.schema,
                    inspectorCommittedSchema = sel.inspectorCommittedSchema,
                    selection = sel.selection,
                    isDragOver = isDragOver,
                    onMainMenuToggle = {
                        isMainMenuOpen = !isMainMenuOpen
                        if (!isMainMenuOpen) activeMenu = null
                    },
                    onMainMenuHover = { activeMenu = it },
                    onTabSelect = { selectedRibbonTab = it },
                    onDismissMenu = {
                        isMainMenuOpen = false
                        activeMenu = null
                    },
                    onOpenFile = openFile,
                    onNewConceptualModel = { addBlankTab() },
                    onDragStateChange = { isDragOverFromCallback = it },
                    onFileDrop = loadPickedFile,
                    onSelectionChange = { selNew ->
                        mutateSelectedTab { it.copy(selection = selNew) }
                    },
                    onSchemaPreview = onSchemaPreview,
                    onSchemaCommit = onSchemaCommit,
                    onRevertSchemaPreview = onRevertSchemaPreview,
                    onCloseCurrentModel = { requestCloseTab(selectedTabIndex) },
                    onSave = { enqueueSave(saveAs = false) },
                    onSaveAs = { enqueueSave(saveAs = true) },
                    entityToolBinding = entityToolBinding,
                    observationToolBinding = observationToolBinding,
                    conceptualCanvasTool = conceptualCanvasTool,
                    onClearConceptualCanvasTool = { conceptualCanvasTool = ConceptualCanvasTool.None },
                )

                pendingCloseTabIndex?.let { closeIdx ->
                    if (closeIdx in tabSessions.indices) {
                        val title = tabSessions[closeIdx].displayTitle()
                        CloseTabUnsavedDialog(
                            documentTitle = title,
                            onSave = {
                                scope.launch {
                                    if (saveTabAt(closeIdx, false)) {
                                        pendingCloseTabIndex = null
                                        performRemoveTab(closeIdx)
                                    }
                                }
                            },
                            onDiscard = {
                                pendingCloseTabIndex = null
                                performRemoveTab(closeIdx)
                            },
                            onCancel = { pendingCloseTabIndex = null },
                        )
                    } else {
                        pendingCloseTabIndex = null
                    }
                }

                if (pendingApplicationQuit) {
                    QuitApplicationUnsavedDialog(
                        showSaveAll = isDesktopTarget,
                        onSaveAll = {
                            scope.launch {
                                val indices = tabSessions.indices.filter { tabSessions[it].needsCloseConfirmation() }
                                for (i in indices) {
                                    if (!saveTabAt(i, false)) return@launch
                                }
                                pendingApplicationQuit = false
                                quitApplicationCompletely()
                            }
                        },
                        onQuitWithoutSaving = {
                            pendingApplicationQuit = false
                            quitApplicationCompletely()
                        },
                        onCancel = { pendingApplicationQuit = false },
                    )
                }
            }
        }
    }
}

/**
 * Desktop window caption and browser tab title: app name, version, and the loaded model name when present.
 */
internal fun formatApplicationWindowTitle(modelDisplayName: String?): String {
    val appName = "Power brModelo ${BuildInfo.displayVersion}"
    val name = modelDisplayName?.trim().orEmpty()
    return if (name.isEmpty()) appName else "$name — $appName"
}

private fun mergeLoadedModel(parsed: ConceptualSchema, openedFromBrm: Boolean, picked: PickedFile): ConceptualSchema =
    parsed.copy(
        name = picked.name,
        filePath = picked.diskPath ?: "",
        openedFromBrm = openedFromBrm,
    )

internal fun parseModelBytes(bytes: ByteArray): ConceptualSchema =
    parseModelBytesWithSource(bytes).first

internal fun parseModelBytesWithSource(bytes: ByteArray): Pair<ConceptualSchema, Boolean> {
    val isBrm = bytes.size > 10 &&
        bytes[6] == 'T'.code.toByte() &&
        bytes[7] == 'P'.code.toByte() &&
        bytes[8] == 'F'.code.toByte() &&
        bytes[9] == '0'.code.toByte()
    val schema = if (isBrm) ConceptualSchemaBrmParser.parse(bytes)
    else ConceptualSchemaXmlParser.parse(bytes)
    return schema to isBrm
}
