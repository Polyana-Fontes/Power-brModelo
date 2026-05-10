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

import games.polyclub.power.brmodelo.ui.ConceptualRibbonOperation
import games.polyclub.power.brmodelo.ui.DropdownEntry
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.generated.resources.atributo_s
import games.polyclub.power.brmodelo.generated.resources.dicionario_dados_3s
import games.polyclub.power.brmodelo.generated.resources.entidade_associativa_s
import games.polyclub.power.brmodelo.generated.resources.entidade_s
import games.polyclub.power.brmodelo.generated.resources.especializacao_exclusiva_s
import games.polyclub.power.brmodelo.generated.resources.especializacao_nao_exclusiva_s
import games.polyclub.power.brmodelo.generated.resources.selecionar_s

internal fun conceptualOperationsDropdownEntries(
    organizeAttributesEnabled: Boolean,
    selectAttributesEnabled: Boolean,
): List<DropdownEntry> =
    listOf(
        DropdownEntry(
            label = "Ocultar Atributo",
            icon = Res.drawable.atributo_s,
            enabled = false,
        ),
        DropdownEntry(
            label = "Organizar Atributos",
            icon = Res.drawable.atributo_s,
            enabled = organizeAttributesEnabled,
            conceptualOperation = ConceptualRibbonOperation.OrganizeAttributes,
        ),
        DropdownEntry(
            label = "Selecionar Atributos",
            icon = Res.drawable.selecionar_s,
            enabled = selectAttributesEnabled,
            conceptualOperation = ConceptualRibbonOperation.SelectAttributes,
        ),
        DropdownEntry(
            label = "Converter em Entidade Associativa",
            icon = Res.drawable.entidade_associativa_s,
            isSeparatorAbove = true,
        ),
        DropdownEntry(
            label = "Converter em Entidade",
            icon = Res.drawable.entidade_s,
        ),
        DropdownEntry(
            label = "Converter Esp. para Restrita",
            icon = Res.drawable.especializacao_exclusiva_s,
            isSeparatorAbove = true,
        ),
        DropdownEntry(
            label = "Converter Esp. para Opcional",
            icon = Res.drawable.especializacao_nao_exclusiva_s,
        ),
        DropdownEntry(
            label = "Dicionário de Dados do Objeto",
            icon = Res.drawable.dicionario_dados_3s,
            isSeparatorAbove = true,
        ),
    )
