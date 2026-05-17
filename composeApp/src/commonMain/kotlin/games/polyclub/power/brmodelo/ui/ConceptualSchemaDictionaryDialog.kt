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

import androidx.compose.foundation.VerticalScrollbar
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ConceptualSchemaDictionaryEntry
import games.polyclub.power.brmodelo.domain.collectConceptualSchemaDictionaryReportEntries
import games.polyclub.power.brmodelo.domain.formatConceptualDataDictionaryMarkdown
import kotlinx.coroutines.launch

/** Pascal `TRichEdit.Print` job name (`dicFull.pas`). */
internal const val ConceptualDataDictionaryPrintJobName: String = "[Dicionário de dados]"

/**
 * Rich preview for the dialog (not Markdown source): monospace bold coloured headers per object,
 * plain body — same spirit as the pre-Markdown `LazyColumn` preview. **Salvar / Imprimir** still use
 * [formatConceptualDataDictionaryMarkdown].
 */
@Composable
private fun rememberConceptualDictionaryPreviewAnnotated(
    entries: List<ConceptualSchemaDictionaryEntry>,
    schemaName: String,
): AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val monoHeaderStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = primary,
        fontSize = 12.sp,
    )
    val bodyStyle = SpanStyle(color = onSurface, fontSize = 12.sp)
    val metaStyle = SpanStyle(color = onSurfaceVariant, fontSize = 12.sp)
    return remember(entries, schemaName, primary, onSurface, onSurfaceVariant) {
        buildAnnotatedString {
            val title = schemaName.trim()
            if (title.isNotEmpty()) {
                withStyle(metaStyle) {
                    append("Esquema: ")
                    append(title)
                }
                append("\n\n")
            }
            entries.forEachIndexed { i, e ->
                val n = (i + 1).toString().padStart(3, '0')
                withStyle(monoHeaderStyle) {
                    append("$n — ${e.typeLabel}: ${e.objectName}")
                }
                append("\n\n")
                val body = e.dictionary.trim()
                if (body.isNotEmpty()) {
                    withStyle(bodyStyle) {
                        append(body)
                    }
                    append("\n\n")
                }
            }
        }
    }
}

@Composable
internal fun ConceptualSchemaDictionaryDialog(
    schema: ConceptualSchema,
    onDismiss: () -> Unit,
    onTransientUserMessage: (String) -> Unit = {},
) {
    val entries = remember(schema) { collectConceptualSchemaDictionaryReportEntries(schema) }
    val exportMarkdown = remember(schema, entries) {
        formatConceptualDataDictionaryMarkdown(entries, schema.name)
    }
    val previewAnnotated = rememberConceptualDictionaryPreviewAnnotated(entries, schema.name)
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                },
            tonalElevation = 6.dp,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Dicionário de dados",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(10.dp))
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                ) {
                    SelectionContainer {
                        BasicText(
                            text = previewAnnotated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(end = 10.dp),
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val ok = saveConceptualDataDictionaryTextFile(
                                        suggestedBaseFileName = schema.name.ifBlank { "modelo" },
                                        markdown = exportMarkdown,
                                    )
                                    if (ok) {
                                        onTransientUserMessage("Dicionário salvo.")
                                    }
                                } catch (ex: Exception) {
                                    onTransientUserMessage("Falha ao salvar: ${ex.message ?: ex::class.simpleName}")
                                }
                            }
                        },
                    ) {
                        Text("Salvar")
                    }
                    if (conceptualDataDictionaryPdfExportSupported()) {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val ok = saveConceptualDataDictionaryPdfFile(
                                            suggestedBaseFileName = schema.name.ifBlank { "modelo" },
                                            entries = entries,
                                            schemaName = schema.name,
                                        )
                                        if (ok) {
                                            onTransientUserMessage("Dicionário salvo em PDF.")
                                        }
                                    } catch (ex: Exception) {
                                        onTransientUserMessage(
                                            "Falha ao salvar PDF: ${ex.message ?: ex::class.simpleName}",
                                        )
                                    }
                                }
                            },
                        ) {
                            Text("Salvar PDF")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val ok = printConceptualDataDictionary(exportMarkdown, ConceptualDataDictionaryPrintJobName)
                                    if (!ok) {
                                        onTransientUserMessage("Impressão cancelada.")
                                    }
                                } catch (ex: Exception) {
                                    onTransientUserMessage("Falha ao imprimir: ${ex.message ?: ex::class.simpleName}")
                                }
                            }
                        },
                    ) {
                        Text("Imprimir")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onDismiss) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}
