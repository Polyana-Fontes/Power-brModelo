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

package games.polyclub.power.brmodelo.ui

import androidx.compose.ui.geometry.Rect
import games.polyclub.power.brmodelo.domain.BulkDeleteCategoryCounts
import games.polyclub.power.brmodelo.domain.ConceptualAttributeToolVariant
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolVariant
import org.jetbrains.compose.resources.DrawableResource

internal enum class MainMenuType {
    NewModel,
    Print
}

internal enum class RibbonTab {
    EsquemaConceitual,
    Opcoes
}

/** Inspector sidebar tabs (Seleção vs Atributos ocultos). */
internal enum class InspectorTab {
    Selecao,
    AtrOcultos,
}

/** Row keys in the inspector "Seleção" grid ([games.polyclub.power.brmodelo.ui.InspectorPanel]). */
internal object InspectorSelectionFieldKeys {
    const val Name = "NOME"
    const val CardinalityRole = "PAPEL"
}

/**
 * One-shot request to switch to [InspectorTab.Selecao] and focus a single-line editable row
 * in [games.polyclub.power.brmodelo.ui.InspectorPanel].
 *
 * [revision] must change whenever [fieldKey] repeats so [LaunchedEffect] re-runs.
 */
internal data class InspectorSelectionFieldFocusRequest(
    val fieldKey: String,
    val revision: Long,
)

/** Ribbon **Operações** dropdown actions (conceptual schema tab). */
internal enum class ConceptualRibbonOperation {
    OrganizeAttributes,
    SelectAttributes,
    PromoteToAssociativeEntity,
    PromoteAttributeToEntity,
    DemoteAssociativeToRelationship,
    DemoteAssociativeToEntity,
    MergeEntityAndRelationshipToAssociative,
    ConvertOptionalSpecializationsToRestricted,
    ConvertRestrictedSpecializationToOptionals,
    HideCanvasAttribute,
    RevealHiddenAttribute,
    EditBulkDataDictionary,
}

internal data class DropdownEntry(
    val label: String,
    val icon: DrawableResource,
    val enabled: Boolean = true,
    val isSeparatorAbove: Boolean = false,
    /** When set, choosing this row runs a conceptual-schema operation (e.g. Organizar Atributos). */
    val conceptualOperation: ConceptualRibbonOperation? = null,
    /** When set, choosing this row selects an entity placement variant on the canvas. */
    val entityVariant: EntityToolVariant? = null,
    /** When set, choosing this row selects a specialization tool variant on the canvas. */
    val specializationVariant: ConceptualSpecializationToolVariant? = null,
    /** When set, choosing this row selects an attribute tool variant on the canvas. */
    val attributeVariant: ConceptualAttributeToolVariant? = null,
    /** Shorter label for the ribbon split button when this variant is selected (optional). */
    val ribbonShortTitle: String? = null,
)

internal data class EntityToolRibbonBinding(
    val variant: EntityToolVariant,
    val isArmed: Boolean,
    val displayTitle: String,
    val displayIcon: DrawableResource,
    val onMainClick: () -> Unit,
    val onDropdownVariant: (EntityToolVariant) -> Unit,
)

/** Toggle for the conceptual-schema “Observação” placement tool (single ribbon button). */
internal data class ObservationToolRibbonBinding(
    val isArmed: Boolean,
    val onClick: () -> Unit,
)

/** Toggle for the conceptual-schema "Ligar objetos" tool. */
internal data class LinkObjectsToolRibbonBinding(
    val isArmed: Boolean,
    val onClick: () -> Unit,
)

/** Toggle for the conceptual-schema “Auto-relacionamento” placement tool. */
internal data class AutoSelfRelationshipToolRibbonBinding(
    val isArmed: Boolean,
    val onClick: () -> Unit,
)

/** Split-button binding for the conceptual-schema “Especialização” tools (three Pascal variants). */
internal data class SpecializationToolRibbonBinding(
    val variant: ConceptualSpecializationToolVariant,
    val isArmed: Boolean,
    val displayTitle: String,
    val displayIcon: DrawableResource,
    val onMainClick: () -> Unit,
    val onDropdownVariant: (ConceptualSpecializationToolVariant) -> Unit,
)

/** Split-button binding for the conceptual-schema “Atributo” tools (Pascal `Tool_Atributo*`). */
internal data class AttributeToolRibbonBinding(
    val variant: ConceptualAttributeToolVariant,
    val isArmed: Boolean,
    val displayTitle: String,
    val displayIcon: DrawableResource,
    val onMainClick: () -> Unit,
    val onDropdownVariant: (ConceptualAttributeToolVariant) -> Unit,
)

/** Live UI for the rubber-band bulk-delete tool (view rect + schema-derived counts). */
internal data class BulkDeleteUiState(
    val viewSelectionRect: Rect,
    val markedElementIds: Set<Int>,
    val counts: BulkDeleteCategoryCounts,
)

/** Live UI for rectangle multi-select preview (same shape as [BulkDeleteUiState]; blue band on canvas). */
internal data class SelectionBandUiState(
    val viewSelectionRect: Rect,
    val markedElementIds: Set<Int>,
    val markedCardinalityConnectionIds: Set<Int> = emptySet(),
    val counts: BulkDeleteCategoryCounts,
)

/** Toggle for the conceptual-schema “Excluir Objetos” tool. */
internal data class BulkDeleteObjectsToolRibbonBinding(
    val isArmed: Boolean,
    val onClick: () -> Unit,
)

/** Toggle for the conceptual-schema “Seleção” (rectangle multi-select) tool. */
internal data class RectangleSelectionToolRibbonBinding(
    val isArmed: Boolean,
    val onClick: () -> Unit,
)

/** **Operações** split/dropdown: enables menu rows and dispatches conceptual commands. */
internal data class OperationsMenuRibbonBinding(
    val organizeAttributesEnabled: Boolean,
    val onOrganizeAttributes: () -> Unit,
    val selectAttributesEnabled: Boolean,
    val onSelectAttributes: () -> Unit,
    val promoteToAssociativeEnabled: Boolean,
    val onPromoteToAssociative: () -> Unit,
    val promoteAttributeToEntityEnabled: Boolean,
    val onPromoteAttributeToEntity: () -> Unit,
    val demoteAssociativeToRelationshipEnabled: Boolean,
    val onDemoteAssociativeToRelationship: () -> Unit,
    val demoteAssociativeToEntityEnabled: Boolean,
    val onDemoteAssociativeToEntity: () -> Unit,
    val mergeEntityAndRelationshipToAssociativeEnabled: Boolean,
    val onMergeEntityAndRelationshipToAssociative: () -> Unit,
    val convertOptionalSpecializationsToRestrictedEnabled: Boolean,
    val onConvertOptionalSpecializationsToRestricted: () -> Unit,
    val convertRestrictedSpecializationToOptionalsEnabled: Boolean,
    val onConvertRestrictedSpecializationToOptionals: () -> Unit,
    val hideCanvasAttributeEnabled: Boolean,
    val onHideCanvasAttribute: () -> Unit,
    val revealHiddenAttributeEnabled: Boolean,
    val onRevealHiddenAttribute: () -> Unit,
    val dataDictionaryBulkEnabled: Boolean,
    val onOpenDataDictionaryBulk: () -> Unit,
    val undoEnabled: Boolean,
    val onUndo: () -> Unit,
    val redoEnabled: Boolean,
    val onRedo: () -> Unit,
    val conceptualFindEnabled: Boolean,
    val onOpenConceptualFind: () -> Unit,
)

internal data class ClipboardRibbonBinding(
    val copyEnabled: Boolean,
    val cutEnabled: Boolean,
    val pasteEnabled: Boolean,
    val onCopy: () -> Unit,
    val onCut: () -> Unit,
    val onPaste: () -> Unit,
)

/** Ribbon **Selecionar Fonte** (Pascal `Exibir_fonte` / `CfgFonte` + `TFontDialog`). */
internal data class SelectFontRibbonBinding(
    val enabled: Boolean,
    val onSelectFont: () -> Unit,
)

/** Desktop MCP ribbon controls; null on Wasm (snackbar-only MCP messaging). */
internal data class RibbonMcpUi(
    val onOpenSettings: () -> Unit,
    val onStartServer: () -> Unit,
    val onStopServer: () -> Unit,
    val onCopyServerAddress: () -> Unit,
    val startServerEnabled: Boolean,
    val stopServerEnabled: Boolean,
)

internal data class MenuEntry(
    val title: String,
    val icon: DrawableResource,
    val dropdown: List<DropdownEntry>? = null,
    val onClick: (() -> Unit)? = null,
    val enabled: Boolean = true,
)
