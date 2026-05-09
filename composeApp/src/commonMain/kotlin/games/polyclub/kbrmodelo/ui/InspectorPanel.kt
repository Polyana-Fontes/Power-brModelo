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

package games.polyclub.kbrmodelo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.polyclub.kbrmodelo.domain.AnnotationType
import games.polyclub.kbrmodelo.domain.ArrowDirection
import games.polyclub.kbrmodelo.domain.CanvasSelection
import games.polyclub.kbrmodelo.domain.Cardinality
import games.polyclub.kbrmodelo.domain.ConceptualSchema
import games.polyclub.kbrmodelo.domain.ElementPosition
import games.polyclub.kbrmodelo.domain.HiddenAttribute
import games.polyclub.kbrmodelo.domain.LineOrientation
import games.polyclub.kbrmodelo.domain.SchemaElement
import games.polyclub.kbrmodelo.domain.SpecializationType
import games.polyclub.kbrmodelo.domain.TextAlignment
import games.polyclub.kbrmodelo.ui.canvas.withPosition

// ── Colour palette ────────────────────────────────────────────────────────────

private val INSPECTOR_BG       = Color(0xFFF0F2F5)
private val INSPECTOR_BORDER   = Color(0xFF8090A0)
private val HEADER_BG          = Color(0xFFD8DDE4)
private val HEADER_BORDER      = Color(0xFFB0BAC4)
private val TAB_ACTIVE_BG      = Color(0xFFFFFFFF)
private val TAB_INACTIVE_BG    = Color(0xFFC4CED8)
private val SECTION_HEADER_BG  = Color(0xFFD4DCE8)
private val CELL_LABEL_BG      = Color(0xFFE0E8F0)
private val CELL_LABEL_FOCUSED = Color(0xFF1050A0)
private val CELL_VALUE_BG      = Color(0xFFFFFFFF)
private val CELL_BORDER        = Color(0xFFB0BEC5)
private val LABEL_COLOR        = Color(0xFF2A3A4A)
private val LABEL_FOCUSED_COLOR = Color(0xFFFFFFFF)
private val VALUE_COLOR        = Color(0xFF1A2535)
private val HINT_BG            = Color(0xFFDDE5EE)
private val HINT_TEXT_COLOR    = Color(0xFF2A3040)

private val CELL_LABEL_WIDTH = 72.dp
private val ROW_TEXT_SIZE    = 10.sp
private val VALUE_TEXT_SIZE  = 10.sp

private enum class InspectorTab { Selecao, AtrOcultos }

// ── Hint strings (sourced from ajuda.pas AutoHelp) ────────────────────────────

private val HINTS: Map<String, String> = mapOf(
    "NOME"           to "Descrição/identificação do objeto.",
    "NOME_MODELO"    to "Nome do modelo conceitual.",
    "OBS"            to "Algo importante a ser anotado para posterior observação.",
    "ALINHAMENTOLT"  to "Reposiciona o controle quanto a posição no modelo (esquerda ou direita).",
    "ALINHAMENTOWH"  to "Reposiciona o controle quanto a altura ou largura.",
    "AUTO_REL"       to "A entidade está auto relacionada.",
    "ESPECIALIZADA"  to "A entidade está especializada.",
    "EA_NOME"        to "Nome do relacionamento contido na entidade associativa.",
    "EA_DIC"         to "Dicionário de dados do relacionamento contido na entidade associativa.",
    "EA_OBS"         to "Algo importante a ser anotado sobre o relacionamento contido na entidade associativa.",
    "CARD_FIXA"      to "Fixar posição: Se fixada, a cardinalidade não se moverá ao mover a entidade ou relacionamento ao qual esteja vinculada.",
    "CARD_POS_LINHA" to "Alinhamento da fixação da cardinalidade.",
    "CARD_TAM_AUT"   to "Controle do tamanho do desenho da cardinalidade.",
    "ENT_FRACA"      to "Tipo de entidade (fraca ou normal).",
    "CARDINALIDADE"  to "Cardinalidade do relacionamento entre as entidades.",
    "PAPEL"          to "Descrição do papel da cardinalidade (descrição/observação).",
    "ATRIB_TAM_AUT"  to "Controle do tamanho do desenho do atributo.",
    "ATRIB_LADO"     to "Alinhamento da fixação do atributo.",
    "IDENTIFICADOR"  to "O atributo pode ser identificador, Opcional, composto e/ou multivalorado.",
    "OPCIONAL"       to "O atributo pode ser identificador, Opcional, composto e/ou multivalorado.",
    "COMPOSTO"       to "O atributo pode ser identificador, Opcional, composto e/ou multivalorado.",
    "MULTIVALORADO"  to "O atributo pode ser identificador, Opcional, composto e/ou multivalorado.",
    "CARD_MIN"       to "Cardinalidade mínima do atributo. Ex: (1, n), neste caso o \"1\".",
    "CARD_MAX"       to "Cardinalidade máxima do atributo. Ex: (1, n), neste caso o \"n\".",
    "TIPO_VALOR"     to "Tipo do atributo, por exemplo: Texto(n), Número(n), booleano etc.",
    "COMPLEMENTO"    to "Tamanho ou complemento do tipo do atributo.",
    "QTD_CAMPOS"     to "Quantidade de campos gerados para atributo multivalorado.",
    "EXCLUSIVA"      to "Especialização exclusiva (onde A é B OU C) ou não exclusiva (A pode ser B E/OU C).",
    "ESP_PARCIAL"    to "Especialização parcial: apenas alguns membros de A são especializados.",
    "MOLDURA"        to "Forma de desenho da moldura do texto.",
    "ALIN_TEXTO"     to "Alinhamento do texto dentro da anotação.",
    "RPOSISETA"      to "Posiciona uma seta ao lado do relacionamento para indicar a direção de leitura do diagrama.",
    "VERSAO"         to "Versão do formato do arquivo de modelo.",
    "AUTOR"          to "Nome(s) do(s) autor(es) do modelo.",
)

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
internal fun InspectorPanel(
    schema: ConceptualSchema? = null,
    selection: CanvasSelection = CanvasSelection.None,
    onSchemaCommit: (ConceptualSchema) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf(InspectorTab.Selecao) }
    // Field key currently focused in the grid — drives the hint text at the bottom.
    var focusedKey by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .width(210.dp)
            .fillMaxHeight()
            .border(1.dp, INSPECTOR_BORDER)
            .background(INSPECTOR_BG)
    ) {
        // ── Tab header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(HEADER_BG),
            verticalAlignment = Alignment.Bottom
        ) {
            TabHeader(
                label = "Seleção",
                selected = activeTab == InspectorTab.Selecao,
                modifier = Modifier.weight(1f),
            ) { activeTab = InspectorTab.Selecao }

            TabHeader(
                label = "Atr. ocultos",
                selected = activeTab == InspectorTab.AtrOcultos,
                modifier = Modifier.width(80.dp),
            ) { activeTab = InspectorTab.AtrOcultos }
        }

        // ── Tab content ───────────────────────────────────────────────────────
        when (activeTab) {
            InspectorTab.Selecao -> SelectionTab(
                schema = schema,
                selection = selection,
                focusedKey = focusedKey,
                onFocusChange = { focusedKey = it },
                onSchemaCommit = onSchemaCommit,
                modifier = Modifier.weight(1f),
            )
            InspectorTab.AtrOcultos -> HiddenAttributesTab(
                schema = schema,
                selection = selection,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Tab header ────────────────────────────────────────────────────────────────

@Composable
private fun TabHeader(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg        = if (selected) TAB_ACTIVE_BG else TAB_INACTIVE_BG
    val textColor = if (selected) Color(0xFF1B2B3B) else Color(0xFF4A5A6A)
    val weight    = if (selected) FontWeight.Bold else FontWeight.Normal
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(bg)
            .border(width = 1.dp, color = HEADER_BORDER)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 10.sp, fontWeight = weight, color = textColor)
    }
}

// ── Selection tab ─────────────────────────────────────────────────────────────

@Composable
private fun SelectionTab(
    schema: ConceptualSchema?,
    selection: CanvasSelection,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hintText = focusedKey?.let { HINTS[it] } ?: ""

    Column(modifier = modifier) {
        // Scrollable properties grid
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                schema == null -> Unit

                selection == CanvasSelection.None ->
                    SchemaMetaContent(schema, focusedKey, onFocusChange, onSchemaCommit)

                selection is CanvasSelection.Element -> {
                    val elem = schema.elements[selection.id]
                    if (elem != null) {
                        ElementContent(elem, schema, focusedKey, onFocusChange, onSchemaCommit)
                    }
                }

                selection is CanvasSelection.Cardinality -> {
                    val conn = schema.connections.firstOrNull { it.id == selection.connectionId }
                    if (conn != null) {
                        CardinalityContent(conn, schema, focusedKey, onFocusChange, onSchemaCommit)
                    }
                }
            }
        }

        // Hint area at the bottom (mirrors the original brModelo hint panel)
        HorizontalDivider(color = Color(0xFF7A9ABF), thickness = 1.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(HINT_BG)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                text = hintText,
                fontSize = 9.sp,
                color = HINT_TEXT_COLOR,
                lineHeight = 13.sp,
            )
        }
    }
}

// ── Schema-level content (nothing selected) ───────────────────────────────────

@Composable
private fun SchemaMetaContent(
    schema: ConceptualSchema,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Informações: Modelo Conceitual")
    ReadOnlyRow("Nome",    schema.name,    "NOME_MODELO",  focusedKey, onFocusChange)
    ReadOnlyRow("Versão",  schema.version, "VERSAO",       focusedKey, onFocusChange)
    EditableRow("Autor(es)", schema.author, "AUTOR", focusedKey, onFocusChange) {
        onSchemaCommit(schema.copy(author = it))
    }
    EditableRow("Observações", schema.observations, "OBS", focusedKey, onFocusChange) {
        onSchemaCommit(schema.copy(observations = it))
    }
}

// ── Element content dispatcher ────────────────────────────────────────────────

@Composable
private fun ElementContent(
    element: SchemaElement,
    schema: ConceptualSchema,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    val friendlyName = when (element) {
        is SchemaElement.Entity           -> "Entidade"
        is SchemaElement.Relationship     -> "Relacionamento"
        is SchemaElement.AssociativeEntity -> "Entidade associativa"
        is SchemaElement.Attribute        -> "Propriedade"
        is SchemaElement.Specialization   -> "Especialização"
        is SchemaElement.SelfRelationship -> "Auto relacionamento"
        is SchemaElement.Annotation       -> "Observação"
    }

    SectionTitle("Edição: $friendlyName")

    // Common fields for all elements
    EditableRow("Nome", element.name, "NOME", focusedKey, onFocusChange) { newName ->
        onSchemaCommit(schema.withElement(element.withName(newName)))
    }
    EditableRow("Observação", element.observations, "OBS", focusedKey, onFocusChange) { v ->
        onSchemaCommit(schema.withElement(element.withObservations(v)))
    }

    SectionTitle("Posição e Tamanho")
    val p = element.position
    EditableRow("Esquerda (Left)", p.x.toString(), "ALINHAMENTOLT", focusedKey, onFocusChange) { v ->
        v.toIntOrNull()?.let {
            onSchemaCommit(schema.withElement(element.withPosition(p.copy(x = it))))
        }
    }
    EditableRow("Acima (Top)", p.y.toString(), "ALINHAMENTOLT", focusedKey, onFocusChange) { v ->
        v.toIntOrNull()?.let {
            onSchemaCommit(schema.withElement(element.withPosition(p.copy(y = it))))
        }
    }
    EditableRow("Largura (Width)", p.width.toString(), "ALINHAMENTOWH", focusedKey, onFocusChange) { v ->
        v.toIntOrNull()?.let {
            onSchemaCommit(schema.withElement(element.withPosition(p.copy(width = it))))
        }
    }
    EditableRow("Altura (Height)", p.height.toString(), "ALINHAMENTOWH", focusedKey, onFocusChange) { v ->
        v.toIntOrNull()?.let {
            onSchemaCommit(schema.withElement(element.withPosition(p.copy(height = it))))
        }
    }

    // Type-specific fields
    when (element) {
        is SchemaElement.Entity           -> EntityFields(element, schema, focusedKey, onFocusChange, onSchemaCommit)
        is SchemaElement.Relationship     -> RelationshipFields(element, schema, focusedKey, onFocusChange, onSchemaCommit)
        is SchemaElement.AssociativeEntity -> AssocEntityFields(element, schema, focusedKey, onFocusChange, onSchemaCommit)
        is SchemaElement.Attribute        -> AttributeFields(element, schema, focusedKey, onFocusChange, onSchemaCommit)
        is SchemaElement.Specialization   -> SpecializationFields(element, schema, focusedKey, onFocusChange, onSchemaCommit)
        is SchemaElement.SelfRelationship -> SelfRelFields(element, schema, focusedKey, onFocusChange, onSchemaCommit)
        is SchemaElement.Annotation       -> AnnotationFields(element, schema, focusedKey, onFocusChange, onSchemaCommit)
    }
}

// ── Entity ────────────────────────────────────────────────────────────────────

@Composable
private fun EntityFields(
    element: SchemaElement.Entity,
    schema: ConceptualSchema,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Esquema")
    val autoRel = schema.connections.any {
        it.elementIdA == element.id && it.elementIdB == element.id
    }
    ReadOnlyRow("Auto relacionado", if (autoRel) "Sim" else "Não", "AUTO_REL", focusedKey, onFocusChange)
    ReadOnlyRow("Especializada",
        if (element.parentSpecializationIds.isNotEmpty()) "Sim" else "Não",
        "ESPECIALIZADA", focusedKey, onFocusChange)
}

// ── Relationship ──────────────────────────────────────────────────────────────

@Composable
private fun RelationshipFields(
    element: SchemaElement.Relationship,
    schema: ConceptualSchema,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Relacionamento")
    DropdownRow(
        label = "Direção",
        selected = element.arrowDirection.label(),
        options = ArrowDirection.entries.map { it.label() },
        key = "RPOSISETA",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { label ->
        val dir = ArrowDirection.entries.firstOrNull { it.label() == label } ?: ArrowDirection.NONE
        onSchemaCommit(schema.withElement(element.copy(arrowDirection = dir)))
    }
}

// ── Associative entity ────────────────────────────────────────────────────────

@Composable
private fun AssocEntityFields(
    element: SchemaElement.AssociativeEntity,
    schema: ConceptualSchema,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Esquema")
    val autoRel = schema.connections.any {
        it.elementIdA == element.id && it.elementIdB == element.id
    }
    ReadOnlyRow("Auto relacionado", if (autoRel) "Sim" else "Não", "AUTO_REL", focusedKey, onFocusChange)

    SectionTitle("Relacionamento")
    EditableRow("+Nome", element.relationshipName, "EA_NOME", focusedKey, onFocusChange) { v ->
        onSchemaCommit(schema.withElement(element.copy(relationshipName = v)))
    }
    EditableRow("+Dicionário", element.relationshipDictionary, "EA_DIC", focusedKey, onFocusChange) { v ->
        onSchemaCommit(schema.withElement(element.copy(relationshipDictionary = v)))
    }
    EditableRow("+Observação", element.relationshipObservations, "EA_OBS", focusedKey, onFocusChange) { v ->
        onSchemaCommit(schema.withElement(element.copy(relationshipObservations = v)))
    }
    DropdownRow(
        label = "+Direção",
        selected = element.arrowDirection.assocLabel(),
        options = assocDirectionOptions(),
        key = "RPOSISETA",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { label ->
        val dir = assocLabelToDirection(label)
        onSchemaCommit(schema.withElement(element.copy(arrowDirection = dir)))
    }
}

// ── Attribute / Property ──────────────────────────────────────────────────────

@Composable
private fun AttributeFields(
    element: SchemaElement.Attribute,
    schema: ConceptualSchema,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Atributo")
    DropdownRow(
        label = "Tamanho aut.",
        selected = if (element.autoSize) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "ATRIB_TAM_AUT",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v -> onSchemaCommit(schema.withElement(element.copy(autoSize = v == "Sim"))) }

    val ownerPos = schema.elements[element.ownerId]?.position
    val attrPos  = element.position
    val side = if (ownerPos != null) {
        val ownerCx = ownerPos.x + ownerPos.width / 2
        val attrCx  = attrPos.x  + attrPos.width  / 2
        if (attrCx >= ownerCx) "Direito" else "Esquerdo"
    } else "Esquerdo"
    ReadOnlyRow("Lado", side, "ATRIB_LADO", focusedKey, onFocusChange)

    DropdownRow(
        label = "Identificador",
        selected = if (element.isIdentifier) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "IDENTIFICADOR",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v -> onSchemaCommit(schema.withElement(element.copy(isIdentifier = v == "Sim"))) }

    DropdownRow(
        label = "Opcional",
        selected = if (element.isOptional) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "OPCIONAL",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v -> onSchemaCommit(schema.withElement(element.copy(isOptional = v == "Sim"))) }

    ReadOnlyRow("Composto", if (element.isComposite) "Sim" else "Não", "COMPOSTO", focusedKey, onFocusChange)

    DropdownRow(
        label = "Multivalorado",
        selected = if (element.isMultiValued) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "MULTIVALORADO",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v -> onSchemaCommit(schema.withElement(element.copy(isMultiValued = v == "Sim"))) }

    ReadOnlyRow("Qtd. Campos", element.multiValuedCount.toString(), "QTD_CAMPOS", focusedKey, onFocusChange)

    EditableRow(
        label = "Card. Mínima",
        value = element.cardinality.minCardinality.toString(),
        key = "CARD_MIN",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
        enabled = element.isMultiValued,
    ) { v ->
        v.toIntOrNull()?.let {
            onSchemaCommit(schema.withElement(element.copy(cardinality = element.cardinality.copy(minCardinality = it))))
        }
    }
    val maxLabel = if (element.cardinality.isUnbounded) "n" else element.cardinality.maxCardinality.toString()
    EditableRow(
        label = "Card. Máxima",
        value = maxLabel,
        key = "CARD_MAX",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
        enabled = element.isMultiValued,
    ) { v ->
        val intVal = if (v == "n") 21 else v.toIntOrNull() ?: return@EditableRow
        onSchemaCommit(schema.withElement(element.copy(cardinality = element.cardinality.copy(maxCardinality = intVal))))
    }

    EditableRow("Tipo", element.valueType, "TIPO_VALOR", focusedKey, onFocusChange) { v ->
        onSchemaCommit(schema.withElement(element.copy(valueType = v)))
    }
    EditableRow("Tamanho", element.complement, "COMPLEMENTO", focusedKey, onFocusChange) { v ->
        onSchemaCommit(schema.withElement(element.copy(complement = v)))
    }
}

// ── Specialization ────────────────────────────────────────────────────────────

@Composable
private fun SpecializationFields(
    element: SchemaElement.Specialization,
    schema: ConceptualSchema,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Especialização")
    ReadOnlyRow(
        "Exclusiva",
        if (element.isRestricted) "Sim" else "Não",
        "EXCLUSIVA", focusedKey, onFocusChange,
    )
    DropdownRow(
        label = "Esp. Parcial",
        selected = if (element.isPartial) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "ESP_PARCIAL",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v -> onSchemaCommit(schema.withElement(element.copy(isPartial = v == "Sim"))) }
}

// ── Self-relationship ─────────────────────────────────────────────────────────

@Composable
private fun SelfRelFields(
    element: SchemaElement.SelfRelationship,
    schema: ConceptualSchema,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Relacionamento")
    DropdownRow(
        label = "Direção",
        selected = element.arrowDirection.label(),
        options = ArrowDirection.entries.map { it.label() },
        key = "RPOSISETA",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { label ->
        val dir = ArrowDirection.entries.firstOrNull { it.label() == label } ?: ArrowDirection.NONE
        onSchemaCommit(schema.withElement(element.copy(arrowDirection = dir)))
    }
}

// ── Annotation ────────────────────────────────────────────────────────────────

private val ANNOTATION_COLORS = listOf(
    "Branco"     to 0xFFFFFFFF,
    "Azul claro" to 0xFFADD8E6,
    "Creme"      to 0xFFFFFDD0,
    "Verde claro" to 0xFF90EE90,
    "Amarelo"    to 0xFFFFFF00,
    "Rosa"       to 0xFFFFB6C1,
    "Cinza claro" to 0xFFD3D3D3,
)

@Composable
private fun AnnotationFields(
    element: SchemaElement.Annotation,
    schema: ConceptualSchema,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Aparência")
    DropdownRow(
        label = "Cor",
        selected = ANNOTATION_COLORS.firstOrNull { it.second.toLong() == element.color?.toLong() }?.first ?: "Branco",
        options = ANNOTATION_COLORS.map { it.first },
        key = "TIPO_VALOR",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { label ->
        val argb = ANNOTATION_COLORS.firstOrNull { it.first == label }?.second?.toInt() ?: return@DropdownRow
        onSchemaCommit(schema.withElement(element.copy(color = argb)))
    }
    DropdownRow(
        label = "Moldura",
        selected = element.annotationType.label(),
        options = AnnotationType.entries.map { it.label() },
        key = "MOLDURA",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { label ->
        val t = AnnotationType.entries.firstOrNull { it.label() == label } ?: AnnotationType.PLAIN
        onSchemaCommit(schema.withElement(element.copy(annotationType = t)))
    }
    DropdownRow(
        label = "Alin. Texto",
        selected = element.alignment.label(),
        options = TextAlignment.entries.map { it.label() },
        key = "ALIN_TEXTO",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { label ->
        val a = TextAlignment.entries.firstOrNull { it.label() == label } ?: TextAlignment.LEFT
        onSchemaCommit(schema.withElement(element.copy(alignment = a)))
    }
}

// ── Cardinality content ───────────────────────────────────────────────────────

@Composable
private fun CardinalityContent(
    conn: games.polyclub.kbrmodelo.domain.Connection,
    schema: ConceptualSchema,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Cardinalidade")

    EditableRow("Papel", conn.cardinalityRole, "PAPEL", focusedKey, onFocusChange) { v ->
        onSchemaCommit(schema.copy(connections = schema.connections.map {
            if (it.id == conn.id) it.copy(cardinalityRole = v) else it
        }))
    }

    DropdownRow(
        label = "Fixar posição",
        selected = if (conn.cardinalityFixed) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "CARD_FIXA",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        onSchemaCommit(schema.copy(connections = schema.connections.map {
            if (it.id == conn.id) it.copy(cardinalityFixed = v == "Sim") else it
        }))
    }

    DropdownRow(
        label = "Posição da Linha",
        selected = conn.orientation.label(),
        options = listOf(LineOrientation.VERTICAL.label(), LineOrientation.HORIZONTAL.label()),
        key = "CARD_POS_LINHA",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        val ori = LineOrientation.entries.firstOrNull { it.label() == v } ?: LineOrientation.HORIZONTAL
        onSchemaCommit(schema.copy(connections = schema.connections.map {
            if (it.id == conn.id) it.copy(orientation = ori) else it
        }))
    }

    DropdownRow(
        label = "Tamanho aut.",
        selected = if (conn.cardinalityAutoSize) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "CARD_TAM_AUT",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        onSchemaCommit(schema.copy(connections = schema.connections.map {
            if (it.id == conn.id) it.copy(cardinalityAutoSize = v == "Sim") else it
        }))
    }

    DropdownRow(
        label = "Entidade fraca",
        selected = if (conn.isWeak) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "ENT_FRACA",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        onSchemaCommit(schema.copy(connections = schema.connections.map {
            if (it.id == conn.id) it.copy(isWeak = v == "Sim") else it
        }))
    }

    DropdownRow(
        label = "Cardinalidade",
        selected = conn.cardinality?.label ?: "-",
        options = Cardinality.entries.map { it.label },
        key = "CARDINALIDADE",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        val card = Cardinality.entries.firstOrNull { it.label == v }
        onSchemaCommit(schema.copy(connections = schema.connections.map {
            if (it.id == conn.id) it.copy(cardinality = card) else it
        }))
    }
}

// ── Grid primitives ───────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SECTION_HEADER_BG)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C2D3E))
    }
}

/** A read-only grid row that highlights when focused (clicked). */
@Composable
private fun ReadOnlyRow(
    label: String,
    value: String,
    key: String,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
) {
    val focused = focusedKey == key
    PropertyRow(label = label, focused = focused, onClick = { onFocusChange(key) }) {
        Text(
            text = value,
            fontSize = VALUE_TEXT_SIZE,
            color = if (focused) Color(0xFF80A0C0) else VALUE_COLOR,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

/** An editable grid row backed by a [BasicTextField]. */
@Composable
private fun EditableRow(
    label: String,
    value: String,
    key: String,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    enabled: Boolean = true,
    onCommit: (String) -> Unit,
) {
    val focused = focusedKey == key
    // Local draft so typing doesn't trigger schema mutations on every keystroke.
    var draft by remember(value) { mutableStateOf(value) }

    PropertyRow(label = label, focused = focused, onClick = { if (enabled) onFocusChange(key) }) {
        if (focused && enabled) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = TextStyle(fontSize = VALUE_TEXT_SIZE, color = VALUE_COLOR),
                cursorBrush = SolidColor(VALUE_COLOR),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .onFocusChanged { fs ->
                        if (!fs.isFocused && draft != value) {
                            onCommit(draft)
                        }
                    },
            )
        } else {
            Text(
                text = if (enabled) value else "",
                fontSize = VALUE_TEXT_SIZE,
                color = if (enabled) VALUE_COLOR else Color(0xFF9AA0A8),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

/** A dropdown grid row. */
@Composable
private fun DropdownRow(
    label: String,
    selected: String,
    options: List<String>,
    key: String,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSelect: (String) -> Unit,
) {
    val focused = focusedKey == key
    var expanded by remember { mutableStateOf(false) }

    PropertyRow(label = label, focused = focused, onClick = {
        onFocusChange(key)
        expanded = true
    }) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text(
                    text = selected,
                    fontSize = VALUE_TEXT_SIZE,
                    color = VALUE_COLOR,
                    modifier = Modifier.weight(1f),
                )
                Text("▾", fontSize = 8.sp, color = Color(0xFF606070))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 11.sp) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
    }
}

/** Base two-column row container used by all property row variants. */
@Composable
private fun PropertyRow(
    label: String,
    focused: Boolean,
    onClick: () -> Unit,
    valueContent: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = CELL_BORDER)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.Top,
    ) {
        // Label cell
        Box(
            modifier = Modifier
                .width(CELL_LABEL_WIDTH)
                .wrapContentHeight()
                .background(if (focused) CELL_LABEL_FOCUSED else CELL_LABEL_BG)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(
                text = label,
                fontSize = ROW_TEXT_SIZE,
                color = if (focused) LABEL_FOCUSED_COLOR else LABEL_COLOR,
                lineHeight = 13.sp,
            )
        }
        // Value cell
        Box(
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight()
                .background(CELL_VALUE_BG),
        ) {
            valueContent()
        }
    }
}

// ── Atr. ocultos tab ──────────────────────────────────────────────────────────

@Composable
private fun HiddenAttributesTab(
    schema: ConceptualSchema?,
    selection: CanvasSelection,
    modifier: Modifier = Modifier,
) {
    val hiddenAttrs: List<HiddenAttribute> = when (selection) {
        is CanvasSelection.Element -> schema?.elements?.get(selection.id)?.hiddenAttributes ?: emptyList()
        else -> emptyList()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HEADER_BG)
                .padding(horizontal = 4.dp, vertical = 3.dp),
        ) {
            listOf("Novo", "Editar", "Excluir").forEach { label ->
                ActionButton(label, onClick = { /* TODO */ })
                Spacer(Modifier.width(2.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HEADER_BG)
                .padding(horizontal = 4.dp, vertical = 3.dp),
        ) {
            ActionButton("Exibir no modelo", onClick = { /* TODO */ })
        }
        HorizontalDivider(color = CELL_BORDER, thickness = 1.dp)

        // Attribute tree
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            if (hiddenAttrs.isEmpty()) {
                Text(
                    text = if (schema == null || selection == CanvasSelection.None)
                        "(nenhum objeto selecionado)" else "(nenhum atributo oculto)",
                    fontSize = 10.sp,
                    color = Color(0xFF7A8A9A),
                )
            } else {
                TreeNode(text = "Atributos:", depth = 0, bold = true)
                hiddenAttrs.forEach { attr ->
                    HiddenAttributeNode(attr, depth = 1)
                }
            }
        }
    }
}

@Composable
private fun HiddenAttributeNode(attr: HiddenAttribute, depth: Int) {
    TreeNode(text = attr.name, depth = depth)
    TreeNode(text = "Propriedades", depth = depth + 1)
    if (attr.isMultiValued) {
        TreeNode(text = "Cardinalidade: ${attr.cardinality.toLabel()}", depth = depth + 2)
    }
    TreeNode(
        text = "Identificador: ${if (attr.isIdentifier) "Sim" else "Não"}",
        depth = depth + 2,
    )
    TreeNode(text = "Tipo: ${attr.type.ifBlank { "-" }}", depth = depth + 2)
    if (attr.isComposite) {
        TreeNode(text = "Atributos", depth = depth + 1)
        attr.children.forEach { child -> HiddenAttributeNode(child, depth + 2) }
    }
}

@Composable
private fun TreeNode(text: String, depth: Int, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("◆ ", fontSize = 8.sp, color = Color(0xFF5080B0))
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = Color(0xFF1A2535),
        )
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFFDCE6F0))
            .border(1.dp, Color(0xFF8090A8))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, fontSize = 10.sp, color = Color(0xFF1A2535))
    }
}

// ── Domain helpers ────────────────────────────────────────────────────────────

private fun SchemaElement.withName(n: String): SchemaElement = when (this) {
    is SchemaElement.Entity           -> copy(name = n)
    is SchemaElement.Relationship     -> copy(name = n)
    is SchemaElement.AssociativeEntity -> copy(name = n)
    is SchemaElement.Attribute        -> copy(name = n)
    is SchemaElement.Specialization   -> copy(name = n)
    is SchemaElement.SelfRelationship -> copy(name = n)
    is SchemaElement.Annotation       -> copy(name = n)
}

private fun SchemaElement.withObservations(o: String): SchemaElement = when (this) {
    is SchemaElement.Entity           -> copy(observations = o)
    is SchemaElement.Relationship     -> copy(observations = o)
    is SchemaElement.AssociativeEntity -> copy(observations = o)
    is SchemaElement.Attribute        -> copy(observations = o)
    is SchemaElement.Specialization   -> copy(observations = o)
    is SchemaElement.SelfRelationship -> copy(observations = o)
    is SchemaElement.Annotation       -> copy(observations = o)
}

private fun ArrowDirection.label(): String = when (this) {
    ArrowDirection.NONE         -> "Não mostrar"
    ArrowDirection.LEFT_UP      -> "A) /\\"
    ArrowDirection.LEFT_DOWN    -> "A) \\"
    ArrowDirection.TOP_RIGHT    -> "B) /\\"
    ArrowDirection.TOP_LEFT     -> "B) \\/"
    ArrowDirection.RIGHT_DOWN   -> "B) <"
    ArrowDirection.RIGHT_UP     -> "B) >"
    ArrowDirection.BOTTOM_LEFT  -> "A) \\/"
    ArrowDirection.BOTTOM_RIGHT -> "A) >"
}

// Associative entity direction has a reduced subset (no A)>/< as in original Pascal)
private fun ArrowDirection.assocLabel(): String = when (this) {
    ArrowDirection.NONE         -> "Não mostrar"
    ArrowDirection.LEFT_UP      -> "A) /\\"
    ArrowDirection.LEFT_DOWN    -> "A) \\"
    ArrowDirection.TOP_RIGHT    -> "B) /\\"
    ArrowDirection.TOP_LEFT     -> "B) \\/"
    ArrowDirection.BOTTOM_LEFT  -> "B) <"
    ArrowDirection.BOTTOM_RIGHT -> "B) >"
    else                        -> "Não mostrar"
}

private fun assocDirectionOptions(): List<String> = listOf(
    "Não mostrar", "A) /\\", "A) \\", "B) /\\", "B) \\/", "B) <", "B) >",
)

private fun assocLabelToDirection(label: String): ArrowDirection = when (label) {
    "A) /\\"  -> ArrowDirection.LEFT_UP
    "A) \\"   -> ArrowDirection.LEFT_DOWN
    "B) /\\"  -> ArrowDirection.TOP_RIGHT
    "B) \\/"  -> ArrowDirection.TOP_LEFT
    "B) <"    -> ArrowDirection.BOTTOM_LEFT
    "B) >"    -> ArrowDirection.BOTTOM_RIGHT
    else      -> ArrowDirection.NONE
}

private fun AnnotationType.label(): String = when (this) {
    AnnotationType.PLAIN -> "Vazio"
    AnnotationType.HINT  -> "Hint"
    AnnotationType.BOX   -> "Caixa"
}

private fun TextAlignment.label(): String = when (this) {
    TextAlignment.LEFT   -> "Esquerda"
    TextAlignment.CENTER -> "Centro"
    TextAlignment.RIGHT  -> "Direita"
}

private fun LineOrientation.label(): String = when (this) {
    LineOrientation.VERTICAL   -> "H. Vert."
    LineOrientation.HORIZONTAL -> "H. Horz."
    else                       -> "H. Horz."
}
