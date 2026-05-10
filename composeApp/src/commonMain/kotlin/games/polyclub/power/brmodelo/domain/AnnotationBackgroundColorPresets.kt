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

package games.polyclub.power.brmodelo.domain

/**
 * Preset background colours for [SchemaElement.Annotation] ([TBaseTexto.Cor] in `mer.pas`).
 *
 * Stored values are Delphi **TColor** integers: absolute colours as Windows **COLORREF** (`0x00BBGGRR`),
 * system colours as `0x80000000 or COLOR_*` (see [VclTColorTable]). XML uses decimal in `<Cor Valor="…"/>`.
 *
 * Order matches default **TColorBox** / Lazarus `GetColorValues` (see FPC `lcl/colorbox.pas`, `lcl/graphics.pp`).
 * UI labels are Brazilian Portuguese; [constant] keeps the Delphi identifier for debugging.
 */
object AnnotationBackgroundColorPresets {

    data class Entry(
        /** Delphi identifier, e.g. `clSkyBlue`. */
        val constant: String,
        /** Short label for the inspector menu (PT-BR). */
        val label: String,
        val colorRef: Int,
    )

    /** [TBaseTexto.Create] default (`clSkyBlue`); same as `valores-padroes.xml` `<Cor Valor="15780518"/>`. */
    const val DEFAULT_COLOR_REF: Int = 15_780_518

    private val labelByConstant: Map<String, String> = mapOf(
        "clBlack" to "Preto",
        "clMaroon" to "Bordô",
        "clGreen" to "Verde",
        "clOlive" to "Oliva",
        "clNavy" to "Azul marinho",
        "clPurple" to "Roxo",
        "clTeal" to "Verde-azulado",
        "clGray" to "Cinza",
        "clSilver" to "Prata",
        "clRed" to "Vermelho",
        "clLime" to "Verde lima",
        "clYellow" to "Amarelo",
        "clBlue" to "Azul",
        "clFuchsia" to "Fúcsia",
        "clAqua" to "Água-marinha",
        "clWhite" to "Branco",
        "clMoneyGreen" to "Verde dinheiro",
        "clSkyBlue" to "Azul céu",
        "clCream" to "Creme",
        "clMedGray" to "Cinza médio",
        "clScrollBar" to "Barra de rolagem",
        "clBackground" to "Plano de fundo",
        "clActiveCaption" to "Legenda ativa",
        "clInactiveCaption" to "Legenda inativa",
        "clMenu" to "Menu",
        "clMenuBar" to "Barra de menu",
        "clMenuHighlight" to "Realce de menu",
        "clMenuText" to "Texto de menu",
        "clWindow" to "Janela",
        "clWindowFrame" to "Moldura da janela",
        "clWindowText" to "Texto da janela",
        "clCaptionText" to "Texto da legenda",
        "clActiveBorder" to "Borda ativa",
        "clInactiveBorder" to "Borda inativa",
        "clAppWorkspace" to "Área de trabalho do app",
        "clHighlight" to "Realce",
        "clHighlightText" to "Texto do realce",
        "clBtnFace" to "Face do botão",
        "clBtnShadow" to "Sombra do botão",
        "clGrayText" to "Texto acinzentado",
        "clBtnText" to "Texto do botão",
        "clInactiveCaptionText" to "Texto da legenda inativa",
        "clBtnHighlight" to "Realce do botão",
        "cl3DDkShadow" to "Sombra 3D escura",
        "cl3DLight" to "Luz 3D",
        "clInfoText" to "Texto de dica",
        "clInfoBk" to "Fundo de dica",
        "clHotLight" to "Realce a quente",
        "clGradientActiveCaption" to "Legenda ativa (gradiente)",
        "clGradientInactiveCaption" to "Legenda inativa (gradiente)",
        "clForm" to "Formulário",
    )

    /** Ordered list for inspector dropdowns (Delphi order, PT-BR labels). */
    val ENTRIES: List<Entry> = VclTColorTable.defaultColorBoxPresets.map { nc ->
        Entry(
            constant = nc.constant,
            label = labelByConstant.getValue(nc.constant),
            colorRef = nc.colorRef,
        )
    }

    /** Label for the inspector; unknown refs show the decimal string (same form as XML `Valor`). */
    fun labelForColorRef(colorRef: Int?): String {
        val ref = colorRef ?: DEFAULT_COLOR_REF
        return ENTRIES.firstOrNull { it.colorRef == ref }?.label ?: ref.toString()
    }

    /**
     * Resolves a dropdown choice: preset label → [Entry.colorRef], or a decimal string → integer.
     */
    fun colorRefForMenuSelection(selectedOption: String): Int? =
        ENTRIES.firstOrNull { it.label == selectedOption }?.colorRef
            ?: selectedOption.toIntOrNull()
}
