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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kbrmodelo.composeapp.generated.resources.Res
import kbrmodelo.composeapp.generated.resources.apagar_l
import kbrmodelo.composeapp.generated.resources.apagar_s
import kbrmodelo.composeapp.generated.resources.atributo_l
import kbrmodelo.composeapp.generated.resources.autorelacionamento_l
import kbrmodelo.composeapp.generated.resources.colar_l
import kbrmodelo.composeapp.generated.resources.copiar_l
import kbrmodelo.composeapp.generated.resources.copiar_s
import kbrmodelo.composeapp.generated.resources.cursor_l
import kbrmodelo.composeapp.generated.resources.entidade_l
import kbrmodelo.composeapp.generated.resources.especializacao_l
import kbrmodelo.composeapp.generated.resources.excluir_2l
import kbrmodelo.composeapp.generated.resources.fonte_l
import kbrmodelo.composeapp.generated.resources.gerar_logico_l
import kbrmodelo.composeapp.generated.resources.ligacao_l
import kbrmodelo.composeapp.generated.resources.log_l
import kbrmodelo.composeapp.generated.resources.log_s
import kbrmodelo.composeapp.generated.resources.operacoes_l
import kbrmodelo.composeapp.generated.resources.recortar_l
import kbrmodelo.composeapp.generated.resources.recortar_s
import kbrmodelo.composeapp.generated.resources.salvar_s
import kbrmodelo.composeapp.generated.resources.texto_l
import kbrmodelo.composeapp.generated.resources.visualizar_l
import org.jetbrains.compose.resources.painterResource

// ─── Colors ───────────────────────────────────────────────────────────────────

private val BG_RIBBON         = Color(0xFFDDE4EE)  // uniform background for both topbar and ribbon
private val BG_GROUP          = Color(0xFFEBF0F8)
private val BORDER_COLOR      = Color(0xFF9AAABB)
private val GROUP_BORDER      = Color(0xFF9AAABB)
// Subtle group title: slightly darker than ribbon bg but not heavy
private val GROUP_TITLE_COLOR = Color(0xFF7A8FA0)
private val SEPARATOR_COLOR   = Color(0xFFBBCCDD)
private val TAB_INACTIVE_BG   = Color(0xFFC4CFDB)
private val TAB_ACTIVE_BG     = BG_RIBBON  // active tab blends with ribbon

// Hover highlight — golden tint matching the brModelo original
private val HOVER_BG          = Color(0xFFFFF3C0)
private val HOVER_BORDER      = Color(0xFFE8A800)
private val HOVER_SHAPE       = RoundedCornerShape(3.dp)

// ─── Sizes ────────────────────────────────────────────────────────────────────

/** Total height of the ribbon area (buttons + group title) */
private val RIBBON_HEIGHT = 92.dp

/** Width of the wide button (e.g. Visualizar Esquema) */
private val WIDE_BUTTON_W = 114.dp

// ─── HeaderRibbon ─────────────────────────────────────────────────────────────

@Composable
internal fun HeaderRibbon(
    selectedTab: RibbonTab,
    onMainMenuClick: () -> Unit,
    onTabSelect: (RibbonTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG_RIBBON)
            .drawBehind {
                // Draw border only on top, left, right and bottom — standard box border
                val stroke = 1.dp.toPx()
                drawRect(color = BORDER_COLOR, style = Stroke(width = stroke))
            }
    ) {
        TopBar(selectedTab = selectedTab, onMainMenuClick = onMainMenuClick, onTabSelect = onTabSelect)
        when (selectedTab) {
            RibbonTab.EsquemaConceitual -> RibbonEsquemaConceitual()
            RibbonTab.Opcoes            -> RibbonOpcoes()
        }
    }
}

// ─── Top bar with menu button and tabs ───────────────────────────────────────

@Composable
private fun TopBar(
    selectedTab: RibbonTab,
    onMainMenuClick: () -> Unit,
    onTabSelect: (RibbonTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            // Same background as ribbon — no visual separation between tabs and ribbon
            .background(BG_RIBBON)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom  // tabs sit at the bottom of the topbar, flush with ribbon
    ) {
        // Hamburger menu button — drawn as 3 solid lines (avoids WASM glyph issues)
        Box(
            modifier = Modifier
                .size(width = 26.dp, height = 22.dp)
                .background(Color(0xFF3E5A7E), RoundedCornerShape(3.dp))
                .clickable(onClick = onMainMenuClick)
                .align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(Color.White, RoundedCornerShape(1.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        RibbonTabButton(
            text = "Esquema Conceitual",
            selected = selectedTab == RibbonTab.EsquemaConceitual,
            onClick = { onTabSelect(RibbonTab.EsquemaConceitual) }
        )
        Spacer(modifier = Modifier.width(2.dp))
        RibbonTabButton(
            text = "Opções",
            selected = selectedTab == RibbonTab.Opcoes,
            onClick = { onTabSelect(RibbonTab.Opcoes) }
        )
    }
}

@Composable
private fun RibbonTabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg        = if (selected) TAB_ACTIVE_BG else TAB_INACTIVE_BG
    val textColor = if (selected) Color(0xFF1B365D) else Color(0xFF445566)
    Box(
        modifier = Modifier
            .height(22.dp)
            .background(bg, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            // Active tab: draw border on top + sides only (no bottom border) so it connects to ribbon
            .then(
                if (selected) {
                    Modifier.drawBehind {
                        val w = size.width
                        val h = size.height
                        val s = 1.dp.toPx()
                        val c = BORDER_COLOR
                        drawLine(c, Offset(0f, h), Offset(0f, s / 2))        // left
                        drawLine(c, Offset(0f, s / 2), Offset(w, s / 2))     // top
                        drawLine(c, Offset(w, s / 2), Offset(w, h))          // right
                        // NO bottom line → tab merges with ribbon below
                    }
                } else {
                    Modifier.drawBehind {
                        val w = size.width
                        val h = size.height
                        val s = 1.dp.toPx()
                        val c = BORDER_COLOR
                        drawLine(c, Offset(0f, h), Offset(0f, s / 2))
                        drawLine(c, Offset(0f, s / 2), Offset(w, s / 2))
                        drawLine(c, Offset(w, s / 2), Offset(w, h))
                        drawLine(c, Offset(0f, h), Offset(w, h))              // bottom for inactive
                    }
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, color = textColor)
    }
}

// ─── Ribbon: Esquema Conceitual ───────────────────────────────────────────────

@Composable
private fun RibbonEsquemaConceitual() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RIBBON_HEIGHT)
            .background(BG_RIBBON)
            .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Ferramentas: 4 sub-groups with vertical separators
        RibbonGroupWithSeparators(
            title = "Ferramentas",
            groups = listOf(
                listOf(MenuEntry("Seleção", Res.drawable.cursor_l)),
                listOf(
                    MenuEntry("Entidade", Res.drawable.entidade_l),
                    MenuEntry("Especialização", Res.drawable.especializacao_l),
                    MenuEntry("Atributo", Res.drawable.atributo_l)
                ),
                listOf(
                    MenuEntry("Auto\nRelacionar", Res.drawable.autorelacionamento_l),
                    MenuEntry("Ligar\nObjetos", Res.drawable.ligacao_l)
                ),
                listOf(
                    MenuEntry("Observação", Res.drawable.texto_l),
                    MenuEntry("Excluir\nObjeto", Res.drawable.excluir_2l)
                )
            )
        )
        Spacer(modifier = Modifier.width(5.dp))
        RibbonGroup(
            title = "Operações",
            items = listOf(
                MenuEntry("Operações", Res.drawable.operacoes_l),
                MenuEntry("Gerar Esquema\nLógico", Res.drawable.gerar_logico_l)
            )
        )
    }
}

// ─── Ribbon: Opções ───────────────────────────────────────────────────────────

@Composable
private fun RibbonOpcoes() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RIBBON_HEIGHT)
            .background(BG_RIBBON)
            .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Documentação: Visualizar (large, wide) + [Exibir, Limpar, Salvar] (small, stacked)
        RibbonGroupMixed(
            title = "Documentação",
            largeButtonWidth = WIDE_BUTTON_W,
            largeEntry = MenuEntry("Visualizar Esquema\nXML com XSLT", Res.drawable.visualizar_l),
            smallEntries = listOf(
                MenuEntry("Exibir Log de Operações", Res.drawable.log_s),
                MenuEntry("Limpar Log", Res.drawable.apagar_s),  // ApagarS.bmp per original XML
                MenuEntry("Salvar Log", Res.drawable.salvar_s)
            )
        )
        Spacer(modifier = Modifier.width(5.dp))
        // Área de Transferência: Colar (large) + [Recortar, Copiar] (stacked)
        RibbonGroupMixed(
            title = "Área de Transferência",
            largeButtonWidth = 56.dp,
            largeEntry = MenuEntry("Colar", Res.drawable.colar_l),
            smallEntries = listOf(
                MenuEntry("Recortar", Res.drawable.recortar_s),
                MenuEntry("Copiar", Res.drawable.copiar_s)
            )
        )
        Spacer(modifier = Modifier.width(5.dp))
        RibbonGroup(
            title = "Fonte",
            items = listOf(MenuEntry("Selecionar\nFonte", Res.drawable.fonte_l))
        )
    }
}

// ─── Ribbon group components ──────────────────────────────────────────────────

/** Standard group: all buttons in a single row, group title below. */
@Composable
private fun RibbonGroup(title: String, items: List<MenuEntry>) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .drawBehind { drawRect(GROUP_BORDER, style = Stroke(width = 1.dp.toPx())) }
            .background(BG_GROUP)
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
        GroupTitle(title)
    }
}

/** Group with vertical separators between sub-groups. */
@Composable
private fun RibbonGroupWithSeparators(title: String, groups: List<List<MenuEntry>>) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .drawBehind { drawRect(GROUP_BORDER, style = Stroke(width = 1.dp.toPx())) }
            .background(BG_GROUP)
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
                if (index > 0) RibbonSeparator()
                group.forEach { RibbonButton(it) }
            }
        }
        GroupTitle(title)
    }
}

/**
 * Mixed ribbon group: large button on the left, small buttons stacked on the right.
 * Used in the "Opções" ribbon tab (Documentação, Área de Transferência).
 */
@Composable
private fun RibbonGroupMixed(
    title: String,
    largeButtonWidth: androidx.compose.ui.unit.Dp,
    largeEntry: MenuEntry,
    smallEntries: List<MenuEntry>
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .drawBehind { drawRect(GROUP_BORDER, style = Stroke(width = 1.dp.toPx())) }
            .background(BG_GROUP)
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
            // Stack small buttons tightly; the Row's CenterVertically handles vertical centering.
            // wrapContentWidth prevents the column from being squeezed by the parent Row.
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                smallEntries.forEach { SmallRibbonButton(it) }
            }
        }
        GroupTitle(title)
    }
}

// ─── Individual button types ──────────────────────────────────────────────────

/**
 * Standard ribbon button: icon at the top, label immediately below.
 * All buttons in the same Row use Alignment.Top so every icon sits at the same height.
 * Labels are naturally aligned because all icons are the same size (32dp).
 */
@Composable
private fun RibbonButton(entry: MenuEntry) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight()
            .hoverable(interactionSource)
            .background(if (isHovered) HOVER_BG else Color.Transparent, HOVER_SHAPE)
            .border(1.dp, if (isHovered) HOVER_BORDER else Color.Transparent, HOVER_SHAPE)
            .padding(horizontal = 3.dp, vertical = 3.dp)
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
    }
}

/** Large ribbon button: tall icon + multi-line label below. Used as the primary button in mixed groups. */
@Composable
private fun LargeRibbonButton(
    entry: MenuEntry,
    buttonWidth: androidx.compose.ui.unit.Dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .width(buttonWidth)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .background(if (isHovered) HOVER_BG else Color.Transparent, HOVER_SHAPE)
            .border(1.dp, if (isHovered) HOVER_BORDER else Color.Transparent, HOVER_SHAPE)
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
private fun SmallRibbonButton(entry: MenuEntry) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    // Fixed height ensures the Row never grows taller than necessary and
    // CenterVertically reliably centers both icon and text on the same axis.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(20.dp)
            .hoverable(interactionSource)
            .background(if (isHovered) HOVER_BG else Color.Transparent, HOVER_SHAPE)
            .border(1.dp, if (isHovered) HOVER_BORDER else Color.Transparent, HOVER_SHAPE)
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

@Composable
private fun RibbonSeparator() {
    Spacer(modifier = Modifier.width(4.dp))
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(SEPARATOR_COLOR)
    )
    Spacer(modifier = Modifier.width(4.dp))
}

@Composable
private fun GroupTitle(title: String) {
    Text(
        text = title,
        fontSize = 9.sp,
        lineHeight = 9.sp,
        color = GROUP_TITLE_COLOR,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 1.dp, bottom = 1.dp)
    )
}
