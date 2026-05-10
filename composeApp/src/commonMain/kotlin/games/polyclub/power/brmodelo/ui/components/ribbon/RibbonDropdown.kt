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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.polyclub.power.brmodelo.ui.DropdownEntry
import games.polyclub.power.brmodelo.ui.components.AppColors
import org.jetbrains.compose.resources.painterResource

private val STRIPE_W = 26.dp
// DropdownMenu adds 8dp vertical padding internally; extend drawBehind by this amount to cover it
private const val MENU_VPAD_DP = 8

/**
 * Dropdown matching the brModelo original style:
 * - Uses Material3 [DropdownMenu] for correct positioning below the button
 * - Draws the icon stripe via [Modifier.drawBehind] with a -8dp vertical offset so the
 *   stripe covers the DropdownMenu's internal vertical padding and reaches the border edges
 * - Icon and text live in the same [Row] so hover works over the entire item width
 */
@Composable
internal fun RibbonDropdownMenu(
    items: List<DropdownEntry>,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onItemSelected: (DropdownEntry) -> Unit = {},
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .drawBehind {
                    drawRect(
                        color = AppColors.dropdownIconStripe,
                        topLeft = Offset(x = 0f, y = -MENU_VPAD_DP.dp.toPx()),
                        size = Size(
                            width = STRIPE_W.toPx(),
                            height = size.height + (MENU_VPAD_DP * 2).dp.toPx()
                        )
                    )
                }
        ) {
            items.forEach { item ->
                if (item.isSeparatorAbove) {
                    HorizontalDivider(color = Color(0xFFDDE4EE), thickness = 1.dp)
                }
                RibbonDropdownItem(
                    item = item,
                    onClick = {
                        onItemSelected(item)
                        onDismiss()
                    },
                )
            }
        }
    }
}

/**
 * Single item row: icon box on the left (over the drawn stripe) + text on the right.
 * Hover is applied to the entire [Row] so both icon and text trigger the highlight.
 */
@Composable
internal fun RibbonDropdownItem(item: DropdownEntry, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .hoverable(interactionSource)
            .background(if (isHovered && item.enabled) AppColors.dropdownHover else Color.Transparent)
            .clickable(
                enabled = item.enabled,
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(STRIPE_W, 22.dp)
        ) {
            Image(
                painter = painterResource(item.icon),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                contentScale = ContentScale.Fit,
                alpha = if (item.enabled) 1f else 0.5f
            )
        }
        Text(
            text = item.label,
            fontSize = 11.sp,
            lineHeight = 11.sp,
            color = if (item.enabled) Color(0xFF1C2B3A) else Color(0xFFAAAAAA),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
