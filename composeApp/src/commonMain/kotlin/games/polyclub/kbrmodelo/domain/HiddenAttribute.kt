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

package games.polyclub.kbrmodelo.domain

/**
 * An attribute that belongs to an entity or relationship but is not rendered on the canvas.
 *
 * Corresponds to `TAtributoOculto` in `att.pas`. These are attributes "hidden" from the
 * diagram because they were explicitly detached from the canvas, but they still carry
 * semantic information about the element (e.g. for logical/physical model generation).
 *
 * Composite hidden attributes are represented via the [children] list (mirroring the
 * `Filhos: TObjectList` on `TAtributoOculto`).
 *
 * @param name         Attribute name. Corresponds to [TAtributoOculto.Nome].
 * @param type         Data type string (e.g. "VARCHAR"). Corresponds to [TAtributoOculto.Tipo].
 * @param isIdentifier Whether this attribute is part of the identifier/key.
 *                     Corresponds to [TAtributoOculto.Identificador].
 * @param cardinality  Min/max cardinality for multi-valued attributes.
 *                     Corresponds to [TAtributoOculto.MinCard] / [TAtributoOculto.MaxCard].
 *                     A cardinality with maxCardinality == 0 means the attribute is NOT multi-valued.
 * @param position     Canvas position stored for when the attribute is made visible again.
 *                     Corresponds to [TAtributoOculto.LeftTop].
 * @param children     Sub-attributes of a composite attribute, in order.
 *                     Corresponds to [TAtributoOculto.Filhos].
 */
data class HiddenAttribute(
    val name: String,
    val type: String,
    val isIdentifier: Boolean,
    val cardinality: AttributeCardinality,
    val position: ElementPosition,
    val children: List<HiddenAttribute> = emptyList(),
) {
    /** True when [cardinality.maxCardinality] > 0, same logic as [TAtributoOculto.Multivalorado]. */
    val isMultiValued: Boolean get() = cardinality.maxCardinality > 0

    /** True when [children] is not empty, same logic as [TAtributoOculto.Composto]. */
    val isComposite: Boolean get() = children.isNotEmpty()
}
