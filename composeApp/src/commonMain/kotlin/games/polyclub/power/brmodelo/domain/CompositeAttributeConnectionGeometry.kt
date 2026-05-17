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
 * Horizontal model-space X where a **composite child** attribute attaches to the link
 * from the parent's [TBarraDeAtributos] bar (`mer.pas`).
 *
 * [compositeEllipseOnLeft] must be [attributeEllipseOnLeft] evaluated on the **composite parent**
 * (entity owner position, composite box position, composite [SchemaElement.Attribute.labelSide]).
 * When `true`, the bar is on the composite's physical **right** and children sit to the right →
 * use the child's **left** edge; when `false` (OrientacaoD / bullet on the right), the bar is on
 * the composite's **left** → use the child's **right** edge so the segment does not cross the label.
 */
internal fun compositeChildBarConnectionX(
    childBox: ElementPosition,
    compositeEllipseOnLeft: Boolean,
): Float =
    if (compositeEllipseOnLeft) childBox.x.toFloat()
    else (childBox.x + childBox.width).toFloat()
