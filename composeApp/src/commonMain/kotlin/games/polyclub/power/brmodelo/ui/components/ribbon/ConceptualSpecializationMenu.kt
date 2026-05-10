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

package games.polyclub.power.brmodelo.ui.components.ribbon

import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolVariant
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.generated.resources.especializacao_exclusiva_l
import games.polyclub.power.brmodelo.generated.resources.especializacao_l
import games.polyclub.power.brmodelo.generated.resources.especializacao_nao_exclusiva
import games.polyclub.power.brmodelo.generated.resources.especializacao_s
import games.polyclub.power.brmodelo.generated.resources.especializacao_exclusiva_s
import games.polyclub.power.brmodelo.generated.resources.especializacao_nao_exclusiva_s
import games.polyclub.power.brmodelo.ui.DropdownEntry
import org.jetbrains.compose.resources.DrawableResource

/** Dropdown rows for the Especialização split control (mirrors Pascal tool variants). */
internal fun conceptualSpecializationDropdownEntries(): List<DropdownEntry> =
    listOf(
        DropdownEntry(
            label = "Especialização",
            icon = Res.drawable.especializacao_s,
            specializationVariant = ConceptualSpecializationToolVariant.Basic,
        ),
        DropdownEntry(
            label = "Especialização Exclusiva com Criação de Entidade",
            icon = Res.drawable.especializacao_exclusiva_s,
            specializationVariant = ConceptualSpecializationToolVariant.ExclusiveWithEntityCreation,
            ribbonShortTitle = "Esp. exclusiva",
        ),
        DropdownEntry(
            label = "Especialização Não-Exclusiva com Criação de Entidade",
            icon = Res.drawable.especializacao_nao_exclusiva_s,
            specializationVariant = ConceptualSpecializationToolVariant.NonExclusiveWithEntityCreation,
            ribbonShortTitle = "Esp. não exclusiva",
        ),
    )

internal fun specializationVariantRibbonPresentation(
    variant: ConceptualSpecializationToolVariant,
): Pair<String, DrawableResource> {
    val row = conceptualSpecializationDropdownEntries().first { it.specializationVariant == variant }
    val title = row.ribbonShortTitle ?: row.label
    val largeIcon = when (variant) {
        ConceptualSpecializationToolVariant.Basic -> Res.drawable.especializacao_l
        ConceptualSpecializationToolVariant.ExclusiveWithEntityCreation ->
            Res.drawable.especializacao_exclusiva_l
        ConceptualSpecializationToolVariant.NonExclusiveWithEntityCreation ->
            Res.drawable.especializacao_nao_exclusiva
    }
    return title to largeIcon
}
