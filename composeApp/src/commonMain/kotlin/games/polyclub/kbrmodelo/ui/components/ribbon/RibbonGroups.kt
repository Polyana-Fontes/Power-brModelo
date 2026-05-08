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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.polyclub.kbrmodelo.ui.MenuEntry
import games.polyclub.kbrmodelo.ui.components.AppColors

/** Standard group: all buttons in a single row, group title below. */
@Composable
internal fun RibbonGroup(title: String, items: List<MenuEntry>) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .drawBehind { drawRect(AppColors.ribbonBorder, style = Stroke(width = 1.dp.toPx())) }
            .background(AppColors.ribbonGroupBg)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Top
        ) {
            items.forEach { RibbonButton(it) }
        }
        RibbonGroupTitle(title)
    }
}

/** Group with vertical separators between sub-groups. */
@Composable
internal fun RibbonGroupWithSeparators(title: String, groups: List<List<MenuEntry>>) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .drawBehind { drawRect(AppColors.ribbonBorder, style = Stroke(width = 1.dp.toPx())) }
            .background(AppColors.ribbonGroupBg)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            groups.forEachIndexed { index, group ->
                if (index > 0) RibbonGroupSeparator()
                group.forEach { RibbonButton(it) }
            }
        }
        RibbonGroupTitle(title)
    }
}

/**
 * Mixed ribbon group: large button on the left, small buttons stacked on the right.
 * Used in the "Opções" ribbon tab (Documentação, Área de Transferência).
 */
@Composable
internal fun RibbonGroupMixed(
    title: String,
    largeButtonWidth: Dp,
    largeEntry: MenuEntry,
    smallEntries: List<MenuEntry>
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .drawBehind { drawRect(AppColors.ribbonBorder, style = Stroke(width = 1.dp.toPx())) }
            .background(AppColors.ribbonGroupBg)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LargeRibbonButton(entry = largeEntry, buttonWidth = largeButtonWidth)
            Spacer(modifier = Modifier.width(4.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                smallEntries.forEach { SmallRibbonButton(it) }
            }
        }
        RibbonGroupTitle(title)
    }
}

@Composable
internal fun RibbonGroupTitle(title: String) {
    Text(
        text = title,
        fontSize = 9.sp,
        lineHeight = 9.sp,
        color = AppColors.ribbonGroupTitle,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 1.dp, bottom = 1.dp)
    )
}

@Composable
internal fun RibbonGroupSeparator() {
    Spacer(modifier = Modifier.width(4.dp))
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(AppColors.ribbonSeparator)
    )
    Spacer(modifier = Modifier.width(4.dp))
}
