/*
 * KbrModelo - Kotlin port of brModelo 3.0 originally written in Pascal
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

package games.polyclub.kbrmodelo.ui.components.ribbon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.polyclub.kbrmodelo.ui.MenuEntry
import games.polyclub.kbrmodelo.ui.components.AppColors
import org.jetbrains.compose.resources.painterResource

/**
 * Standard ribbon button: icon at the top, label immediately below.
 * When [entry.dropdown] is non-null the button shows a ▾ indicator and opens a dropdown on click.
 */
@Composable
internal fun RibbonButton(entry: MenuEntry) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showDropdown by remember { mutableStateOf(false) }
    val hasDropdown = !entry.dropdown.isNullOrEmpty()
    val isActive = isHovered || showDropdown

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
                .hoverable(interactionSource)
                .background(if (isActive) AppColors.hoverBg else Color.Transparent, AppColors.hoverShape)
                .border(1.dp, if (isActive) AppColors.hoverBorder else Color.Transparent, AppColors.hoverShape)
                .padding(horizontal = 3.dp, vertical = 3.dp)
                .then(
                    if (hasDropdown) Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { showDropdown = !showDropdown } else Modifier
                )
        ) {
            Image(
                painter = painterResource(entry.icon),
                contentDescription = entry.title,
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = entry.title,
                fontSize = 9.sp,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center,
                lineHeight = 10.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
            if (hasDropdown) {
                val arrowColor = Color(0xFF556677)
                Canvas(modifier = Modifier.padding(top = 2.dp).size(width = 8.dp, height = 4.dp)) {
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        },
                        color = arrowColor
                    )
                }
            }
        }

        if (hasDropdown) {
            RibbonDropdownMenu(
                items = entry.dropdown!!,
                expanded = showDropdown,
                onDismiss = { showDropdown = false }
            )
        }
    }
}

/** Large ribbon button: tall icon + multi-line label below. Used as the primary button in mixed groups. */
@Composable
internal fun LargeRibbonButton(entry: MenuEntry, buttonWidth: Dp) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .width(buttonWidth)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .background(if (isHovered) AppColors.hoverBg else Color.Transparent, AppColors.hoverShape)
            .border(1.dp, if (isHovered) AppColors.hoverBorder else Color.Transparent, AppColors.hoverShape)
            .padding(horizontal = 3.dp, vertical = 3.dp)
    ) {
        Image(
            painter = painterResource(entry.icon),
            contentDescription = entry.title,
            modifier = Modifier.size(42.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = entry.title,
            fontSize = 9.sp,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            lineHeight = 10.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
    }
}

/** Small stacked ribbon button: icon + label side by side, for use inside mixed groups. */
@Composable
internal fun SmallRibbonButton(entry: MenuEntry) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    // Fixed height ensures the Row never grows taller than necessary and
    // CenterVertically reliably centers both icon and text on the same axis.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(20.dp)
            .hoverable(interactionSource)
            .background(if (isHovered) AppColors.hoverBg else Color.Transparent, AppColors.hoverShape)
            .border(1.dp, if (isHovered) AppColors.hoverBorder else Color.Transparent, AppColors.hoverShape)
            .padding(horizontal = 2.dp)
    ) {
        Image(
            painter = painterResource(entry.icon),
            contentDescription = entry.title,
            modifier = Modifier.size(16.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = entry.title,
            fontSize = 9.sp,
            // lineHeight = fontSize removes the default font descender padding that
            // causes the text baseline to appear lower than the icon center.
            lineHeight = 9.sp,
            color = Color(0xFF2C3E50),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
