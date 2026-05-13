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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.polyclub.power.brmodelo.ui.RibbonMcpUi
import games.polyclub.power.brmodelo.ui.components.AppColors
import games.polyclub.power.brmodelo.ui.isDesktopTarget

private val IA_LARGE_BUTTON_W = 72.dp

/**
 * Ribbon group **Inteligência Artificial**: large MCP settings control plus **Iniciar** / **Parar** placeholders.
 * Icons are drawn with vector-style Canvas paths (no PNG assets).
 */
@Composable
internal fun RibbonGroupIa(
    ribbonMcp: RibbonMcpUi?,
    onRibbonUserMessage: (String) -> Unit,
) {
    val startEnabled = ribbonMcp?.startServerEnabled == true
    val stopEnabled = ribbonMcp?.stopServerEnabled == true
    val onMcpClick = {
        if (ribbonMcp != null) {
            ribbonMcp.onOpenSettings()
        } else {
            onRibbonUserMessage(iaRibbonMcpSettingsBannerText(isDesktopTarget))
        }
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .drawBehind { drawRect(AppColors.ribbonBorder, style = Stroke(width = 1.dp.toPx())) }
            .background(AppColors.ribbonGroupBg)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IaLargeMcpButton(
                buttonWidth = IA_LARGE_BUTTON_W,
                onClick = onMcpClick,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.wrapContentWidth(),
            ) {
                IaSmallGlyphButton(
                    title = "Iniciar",
                    enabled = startEnabled,
                    onClick = { ribbonMcp?.onStartServer?.invoke() },
                    icon = { m, en -> RibbonPlayGlyphIcon(m, en) },
                )
                IaSmallGlyphButton(
                    title = "Parar",
                    enabled = stopEnabled,
                    onClick = { ribbonMcp?.onStopServer?.invoke() },
                    icon = { m, en -> RibbonStopGlyphIcon(m, en) },
                )
            }
        }
        RibbonGroupTitle("Inteligência Artificial", maxLines = 2)
    }
}

@Composable
private fun IaLargeMcpButton(
    buttonWidth: Dp,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isActive = isHovered
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .width(buttonWidth)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(if (isActive) AppColors.hoverBg else Color.Transparent, AppColors.hoverShape)
            .border(1.dp, if (isActive) AppColors.hoverBorder else Color.Transparent, AppColors.hoverShape)
            .padding(horizontal = 3.dp, vertical = 3.dp),
    ) {
        RibbonMcpHubIcon(
            modifier = Modifier.size(42.dp),
            enabled = true,
        )
        Text(
            text = "MCP",
            fontSize = 9.sp,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            lineHeight = 10.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        )
    }
}

@Composable
private fun IaSmallGlyphButton(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Modifier, Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val canClick = enabled
    val isActive = enabled && isHovered
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(20.dp)
            .then(if (enabled) Modifier.hoverable(interactionSource) else Modifier)
            .then(
                if (canClick) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .background(if (isActive) AppColors.hoverBg else Color.Transparent, AppColors.hoverShape)
            .border(1.dp, if (isActive) AppColors.hoverBorder else Color.Transparent, AppColors.hoverShape)
            .padding(horizontal = 2.dp),
    ) {
        icon(Modifier.size(16.dp), enabled)
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            color = if (enabled) Color(0xFF2C3E50) else Color(0xFF8A8A8A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
