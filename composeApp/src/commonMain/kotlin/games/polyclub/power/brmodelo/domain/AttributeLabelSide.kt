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
 * Which side of the attribute box shows the ellipse (“bullet”) and stub, and how the name is aligned.
 *
 * Mirrors the Delphi field historically named `ForcaOrientacao` (“forced orientation” of the label)
 * and the MER XML `<Orientacao Valor="…"/>`, using the same integer codes as `OrientacaoD` / `OrientacaoE`
 * in `mer.pas`. The inspector still shows localized captions (**Lado**: “Direito” / “Esquerdo”).
 */
enum class AttributeLabelSide(val pascalXmlCode: Int) {
    /** Ellipse and stub on the right; label right-aligned (`OrientacaoD`, MER value `2`). */
    BULLET_RIGHT(2),

    /** Ellipse and stub on the left; label left-aligned (`OrientacaoE`, MER value `3`; default in `TAtributo.Create`). */
    BULLET_LEFT(3),
    ;

    companion object {
        /** Maps a MER / Delphi integer; unknown values coerce to [BULLET_LEFT] like legacy loads. */
        fun fromPascalXmlCode(code: Int): AttributeLabelSide =
            entries.firstOrNull { it.pascalXmlCode == code } ?: BULLET_LEFT
    }
}

/**
 * Effective layout after Pascal [TAtributo.Paint] (`mer.pas`): `Ponto := Posi` then
 * `if Ponto=1 then Orientacao := OrientacaoE` and `if Ponto=3 then Orientacao := OrientacaoD`.
 * Here [Posi] is [TLigacao.MePonto](Self) — the **attribute-end** encaixe index on that link (`PontoInicial`
 * when the attribute is `BaseInicial`), **not** “which side of the owner the box sits on”.
 *
 * [conceptualAttributeAttachPonto] on `(owner, attribute)` returns the owner-side sector (1=left, 2=top,
 * 3=right, 4=bottom). That sector is **dual** to the stub side on the attribute: an attribute strictly **west**
 * of the owner connects from its **right** edge → MePonto `3` → [BULLET_RIGHT]; strictly **east** connects
 * from its **left** edge → MePonto `1` → [BULLET_LEFT]. Top/bottom (`2` / `4`) do not hit those `Paint`
 * branches, so the stored MER [stored] value is kept (same as Pascal when `Ponto` is 2 or 4).
 */
fun effectiveAttributeLabelSide(
    ownerPos: ElementPosition?,
    attrPos: ElementPosition,
    stored: AttributeLabelSide,
): AttributeLabelSide {
    val ownerSide = ownerPos?.let { conceptualAttributeAttachPonto(it, attrPos) } ?: return stored
    return when (ownerSide) {
        1 -> AttributeLabelSide.BULLET_RIGHT
        3 -> AttributeLabelSide.BULLET_LEFT
        else -> stored
    }
}

/** `true` when the ellipse sits on the attribute’s left (Pascal `OrientacaoE` / [BULLET_LEFT] branch). */
fun attributeEllipseOnLeft(ownerPos: ElementPosition?, attrPos: ElementPosition, stored: AttributeLabelSide): Boolean =
    effectiveAttributeLabelSide(ownerPos, attrPos, stored) == AttributeLabelSide.BULLET_LEFT
