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

package games.polyclub.power.brmodelo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import games.polyclub.power.brmodelo.domain.LabelStyle
import games.polyclub.power.brmodelo.domain.VclTColorTable

/**
 * Target of the conceptual font dialog: element canvas label or a connection's cardinality label.
 */
internal sealed interface ConceptualFontChooserTarget {
    data class Element(val elementId: Int) : ConceptualFontChooserTarget
    data class Cardinality(val connectionId: Int) : ConceptualFontChooserTarget
}

/**
 * Opens the font dialog for a canvas element label ([ConceptualFontChooserTarget.Element]) or
 * a connection's cardinality label ([ConceptualFontChooserTarget.Cardinality]) on tab [editorTabId].
 *
 * [openNonce] must change on every open so dialog fields re-initialise from [initial] (Compose [remember] keys).
 */
internal data class ConceptualLabelFontChooserRequest(
    val editorTabId: Long,
    val target: ConceptualFontChooserTarget,
    val initial: LabelStyle,
    val openNonce: Long,
)

// ── Inspector sidebar palette (matches [InspectorPanel] selection / grid) ─────

private val FD_SECTION_HEADER_BG = Color(0xFFD2D5D8)
private val FD_CELL_BORDER = Color(0xFFB8BCC0)
private val FD_CELL_LABEL_BG = Color(0xFFDFE2E6)
private val FD_CELL_VALUE_BG = Color(0xFFFFFFFF)
private val FD_LABEL_COLOR = Color(0xFF2A3A4A)
private val FD_VALUE_COLOR = Color(0xFF1A2535)
private val FD_SURFACE = Color(0xFFF0F2F5)

/** Windows-style list / field selection (similar to classic `TFontDialog`). */
private val WIN_SELECTION_BG = Color(0xFF0078D7)
private val WIN_SELECTION_FG = Color.White

private val FD_ROW_TEXT = TextStyle(fontSize = 10.sp, lineHeight = 10.sp, color = FD_VALUE_COLOR)

private val LABEL_PREVIEW_BASE = TextStyle(fontSize = 11.sp, color = Color.Black)

private val FONT_SIZE_PRESETS = listOf(
    8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72,
)

/** When the OS returns no families or the filter matches nothing, keep the list populated. */
private val FONT_FAMILY_FALLBACK = listOf(
    "Tahoma", "Arial", "Helvetica", "Verdana", "Segoe UI", "Calibri", "Cambria",
    "DejaVu Sans", "Liberation Sans", "Noto Sans", "Ubuntu", "Cantarell",
    "Times New Roman", "Georgia", "Courier New", "Consolas",
)

private val FONT_STYLE_ROWS = listOf("Normal", "Itálico", "Negrito", "Negrito Itálico")

private data class FontColorMenuEntry(val label: String, val colorRef: Int?)

/**
 * PT-BR labels for the standard Lazarus/VCL [TColorBox] first sixteen (`clBlack` … `clWhite`).
 * [VclTColorTable.defaultColorBoxPresets] supplies the same integers as Delphi/MER XML (`FonteCor`, etc.).
 */
private val FONT_COLOR_STANDARD_LABELS = listOf(
    "Preto",
    "Castanho",
    "Verde",
    "Verde-oliva",
    "Azul-marinho",
    "Roxo",
    "Azul petróleo",
    "Cinza",
    "Prateado",
    "Vermelho",
    "Verde limão",
    "Amarelo",
    "Azul",
    "Fúcsia",
    "Azul-piscina",
    "Branco",
)

private val FONT_COLOR_MENU: List<FontColorMenuEntry> = listOf(
    FontColorMenuEntry("Padrão do diagrama", null),
) + FONT_COLOR_STANDARD_LABELS.zip(
    VclTColorTable.defaultColorBoxPresets.take(FONT_COLOR_STANDARD_LABELS.size),
) { label, nc -> FontColorMenuEntry(label, nc.colorRef) }

private fun fontColorMenuLabelForRef(colorRef: Int?): String =
    FONT_COLOR_MENU.firstOrNull { it.colorRef == colorRef }?.label
        ?: (colorRef?.toString() ?: FONT_COLOR_MENU.first().label)

/** Classic `TFontDialog` layout using the same grid / typography language as the inspector **Seleção** tab. */
@Composable
internal fun ConceptualLabelFontChooserDialog(
    request: ConceptualLabelFontChooserRequest,
    onDismiss: () -> Unit,
    /** Reset label style to [games.polyclub.power.brmodelo.domain.ConceptualPlacementDefaults.labelStyle] and close. */
    onResetToDefault: () -> Unit,
    /** **OK**: commit and close. */
    onConfirm: (LabelStyle) -> Unit,
) {
    val k = request.openNonce
    val initial = request.initial
    var familyName by remember(k) {
        mutableStateOf(initial.fontFamilyName?.trim()?.takeIf { it.isNotEmpty() } ?: "Tahoma")
    }
    var sizePoints by remember(k) {
        mutableStateOf(initial.fontSizePoints ?: 8)
    }
    var bold by remember(k) { mutableStateOf(initial.bold) }
    var italic by remember(k) { mutableStateOf(initial.italic) }
    var underline by remember(k) { mutableStateOf(initial.underline) }
    var strikeThrough by remember(k) { mutableStateOf(initial.strikeThrough) }
    var colorRef by remember(k) { mutableStateOf<Int?>(initial.color) }

    var osFontFamilies by remember(k) { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(k) {
        osFontFamilies = platformLabelFontFamilyNames()
    }
    val platformFamilies = remember(familyName, osFontFamilies) {
        val fromOs = osFontFamilies.map { it.trim() }.filter { it.isNotEmpty() }
        (fromOs + FONT_FAMILY_FALLBACK).distinct().sorted()
    }
    // Always list every installed family; the text field is the current name (not a live filter),
    // so opening with e.g. "Tahoma" still shows the full browse list like the classic Windows dialog.
    val filteredFonts = remember(familyName, platformFamilies) {
        val cur = familyName.trim().ifEmpty { null }
        if (cur != null && cur.isNotEmpty() && platformFamilies.none { it.equals(cur, ignoreCase = true) }) {
            listOf(cur) + platformFamilies
        } else {
            platformFamilies
        }
    }

    fun styleRowLabel(): String = when {
        bold && italic -> "Negrito Itálico"
        bold -> "Negrito"
        italic -> "Itálico"
        else -> "Normal"
    }

    fun applyStyleRowLabel(label: String) {
        when (label) {
            "Normal" -> {
                bold = false
                italic = false
            }
            "Itálico" -> {
                bold = false
                italic = true
            }
            "Negrito" -> {
                bold = true
                italic = false
            }
            "Negrito Itálico" -> {
                bold = true
                italic = true
            }
        }
    }

    val fontScriptPassthrough = remember(k) { initial.fontScript }
    val scriptDisplayText = remember(fontScriptPassthrough) {
        fontScriptPassthrough?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
    }

    fun buildDraft(): LabelStyle {
        val nm = familyName.trim().ifEmpty { "Tahoma" }
        val sz = sizePoints.coerceIn(4, 144)
        return LabelStyle(
            color = colorRef,
            bold = bold,
            italic = italic,
            underline = underline,
            strikeThrough = strikeThrough,
            fontFamilyName = nm,
            fontSizePoints = sz,
            fontScript = fontScriptPassthrough,
        )
    }

    val previewStyle: TextStyle = remember(familyName, sizePoints, bold, italic, underline, strikeThrough, colorRef) {
        buildDraft().mergeOntoCanvasTextStyle(LABEL_PREVIEW_BASE)
    }

    var colorMenuExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var colorAnchorWidth by remember { mutableStateOf(0.dp) }

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            val maxBody = (maxHeight - 24.dp).coerceAtLeast(200.dp)
            Surface(
                color = FD_SURFACE,
                shape = RoundedCornerShape(2.dp),
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .border(1.dp, FD_CELL_BORDER, RoundedCornerShape(2.dp)),
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxBody)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        FontDialogHeadingBar("Fonte")
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                FontDialogScrollColumn(
                                    caption = "Fonte:",
                                    listScrollSessionKey = k,
                                    listLayoutEpoch = osFontFamilies.size,
                                    fieldText = familyName,
                                    fieldEditable = true,
                                    onFieldChange = { familyName = it },
                                    lines = filteredFonts,
                                    isLineSelected = { it.equals(familyName.trim(), ignoreCase = true) },
                                    onPickLine = { familyName = it },
                                    showTruetypeMark = true,
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .fillMaxHeight(),
                                )
                                FontDialogScrollColumn(
                                    caption = "Estilo da fonte:",
                                    listScrollSessionKey = k,
                                    fieldText = styleRowLabel(),
                                    fieldEditable = false,
                                    onFieldChange = {},
                                    lines = FONT_STYLE_ROWS,
                                    isLineSelected = { it == styleRowLabel() },
                                    onPickLine = { applyStyleRowLabel(it) },
                                    showTruetypeMark = false,
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .fillMaxHeight(),
                                )
                                FontDialogScrollColumn(
                                    caption = "Tamanho:",
                                    listScrollSessionKey = k,
                                    fieldText = sizePoints.toString(),
                                    fieldEditable = true,
                                    onFieldChange = { raw ->
                                        raw.toIntOrNull()?.let { sizePoints = it.coerceIn(4, 144) }
                                    },
                                    lines = FONT_SIZE_PRESETS.map { it.toString() },
                                    isLineSelected = { it == sizePoints.toString() },
                                    onPickLine = { sizePoints = it.toIntOrNull() ?: sizePoints },
                                    showTruetypeMark = false,
                                    modifier = Modifier
                                        .weight(0.65f)
                                        .fillMaxHeight(),
                                )
                                Column(
                                    modifier = Modifier
                                        .width(92.dp)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Spacer(Modifier.height(18.dp))
                                    OutlinedButton(
                                        onClick = { onConfirm(buildDraft()) },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                    ) {
                                        Text("OK", style = FD_ROW_TEXT)
                                    }
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                    ) {
                                        Text("Cancelar", style = FD_ROW_TEXT)
                                    }
                                    OutlinedButton(
                                        onClick = onResetToDefault,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                    ) {
                                        Text("Padrão", style = FD_ROW_TEXT)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FontDialogGroupBox(
                                    title = "Efeitos",
                                    modifier = Modifier.weight(0.42f),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = strikeThrough, onCheckedChange = { strikeThrough = it == true })
                                        Text("Riscado", style = FD_ROW_TEXT.copy(color = FD_LABEL_COLOR))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = underline, onCheckedChange = { underline = it == true })
                                        Text("Sublinhado", style = FD_ROW_TEXT.copy(color = FD_LABEL_COLOR))
                                    }
                                }
                                FontDialogGroupBox(
                                    title = "Exemplo",
                                    modifier = Modifier.weight(0.58f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(FD_CELL_VALUE_BG)
                                            .border(1.dp, FD_CELL_BORDER)
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("AaBbYyZz", style = previewStyle)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Cor:", style = FD_ROW_TEXT.copy(color = FD_LABEL_COLOR))
                                Box(modifier = Modifier.width(176.dp)) {
                                    val label = fontColorMenuLabelForRef(colorRef)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, FD_CELL_BORDER)
                                            .background(FD_CELL_VALUE_BG)
                                            .clickable { colorMenuExpanded = true }
                                            .onGloballyPositioned { coords ->
                                                colorAnchorWidth = with(density) { coords.size.width.toDp() }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        val sw = colorRef?.let { vclColorRefToCompose(it) } ?: Color.Transparent
                                        Box(
                                            Modifier
                                                .size(14.dp)
                                                .border(0.5.dp, FD_CELL_BORDER)
                                                .background(sw),
                                        )
                                        Text(
                                            label,
                                            style = FD_ROW_TEXT,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text("▾", style = FD_ROW_TEXT)
                                    }
                                    DropdownMenu(
                                        expanded = colorMenuExpanded,
                                        onDismissRequest = { colorMenuExpanded = false },
                                        modifier = Modifier.then(
                                            if (colorAnchorWidth > 0.dp) Modifier.width(colorAnchorWidth) else Modifier,
                                        ),
                                    ) {
                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                            FONT_COLOR_MENU.forEachIndexed { index, entry ->
                                                val picked = entry.colorRef == colorRef
                                                val sw = entry.colorRef?.let { vclColorRefToCompose(it) }
                                                    ?: Color.Transparent
                                                FontDialogColorPresetMenuItem(
                                                    swatchColor = sw,
                                                    text = entry.label,
                                                    selected = picked,
                                                    onClick = {
                                                        colorRef = entry.colorRef
                                                        colorMenuExpanded = false
                                                    },
                                                )
                                                if (index < FONT_COLOR_MENU.lastIndex) {
                                                    HorizontalDivider(
                                                        color = FD_CELL_BORDER,
                                                        thickness = 0.5.dp,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Text("Script:", style = FD_ROW_TEXT.copy(color = FD_LABEL_COLOR))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .widthIn(min = 112.dp)
                                        .border(1.dp, FD_CELL_BORDER)
                                        .background(FD_CELL_LABEL_BG)
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        scriptDisplayText,
                                        style = FD_ROW_TEXT.copy(color = FD_LABEL_COLOR),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One colour preset line in the font dialog menu: same layout as [InspectorPanel]'s colour dropdown rows
 * (14.dp swatch, 0.5.dp border, 10sp text, compact vertical padding).
 */
@Composable
private fun FontDialogColorPresetMenuItem(
    swatchColor: Color,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) WIN_SELECTION_BG else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .padding(end = 8.dp)
                .size(14.dp)
                .border(width = 0.5.dp, color = FD_CELL_BORDER)
                .background(swatchColor),
        )
        Text(
            text = text,
            style = FD_ROW_TEXT.copy(
                color = if (selected) WIN_SELECTION_FG else FD_VALUE_COLOR,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FontDialogHeadingBar(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FD_SECTION_HEADER_BG)
            .border(width = 0.dp, Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C2D3E),
            ),
        )
    }
    HorizontalDivider(color = FD_CELL_BORDER, thickness = 0.5.dp)
}

@Composable
private fun FontDialogGroupBox(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .border(1.dp, FD_CELL_BORDER, RoundedCornerShape(2.dp))
            .background(FD_CELL_VALUE_BG, RoundedCornerShape(2.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FD_SECTION_HEADER_BG)
                .padding(horizontal = 4.dp, vertical = 1.dp),
        ) {
            Text(
                title,
                style = TextStyle(
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C2D3E),
                ),
            )
        }
        HorizontalDivider(color = FD_CELL_BORDER, thickness = 0.5.dp)
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

@Composable
private fun FontDialogScrollColumn(
    caption: String,
    /** Changes when the dialog is opened again; also drives scroll when [lines] gains rows (e.g. Wasm font load). */
    listScrollSessionKey: Long,
    /** Bumps when the backing catalogue changes without [lines.size] changing (e.g. OS font list replaced). */
    listLayoutEpoch: Int = 0,
    fieldText: String,
    fieldEditable: Boolean,
    onFieldChange: (String) -> Unit,
    lines: List<String>,
    isLineSelected: (String) -> Boolean,
    onPickLine: (String) -> Unit,
    showTruetypeMark: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listScrollSessionKey, lines.size, listLayoutEpoch) {
        val idx = lines.indexOfFirst { isLineSelected(it) }
        if (idx >= 0 && lines.isNotEmpty()) {
            listState.scrollToItem(idx)
        }
    }

    Column(modifier.fillMaxHeight()) {
        Text(caption, style = FD_ROW_TEXT.copy(color = FD_LABEL_COLOR))
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FD_CELL_BORDER)
                .background(FD_CELL_VALUE_BG)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            if (fieldEditable) {
                BasicTextField(
                    value = fieldText,
                    onValueChange = onFieldChange,
                    textStyle = FD_ROW_TEXT.copy(color = FD_VALUE_COLOR),
                    singleLine = true,
                    cursorBrush = SolidColor(FD_VALUE_COLOR),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = fieldText,
                    style = FD_ROW_TEXT.copy(color = FD_VALUE_COLOR),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, FD_CELL_BORDER)
                .background(FD_CELL_VALUE_BG),
        ) {
            items(
                count = lines.size,
                key = { idx -> "$caption#$idx" },
            ) { idx ->
                val line = lines[idx]
                val rowSelected = isLineSelected(line)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rowSelected) WIN_SELECTION_BG else Color.Transparent)
                        .clickable { onPickLine(line) }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (showTruetypeMark) {
                        Text(
                            "TT",
                            style = FD_ROW_TEXT.copy(
                                fontSize = 8.sp,
                                lineHeight = 8.sp,
                                color = if (rowSelected) WIN_SELECTION_FG.copy(alpha = 0.85f) else FD_LABEL_COLOR,
                            ),
                        )
                    }
                    Text(
                        line,
                        style = FD_ROW_TEXT.copy(
                            color = if (rowSelected) WIN_SELECTION_FG else FD_VALUE_COLOR,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
