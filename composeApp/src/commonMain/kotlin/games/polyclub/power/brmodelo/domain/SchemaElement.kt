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
 * Visual and semantic style applied to element labels.
 *
 * Corresponds to the `Font`, `FontColor`, and `FontStyles` published properties on `TBase` in `mer.pas`,
 * serialized under `<Fonte>` (`FonteNome`, `FonteTamanho`, `FonteEstilo`, `FonteCor`, optional `FonteScript`) in MER XML.
 *
 * [fontScript] stores Delphi `TFont.Charset` as in `.brM` (e.g. `TURKISH_CHARSET`). It is **not** applied to canvas text;
 * it exists only for round-trip fidelity with legacy files.
 */
data class LabelStyle(
    val color: Int? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    /** Underline (Delphi `fsUnderline` in `FonteEstilo`). */
    val underline: Boolean = false,
    /** Strikethrough (Delphi `fsStrikeOut` in `FonteEstilo`). */
    val strikeThrough: Boolean = false,
    /** Windows / logical font family name (e.g. `Tahoma`). `null` = canvas default family. */
    val fontFamilyName: String? = null,
    /** Point size as stored in MER (`FonteTamanho`), same as Pascal `TFont.Size`. `null` = legacy canvas default (~11 sp). */
    val fontSizePoints: Int? = null,
    /** Delphi `TFont.Charset` identifier or decimal string from `.brM`; optional `<FonteScript>` in MER XML. Not used for rendering. */
    val fontScript: String? = null,
)

/**
 * Direction of the optional directional arrow (TSeta) placed beside a relationship diamond.
 *
 * Corresponds to `SetaDirecao` on `TBaseRelacao` / `TEntidadeAssoss` and
 * `TSeta.Posicao` in mer.pas. Integer values match Pascal's TSeta positions exactly:
 *
 * | Code | Side   | Arrowhead |
 * |------|--------|-----------|
 * | 1    | LEFT   | UP        |
 * | 2    | LEFT   | DOWN      |
 * | 3    | TOP    | RIGHT     |
 * | 4    | TOP    | LEFT      |
 * | 5    | RIGHT  | DOWN      |
 * | 6    | RIGHT  | UP        |
 * | 7    | BOTTOM | LEFT      |
 * | 8    | BOTTOM | RIGHT     |
 */
enum class ArrowDirection(val code: Int) {
    NONE(0),
    LEFT_UP(1),
    LEFT_DOWN(2),
    TOP_RIGHT(3),
    TOP_LEFT(4),
    RIGHT_DOWN(5),
    RIGHT_UP(6),
    BOTTOM_LEFT(7),
    BOTTOM_RIGHT(8);

    companion object {
        fun fromCode(code: Int): ArrowDirection =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}

/**
 * Type of a free-text annotation element on the canvas.
 *
 * Corresponds to `TTexto.Tipo` and the Pascal constants
 * `TextoTipoBranco = 0`, `TextoTipoHint = 1`, `TextoTipoBox = 2`.
 */
enum class AnnotationType(val code: Int) {
    PLAIN(0),
    HINT(1),
    BOX(2);

    companion object {
        fun fromCode(code: Int): AnnotationType =
            entries.firstOrNull { it.code == code } ?: PLAIN
    }
}

/**
 * Text alignment for annotations.
 *
 * Corresponds to `TTexto.TextAlin` and the Pascal constants
 * `TextoAlinEsq = 0`, `TextoAlinCen = 1`, `TextoAlinDir = 2`.
 */
enum class TextAlignment(val code: Int) {
    LEFT(0),
    CENTER(1),
    RIGHT(2);

    companion object {
        fun fromCode(code: Int): TextAlignment =
            entries.firstOrNull { it.code == code } ?: LEFT
    }
}

/**
 * Type of a [Specialization] node.
 *
 * Corresponds to `TEspecializacao.Tipo` and the Pascal constants
 * `EspRestrita = 0`, `EspOpicional = 1`.
 */
enum class SpecializationType(val code: Int) {
    RESTRICTED(0),
    OPTIONAL(1);

    companion object {
        fun fromCode(code: Int): SpecializationType =
            entries.firstOrNull { it.code == code } ?: OPTIONAL
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sealed class hierarchy
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sealed hierarchy for all elements that can be placed on a conceptual schema canvas.
 *
 * Every subclass maps to a Pascal `TBase` descendant from `mer.pas`.
 * Cross-element references are expressed as [Int] IDs that resolve against
 * [games.polyclub.power.brmodelo.domain.ConceptualSchema.elements]. This mirrors the Pascal `OID` / `_Dono` pattern.
 *
 * @property id         Unique identifier within the schema. Corresponds to `TBase.OID`.
 * @property name       Display name / caption. Corresponds to `TBase.Nome`.
 * @property position   Canvas bounds. Corresponds to `TBase` Left/Top/Width/Height.
 * @property observations Free-text notes. Corresponds to `TBase.Observacoes`.
 * @property dictionary  Dictionary entry. Corresponds to `TBase.Dicionario`.
 * @property labelStyle  Font/color styling for the element's label.
 * @property hiddenAttributes Attributes removed from canvas but kept for model generation.
 *                            Corresponds to `TBase.AOcultos`.
 */
sealed class SchemaElement {
    abstract val id: Int
    abstract val name: String
    abstract val position: ElementPosition
    abstract val observations: String
    abstract val dictionary: String
    abstract val labelStyle: LabelStyle
    abstract val hiddenAttributes: List<HiddenAttribute>

    // ── Entity ──────────────────────────────────────────────────────────────

    /**
     * A regular or weak entity in the ER diagram.
     *
     * Corresponds to `TEntidade` (which extends `TBaseEntidade` → `TBase`).
     *
     * @param isWeak               Whether this is a weak entity (drawn with double border).
     *                             Derived from whether any of its connections has [games.polyclub.power.brmodelo.domain.Connection.isWeak] == true.
     * @param specializationId     ID of the [Specialization] node this entity is the *base* of,
     *                             if any. Corresponds to `_AutoRelacao` / `Especializacoes`.
     * @param parentSpecializationIds IDs of [Specialization] nodes owned by this entity as **generalization base**
     *                             (`baseEntityId` == this entity), matching XML `<Especializacoes>`. May also list
     *                             originating specs for subtype entities created by the specialization tool.
     */
    data class Entity(
        override val id: Int,
        override val name: String,
        override val position: ElementPosition,
        override val observations: String = "",
        override val dictionary: String = "",
        override val labelStyle: LabelStyle = LabelStyle(),
        override val hiddenAttributes: List<HiddenAttribute> = emptyList(),
        val isWeak: Boolean = false,
        val specializationId: Int? = null,
        val parentSpecializationIds: List<Int> = emptyList(),
    ) : SchemaElement()

    // ── Relationship ─────────────────────────────────────────────────────────

    /**
     * A binary or n-ary relationship between entities.
     *
     * Corresponds to `TRelacao` (which extends `TMaxRelacao` → `TBaseRelacao` → `TBase`).
     *
     * @param arrowDirection Direction of the optional directional arrow inside the diamond.
     *                       Corresponds to `TBaseRelacao.SetaDirecao`.
     * @param showName       When false the name label is not drawn.
     *                       Corresponds to `TBaseRelacao.NaoPinteNome`.
     */
    data class Relationship(
        override val id: Int,
        override val name: String,
        override val position: ElementPosition,
        override val observations: String = "",
        override val dictionary: String = "",
        override val labelStyle: LabelStyle = LabelStyle(),
        override val hiddenAttributes: List<HiddenAttribute> = emptyList(),
        val arrowDirection: ArrowDirection = ArrowDirection.NONE,
        val showName: Boolean = true,
    ) : SchemaElement()

    // ── Associative Entity ───────────────────────────────────────────────────

    /**
     * An associative entity (entity that is also a relationship).
     *
     * Corresponds to `TEntidadeAssoss` in the original Pascal source.
     * The embedded `TChildRelacao` is modelled as the inline properties below.
     *
     * @param relationshipName        Name of the inner relationship diamond.
     *                                Corresponds to `TEntidadeAssoss.RelacaoNome`.
     * @param relationshipDictionary  Dictionary text for the inner relationship.
     *                                Corresponds to `TEntidadeAssoss.RelecaoDicionario`.
     * @param relationshipObservations Observations for the inner relationship.
     *                                Corresponds to `TEntidadeAssoss.RelecaoObservacao`.
     * @param arrowDirection          Arrow direction inside the diamond.
     *                                Corresponds to `TEntidadeAssoss.SetaDirecao`.
     */
    data class AssociativeEntity(
        override val id: Int,
        override val name: String,
        override val position: ElementPosition,
        override val observations: String = "",
        override val dictionary: String = "",
        override val labelStyle: LabelStyle = LabelStyle(),
        override val hiddenAttributes: List<HiddenAttribute> = emptyList(),
        val relationshipName: String = "",
        val relationshipDictionary: String = "",
        val relationshipObservations: String = "",
        val arrowDirection: ArrowDirection = ArrowDirection.NONE,
    ) : SchemaElement()

    // ── Attribute ────────────────────────────────────────────────────────────

    /**
     * An attribute of an entity, relationship, or another composite attribute.
     *
     * Corresponds to `TAtributo` in `mer.pas`.
     *
     * @param ownerId          ID of the [SchemaElement] this attribute belongs to.
     *                         Corresponds to `TAtributo._Dono`.
     * @param isIdentifier     Whether this attribute is part of the primary key.
     *                         Corresponds to `TAtributo.Identificador`.
     * @param isMultiValued    Whether this attribute can have multiple values.
     *                         Corresponds to `TAtributo.Multivalorado` (derived from MaxCard > 1).
     * @param isOptional       Whether the attribute may have no value.
     *                         Corresponds to `TAtributo.Opcional`.
     * @param cardinality      Explicit min/max cardinality for multi-valued attributes.
     *                         Corresponds to `TAtributo.MinCard` / `TAtributo.MaxCard`.
     * @param multiValuedCount Physical-model hint persisted as QtdeMultivalorado in binary/XML.
     *                         Must match [games.polyclub.power.brmodelo.domain.ConceptualSchema.canonicalQtdeMultivalorado] after normalization
     *                         (see that function for visible vs oculto subtree rules; legacy saves often disagree).
     * @param valueType        Data type label (e.g. "VARCHAR").
     *                         Corresponds to `TAtributo.TipoDoValor`.
     * @param complement       Type complement (e.g. "100" for "VARCHAR(100)").
     *                         Corresponds to `TAtributo.Complemento`.
     * @param autoSize         Whether the element resizes itself to fit the label.
     *                         Corresponds to `TAtributo.TamAuto`.
     * @param deviationAngle   Layout hint: deviation angle from the owner for line routing.
     *                         Corresponds to `TAtributo.Desvio`.
     * @param labelSide        Which side the ellipse and label use ([AttributeLabelSide]); corresponds to
     *                         Pascal `TAtributo.ForcaOrientacao` and MER `<Orientacao Valor="2|3"/>`.
     *                         When the attribute attaches to the owner’s left or right edge, [mer.pas]
     *                         `TAtributo.Paint` overrides this from geometry; for top/bottom this stored value applies.
     * @param childAttributeIds IDs of child [Attribute] elements for composite attributes.
     *                          Non-empty means composite on the canvas (bar children).
     * @param compostoPersisted When true, the attribute stays composite in the model even with no
     *                          visible bar children: either loaded from XML/brM with `Composto` and
     *                          no `<BarraDeAtributos>`, or after **hiding** all composite children
     *                          (they move to [hiddenAttributes]). Cleared when the last canvas child
     *                          is **deleted** and there are no ocultos left. Mirrors Delphi `TAtributo.Composto`
     *                          beyond the simple `Atributos.Count > 0` check.
     */
    data class Attribute(
        override val id: Int,
        override val name: String,
        override val position: ElementPosition,
        override val observations: String = "",
        override val dictionary: String = "",
        override val labelStyle: LabelStyle = LabelStyle(),
        override val hiddenAttributes: List<HiddenAttribute> = emptyList(),
        val ownerId: Int,
        val isIdentifier: Boolean = false,
        val isMultiValued: Boolean = false,
        val isOptional: Boolean = false,
        val cardinality: AttributeCardinality = AttributeCardinality(
            0,
            0
        ),
        val multiValuedCount: Int = 0,
        val valueType: String = "",
        val complement: String = "",
        val autoSize: Boolean = true,
        val deviationAngle: Int = 0,
        val labelSide: AttributeLabelSide = AttributeLabelSide.BULLET_LEFT,
        val childAttributeIds: List<Int> = emptyList(),
        val compostoPersisted: Boolean = false,
    ) : SchemaElement() {
        /** True when there are canvas children or composite semantics are kept via [compostoPersisted]. */
        val isComposite: Boolean get() = childAttributeIds.isNotEmpty() || compostoPersisted
    }

    // ── Specialization ───────────────────────────────────────────────────────

    /**
     * A specialization (ISA) hierarchy node.
     *
     * Corresponds to `TEspecializacao` in `mer.pas`.
     * The base entity is stored as [baseEntityId] and child entities connect to this
     * node via [games.polyclub.power.brmodelo.domain.Connection] elements (mirroring the ligações on `TEspecializacao`).
     *
     * @param baseEntityId  ID of the [Entity] that is the generalization (supertype).
     *                      Corresponds to `TEspecializacao.EntidadeBase`.
     * @param type          Restricted or optional specialization.
     *                      Corresponds to `TEspecializacao.Tipo`.
     * @param isPartial     Whether the specialization covers only part of the base entity's
     *                      instances. Corresponds to `TEspecializacao.Parcial`.
     */
    data class Specialization(
        override val id: Int,
        override val name: String = "",
        override val position: ElementPosition,
        override val observations: String = "",
        override val dictionary: String = "",
        override val labelStyle: LabelStyle = LabelStyle(),
        override val hiddenAttributes: List<HiddenAttribute> = emptyList(),
        val baseEntityId: Int,
        val type: SpecializationType = SpecializationType.OPTIONAL,
        val isPartial: Boolean = false,
    ) : SchemaElement() {
        /** True when [type] == [games.polyclub.power.brmodelo.domain.SpecializationType.RESTRICTED], same as `TEspecializacao.Restrito`. */
        val isRestricted: Boolean get() = type == SpecializationType.RESTRICTED
    }

    // ── Self-Relationship ────────────────────────────────────────────────────

    /**
     * A self-referencing (recursive) relationship of a single entity.
     *
     * Corresponds to `TAutoRelacao` in `mer.pas`.
     * Rendered as a small diamond attached to one side of the owning entity.
     *
     * @param ownerEntityId ID of the [Entity] this self-relationship belongs to.
     *                      Corresponds to `TAutoRelacao.Pai`.
     * @param arrowDirection Direction of the optional directional arrow inside the diamond.
     *                       Corresponds to `TBaseRelacao.SetaDirecao`.
     */
    data class SelfRelationship(
        override val id: Int,
        override val name: String,
        override val position: ElementPosition,
        override val observations: String = "",
        override val dictionary: String = "",
        override val labelStyle: LabelStyle = LabelStyle(),
        override val hiddenAttributes: List<HiddenAttribute> = emptyList(),
        val ownerEntityId: Int,
        val arrowDirection: ArrowDirection = ArrowDirection.NONE,
    ) : SchemaElement()

    // ── Annotation ───────────────────────────────────────────────────────────

    /**
     * A free-text annotation placed anywhere on the canvas.
     *
     * Corresponds to `TTexto` in `mer.pas`.
     *
     * @param color         Background colour for drawn box styles, as Windows **COLORREF** (`0x00BBGGRR`),
     *                      same integer as XML `<Cor Valor="…"/>` and Delphi `TTexto.Cor`.
     * @param annotationType Visual style of the annotation.
     *                      Corresponds to `TTexto.Tipo`.
     * @param alignment     Text alignment inside the annotation.
     *                      Corresponds to `TTexto.TextAlin`.
     * @param autoSize      Whether the box auto-resizes to fit text.
     *                      Corresponds to `TBaseTexto.TamAuto`.
     */
    data class Annotation(
        override val id: Int,
        override val name: String,
        override val position: ElementPosition,
        override val observations: String = "",
        override val dictionary: String = "",
        override val labelStyle: LabelStyle = LabelStyle(),
        override val hiddenAttributes: List<HiddenAttribute> = emptyList(),
        val color: Int? = null,
        val annotationType: AnnotationType = AnnotationType.PLAIN,
        val alignment: TextAlignment = TextAlignment.LEFT,
        val autoSize: Boolean = true,
    ) : SchemaElement()
}

/**
 * Applies rigid minimum width/height from [ElementPosition] for all canvas elements.
 */
fun SchemaElement.withCoercedMinimumDimensions(): SchemaElement {
    val p = position.coercedToMinimumDimensions()
    if (p == position) return this
    return when (this) {
        is SchemaElement.Entity -> copy(position = p)
        is SchemaElement.Relationship -> copy(position = p)
        is SchemaElement.AssociativeEntity -> copy(position = p)
        is SchemaElement.Attribute -> copy(position = p)
        is SchemaElement.Specialization -> copy(position = p)
        is SchemaElement.SelfRelationship -> copy(position = p)
        is SchemaElement.Annotation -> copy(position = p)
    }
}
