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
 * Default geometry and label styling for elements created via the ribbon placement tools.
 *
 * Values match [desktopTest/resources/valores-padroes.xml] (Entidade1, Relacao1, EntAssoss1, Texto).
 * The associative inner diamond size follows the renderer rule: outer width/height minus 30 px
 * (same as the original InflateRect -15 on each side).
 */
object ConceptualPlacementDefaults {
    /** Font colour from `FonteCor Valor="0"` in the reference XML. */
    val labelStyle: LabelStyle = LabelStyle(color = 0, bold = false, italic = false)

    const val entityWidth: Int = 102
    const val entityHeight: Int = 66

    const val relationshipWidth: Int = 102
    const val relationshipHeight: Int = 51

    const val associativeOuterWidth: Int = 127
    const val associativeOuterHeight: Int = 66

    /** `<Texto>` sample in valores-padroes.xml (`Left`/`Top` are placement-only; size and style below). */
    const val annotationWidth: Int = 150
    const val annotationHeight: Int = 22

    /** `<Cor Valor="15780518"/>`. */
    const val annotationColorArgb: Int = 15_780_518

    /** `<Tipo Valor="1"/>` → [AnnotationType.HINT]. */
    val annotationType: AnnotationType = AnnotationType.HINT

    /** `<TextAlin Valor="0"/>` → [TextAlignment.LEFT]. */
    val annotationTextAlignment: TextAlignment = TextAlignment.LEFT

    /** `<TamAuto Valor="-1"/>` (Delphi true). */
    const val annotationAutoSize: Boolean = true

    /** `<Texto nome="Obs.: "/>` — placement always uses this caption (no deduplication). */
    const val annotationDefaultName: String = "Obs.: "
}
