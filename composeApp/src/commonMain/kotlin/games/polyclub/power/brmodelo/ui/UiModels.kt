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

import org.jetbrains.compose.resources.DrawableResource

internal enum class MainMenuType {
    NewModel,
    Print
}

internal enum class RibbonTab {
    EsquemaConceitual,
    Opcoes
}

internal data class DropdownEntry(
    val label: String,
    val icon: DrawableResource,
    val enabled: Boolean = true,
    val isSeparatorAbove: Boolean = false,
    /** When set, choosing this row selects an entity placement variant on the canvas. */
    val entityVariant: EntityToolVariant? = null,
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

internal data class MenuEntry(
    val title: String,
    val icon: DrawableResource,
    val dropdown: List<DropdownEntry>? = null
)
