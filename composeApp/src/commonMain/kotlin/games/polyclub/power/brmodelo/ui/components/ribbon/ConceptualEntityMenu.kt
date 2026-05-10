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

import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.generated.resources.entidade_associativa_l
import games.polyclub.power.brmodelo.generated.resources.entidade_associativa_s
import games.polyclub.power.brmodelo.generated.resources.entidade_l
import games.polyclub.power.brmodelo.generated.resources.entidade_s
import games.polyclub.power.brmodelo.generated.resources.relacao_l
import games.polyclub.power.brmodelo.generated.resources.relacao_s
import games.polyclub.power.brmodelo.ui.DropdownEntry
import games.polyclub.power.brmodelo.ui.EntityToolVariant
import org.jetbrains.compose.resources.DrawableResource

/** Dropdown rows for the Entidade split control (shared by ribbon data and tool state). */
internal fun conceptualEntityDropdownEntries(): List<DropdownEntry> =
    listOf(
        DropdownEntry(
            label = "Entidade",
            icon = Res.drawable.entidade_s,
            entityVariant = EntityToolVariant.Plain,
        ),
        DropdownEntry(
            label = "Relação",
            icon = Res.drawable.relacao_s,
            entityVariant = EntityToolVariant.Relation,
        ),
        DropdownEntry(
            label = "Entidade Associativa",
            icon = Res.drawable.entidade_associativa_s,
            entityVariant = EntityToolVariant.Associative,
            ribbonShortTitle = "Ent. Assoc",
        ),
    )

/** Label and large ribbon icon for the given entity variant (split button highlight). */
internal fun entityVariantRibbonPresentation(variant: EntityToolVariant): Pair<String, DrawableResource> {
    val row = conceptualEntityDropdownEntries().first { it.entityVariant == variant }
    val title = row.ribbonShortTitle ?: row.label
    val largeIcon = when (variant) {
        EntityToolVariant.Plain -> Res.drawable.entidade_l
        EntityToolVariant.Relation -> Res.drawable.relacao_l
        EntityToolVariant.Associative -> Res.drawable.entidade_associativa_l
    }
    return title to largeIcon
}
