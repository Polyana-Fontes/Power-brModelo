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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import games.polyclub.power.brmodelo.ui.AttributeToolRibbonBinding
import games.polyclub.power.brmodelo.ui.AutoSelfRelationshipToolRibbonBinding
import games.polyclub.power.brmodelo.ui.EntityToolRibbonBinding
import games.polyclub.power.brmodelo.ui.LinkObjectsToolRibbonBinding
import games.polyclub.power.brmodelo.ui.ObservationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.OperationsMenuRibbonBinding
import games.polyclub.power.brmodelo.ui.RibbonTab
import games.polyclub.power.brmodelo.ui.SpecializationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.components.AppColors

@Composable
internal fun HeaderRibbon(
    selectedTab: RibbonTab,
    entityToolBinding: EntityToolRibbonBinding? = null,
    observationToolBinding: ObservationToolRibbonBinding? = null,
    linkObjectsToolBinding: LinkObjectsToolRibbonBinding? = null,
    autoSelfRelationshipToolBinding: AutoSelfRelationshipToolRibbonBinding? = null,
    specializationToolBinding: SpecializationToolRibbonBinding? = null,
    attributeToolBinding: AttributeToolRibbonBinding? = null,
    operationsMenuBinding: OperationsMenuRibbonBinding? = null,
    onMainMenuClick: () -> Unit,
    onTabSelect: (RibbonTab) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.ribbonBg)
            .drawBehind {
                drawRect(color = AppColors.ribbonBorder, style = Stroke(width = 1.dp.toPx()))
            }
    ) {
        RibbonTopBar(
            selectedTab = selectedTab,
            onMainMenuClick = onMainMenuClick,
            onTabSelect = onTabSelect
        )
        when (selectedTab) {
            RibbonTab.EsquemaConceitual -> RibbonEsquemaConceitual(
                entityToolBinding = entityToolBinding,
                observationToolBinding = observationToolBinding,
                linkObjectsToolBinding = linkObjectsToolBinding,
                autoSelfRelationshipToolBinding = autoSelfRelationshipToolBinding,
                specializationToolBinding = specializationToolBinding,
                attributeToolBinding = attributeToolBinding,
                operationsMenuBinding = operationsMenuBinding,
            )
            RibbonTab.Opcoes            -> RibbonOpcoes()
        }
    }
}
