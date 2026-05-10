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

import games.polyclub.power.brmodelo.domain.ConceptualAttributeToolVariant
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.generated.resources.atributo_composto_s
import games.polyclub.power.brmodelo.generated.resources.atributo_identificador_s
import games.polyclub.power.brmodelo.generated.resources.atributo_l
import games.polyclub.power.brmodelo.generated.resources.atributo_multivalorado_s
import games.polyclub.power.brmodelo.generated.resources.atributo_opcional_s
import games.polyclub.power.brmodelo.generated.resources.atributo_s
import games.polyclub.power.brmodelo.ui.DropdownEntry
import org.jetbrains.compose.resources.DrawableResource

/** Dropdown rows for the Atributo split control (mirrors Pascal tool variants). */
internal fun conceptualAttributeDropdownEntries(): List<DropdownEntry> =
    listOf(
        DropdownEntry(
            label = "Atributo",
            icon = Res.drawable.atributo_s,
            attributeVariant = ConceptualAttributeToolVariant.Basic,
        ),
        DropdownEntry(
            label = "Atributo Identificador",
            icon = Res.drawable.atributo_identificador_s,
            attributeVariant = ConceptualAttributeToolVariant.Identifier,
            ribbonShortTitle = "Identificador",
        ),
        DropdownEntry(
            label = "Atributo Multivalorado",
            icon = Res.drawable.atributo_multivalorado_s,
            attributeVariant = ConceptualAttributeToolVariant.MultiValued,
            ribbonShortTitle = "Multivalorado",
        ),
        DropdownEntry(
            label = "Atributo Composto",
            icon = Res.drawable.atributo_composto_s,
            attributeVariant = ConceptualAttributeToolVariant.Composite,
            ribbonShortTitle = "Composto",
        ),
        DropdownEntry(
            label = "Atributo Opcional",
            icon = Res.drawable.atributo_opcional_s,
            attributeVariant = ConceptualAttributeToolVariant.Optional,
            ribbonShortTitle = "Opcional",
        ),
    )

internal fun attributeVariantRibbonPresentation(
    variant: ConceptualAttributeToolVariant,
): Pair<String, DrawableResource> {
    val row = conceptualAttributeDropdownEntries().first { it.attributeVariant == variant }
    val title = row.ribbonShortTitle ?: row.label
    return title to Res.drawable.atributo_l
}
