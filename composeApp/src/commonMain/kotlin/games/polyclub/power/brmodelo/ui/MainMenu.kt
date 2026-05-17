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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.generated.resources.abrir_2s
import games.polyclub.power.brmodelo.generated.resources.configuracoes_2s
import games.polyclub.power.brmodelo.generated.resources.dicionario_dados_3s
import games.polyclub.power.brmodelo.generated.resources.exportar_bitmap_s
import games.polyclub.power.brmodelo.generated.resources.exportar_jpeg_s
import games.polyclub.power.brmodelo.generated.resources.fechar_2s
import games.polyclub.power.brmodelo.generated.resources.imprimir_s
import games.polyclub.power.brmodelo.generated.resources.modelo_conceitual_2s
import games.polyclub.power.brmodelo.generated.resources.modelo_logico_s
import games.polyclub.power.brmodelo.generated.resources.novo_s
import games.polyclub.power.brmodelo.generated.resources.sair_s_096
import games.polyclub.power.brmodelo.generated.resources.salvar_como_s
import games.polyclub.power.brmodelo.generated.resources.salvar_s
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val MENU_BG     = Color(0xFFEEF2F8)
private val MENU_ACTIVE = Color(0xFFFFBF00)
private val MENU_BORDER = Color(0xFFA0AEBC)
private val SUBMENU_BG  = Color(0xFFF8F9FC)
private val MENU_TEXT   = Color(0xFF1C2B3A)
private val SEPARATOR   = Color(0xFFC0CAD4)

@Composable
internal fun FunctionalMainMenu(
    modifier: Modifier = Modifier,
    activeMenu: MainMenuType?,
    onMenuHover: (MainMenuType) -> Unit,
    onOpenFile: () -> Unit = {},
    onNewConceptualModel: () -> Unit = {},
    onCloseCurrentModel: () -> Unit = {},
    onQuitApplication: () -> Unit = {},
    onExportJpeg: () -> Unit = {},
    onExportPng: () -> Unit = {},
    onOpenSchemaDataDictionary: () -> Unit = {},
    schemaDataDictionaryEnabled: Boolean = false,
    onSave: () -> Unit = {},
    onSaveAs: () -> Unit = {},
) {
    Row(
        modifier = modifier.border(1.dp, MENU_BORDER)
    ) {
        MainMenuItems(
            activeMenu = activeMenu,
            onMenuHover = onMenuHover,
            onOpenFile = onOpenFile,
            onCloseCurrentModel = onCloseCurrentModel,
            onQuitApplication = onQuitApplication,
            onSave = onSave,
            onSaveAs = onSaveAs,
        )
        // The right panel always shows; content varies by hovered item
        when (activeMenu) {
            MainMenuType.NewModel -> NewModelSubmenu(onNewConceptualModel = onNewConceptualModel)
            MainMenuType.Print    -> PrintSubmenu(
                onExportJpeg = onExportJpeg,
                onExportPng = onExportPng,
                onOpenSchemaDataDictionary = onOpenSchemaDataDictionary,
                schemaDataDictionaryEnabled = schemaDataDictionaryEnabled,
            )
            null                  -> RecentModelsPanel()
        }
    }
}

@Composable
private fun MainMenuItems(
    activeMenu: MainMenuType?,
    onMenuHover: (MainMenuType) -> Unit,
    onOpenFile: () -> Unit,
    onCloseCurrentModel: () -> Unit = {},
    onQuitApplication: () -> Unit = {},
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(170.dp)
            .fillMaxHeight()
            .background(MENU_BG)
    ) {
        MainMenuItem(
            title = "Novo Modelo",
            icon = Res.drawable.novo_s,
            hasSubmenu = true,
            selected = activeMenu == MainMenuType.NewModel,
            onHover = { onMenuHover(MainMenuType.NewModel) }
        )
        MainMenuItem(
            "Abrir",
            Res.drawable.abrir_2s,
            onClick = onOpenFile
        )
        MainMenuItem(
            "Salvar",
            Res.drawable.salvar_s,
            onClick = onSave,
        )
        MainMenuItem(
            "Salvar Como...",
            Res.drawable.salvar_como_s,
            onClick = onSaveAs
        )
        MainMenuItem(
            "Fechar Modelo Atual",
            Res.drawable.fechar_2s,
            onClick = onCloseCurrentModel
        )

        HorizontalDivider(color = SEPARATOR, thickness = 1.dp, modifier = Modifier.padding(horizontal = 4.dp))

        MainMenuItem(
            title = "Imprimir",
            icon = Res.drawable.imprimir_s,
            hasSubmenu = true,
            selected = activeMenu == MainMenuType.Print,
            onHover = { onMenuHover(MainMenuType.Print) }
        )

        HorizontalDivider(color = SEPARATOR, thickness = 1.dp, modifier = Modifier.padding(horizontal = 4.dp))

        MainMenuItem("Configurações", Res.drawable.configuracoes_2s, enabled = false)

        if (isDesktopTarget) {
            HorizontalDivider(color = SEPARATOR, thickness = 1.dp, modifier = Modifier.padding(horizontal = 4.dp))
            MainMenuItem(
                title = "Sair",
                icon = Res.drawable.sair_s_096,
                onClick = onQuitApplication,
            )
        }
    }
}

@Composable
private fun MainMenuItem(
    title: String,
    icon: DrawableResource,
    hasSubmenu: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    onHover: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    var isHovered by remember { mutableStateOf(false) }
    val bg = when {
        !enabled -> Color(0xFFE8EBF0)
        selected  -> MENU_ACTIVE
        isHovered -> MENU_ACTIVE.copy(alpha = 0.45f)
        else      -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                if (enabled) {
                                    isHovered = true
                                    if (hasSubmenu) onHover()
                                }
                            }
                            PointerEventType.Exit  -> isHovered = false
                            PointerEventType.Release -> if (!hasSubmenu && enabled) onClick()
                        }
                    }
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = title,
            modifier = Modifier.size(18.dp),
            contentScale = ContentScale.Fit,
            alpha = if (enabled) 1f else 0.5f,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = if (enabled) MENU_TEXT else Color(0xFF8899AA),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false
        )
        if (hasSubmenu) {
            Text("▶", fontSize = 9.sp, color = Color(0xFF667788))
        }
    }
}

// ─── Painel padrão: Modelos Utilizados Recentemente ──────────────────────────

@Composable
private fun RecentModelsPanel() {
    Column(
        modifier = Modifier
            .width(230.dp)
            .fillMaxHeight()
            .background(SUBMENU_BG)
            .border(width = 1.dp, color = MENU_BORDER)
            .padding(10.dp)
    ) {
        Text(
            "Modelos Utilizados Recentemente",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color(0xFF253041)
        )
        HorizontalDivider(
            color = SEPARATOR,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        Text("(nenhum)", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color(0xFF888888))
    }
}

// ─── Submenu: Novo Modelo ─────────────────────────────────────────────────────

@Composable
private fun NewModelSubmenu(onNewConceptualModel: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(230.dp)
            .fillMaxHeight()
            .background(SUBMENU_BG)
            .border(width = 1.dp, color = MENU_BORDER)
            .padding(10.dp)
    ) {
        Text("Criar novo Modelo", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF253041))
        Spacer(modifier = Modifier.height(8.dp))
        SubmenuCard(
            title = "Modelo Conceitual",
            description = "Criar um novo modelo conceitual",
            icon = Res.drawable.modelo_conceitual_2s,
            onClick = onNewConceptualModel,
        )
        Spacer(modifier = Modifier.height(6.dp))
        SubmenuCard(
            title = "Modelo Lógico",
            description = "Criar um novo modelo lógico",
            icon = Res.drawable.modelo_logico_s,
            enabled = false,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            "Modelos Utilizados Recentemente",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color(0xFF253041)
        )
        HorizontalDivider(color = SEPARATOR, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
        Text("(nenhum)", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color(0xFF888888))
    }
}

// ─── Submenu: Imprimir ────────────────────────────────────────────────────────

@Composable
private fun PrintSubmenu(
    onExportJpeg: () -> Unit = {},
    onExportPng: () -> Unit = {},
    onOpenSchemaDataDictionary: () -> Unit = {},
    schemaDataDictionaryEnabled: Boolean = false,
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(SUBMENU_BG)
            .border(width = 1.dp, color = MENU_BORDER)
            .padding(10.dp)
    ) {
        Text("Impressão de Modelos", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF253041))
        Spacer(modifier = Modifier.height(8.dp))
        SubmenuCard(
            "Imprimir",
            "Permite a impressão do modelo corrente.",
            Res.drawable.imprimir_s,
            enabled = false,
        )
        Spacer(modifier = Modifier.height(6.dp))
        SubmenuCard(
            "Gerar Dicionário do Esquema",
            "Permite gerar o dicionário de dados geral do esquema.",
            Res.drawable.dicionario_dados_3s,
            enabled = schemaDataDictionaryEnabled,
            onClick = onOpenSchemaDataDictionary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        SubmenuCard(
            title = "Exportar em JPEG",
            description = "Exporta o modelo atual como JPEG (com fundo).",
            icon = Res.drawable.exportar_jpeg_s,
            onClick = onExportJpeg,
        )
        Spacer(modifier = Modifier.height(6.dp))
        SubmenuCard(
            title = "Exportar em PNG",
            description = "Exporta o modelo atual como PNG (fundo transparente).",
            icon = Res.drawable.exportar_bitmap_s,
            onClick = onExportPng,
        )
    }
}

// ─── Componente compartilhado ─────────────────────────────────────────────────

@Composable
private fun SubmenuCard(
    title: String,
    description: String,
    icon: DrawableResource,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    var isHovered by remember { mutableStateOf(false) }
    val bg = when {
        !enabled -> Color(0xFFE5E8ED)
        isHovered -> Color(0xFFDDE6F4)
        else -> Color(0xFFF0F4FA)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(bg)
            .border(1.dp, Color(0xFFCDD6E2))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter   -> if (enabled) isHovered = true
                            PointerEventType.Exit    -> isHovered = false
                            PointerEventType.Release -> if (enabled) onClick()
                        }
                    }
                }
            }
            .padding(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = title,
            modifier = Modifier.size(18.dp).padding(top = 1.dp),
            contentScale = ContentScale.Fit,
            alpha = if (enabled) 1f else 0.5f,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (enabled) MENU_TEXT else Color(0xFF8899AA),
            )
            Text(
                description,
                fontSize = 10.sp,
                color = if (enabled) Color(0xFF4D5C6A) else Color(0xFF9AA5AE),
                lineHeight = 12.sp,
            )
        }
    }
}
