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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.collectConceptualSchemaDictionaryReportEntries
import games.polyclub.power.brmodelo.domain.formatConceptualDataDictionaryPlainText
import kotlinx.coroutines.launch

/** Pascal `TRichEdit.Print` job name (`dicFull.pas`). */
internal const val ConceptualDataDictionaryPrintJobName: String = "[Dicionário de dados]"

@Composable
internal fun ConceptualSchemaDictionaryDialog(
    schema: ConceptualSchema,
    onDismiss: () -> Unit,
    onTransientUserMessage: (String) -> Unit = {},
) {
    val entries = remember(schema) { collectConceptualSchemaDictionaryReportEntries(schema) }
    val plainText = remember(entries) { formatConceptualDataDictionaryPlainText(entries) }
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                ) {
                    itemsIndexed(entries, key = { i, e -> "${e.typeLabel}:${e.objectName}:$i" }) { i, e ->
                        val n = (i + 1).toString().padStart(3, '0')
                        Text(
                            text = "$n - ${e.typeLabel}: ${e.objectName}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PascalDicFullHeaderBlue,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                        val body = e.dictionary.trim()
                        if (body.isNotEmpty()) {
                            Text(
                                text = body,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
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
                                        plainText = plainText,
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
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val ok = printConceptualDataDictionary(plainText, ConceptualDataDictionaryPrintJobName)
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

/** Delphi `clBlue` on `TRichEdit` header lines (`dicFull.pas`). */
private val PascalDicFullHeaderBlue = Color(0xFF0000FF)
