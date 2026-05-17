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
 * Uses the same active edge as [attributeEllipseOnLeft] with the **composite** as owner
 * ([composite.position]) and the child's stored [SchemaElement.Attribute.labelSide], so each child
 * can differ from the parent composite (MER `<Orientacao>` per child). The segment meets the stub
 * side ([games.polyclub.power.brmodelo.ui.canvas.attributeActiveEdgeConnectorY] in the renderer), not
 * the opposite bbox edge — otherwise a child with `OrientacaoE` while the bar sits on the composite's
 * left would get a horizontal leg through the label (Pascal `MER-PousadaSolDaManha-uf-movido2.xml`).
 */
internal fun compositeChildBarConnectionX(
    childBox: ElementPosition,
    childEllipseOnLeft: Boolean,
): Float =
    if (childEllipseOnLeft) childBox.x.toFloat()
    else (childBox.x + childBox.width).toFloat()

/** Resolves [childEllipseOnLeft] from the composite parent box + child layout (Pascal `TAtributo.Paint`). */
internal fun compositeChildBarConnectionX(
    schema: ConceptualSchema,
    composite: SchemaElement.Attribute,
    child: SchemaElement.Attribute,
): Float {
    val childEllipseOnLeft = attributeEllipseOnLeft(
        composite.position,
        child.position,
        child.labelSide,
    )
    return compositeChildBarConnectionX(child.position, childEllipseOnLeft)
}
