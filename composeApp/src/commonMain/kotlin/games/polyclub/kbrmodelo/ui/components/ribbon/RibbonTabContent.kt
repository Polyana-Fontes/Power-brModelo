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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import games.polyclub.kbrmodelo.ui.DropdownEntry
import games.polyclub.kbrmodelo.ui.MenuEntry
import games.polyclub.kbrmodelo.ui.components.AppColors
import kbrmodelo.composeapp.generated.resources.Res
import kbrmodelo.composeapp.generated.resources.apagar_s
import kbrmodelo.composeapp.generated.resources.atributo_composto_s
import kbrmodelo.composeapp.generated.resources.atributo_identificador_s
import kbrmodelo.composeapp.generated.resources.atributo_l
import kbrmodelo.composeapp.generated.resources.atributo_multivalorado_s
import kbrmodelo.composeapp.generated.resources.atributo_opcional_s
import kbrmodelo.composeapp.generated.resources.atributo_s
import kbrmodelo.composeapp.generated.resources.autorelacionamento_l
import kbrmodelo.composeapp.generated.resources.colar_l
import kbrmodelo.composeapp.generated.resources.copiar_s
import kbrmodelo.composeapp.generated.resources.cursor_l
import kbrmodelo.composeapp.generated.resources.dicionario_dados_3s
import kbrmodelo.composeapp.generated.resources.entidade_associativa_s
import kbrmodelo.composeapp.generated.resources.entidade_l
import kbrmodelo.composeapp.generated.resources.entidade_s
import kbrmodelo.composeapp.generated.resources.especializacao_exclusiva_s
import kbrmodelo.composeapp.generated.resources.especializacao_l
import kbrmodelo.composeapp.generated.resources.especializacao_nao_exclusiva_s
import kbrmodelo.composeapp.generated.resources.especializacao_s
import kbrmodelo.composeapp.generated.resources.excluir_2l
import kbrmodelo.composeapp.generated.resources.fonte_l
import kbrmodelo.composeapp.generated.resources.gerar_logico_l
import kbrmodelo.composeapp.generated.resources.ligacao_l
import kbrmodelo.composeapp.generated.resources.log_s
import kbrmodelo.composeapp.generated.resources.operacoes_l
import kbrmodelo.composeapp.generated.resources.recortar_s
import kbrmodelo.composeapp.generated.resources.relacao_s
import kbrmodelo.composeapp.generated.resources.salvar_s
import kbrmodelo.composeapp.generated.resources.selecionar_s
import kbrmodelo.composeapp.generated.resources.texto_l
import kbrmodelo.composeapp.generated.resources.visualizar_l

private val RIBBON_HEIGHT = 92.dp
private val WIDE_BUTTON_W = 114.dp

@Composable
internal fun RibbonEsquemaConceitual() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RIBBON_HEIGHT)
            .background(AppColors.ribbonBg)
            .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        RibbonGroupWithSeparators(
            title = "Ferramentas",
            groups = listOf(
                listOf(MenuEntry("Seleção", Res.drawable.cursor_l)),
                listOf(
                    MenuEntry(
                        title = "Entidade",
                        icon = Res.drawable.entidade_l,
                        dropdown = listOf(
                            DropdownEntry("Entidade", Res.drawable.entidade_s),
                            DropdownEntry("Relação", Res.drawable.entidade_associativa_s),
                            DropdownEntry("Entidade Associativa", Res.drawable.relacao_s)
                        )
                    ),
                    MenuEntry(
                        title = "Especialização",
                        icon = Res.drawable.especializacao_l,
                        dropdown = listOf(
                            DropdownEntry("Especialização", Res.drawable.especializacao_s),
                            DropdownEntry("Especialização Exclusiva com Criação de Entidade", Res.drawable.especializacao_exclusiva_s),
                            DropdownEntry("Especialização Não-Exclusiva com Criação de Entidade", Res.drawable.especializacao_nao_exclusiva_s)
                        )
                    ),
                    MenuEntry(
                        title = "Atributo",
                        icon = Res.drawable.atributo_l,
                        dropdown = listOf(
                            DropdownEntry("Atributo", Res.drawable.atributo_s),
                            DropdownEntry("Atributo Identificador", Res.drawable.atributo_identificador_s),
                            DropdownEntry("Atributo Multivalorado", Res.drawable.atributo_multivalorado_s),
                            DropdownEntry("Atributo Composto", Res.drawable.atributo_composto_s),
                            DropdownEntry("Atributo Opcional", Res.drawable.atributo_opcional_s)
                        )
                    )
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
                MenuEntry(
                    title = "Operações",
                    icon = Res.drawable.operacoes_l,
                    dropdown = listOf(
                        DropdownEntry("Ocultar Atributo", Res.drawable.atributo_s, enabled = false),
                        DropdownEntry("Organizar Atributos", Res.drawable.atributo_s, enabled = false),
                        DropdownEntry("Selecionar Atributo", Res.drawable.selecionar_s, isSeparatorAbove = true),
                        DropdownEntry("Promover à Entidade Associativa", Res.drawable.entidade_associativa_s),
                        DropdownEntry("Promover à Entidade", Res.drawable.entidade_s),
                        DropdownEntry("Converter Esp. para Restrita", Res.drawable.especializacao_exclusiva_s),
                        DropdownEntry("Converter Esp. para Opcional", Res.drawable.especializacao_nao_exclusiva_s),
                        DropdownEntry("Dicionário de Dados do Objeto", Res.drawable.dicionario_dados_3s)
                    )
                ),
                MenuEntry("Gerar Esquema\nLógico", Res.drawable.gerar_logico_l)
            )
        )
    }
}

@Composable
internal fun RibbonOpcoes() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RIBBON_HEIGHT)
            .background(AppColors.ribbonBg)
            .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        RibbonGroupMixed(
            title = "Documentação",
            largeButtonWidth = WIDE_BUTTON_W,
            largeEntry = MenuEntry("Visualizar Esquema\nXML com XSLT", Res.drawable.visualizar_l),
            smallEntries = listOf(
                MenuEntry("Exibir Log de Operações", Res.drawable.log_s),
                MenuEntry("Limpar Log", Res.drawable.apagar_s),
                MenuEntry("Salvar Log", Res.drawable.salvar_s)
            )
        )
        Spacer(modifier = Modifier.width(5.dp))
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
