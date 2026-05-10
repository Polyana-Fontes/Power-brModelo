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
import games.polyclub.power.brmodelo.ui.DropdownEntry
import games.polyclub.power.brmodelo.ui.AutoSelfRelationshipToolRibbonBinding
import games.polyclub.power.brmodelo.ui.EntityToolRibbonBinding
import games.polyclub.power.brmodelo.ui.LinkObjectsToolRibbonBinding
import games.polyclub.power.brmodelo.ui.MenuEntry
import games.polyclub.power.brmodelo.ui.ObservationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.SpecializationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.components.AppColors
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.generated.resources.apagar_s
import games.polyclub.power.brmodelo.generated.resources.atributo_composto_s
import games.polyclub.power.brmodelo.generated.resources.atributo_identificador_s
import games.polyclub.power.brmodelo.generated.resources.atributo_l
import games.polyclub.power.brmodelo.generated.resources.atributo_multivalorado_s
import games.polyclub.power.brmodelo.generated.resources.atributo_opcional_s
import games.polyclub.power.brmodelo.generated.resources.atributo_s
import games.polyclub.power.brmodelo.generated.resources.autorelacionamento_l
import games.polyclub.power.brmodelo.generated.resources.colar_l
import games.polyclub.power.brmodelo.generated.resources.copiar_s
import games.polyclub.power.brmodelo.generated.resources.cursor_l
import games.polyclub.power.brmodelo.generated.resources.dicionario_dados_3s
import games.polyclub.power.brmodelo.generated.resources.entidade_associativa_s
import games.polyclub.power.brmodelo.generated.resources.entidade_l
import games.polyclub.power.brmodelo.generated.resources.entidade_s
import games.polyclub.power.brmodelo.generated.resources.especializacao_exclusiva_s
import games.polyclub.power.brmodelo.generated.resources.especializacao_l
import games.polyclub.power.brmodelo.generated.resources.especializacao_nao_exclusiva_s
import games.polyclub.power.brmodelo.generated.resources.excluir_2l
import games.polyclub.power.brmodelo.generated.resources.fonte_l
import games.polyclub.power.brmodelo.generated.resources.gerar_logico_l
import games.polyclub.power.brmodelo.generated.resources.ligacao_l
import games.polyclub.power.brmodelo.generated.resources.log_s
import games.polyclub.power.brmodelo.generated.resources.operacoes_l
import games.polyclub.power.brmodelo.generated.resources.recortar_s
import games.polyclub.power.brmodelo.generated.resources.relacao_s
import games.polyclub.power.brmodelo.generated.resources.salvar_s
import games.polyclub.power.brmodelo.generated.resources.selecionar_s
import games.polyclub.power.brmodelo.generated.resources.texto_l
import games.polyclub.power.brmodelo.generated.resources.visualizar_l

private val RIBBON_HEIGHT = 92.dp
private val WIDE_BUTTON_W = 114.dp

@Composable
internal fun RibbonEsquemaConceitual(
    entityToolBinding: EntityToolRibbonBinding? = null,
    observationToolBinding: ObservationToolRibbonBinding? = null,
    linkObjectsToolBinding: LinkObjectsToolRibbonBinding? = null,
    autoSelfRelationshipToolBinding: AutoSelfRelationshipToolRibbonBinding? = null,
    specializationToolBinding: SpecializationToolRibbonBinding? = null,
) {
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
            entityToolBinding = entityToolBinding,
            observationToolBinding = observationToolBinding,
            linkObjectsToolBinding = linkObjectsToolBinding,
            autoSelfRelationshipToolBinding = autoSelfRelationshipToolBinding,
            specializationToolBinding = specializationToolBinding,
            groups = listOf(
                listOf(MenuEntry("Seleção", Res.drawable.cursor_l)),
                listOf(
                    MenuEntry(
                        title = "Entidade",
                        icon = Res.drawable.entidade_l,
                        dropdown = conceptualEntityDropdownEntries(),
                    ),
                    MenuEntry(
                        title = "Especialização",
                        icon = Res.drawable.especializacao_l,
                        dropdown = conceptualSpecializationDropdownEntries(),
                    ),
                    MenuEntry(
                        title = "Atributo",
                        icon = Res.drawable.atributo_l,
                        dropdown = listOf(
                            DropdownEntry(
                                "Atributo",
                                Res.drawable.atributo_s
                            ),
                            DropdownEntry(
                                "Atributo Identificador",
                                Res.drawable.atributo_identificador_s
                            ),
                            DropdownEntry(
                                "Atributo Multivalorado",
                                Res.drawable.atributo_multivalorado_s
                            ),
                            DropdownEntry(
                                "Atributo Composto",
                                Res.drawable.atributo_composto_s
                            ),
                            DropdownEntry(
                                "Atributo Opcional",
                                Res.drawable.atributo_opcional_s
                            )
                        )
                    )
                ),
                listOf(
                    MenuEntry(
                        "Auto\nRelacionar",
                        Res.drawable.autorelacionamento_l
                    ),
                    MenuEntry(
                        "Ligar\nObjetos",
                        Res.drawable.ligacao_l
                    )
                ),
                listOf(
                    MenuEntry("Observação", Res.drawable.texto_l),
                ),
                listOf(
                    MenuEntry(
                        "Excluir\nObjeto",
                        Res.drawable.excluir_2l
                    ),
                ),
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
                        DropdownEntry(
                            "Ocultar Atributo",
                            Res.drawable.atributo_s,
                            enabled = false
                        ),
                        DropdownEntry(
                            "Organizar Atributos",
                            Res.drawable.atributo_s,
                            enabled = false
                        ),
                        DropdownEntry(
                            "Selecionar Atributo",
                            Res.drawable.selecionar_s,
                            isSeparatorAbove = true
                        ),
                        DropdownEntry(
                            "Promover à Entidade Associativa",
                            Res.drawable.entidade_associativa_s
                        ),
                        DropdownEntry(
                            "Promover à Entidade",
                            Res.drawable.entidade_s
                        ),
                        DropdownEntry(
                            "Converter Esp. para Restrita",
                            Res.drawable.especializacao_exclusiva_s
                        ),
                        DropdownEntry(
                            "Converter Esp. para Opcional",
                            Res.drawable.especializacao_nao_exclusiva_s
                        ),
                        DropdownEntry(
                            "Dicionário de Dados do Objeto",
                            Res.drawable.dicionario_dados_3s
                        )
                    )
                ),
                MenuEntry(
                    "Gerar Esquema\nLógico",
                    Res.drawable.gerar_logico_l
                )
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
            largeEntry = MenuEntry(
                "Visualizar Esquema\nXML com XSLT",
                Res.drawable.visualizar_l
            ),
            smallEntries = listOf(
                MenuEntry(
                    "Exibir Log de Operações",
                    Res.drawable.log_s
                ),
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
            items = listOf(
                MenuEntry(
                    "Selecionar\nFonte",
                    Res.drawable.fonte_l
                )
            )
        )
    }
}
