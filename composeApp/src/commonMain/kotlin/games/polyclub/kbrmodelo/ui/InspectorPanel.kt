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

package games.polyclub.kbrmodelo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val INSPECTOR_BG    = Color(0xFFF0F2F5)
private val INSPECTOR_BORDER = Color(0xFF8090A0)
private val HEADER_BG       = Color(0xFFD8DDE4)
private val HEADER_BORDER   = Color(0xFFB0BAC4)
private val TAB_ACTIVE_BG   = Color(0xFFFFFFFF)
private val TAB_INACTIVE_BG = Color(0xFFC4CED8)
private val SECTION_HEADER_BG = Color(0xFFCFD8E3)
private val CELL_LABEL_BG   = Color(0xFFE8EDF2)
private val CELL_VALUE_BG   = Color(0xFFFFFFFF)
private val CELL_BORDER     = Color(0xFFBDC7D1)
private val LABEL_COLOR     = Color(0xFF3A4A5A)
private val VALUE_COLOR     = Color(0xFF1A2535)

private enum class InspectorTab { Selecao, AtrOcultos }

@Composable
internal fun InspectorPanel() {
    var activeTab by remember { mutableStateOf(InspectorTab.Selecao) }

    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .border(1.dp, INSPECTOR_BORDER)
            .background(INSPECTOR_BG)
    ) {
        // ── Header tabs: "Seleção" / "Atr. ocultos"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(HEADER_BG)
                .border(width = 1.dp, color = HEADER_BORDER),
            verticalAlignment = Alignment.Bottom
        ) {
            InspectorTab(
                label = "Seleção",
                selected = activeTab == InspectorTab.Selecao,
                modifier = Modifier.weight(1f)
            ) { activeTab = InspectorTab.Selecao }

            InspectorTab(
                label = "Atr. ocultos",
                selected = activeTab == InspectorTab.AtrOcultos,
                modifier = Modifier.width(80.dp)
            ) { activeTab = InspectorTab.AtrOcultos }
        }

        when (activeTab) {
            InspectorTab.Selecao    -> SelectionContent(modifier = Modifier.weight(1f))
            InspectorTab.AtrOcultos -> HiddenAttributesContent(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InspectorTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg        = if (selected) TAB_ACTIVE_BG else TAB_INACTIVE_BG
    val textColor = if (selected) Color(0xFF1B2B3B) else Color(0xFF4A5A6A)
    val weight    = if (selected) FontWeight.Bold else FontWeight.Normal
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(bg)
            .border(width = 1.dp, color = HEADER_BORDER)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 11.sp, fontWeight = weight, color = textColor)
    }
}

// ─── Aba "Seleção" ────────────────────────────────────────────────────────────

@Composable
private fun SelectionContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Text(
            text = "Informações: Modelo Conceitual",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF263442),
            modifier = Modifier
                .fillMaxWidth()
                .background(SECTION_HEADER_BG)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
        HorizontalDivider(color = HEADER_BORDER, thickness = 1.dp)

        // 2-column spreadsheet grid
        GridRow(label = "Nome", value = "teste-em-xml")
        GridRow(label = "Versão", value = "2.0.0")
        GridRow(label = "Autor(es)", value = "")
        GridRow(label = "Observações", value = "")
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(color = Color(0xFF7A9ABF), thickness = 2.dp)
    }
}

// ─── Aba "Atr. ocultos" ───────────────────────────────────────────────────────

@Composable
private fun HiddenAttributesContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)) {
        Text(
            text = "(nenhum atributo oculto)",
            fontSize = 11.sp,
            color = Color(0xFF7A8A9A)
        )
    }
}

// ─── Grade de 2 colunas ───────────────────────────────────────────────────────

@Composable
private fun GridRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = CELL_BORDER)
    ) {
        // Label cell (fixed width, gray)
        Box(
            modifier = Modifier
                .width(72.dp)
                .background(CELL_LABEL_BG)
                .border(width = 1.dp, color = CELL_BORDER)
                .padding(horizontal = 4.dp, vertical = 3.dp)
        ) {
            Text(text = label, fontSize = 10.sp, color = LABEL_COLOR)
        }
        // Value cell (fills remaining space, white)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(CELL_VALUE_BG)
                .padding(horizontal = 4.dp, vertical = 3.dp)
        ) {
            Text(text = value, fontSize = 11.sp, color = VALUE_COLOR)
        }
    }
}
