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

package games.polyclub.power.brmodelo.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import games.polyclub.power.brmodelo.domain.BulkDeleteCategoryCounts
import games.polyclub.power.brmodelo.domain.AnnotationType
import games.polyclub.power.brmodelo.domain.ArrowDirection
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.Cardinality
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.HiddenAttribute
import games.polyclub.power.brmodelo.domain.LineOrientation
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.domain.SpecializationType
import games.polyclub.power.brmodelo.domain.TextAlignment
import games.polyclub.power.brmodelo.ui.canvas.autoSizedAttributePosition
import games.polyclub.power.brmodelo.ui.canvas.connectionCardinalityBoxForModel
import games.polyclub.power.brmodelo.ui.canvas.materializeCardinalityPositionForFixed
import games.polyclub.power.brmodelo.ui.canvas.withPosition
import games.polyclub.power.brmodelo.ui.components.CHROMIUM_TAB_ACTIVE_HEIGHT
import games.polyclub.power.brmodelo.ui.components.CHROMIUM_TAB_INACTIVE_HEIGHT
import games.polyclub.power.brmodelo.ui.components.CHROMIUM_TAB_STRIP_HEIGHT
import games.polyclub.power.brmodelo.ui.components.ChromiumTab
import games.polyclub.power.brmodelo.ui.components.ChromiumTabShape
import games.polyclub.power.brmodelo.ui.label
import games.polyclub.power.brmodelo.ui.withName
import games.polyclub.power.brmodelo.ui.withObservations

// ── Colour palette ────────────────────────────────────────────────────────────

private val INSPECTOR_BG       = Color(0xFFF0F2F5)
private val INSPECTOR_BORDER   = Color(0xFF8090A0)
private val HEADER_BG          = Color(0xFFD8D8D8)  // neutral gray strip background
private val TAB_ACTIVE_BG      = Color(0xFFFFFFFF)
private val TAB_INACTIVE_BG    = Color(0xFFBBBBBB)  // neutral gray for inactive tab
private val TAB_STRIP_BORDER   = Color(0xFF8C8C8C)  // neutral gray border
private val SECTION_HEADER_BG  = Color(0xFFD2D5D8)  // neutral gray section header
private val CELL_LABEL_BG      = Color(0xFFDFE2E6)  // neutral gray label cell
private val CELL_LABEL_FOCUSED = Color(0xFF4A5868)  // muted slate for focused label bg
private val CELL_VALUE_BG      = Color(0xFFFFFFFF)
/** Value cell background for read-only / locked fields (slightly darker than [CELL_VALUE_BG]). */
private val CELL_VALUE_READ_ONLY_BG = Color(0xFFECEEF1)
private val CELL_BORDER        = Color(0xFFB8BCC0)
private val LABEL_COLOR        = Color(0xFF2A3A4A)
private val LABEL_FOCUSED_COLOR = Color(0xFFFFFFFF)
private val VALUE_COLOR        = Color(0xFF1A2535)
private val HINT_BG            = Color(0xFFDCDFE2)  // neutral gray hint area
private val HINT_TEXT_COLOR    = Color(0xFF2A3040)
private val BULK_DELETE_INSPECTOR_WARN = Color(0xFFB00020)

private val CELL_LABEL_WIDTH = 96.dp
private val ROW_TEXT_SIZE    = 10.sp
private val VALUE_TEXT_SIZE  = 10.sp

/** Same font size as line height removes Compose Text's default extra vertical padding (critical for compact rows). */
private fun inspectorValueTextStyle(color: Color): TextStyle = TextStyle(
    fontSize = VALUE_TEXT_SIZE,
    lineHeight = VALUE_TEXT_SIZE,
    color = color,
)

private val INSPECTOR_DROPDOWN_CARET_STYLE = TextStyle(
    fontSize = 8.sp,
    lineHeight = 8.sp,
    color = Color(0xFF606070),
)

private enum class InspectorTab { Selecao, AtrOcultos }

/** Escapes transient canvas previews back to the last committed schema ([SchemaHistory.current]). */
private val LocalRevertSchemaPreview = staticCompositionLocalOf<() -> Unit> { { } }

// ChromiumTabShape is defined in components/ChromiumTabs.kt and imported via the same package.

// ── Hint strings (sourced from ajuda.pas AutoHelp) ────────────────────────────

private val HINTS: Map<String, String> = mapOf(
    "NOME"           to "Descrição/identificação do objeto.",
    "NOME_MODELO"    to "Nome do modelo conceitual.",
    "OBS"            to "Algo importante a ser anotado para posterior observação.",
    "ALINHAMENTOLT_X" to "Reposiciona o controle quanto a posição no modelo (esquerda ou direita).",
    "ALINHAMENTOLT_Y" to "Reposiciona o controle quanto a posição vertical no modelo.",
    "ALINHAMENTOWH_W" to "Reposiciona o controle quanto à largura.",
    "ALINHAMENTOWH_H" to "Reposiciona o controle quanto à altura.",
    "ATRIB_WH_W" to "Largura do atributo (somente leitura com tamanho automático).",
    "ATRIB_WH_H" to "Altura do atributo (somente leitura com tamanho automático).",
    "AUTO_REL"       to "A entidade está auto relacionada.",
    "ESPECIALIZADA"  to "Indica se a entidade é generalização com pelo menos um triângulo de especialização (Pascal Especializacoes / XML ehEsp).",
    "EA_NOME"        to "Nome do relacionamento contido na entidade associativa.",
    "EA_DIC"         to "Dicionário de dados do relacionamento contido na entidade associativa.",
    "EA_OBS"         to "Algo importante a ser anotado sobre o relacionamento contido na entidade associativa.",
    "CARD_FIXA"      to "Fixar posição: Se fixada, a cardinalidade não se moverá ao mover a entidade ou relacionamento ao qual esteja vinculada.",
    "CARD_POS_LINHA" to "Alinhamento da fixação da cardinalidade.",
    "CARD_TAM_AUT"   to "Controle do tamanho do desenho da cardinalidade.",
    "ENT_FRACA"      to "Tipo de entidade (fraca ou normal).",
    "CARDINALIDADE"  to "Cardinalidade do relacionamento entre as entidades.",
    "PAPEL"          to "Descrição do papel da cardinalidade (descrição/observação).",
    "CARD_OBS"       to "Algo importante a ser anotado para posterior observação.",
    "CARD_LT_X"      to "Posição horizontal da caixa da cardinalidade no modelo. Editável só quando \"Fixar posição\" for Sim.",
    "CARD_LT_Y"      to "Posição vertical da caixa da cardinalidade no modelo. Editável só quando \"Fixar posição\" for Sim.",
    "CARD_WH_W"      to "Largura da caixa da cardinalidade (somente leitura com tamanho automático).",
    "CARD_WH_H"      to "Altura da caixa da cardinalidade (somente leitura com tamanho automático).",
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
    inspectorCommittedSchema: ConceptualSchema? = null,
    selection: CanvasSelection = CanvasSelection.None,
    conceptualCanvasTool: ConceptualCanvasTool = ConceptualCanvasTool.None,
    bulkDeleteUiState: BulkDeleteUiState? = null,
    onSchemaPreview: (ConceptualSchema) -> Unit = {},
    onSchemaCommit: (ConceptualSchema) -> Unit = {},
    onRevertSchemaPreview: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf(InspectorTab.Selecao) }
    // Field key currently focused in the grid — drives the hint text at the bottom.
    var focusedKey by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalRevertSchemaPreview provides onRevertSchemaPreview) {
        Column(
            modifier = modifier
                .width(210.dp)
                .fillMaxHeight()
                .background(INSPECTOR_BG)
        ) {
        // ── Tab header ────────────────────────────────────────────────────────
            InspectorTabStrip(
                activeTab = activeTab,
                onTabChange = { activeTab = it },
            )

        // ── Tab content ───────────────────────────────────────────────────────
        when (activeTab) {
            InspectorTab.Selecao -> SelectionTab(
                schema = schema,
                committedSchema = inspectorCommittedSchema,
                selection = selection,
                conceptualCanvasTool = conceptualCanvasTool,
                bulkDeleteUiState = bulkDeleteUiState,
                focusedKey = focusedKey,
                onFocusChange = { focusedKey = it },
                onSchemaPreview = onSchemaPreview,
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
}

// ── Chromium-style tab strip ──────────────────────────────────────────────────

@Composable
private fun InspectorTabStrip(
    activeTab: InspectorTab,
    onTabChange: (InspectorTab) -> Unit,
) {
    val density = LocalDensity.current
    val topCornerPx   = with(density) { 5.dp.toPx() }
    val bottomCurvePx = with(density) { 4.dp.toPx() }
    val tabShape = remember(topCornerPx, bottomCurvePx) {
        ChromiumTabShape(
            topCornerRadius = topCornerPx,
            bottomCurveRadius = bottomCurvePx
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHROMIUM_TAB_STRIP_HEIGHT)
            .background(HEADER_BG),
    ) {
        HorizontalDivider(
            color = TAB_STRIP_BORDER,
            thickness = 1.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1.5f),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                // Horizontal insets give the tab curves room so they don't overflow outside the strip.
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            ChromiumTab(
                label = "Seleção",
                selected = activeTab == InspectorTab.Selecao,
                tabShape = tabShape,
                activeTabBg = TAB_ACTIVE_BG,
                inactiveTabBg = TAB_INACTIVE_BG,
                borderColor = TAB_STRIP_BORDER,
                modifier = Modifier
                    .weight(1f)
                    .height(if (activeTab == InspectorTab.Selecao) CHROMIUM_TAB_ACTIVE_HEIGHT else CHROMIUM_TAB_INACTIVE_HEIGHT)
                    .zIndex(if (activeTab == InspectorTab.Selecao) 2f else 1f),
                onClick = { onTabChange(InspectorTab.Selecao) },
            )
            ChromiumTab(
                label = "Atr. ocultos",
                selected = activeTab == InspectorTab.AtrOcultos,
                tabShape = tabShape,
                activeTabBg = TAB_ACTIVE_BG,
                inactiveTabBg = TAB_INACTIVE_BG,
                borderColor = TAB_STRIP_BORDER,
                modifier = Modifier
                    .width(84.dp)
                    .height(if (activeTab == InspectorTab.AtrOcultos) CHROMIUM_TAB_ACTIVE_HEIGHT else CHROMIUM_TAB_INACTIVE_HEIGHT)
                    .zIndex(if (activeTab == InspectorTab.AtrOcultos) 2f else 1f),
                onClick = { onTabChange(InspectorTab.AtrOcultos) },
            )
        }
    }
}

// ChromiumTab is defined in components/ChromiumTabs.kt.

@Composable
private fun BulkDeleteToolInspectorSection(ui: BulkDeleteUiState?) {
    SectionTitle("Ferramenta: excluir objetos")
    Text(
        text = "Esta ferramenta exclui objetos do modelo. Ao soltar o botão esquerdo, todos os elementos " +
            "que estiverem dentro ou parcialmente dentro do retângulo serão removidos de uma só vez " +
            "(um único passo no histórico para desfazer). Arraste na área do diagrama para desenhar o retângulo. " +
            "Com o botão esquerdo, o diagrama não é arrastado — use o botão do meio do mouse para mover a vista.",
        fontSize = 9.sp,
        color = BULK_DELETE_INSPECTOR_WARN,
        lineHeight = 12.sp,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
    )
    val c = ui?.counts
    if (c != null && c.total > 0) {
        if (c.entities > 0) {
            Text("Entidades: ${c.entities}", fontSize = 9.sp, color = VALUE_COLOR, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
        }
        if (c.relationships > 0) {
            Text("Relações: ${c.relationships}", fontSize = 9.sp, color = VALUE_COLOR, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
        }
        if (c.associativeEntities > 0) {
            Text(
                "Entidades associativas: ${c.associativeEntities}",
                fontSize = 9.sp,
                color = VALUE_COLOR,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        if (c.specializations > 0) {
            Text(
                "Especializações: ${c.specializations}",
                fontSize = 9.sp,
                color = VALUE_COLOR,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        if (c.attributes > 0) {
            Text("Atributos: ${c.attributes}", fontSize = 9.sp, color = VALUE_COLOR, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
        }
        if (c.hiddenAttributesLeaves > 0) {
            Text(
                "Atributos ocultos (total na árvore): ${c.hiddenAttributesLeaves}",
                fontSize = 9.sp,
                color = VALUE_COLOR,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        if (c.observations > 0) {
            Text("Observações: ${c.observations}", fontSize = 9.sp, color = VALUE_COLOR, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
        }
        Text(
            "Total: ${c.total}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = VALUE_COLOR,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

// ── Selection tab ─────────────────────────────────────────────────────────────

@Composable
private fun SelectionTab(
    schema: ConceptualSchema?,
    committedSchema: ConceptualSchema?,
    selection: CanvasSelection,
    conceptualCanvasTool: ConceptualCanvasTool,
    bulkDeleteUiState: BulkDeleteUiState?,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaPreview: (ConceptualSchema) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val layoutDirection = LocalLayoutDirection.current
    val hintText = focusedKey?.let { HINTS[it] } ?: ""

    Column(modifier = modifier) {
        // Scrollable properties grid
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (conceptualCanvasTool is ConceptualCanvasTool.BulkDeleteObjects) {
                BulkDeleteToolInspectorSection(bulkDeleteUiState)
                HorizontalDivider(color = Color(0xFF7A9ABF), thickness = 1.dp)
            }
            when {
                schema == null -> Unit

                selection == CanvasSelection.None ->
                    SchemaMetaContent(
                        schema = schema,
                        committedSchema = committedSchema,
                        focusedKey = focusedKey,
                        onFocusChange = onFocusChange,
                        onSchemaPreview = onSchemaPreview,
                        onSchemaCommit = onSchemaCommit,
                    )

                selection is CanvasSelection.Element -> {
                    val elem = schema.elements[selection.id]
                    if (elem != null) {
                        ElementContent(
                            element = elem,
                            schema = schema,
                            committedSchema = committedSchema,
                            focusedKey = focusedKey,
                            onFocusChange = onFocusChange,
                            onSchemaPreview = onSchemaPreview,
                            onSchemaCommit = onSchemaCommit,
                            textMeasurer = textMeasurer,
                            layoutDirection = layoutDirection,
                        )
                    }
                }

                selection is CanvasSelection.Cardinality -> {
                    val conn = schema.connections.firstOrNull { it.id == selection.connectionId }
                    if (conn != null) {
                        CardinalityContent(
                            conn = conn,
                            schema = schema,
                            committedSchema = committedSchema,
                            focusedKey = focusedKey,
                            onFocusChange = onFocusChange,
                            onSchemaPreview = onSchemaPreview,
                            onSchemaCommit = onSchemaCommit,
                        )
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
    committedSchema: ConceptualSchema?,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaPreview: (ConceptualSchema) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Informações: Modelo Conceitual")
    val modelNameCommitted = committedSchema?.name ?: schema.name
    EditableRow(
        label = "Nome",
        value = modelNameCommitted,
        key = "NOME_MODELO",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
        onLiveDraftChange = { onSchemaPreview(schema.copy(name = it)) },
    ) {
        onSchemaCommit(schema.copy(name = it))
    }
    ReadOnlyRow(
        "Versão",
        schema.version,
        "VERSAO",
        focusedKey,
        onFocusChange
    )
    EditableRow(
        "Autor(es)",
        schema.author,
        "AUTOR",
        focusedKey,
        onFocusChange
    ) {
        onSchemaCommit(schema.copy(author = it))
    }
    EditableRow(
        "Observações",
        schema.observations,
        "OBS",
        focusedKey,
        onFocusChange
    ) {
        onSchemaCommit(schema.copy(observations = it))
    }
}

// ── Attribute auto-size (inspector + canvas) ─────────────────────────────────

private fun SchemaElement.Attribute.resizedIfAuto(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    layoutDirection: LayoutDirection,
): SchemaElement.Attribute {
    if (!autoSize) return this
    return copy(position = autoSizedAttributePosition(this, schema.withElement(this), textMeasurer, layoutDirection))
}

private fun commitAttributeElement(
    schema: ConceptualSchema,
    attr: SchemaElement.Attribute,
    textMeasurer: TextMeasurer,
    layoutDirection: LayoutDirection,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    onSchemaCommit(schema.withElement(attr.resizedIfAuto(schema, textMeasurer, layoutDirection)))
}

private fun previewAttributeElement(
    schema: ConceptualSchema,
    attr: SchemaElement.Attribute,
    textMeasurer: TextMeasurer,
    layoutDirection: LayoutDirection,
    onSchemaPreview: (ConceptualSchema) -> Unit,
) {
    onSchemaPreview(schema.withElement(attr.resizedIfAuto(schema, textMeasurer, layoutDirection)))
}

// ── Element content dispatcher ────────────────────────────────────────────────

@Composable
private fun ElementContent(
    element: SchemaElement,
    schema: ConceptualSchema,
    committedSchema: ConceptualSchema?,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaPreview: (ConceptualSchema) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
    textMeasurer: TextMeasurer,
    layoutDirection: LayoutDirection,
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
    val nameCommitted = committedSchema?.elements?.get(element.id)?.name ?: element.name
    EditableRow(
        label = "Nome",
        value = nameCommitted,
        key = "NOME",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
        onLiveDraftChange = { draft ->
            val el = element.withName(draft)
            if (el is SchemaElement.Attribute && el.autoSize) {
                previewAttributeElement(schema, el, textMeasurer, layoutDirection, onSchemaPreview)
            } else {
                onSchemaPreview(schema.withElement(el))
            }
        },
    ) { newName ->
        val el = element.withName(newName)
        if (el is SchemaElement.Attribute && el.autoSize) {
            commitAttributeElement(schema, el, textMeasurer, layoutDirection, onSchemaCommit)
        } else {
            onSchemaCommit(schema.withElement(el))
        }
    }
    EditableRow(
        "Observação",
        element.observations,
        "OBS",
        focusedKey,
        onFocusChange
    ) { v ->
        onSchemaCommit(schema.withElement(element.withObservations(v)))
    }

    SectionTitle("Posição e Tamanho")
    val p = element.position
    val attrAutoSize = element is SchemaElement.Attribute && element.autoSize
    EditableRow(
        "Esquerda (Left)",
        p.x.toString(),
        "ALINHAMENTOLT_X",
        focusedKey,
        onFocusChange
    ) { v ->
        v.toIntOrNull()?.let {
            onSchemaCommit(schema.withElement(element.withPosition(p.copy(x = it))))
        }
    }
    EditableRow(
        "Acima (Top)",
        p.y.toString(),
        "ALINHAMENTOLT_Y",
        focusedKey,
        onFocusChange
    ) { v ->
        v.toIntOrNull()?.let {
            onSchemaCommit(schema.withElement(element.withPosition(p.copy(y = it))))
        }
    }
    if (attrAutoSize) {
        ReadOnlyRow(
            label = "Largura (Width)",
            value = p.width.toString(),
            key = "ATRIB_WH_W",
            focusedKey = focusedKey,
            onFocusChange = onFocusChange,
        )
        ReadOnlyRow(
            label = "Altura (Height)",
            value = p.height.toString(),
            key = "ATRIB_WH_H",
            focusedKey = focusedKey,
            onFocusChange = onFocusChange,
        )
    } else {
        EditableRow(
            "Largura (Width)",
            p.width.toString(),
            "ALINHAMENTOWH_W",
            focusedKey,
            onFocusChange
        ) { v ->
            v.toIntOrNull()?.let {
                onSchemaCommit(schema.withElement(element.withPosition(p.copy(width = it))))
            }
        }
        EditableRow(
            "Altura (Height)",
            p.height.toString(),
            "ALINHAMENTOWH_H",
            focusedKey,
            onFocusChange
        ) { v ->
            v.toIntOrNull()?.let {
                onSchemaCommit(schema.withElement(element.withPosition(p.copy(height = it))))
            }
        }
    }

    // Type-specific fields
    when (element) {
        is SchemaElement.Entity           -> EntityFields(
            element,
            schema,
            focusedKey,
            onFocusChange,
            onSchemaCommit
        )
        is SchemaElement.Relationship     -> RelationshipFields(
            element,
            schema,
            focusedKey,
            onFocusChange,
            onSchemaCommit
        )
        is SchemaElement.AssociativeEntity -> AssocEntityFields(
            element = element,
            schema = schema,
            committedSchema = committedSchema,
            focusedKey = focusedKey,
            onFocusChange = onFocusChange,
            onSchemaPreview = onSchemaPreview,
            onSchemaCommit = onSchemaCommit,
        )
        is SchemaElement.Attribute -> AttributeFields(
            element = element,
            schema = schema,
            committedSchema = committedSchema,
            focusedKey = focusedKey,
            onFocusChange = onFocusChange,
            onSchemaPreview = onSchemaPreview,
            onSchemaCommit = onSchemaCommit,
            textMeasurer = textMeasurer,
            layoutDirection = layoutDirection,
        )
        is SchemaElement.Specialization   -> SpecializationFields(
            element,
            schema,
            focusedKey,
            onFocusChange,
            onSchemaCommit
        )
        is SchemaElement.SelfRelationship -> SelfRelFields(
            element,
            schema,
            focusedKey,
            onFocusChange,
            onSchemaCommit
        )
        is SchemaElement.Annotation       -> AnnotationFields(
            element,
            schema,
            focusedKey,
            onFocusChange,
            onSchemaCommit
        )
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
    val autoRel = schema.selfRelationships.any { it.ownerEntityId == element.id }
    val isSpecializationGeneralizationBase =
        schema.specializations.any { it.baseEntityId == element.id }
    ReadOnlyRow(
        "Auto relacionado",
        if (autoRel) "Sim" else "Não",
        "AUTO_REL",
        focusedKey,
        onFocusChange
    )
    ReadOnlyRow(
        "Especializada",
        if (isSpecializationGeneralizationBase) "Sim" else "Não",
        "ESPECIALIZADA", focusedKey, onFocusChange
    )
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
        val dir =
            ArrowDirection.entries.firstOrNull { it.label() == label }
                ?: ArrowDirection.NONE
        onSchemaCommit(schema.withElement(element.copy(arrowDirection = dir)))
    }
}

// ── Associative entity ────────────────────────────────────────────────────────

@Composable
private fun AssocEntityFields(
    element: SchemaElement.AssociativeEntity,
    schema: ConceptualSchema,
    committedSchema: ConceptualSchema?,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaPreview: (ConceptualSchema) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    SectionTitle("Esquema")
    val autoRel = schema.selfRelationships.any { it.ownerEntityId == element.id }
    ReadOnlyRow(
        "Auto relacionado",
        if (autoRel) "Sim" else "Não",
        "AUTO_REL",
        focusedKey,
        onFocusChange
    )

    SectionTitle("Relacionamento")
    val relationshipNameCommitted =
        (committedSchema?.elements?.get(element.id) as? SchemaElement.AssociativeEntity)?.relationshipName
            ?: element.relationshipName
    EditableRow(
        label = "+Nome",
        value = relationshipNameCommitted,
        key = "EA_NOME",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
        onLiveDraftChange = { draft ->
            onSchemaPreview(schema.withElement(element.copy(relationshipName = draft)))
        },
    ) { v ->
        onSchemaCommit(schema.withElement(element.copy(relationshipName = v)))
    }
    EditableRow(
        "+Dicionário",
        element.relationshipDictionary,
        "EA_DIC",
        focusedKey,
        onFocusChange
    ) { v ->
        onSchemaCommit(schema.withElement(element.copy(relationshipDictionary = v)))
    }
    EditableRow(
        "+Observação",
        element.relationshipObservations,
        "EA_OBS",
        focusedKey,
        onFocusChange
    ) { v ->
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
    committedSchema: ConceptualSchema?,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaPreview: (ConceptualSchema) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
    textMeasurer: TextMeasurer,
    layoutDirection: LayoutDirection,
) {
    SectionTitle("Atributo")
    DropdownRow(
        label = "Tamanho aut.",
        selected = if (element.autoSize) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "ATRIB_TAM_AUT",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        val sim = v == "Sim"
        val next = element.copy(autoSize = sim)
        if (sim) {
            commitAttributeElement(schema, next, textMeasurer, layoutDirection, onSchemaCommit)
        } else {
            onSchemaCommit(schema.withElement(next))
        }
    }

    val ownerPos = schema.elements[element.ownerId]?.position
    val attrPos  = element.position
    val side = if (ownerPos != null) {
        val ownerCx = ownerPos.x + ownerPos.width / 2
        val attrCx  = attrPos.x  + attrPos.width  / 2
        if (attrCx >= ownerCx) "Direito" else "Esquerdo"
    } else "Esquerdo"
    ReadOnlyRow(
        "Lado",
        side,
        "ATRIB_LADO",
        focusedKey,
        onFocusChange
    )

    DropdownRow(
        label = "Identificador",
        selected = if (element.isIdentifier) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "IDENTIFICADOR",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        val next = element.copy(isIdentifier = v == "Sim")
        commitAttributeElement(schema, next, textMeasurer, layoutDirection, onSchemaCommit)
    }

    DropdownRow(
        label = "Opcional",
        selected = if (element.isOptional) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "OPCIONAL",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        val next = element.copy(isOptional = v == "Sim")
        commitAttributeElement(schema, next, textMeasurer, layoutDirection, onSchemaCommit)
    }

    ReadOnlyRow(
        "Composto",
        if (element.isComposite) "Sim" else "Não",
        "COMPOSTO",
        focusedKey,
        onFocusChange
    )

    DropdownRow(
        label = "Multivalorado",
        selected = if (element.isMultiValued) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "MULTIVALORADO",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        val next = element.copy(isMultiValued = v == "Sim")
        commitAttributeElement(schema, next, textMeasurer, layoutDirection, onSchemaCommit)
    }

    ReadOnlyRow(
        "Qtd. Campos",
        schema.canonicalQtdeMultivalorado(element).toString(),
        "QTD_CAMPOS",
        focusedKey,
        onFocusChange
    )

    val attrCommitted = committedSchema?.elements?.get(element.id) as? SchemaElement.Attribute ?: element
    val minCardinalityCommitted = attrCommitted.cardinality.minCardinality.toString()
    EditableRow(
        label = "Card. Mínima",
        value = minCardinalityCommitted,
        key = "CARD_MIN",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
        enabled = element.isMultiValued,
        onLiveDraftChange = { draft ->
            draft.toIntOrNull()?.let { min ->
                val next = element.copy(cardinality = element.cardinality.copy(minCardinality = min))
                previewAttributeElement(schema, next, textMeasurer, layoutDirection, onSchemaPreview)
            }
        },
    ) { v ->
        v.toIntOrNull()?.let {
            val next = element.copy(cardinality = element.cardinality.copy(minCardinality = it))
            commitAttributeElement(schema, next, textMeasurer, layoutDirection, onSchemaCommit)
        }
    }
    val maxLabelCommitted =
        if (attrCommitted.cardinality.isUnbounded) "n" else attrCommitted.cardinality.maxCardinality.toString()
    EditableRow(
        label = "Card. Máxima",
        value = maxLabelCommitted,
        key = "CARD_MAX",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
        enabled = element.isMultiValued,
        onLiveDraftChange = { draft ->
            val maxVal = parseAttributeMaxCardinalityDraft(draft)
            if (maxVal != null) {
                val next = element.copy(cardinality = element.cardinality.copy(maxCardinality = maxVal))
                previewAttributeElement(schema, next, textMeasurer, layoutDirection, onSchemaPreview)
            }
        },
    ) { v ->
        val intVal = parseAttributeMaxCardinalityDraft(v)
            ?: return@EditableRow
        val next = element.copy(cardinality = element.cardinality.copy(maxCardinality = intVal))
        commitAttributeElement(schema, next, textMeasurer, layoutDirection, onSchemaCommit)
    }

    EditableRow(
        "Tipo",
        element.valueType,
        "TIPO_VALOR",
        focusedKey,
        onFocusChange
    ) { v ->
        onSchemaCommit(schema.withElement(element.copy(valueType = v)))
    }
    EditableRow(
        "Tamanho",
        element.complement,
        "COMPLEMENTO",
        focusedKey,
        onFocusChange
    ) { v ->
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
        val dir =
            ArrowDirection.entries.firstOrNull { it.label() == label }
                ?: ArrowDirection.NONE
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
        selected = ANNOTATION_COLORS.firstOrNull { it.second.toLong() == element.color?.toLong() }?.first
            ?: "Branco",
        options = ANNOTATION_COLORS.map { it.first },
        key = "TIPO_VALOR",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { label ->
        val argb =
            ANNOTATION_COLORS.firstOrNull { it.first == label }?.second?.toInt()
                ?: return@DropdownRow
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
        val t =
            AnnotationType.entries.firstOrNull { it.label() == label }
                ?: AnnotationType.PLAIN
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
        val a =
            TextAlignment.entries.firstOrNull { it.label() == label }
                ?: TextAlignment.LEFT
        onSchemaCommit(schema.withElement(element.copy(alignment = a)))
    }
}

// ── Cardinality content ───────────────────────────────────────────────────────

@Composable
private fun CardinalityContent(
    conn: games.polyclub.power.brmodelo.domain.Connection,
    schema: ConceptualSchema,
    committedSchema: ConceptualSchema?,
    focusedKey: String?,
    onFocusChange: (String?) -> Unit,
    onSchemaPreview: (ConceptualSchema) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()

    fun updateConn(block: (games.polyclub.power.brmodelo.domain.Connection) -> games.polyclub.power.brmodelo.domain.Connection) {
        onSchemaCommit(
            schema.copy(connections = schema.connections.map { if (it.id == conn.id) block(it) else it }),
        )
    }

    val box = connectionCardinalityBoxForModel(schema, conn, textMeasurer)

    SectionTitle("Edição: Cardinalidade")

    val papelCommitted =
        committedSchema?.connections?.firstOrNull { it.id == conn.id }?.cardinalityRole ?: conn.cardinalityRole
    EditableRow(
        label = "Papel",
        value = papelCommitted,
        key = "PAPEL",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
        onLiveDraftChange = { draft ->
            onSchemaPreview(
                schema.copy(
                    connections = schema.connections.map {
                        if (it.id == conn.id) it.copy(cardinalityRole = draft) else it
                    },
                ),
            )
        },
    ) { v ->
        updateConn { it.copy(cardinalityRole = v) }
    }

    val obsCommitted =
        committedSchema?.connections?.firstOrNull { it.id == conn.id }?.cardinalityObservations
            ?: conn.cardinalityObservations
    EditableRow(
        label = "Observação",
        value = obsCommitted,
        key = "CARD_OBS",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
        onLiveDraftChange = { draft ->
            onSchemaPreview(
                schema.copy(
                    connections = schema.connections.map {
                        if (it.id == conn.id) it.copy(cardinalityObservations = draft) else it
                    },
                ),
            )
        },
    ) { v ->
        updateConn { it.copy(cardinalityObservations = v) }
    }

    SectionTitle("Posição e Tamanho")

    if (box != null) {
        fun ensureBoxForEdit(): ElementPosition =
            connectionCardinalityBoxForModel(schema, conn, textMeasurer)
                ?: materializeCardinalityPositionForFixed(schema, conn, textMeasurer)!!

        val positionFieldsEditable = conn.cardinalityFixed
        if (positionFieldsEditable) {
            EditableRow(
                label = "Esquerda",
                value = box.x.toString(),
                key = "CARD_LT_X",
                focusedKey = focusedKey,
                onFocusChange = onFocusChange,
                onLiveDraftChange = { draft ->
                    draft.toIntOrNull()?.let { x ->
                        val b = ensureBoxForEdit()
                        onSchemaPreview(
                            schema.copy(
                                connections = schema.connections.map {
                                    if (it.id == conn.id) {
                                        it.copy(cardinalityPosition = b.copy(x = x))
                                    } else {
                                        it
                                    }
                                },
                            ),
                        )
                    }
                },
            ) { v ->
                v.toIntOrNull()?.let { x ->
                    val b = ensureBoxForEdit()
                    updateConn { it.copy(cardinalityPosition = b.copy(x = x)) }
                }
            }

            EditableRow(
                label = "Acima",
                value = box.y.toString(),
                key = "CARD_LT_Y",
                focusedKey = focusedKey,
                onFocusChange = onFocusChange,
                onLiveDraftChange = { draft ->
                    draft.toIntOrNull()?.let { y ->
                        val b = ensureBoxForEdit()
                        onSchemaPreview(
                            schema.copy(
                                connections = schema.connections.map {
                                    if (it.id == conn.id) {
                                        it.copy(cardinalityPosition = b.copy(y = y))
                                    } else {
                                        it
                                    }
                                },
                            ),
                        )
                    }
                },
            ) { v ->
                v.toIntOrNull()?.let { y ->
                    val b = ensureBoxForEdit()
                    updateConn { it.copy(cardinalityPosition = b.copy(y = y)) }
                }
            }
        } else {
            ReadOnlyRow(
                label = "Esquerda",
                value = box.x.toString(),
                key = "CARD_LT_X",
                focusedKey = focusedKey,
                onFocusChange = onFocusChange,
            )
            ReadOnlyRow(
                label = "Acima",
                value = box.y.toString(),
                key = "CARD_LT_Y",
                focusedKey = focusedKey,
                onFocusChange = onFocusChange,
            )
        }

        if (conn.cardinalityAutoSize) {
            ReadOnlyRow(
                label = "Largura",
                value = box.width.toString(),
                key = "CARD_WH_W",
                focusedKey = focusedKey,
                onFocusChange = onFocusChange,
            )
            ReadOnlyRow(
                label = "Altura",
                value = box.height.toString(),
                key = "CARD_WH_H",
                focusedKey = focusedKey,
                onFocusChange = onFocusChange,
            )
        } else {
            EditableRow(
                label = "Largura",
                value = box.width.toString(),
                key = "CARD_WH_W",
                focusedKey = focusedKey,
                onFocusChange = onFocusChange,
                onLiveDraftChange = { draft ->
                    draft.toIntOrNull()?.let { w ->
                        val b = ensureBoxForEdit()
                        onSchemaPreview(
                            schema.copy(
                                connections = schema.connections.map {
                                    if (it.id == conn.id) {
                                        it.copy(
                                            cardinalityPosition = b.copy(
                                                width = w.coerceAtLeast(10),
                                            ),
                                        )
                                    } else {
                                        it
                                    }
                                },
                            ),
                        )
                    }
                },
            ) { v ->
                v.toIntOrNull()?.let { w ->
                    val b = ensureBoxForEdit()
                    updateConn {
                        it.copy(cardinalityPosition = b.copy(width = w.coerceAtLeast(10)))
                    }
                }
            }
            EditableRow(
                label = "Altura",
                value = box.height.toString(),
                key = "CARD_WH_H",
                focusedKey = focusedKey,
                onFocusChange = onFocusChange,
                onLiveDraftChange = { draft ->
                    draft.toIntOrNull()?.let { h ->
                        val b = ensureBoxForEdit()
                        onSchemaPreview(
                            schema.copy(
                                connections = schema.connections.map {
                                    if (it.id == conn.id) {
                                        it.copy(
                                            cardinalityPosition = b.copy(
                                                height = h.coerceAtLeast(10),
                                            ),
                                        )
                                    } else {
                                        it
                                    }
                                },
                            ),
                        )
                    }
                },
            ) { v ->
                v.toIntOrNull()?.let { h ->
                    val b = ensureBoxForEdit()
                    updateConn {
                        it.copy(cardinalityPosition = b.copy(height = h.coerceAtLeast(10)))
                    }
                }
            }
        }
    }

    DropdownRow(
        label = "Fixar posição",
        selected = if (conn.cardinalityFixed) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "CARD_FIXA",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        val wantFixed = v == "Sim"
        onSchemaCommit(
            schema.copy(
                connections = schema.connections.map {
                    if (it.id != conn.id) return@map it
                    when {
                        wantFixed && it.cardinalityPosition == null -> {
                            val pos = materializeCardinalityPositionForFixed(schema, it, textMeasurer)
                            if (pos != null) {
                                it.copy(cardinalityFixed = true, cardinalityPosition = pos)
                            } else {
                                it.copy(cardinalityFixed = true)
                            }
                        }
                        !wantFixed -> {
                            val unfixed = it.copy(cardinalityFixed = false)
                            val pos = materializeCardinalityPositionForFixed(schema, unfixed, textMeasurer)
                            unfixed.copy(cardinalityPosition = pos ?: unfixed.cardinalityPosition)
                        }
                        else -> it.copy(cardinalityFixed = true)
                    }
                },
            ),
        )
    }

    DropdownRow(
        label = "Posição da Linha",
        selected = conn.orientation.label(),
        options = LineOrientation.entries.map { it.label() },
        key = "CARD_POS_LINHA",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        val ori =
            LineOrientation.entries.firstOrNull { it.label() == v }
                ?: LineOrientation.HORIZONTAL
        updateConn { it.copy(orientation = ori) }
    }

    DropdownRow(
        label = "Tamanho aut.",
        selected = if (conn.cardinalityAutoSize) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "CARD_TAM_AUT",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        val auto = v == "Sim"
        onSchemaCommit(
            schema.copy(
                connections = schema.connections.map {
                    if (it.id != conn.id) return@map it
                    var c = it.copy(cardinalityAutoSize = auto)
                    if (!auto && (c.cardinalityPosition == null || c.cardinalityPosition.width <= 0 ||
                            c.cardinalityPosition.height <= 0)
                    ) {
                        val b = materializeCardinalityPositionForFixed(schema, it, textMeasurer)
                        if (b != null) c = c.copy(cardinalityPosition = b)
                    }
                    c
                },
            ),
        )
    }

    SectionTitle("Esquema")

    DropdownRow(
        label = "Entidade fraca",
        selected = if (conn.isWeak) "Sim" else "Não",
        options = listOf("Sim", "Não"),
        key = "ENT_FRACA",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        updateConn { it.copy(isWeak = v == "Sim") }
    }

    DropdownRow(
        label = "Cardinalidade",
        selected = conn.cardinality?.label ?: "-",
        options = Cardinality.entries.map { it.label },
        key = "CARDINALIDADE",
        focusedKey = focusedKey,
        onFocusChange = onFocusChange,
    ) { v ->
        val card =
            Cardinality.entries.firstOrNull { it.label == v }
        updateConn { it.copy(cardinality = card) }
    }
}

// ── Grid primitives ───────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SECTION_HEADER_BG)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C2D3E),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    valueCellBackground: Color = CELL_VALUE_READ_ONLY_BG,
) {
    val focused = focusedKey == key
    PropertyRow(
        label = label,
        focused = focused,
        onLabelClick = { onFocusChange(key) },
        valueCellModifier = Modifier.clickable { onFocusChange(key) },
        valueCellBackground = valueCellBackground,
    ) {
        Text(
            text = value,
            style = inspectorValueTextStyle(
                if (focused) Color(
                    0xFF80A0C0
                ) else VALUE_COLOR
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
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
    onLiveDraftChange: ((String) -> Unit)? = null,
    onCommit: (String) -> Unit,
) {
    val revertPreview = LocalRevertSchemaPreview.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val focused = focusedKey == key

    var textFieldValue by remember(key, value) {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    // BasicTextField emits an initial unfocused event before it ever gains focus; ignoring blur until
    // we have seen a real focus avoids clearing [focusedKey] immediately (row would "flash" unselected).
    var hadRealFocusInSession by remember(key, value) { mutableStateOf(false) }

    LaunchedEffect(value, focused) {
        if (!focused) {
            textFieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
        }
    }

    LaunchedEffect(focused, key, enabled) {
        if (focused && enabled) {
            focusRequester.requestFocus()
            val len = textFieldValue.text.length
            textFieldValue = textFieldValue.copy(selection = TextRange(len))
        }
    }

    val activateRow = { if (enabled) onFocusChange(key) }
    val valueCellModifier =
        if (focused && enabled) Modifier else Modifier.clickable(onClick = activateRow)

    PropertyRow(
        label = label,
        focused = focused,
        onLabelClick = activateRow,
        valueCellModifier = valueCellModifier,
        valueCellBackground = if (enabled) CELL_VALUE_BG else CELL_VALUE_READ_ONLY_BG,
    ) {
        if (focused && enabled) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { new ->
                    textFieldValue = new
                    onLiveDraftChange?.invoke(new.text)
                },
                textStyle = inspectorValueTextStyle(
                    VALUE_COLOR
                ),
                cursorBrush = SolidColor(VALUE_COLOR),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { evt ->
                        if (evt.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (evt.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                focusManager.clearFocus()
                                true
                            }

                            Key.Escape -> {
                                textFieldValue =
                                    TextFieldValue(text = value, selection = TextRange(value.length))
                                if (onLiveDraftChange != null) revertPreview()
                                focusManager.clearFocus()
                                true
                            }

                            else -> false
                        }
                    }
                    .onFocusChanged { fs ->
                        when {
                            fs.isFocused -> hadRealFocusInSession = true
                            hadRealFocusInSession -> {
                                hadRealFocusInSession = false
                                val text = textFieldValue.text
                                if (text != value) onCommit(text)
                                onFocusChange(null)
                            }
                        }
                    },
                decorationBox = { inner -> inner() },
            )
        } else {
            Text(
                text = value,
                style = inspectorValueTextStyle(
                    when {
                        !enabled -> Color(0xFF9AA0A8)
                        focused -> Color(0xFF80A0C0)
                        else -> VALUE_COLOR
                    },
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

/** A dropdown grid row (menu opens when the label or value cell is activated). */
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
    val density = LocalDensity.current
    var anchorWidth by remember { mutableStateOf(0.dp) }

    val openMenu = {
        onFocusChange(key)
        expanded = true
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(width = 0.5.dp, color = CELL_BORDER),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(CELL_LABEL_WIDTH)
                .fillMaxHeight()
                .clickable(onClick = openMenu)
                .background(if (focused) CELL_LABEL_FOCUSED else CELL_LABEL_BG)
                .padding(horizontal = 4.dp, vertical = 1.dp),
        ) {
            Text(
                text = label,
                fontSize = ROW_TEXT_SIZE,
                lineHeight = ROW_TEXT_SIZE,
                color = if (focused) LABEL_FOCUSED_COLOR else LABEL_COLOR,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(CELL_VALUE_BG),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .onGloballyPositioned { coords ->
                        anchorWidth = with(density) { coords.size.width.toDp() }
                    }
                    .clickable(onClick = openMenu)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    text = selected,
                    style = inspectorValueTextStyle(
                        VALUE_COLOR
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(text = "▾", style = INSPECTOR_DROPDOWN_CARET_STYLE)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.then(
                    if (anchorWidth > 0.dp) Modifier.width(anchorWidth) else Modifier,
                ),
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    options.forEachIndexed { index, option ->
                        InspectorDropdownMenuItem(
                            text = option,
                            onClick = {
                                expanded = false
                                onSelect(option)
                            },
                        )
                        if (index < options.lastIndex) {
                            HorizontalDivider(color = CELL_BORDER, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact menu line for the inspector grid: avoids Material3 [DropdownMenuItem]'s fixed 48.dp min height.
 */
@Composable
private fun InspectorDropdownMenuItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = inspectorValueTextStyle(VALUE_COLOR),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Base two-column row container used by all property row variants. */
@Composable
private fun PropertyRow(
    label: String,
    focused: Boolean,
    onLabelClick: () -> Unit,
    valueCellModifier: Modifier,
    valueCellBackground: Color = CELL_VALUE_BG,
    valueContent: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // IntrinsicSize.Min lets both cells share the height of whichever is taller,
            // so the value cell background always fills the full row height even when the
            // label wraps to multiple lines.
            .height(IntrinsicSize.Min)
            .border(width = 0.5.dp, color = CELL_BORDER),
        verticalAlignment = Alignment.Top,
    ) {
        // Label cell
        Box(
            modifier = Modifier
                .width(CELL_LABEL_WIDTH)
                .fillMaxHeight()
                .clickable(onClick = onLabelClick)
                .background(if (focused) CELL_LABEL_FOCUSED else CELL_LABEL_BG)
                .padding(horizontal = 4.dp, vertical = 1.dp),
        ) {
            Text(
                text = label,
                fontSize = ROW_TEXT_SIZE,
                lineHeight = ROW_TEXT_SIZE,
                color = if (focused) LABEL_FOCUSED_COLOR else LABEL_COLOR,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Value cell
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(valueCellBackground)
                .then(valueCellModifier),
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
            ActionButton(
                "Exibir no modelo",
                onClick = { /* TODO */ })
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
                TreeNode(
                    text = "Atributos:",
                    depth = 0,
                    bold = true
                )
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
        TreeNode(
            text = "Cardinalidade: ${attr.cardinality.toLabel()}",
            depth = depth + 2
        )
    }
    TreeNode(
        text = "Identificador: ${if (attr.isIdentifier) "Sim" else "Não"}",
        depth = depth + 2,
    )
    TreeNode(
        text = "Tipo: ${attr.type.ifBlank { "-" }}",
        depth = depth + 2
    )
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

/** Parses multivalued attribute max cardinality field text ("n" → unbounded sentinel 21). */
private fun parseAttributeMaxCardinalityDraft(v: String): Int? =
    if (v == "n") 21 else v.toIntOrNull()

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
    LineOrientation.DIAGONAL   -> "H. Diag."
    LineOrientation.LEFT       -> "H. Esg."
}
