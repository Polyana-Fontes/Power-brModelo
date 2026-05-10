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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import games.polyclub.power.brmodelo.ui.AttributeToolRibbonBinding
import games.polyclub.power.brmodelo.ui.DropdownEntry
import games.polyclub.power.brmodelo.ui.BulkDeleteObjectsToolRibbonBinding
import games.polyclub.power.brmodelo.ui.RectangleSelectionToolRibbonBinding
import games.polyclub.power.brmodelo.ui.AutoSelfRelationshipToolRibbonBinding
import games.polyclub.power.brmodelo.ui.EntityToolRibbonBinding
import games.polyclub.power.brmodelo.ui.LinkObjectsToolRibbonBinding
import games.polyclub.power.brmodelo.ui.MenuEntry
import games.polyclub.power.brmodelo.ui.ObservationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.SpecializationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.components.AppColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val SPLIT_ARROW_STRIP_H = 14.dp
private val SPLIT_INNER_SEGMENT_SHAPE = RoundedCornerShape(2.dp)

/** Conceptual-schema ribbon entries that use a split control (main = default tool, arrow = category menu). */
private val RIBBON_SPLIT_DROPDOWN_TITLES = setOf(
    "Entidade",
    "Atributo",
)

/**
 * Ribbon control with a split layout: top = primary tool, bottom strip = ▾ opens category menu only.
 * Shared chrome (background + border) follows hover anywhere on the control bounds, not only on the two click targets.
 * Each hit target still has its own hover highlight (tint + soft border) on top of that chrome.
 */
@Composable
internal fun RibbonSplitDropdownButton(
    entry: MenuEntry,
    displayTitle: String = entry.title,
    displayIcon: DrawableResource = entry.icon,
    isPrimaryToolArmed: Boolean = false,
    onMainClick: () -> Unit = {},
    onDropdownItemSelected: (DropdownEntry) -> Unit = {},
) {
    val dropdownItems = entry.dropdown ?: return
    var showDropdown by remember { mutableStateOf(false) }

    val outerInteraction = remember { MutableInteractionSource() }
    val mainInteraction = remember { MutableInteractionSource() }
    val arrowInteraction = remember { MutableInteractionSource() }
    val isOuterHovered by outerInteraction.collectIsHoveredAsState()
    val isMainHovered by mainInteraction.collectIsHoveredAsState()
    val isArrowHovered by arrowInteraction.collectIsHoveredAsState()
    val isControlActive =
        isOuterHovered || isMainHovered || isArrowHovered || showDropdown

    val outerBg = when {
        isControlActive -> AppColors.hoverBg
        isPrimaryToolArmed -> AppColors.ribbonToolArmedIdleFill
        else -> Color.Transparent
    }
    val outerBorder = when {
        isControlActive -> AppColors.hoverBorder
        isPrimaryToolArmed -> AppColors.ribbonToolArmedBorder
        else -> Color.Transparent
    }

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                // Min collapses to icon width when the main segment uses fillMaxWidth(); Max uses the
                // widest child's intrinsic width (full short title e.g. "Ent. Assoc.").
                .width(IntrinsicSize.Max)
                .fillMaxHeight()
                .hoverable(outerInteraction)
                .background(outerBg, AppColors.hoverShape)
                .border(1.dp, outerBorder, AppColors.hoverShape)
                .padding(horizontal = 3.dp, vertical = 3.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        color = if (isMainHovered) AppColors.ribbonSplitMainSegmentHover else Color.Transparent,
                        shape = SPLIT_INNER_SEGMENT_SHAPE,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isMainHovered) AppColors.hoverBorder.copy(alpha = 0.45f) else Color.Transparent,
                        shape = SPLIT_INNER_SEGMENT_SHAPE,
                    )
                    .clickable(
                        interactionSource = mainInteraction,
                        indication = null,
                    ) {
                        showDropdown = false
                        onMainClick()
                    },
            ) {
                Image(
                    painter = painterResource(displayIcon),
                    contentDescription = displayTitle,
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = displayTitle,
                    fontSize = 9.sp,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 3.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 2.dp),
                thickness = 1.dp,
                color = if (isControlActive) AppColors.ribbonSplitDividerActive else Color.Transparent,
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(1.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SPLIT_ARROW_STRIP_H)
                        .background(
                            color = if (isArrowHovered) AppColors.ribbonSplitArrowSegmentHover else Color.Transparent,
                            shape = SPLIT_INNER_SEGMENT_SHAPE,
                        )
                        .border(
                            width = 1.dp,
                            color = if (isArrowHovered) AppColors.hoverBorder.copy(alpha = 0.45f) else Color.Transparent,
                            shape = SPLIT_INNER_SEGMENT_SHAPE,
                        )
                        .clickable(
                            interactionSource = arrowInteraction,
                            indication = null,
                        ) { showDropdown = !showDropdown },
                ) {
                    val arrowColor = Color(0xFF556677)
                    Canvas(modifier = Modifier.size(width = 8.dp, height = 4.dp)) {
                        drawPath(
                            path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                lineTo(size.width / 2f, size.height)
                                close()
                            },
                            color = arrowColor,
                        )
                    }
                }
            }
        }

        RibbonDropdownMenu(
            items = dropdownItems,
            expanded = showDropdown,
            onDismiss = { showDropdown = false },
            onItemSelected = onDropdownItemSelected,
        )
    }
}

/**
 * Single conceptual-schema tool button with optional “armed” chrome (same family as the entity split).
 */
@Composable
internal fun RibbonArmedToolButton(
    entry: MenuEntry,
    isArmed: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isActive = isHovered
    val bg = when {
        isActive -> AppColors.hoverBg
        isArmed -> AppColors.ribbonToolArmedIdleFill
        else -> Color.Transparent
    }
    val border = when {
        isActive -> AppColors.hoverBorder
        isArmed -> AppColors.ribbonToolArmedBorder
        else -> Color.Transparent
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight()
            .hoverable(interactionSource)
            .background(bg, AppColors.hoverShape)
            .border(1.dp, border, AppColors.hoverShape)
            .padding(horizontal = 3.dp, vertical = 3.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        val labelLines = if ('\n' in entry.title) 2 else 1
        Image(
            painter = painterResource(entry.icon),
            contentDescription = entry.title.replace('\n', ' '),
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = entry.title,
            fontSize = 9.sp,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            lineHeight = 10.sp,
            maxLines = labelLines,
            softWrap = labelLines > 1,
            overflow = if (labelLines > 1) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/**
 * Dispatches to [RibbonSplitDropdownButton] for conceptual-schema tools with a category menu
 * ([RIBBON_SPLIT_DROPDOWN_TITLES]); otherwise [RibbonButton].
 */
@Composable
internal fun RibbonMenuEntryButton(
    entry: MenuEntry,
    entityToolBinding: EntityToolRibbonBinding? = null,
    observationToolBinding: ObservationToolRibbonBinding? = null,
    linkObjectsToolBinding: LinkObjectsToolRibbonBinding? = null,
    autoSelfRelationshipToolBinding: AutoSelfRelationshipToolRibbonBinding? = null,
    specializationToolBinding: SpecializationToolRibbonBinding? = null,
    attributeToolBinding: AttributeToolRibbonBinding? = null,
    bulkDeleteObjectsToolBinding: BulkDeleteObjectsToolRibbonBinding? = null,
    rectangleSelectionToolBinding: RectangleSelectionToolRibbonBinding? = null,
) {
    if (entry.title == "Observação" && observationToolBinding != null) {
        RibbonArmedToolButton(
            entry = entry,
            isArmed = observationToolBinding.isArmed,
            onClick = observationToolBinding.onClick,
        )
    } else if (entry.title == "Auto\nRelacionar" && autoSelfRelationshipToolBinding != null) {
        RibbonArmedToolButton(
            entry = entry,
            isArmed = autoSelfRelationshipToolBinding.isArmed,
            onClick = autoSelfRelationshipToolBinding.onClick,
        )
    } else if (entry.title == "Ligar\nObjetos" && linkObjectsToolBinding != null) {
        RibbonArmedToolButton(
            entry = entry,
            isArmed = linkObjectsToolBinding.isArmed,
            onClick = linkObjectsToolBinding.onClick,
        )
    } else if (entry.title == "Excluir\nObjetos" && bulkDeleteObjectsToolBinding != null) {
        RibbonArmedToolButton(
            entry = entry,
            isArmed = bulkDeleteObjectsToolBinding.isArmed,
            onClick = bulkDeleteObjectsToolBinding.onClick,
        )
    } else if (entry.title == "Seleção" && rectangleSelectionToolBinding != null) {
        RibbonArmedToolButton(
            entry = entry,
            isArmed = rectangleSelectionToolBinding.isArmed,
            onClick = rectangleSelectionToolBinding.onClick,
        )
    } else if (entry.title == "Entidade" && entityToolBinding != null && !entry.dropdown.isNullOrEmpty()) {
        RibbonSplitDropdownButton(
            entry = entry,
            displayTitle = entityToolBinding.displayTitle,
            displayIcon = entityToolBinding.displayIcon,
            isPrimaryToolArmed = entityToolBinding.isArmed,
            onMainClick = entityToolBinding.onMainClick,
            onDropdownItemSelected = { row ->
                row.entityVariant?.let { entityToolBinding.onDropdownVariant(it) }
            },
        )
    } else if (entry.title == "Especialização" && specializationToolBinding != null && !entry.dropdown.isNullOrEmpty()) {
        RibbonSplitDropdownButton(
            entry = entry,
            displayTitle = specializationToolBinding.displayTitle,
            displayIcon = specializationToolBinding.displayIcon,
            isPrimaryToolArmed = specializationToolBinding.isArmed,
            onMainClick = specializationToolBinding.onMainClick,
            onDropdownItemSelected = { row ->
                row.specializationVariant?.let { specializationToolBinding.onDropdownVariant(it) }
            },
        )
    } else if (entry.title == "Atributo" && attributeToolBinding != null && !entry.dropdown.isNullOrEmpty()) {
        RibbonSplitDropdownButton(
            entry = entry,
            displayTitle = attributeToolBinding.displayTitle,
            displayIcon = attributeToolBinding.displayIcon,
            isPrimaryToolArmed = attributeToolBinding.isArmed,
            onMainClick = attributeToolBinding.onMainClick,
            onDropdownItemSelected = { row ->
                row.attributeVariant?.let { attributeToolBinding.onDropdownVariant(it) }
            },
        )
    } else if (entry.title in RIBBON_SPLIT_DROPDOWN_TITLES && !entry.dropdown.isNullOrEmpty()) {
        RibbonSplitDropdownButton(entry = entry)
    } else {
        RibbonButton(entry)
    }
}

/**
 * Standard ribbon button: icon at the top, label immediately below.
 * When [entry.dropdown] is non-null the button shows a ▾ indicator and opens a dropdown on click.
 */
@Composable
internal fun RibbonButton(
    entry: MenuEntry,
    onDropdownItemSelected: (DropdownEntry) -> Unit = {},
) {
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
                onDismiss = { showDropdown = false },
                onItemSelected = {
                    onDropdownItemSelected(it)
                    showDropdown = false
                },
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
