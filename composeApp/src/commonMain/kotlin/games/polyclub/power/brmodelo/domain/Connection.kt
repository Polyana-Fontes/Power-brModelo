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
 * Orientation of a connection line segment.
 *
 * Corresponds to the Pascal constants `OrientacaoV = 0`, `OrientacaoH = 1`,
 * `OrientacaoD = 2`, `OrientacaoE = 3` used by `TLinha` and `TLigacao`.
 */
enum class LineOrientation(val code: Int) {
    VERTICAL(0),
    HORIZONTAL(1),
    DIAGONAL(2),
    LEFT(3);

    companion object {
        /** Unknown codes default to vertical like `TLigacao.Create` (`FOrientacao := OrientacaoV`) in `mer.pas`. */
        fun fromCode(code: Int): LineOrientation =
            entries.firstOrNull { it.code == code } ?: VERTICAL
    }
}

/**
 * A directed connection between two [games.polyclub.power.brmodelo.domain.SchemaElement]s.
 *
 * Corresponds to `TLigacao` in `mer.pas`. In the original Pascal code a
 * `TLigacao` always connects exactly two elements (`E1` and `E2`). The
 * cardinality label is displayed near [elementIdB] (the "end" side).
 *
 * Connections are used for:
 * - Entity ↔ Relationship (or AssociativeEntity / SelfRelationship)
 * - Entity ↔ Specialization (base entity or child entity)
 * - Attribute ↔ its owner entity/relationship
 * - Attribute ↔ its child attributes (composite)
 *
 * @param id                  Unique identifier within the schema.
 * @param elementIdA          ID of the first connected element. Corresponds to `TLigacao.E1.OID`.
 * @param elementIdB          ID of the second connected element. Corresponds to `TLigacao.E2.OID`.
 * @param cardinality         Cardinality label shown near [elementIdB].
 *                            Corresponds to `TLigacao.Cardinalidade` (1–4) mapped via [games.polyclub.power.brmodelo.domain.Cardinality].
 *                            Null when no cardinality applies (e.g. attribute connections).
 * @param showCardinality     Whether the cardinality label is visible on the canvas.
 *                            Corresponds to `TLigacao.MostraCardinalidade`.
 * @param cardinalityFixed    When true, the label box is not auto-recomputed from connection geometry when
 *                            endpoints move; the stored position is translated with dragged elements instead.
 *                            Does not block manual editing of coordinates in the inspector.
 *                            Corresponds to `TCardinalidade.Fixa`.
 * @param isWeak              Whether this connection represents a weak-entity participation
 *                            (drawn as a double line). Corresponds to `TLigacao.Fraca`.
 * @param orientation         Primary orientation of the line segments.
 *                            Corresponds to `TLigacao.Orientacao`.
 * @param cardinalityRole     Optional role name displayed after the cardinality label
 *                            (e.g. "Responsável" → "(1,1) Responsável").
 *                            Corresponds to `TCardinalidade.nome` in the original Pascal source.
 * @param cardinalityPosition Position of the floating cardinality label on the canvas.
 *                            Corresponds to `TCardinalidade` Left/Top (a `TBase` subclass).
 * @param cardinalityAutoSize Whether the cardinality label box auto-resizes to fit its text.
 *                            Corresponds to `TCardinalidade.TamAuto`.
 * @param useAssociativeOuterForEndA When [elementIdA] refers to an [SchemaElement.AssociativeEntity] and the
 *                                   other end is not an attribute, use the outer rectangle for routing instead
 *                                   of the inner diamond. Normally `false` because the relationship side uses the inner shape.
 * @param useAssociativeOuterForEndB Same for [elementIdB] — typically `true` when the **entity** side of the link
 *                                   was chosen on the outer rectangle of an associative entity.
 * @param cardinalityObservations Free text on the cardinality label (Pascal `TCardinalidade.Observacao`).
 * @param cardinalityDictionary Free text on the cardinality label (XML `<Dicionario>` under `<Cardinalidade>`).
 * @param cardinalityLabelStyle Font, size, colour, and emphasis for the cardinality label text (`TCardinalidade`
 *                                `<Fonte>` in MER XML / DFM font properties on `TCardinalidade` in `.brM`).
 */
data class Connection(
    val id: Int,
    val elementIdA: Int,
    val elementIdB: Int,
    val cardinality: Cardinality? = null,
    val showCardinality: Boolean = true,
    val cardinalityFixed: Boolean = false,
    val isWeak: Boolean = false,
    /** Default matches Pascal `TLigacao.Create`: `FOrientacao := OrientacaoV` (inspector **H. Vert.**). */
    val orientation: LineOrientation = LineOrientation.VERTICAL,
    val cardinalityRole: String = "",
    val cardinalityObservations: String = "",
    val cardinalityDictionary: String = "",
    val cardinalityPosition: ElementPosition? = null,
    val cardinalityAutoSize: Boolean = true,
    val useAssociativeOuterForEndA: Boolean = false,
    val useAssociativeOuterForEndB: Boolean = false,
    val cardinalityLabelStyle: LabelStyle = LabelStyle(),
) {
    companion object {
        /**
         * Default `Width` / `Height` for `TCardinalidade` in brModelo XML
         * (see `desktopTest/resources/valores-padroes.xml`).
         */
        const val DEFAULT_LABEL_WIDTH = 36
        const val DEFAULT_LABEL_HEIGHT = 20

        /** Default `Cor` (background) for the cardinality label control. */
        const val DEFAULT_LABEL_BACKGROUND_COLOR = 15780518
    }
}
