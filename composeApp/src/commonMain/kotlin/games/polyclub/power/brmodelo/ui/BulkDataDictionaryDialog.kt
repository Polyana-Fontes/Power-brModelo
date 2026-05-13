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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import games.polyclub.power.brmodelo.domain.ConceptualDictionarySlotKey
import games.polyclub.power.brmodelo.domain.ConceptualDictionarySlotRow

@Composable
internal fun BulkDataDictionaryDialog(
    rows: List<ConceptualDictionarySlotRow>,
    onDismiss: () -> Unit,
    /** Only includes entries whose text differs from [ConceptualDictionarySlotRow.initialText]. May be empty. */
    onCommit: (List<Pair<ConceptualDictionarySlotKey, String>>) -> Unit,
) {
    var draftTexts by remember(rows) {
        mutableStateOf(rows.associate { it.key to it.initialText })
    }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val initialByKey = remember(rows) { rows.associate { it.key to it.initialText } }
    val isDirty = remember(draftTexts, initialByKey) {
        draftTexts.any { (k, v) -> initialByKey[k] != v }
    }

    fun requestClose() {
        if (isDirty) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Descartar alterações?") },
            text = {
                Text(
                    "Existem edições pendentes no dicionário de dados. " +
                        "Deseja fechar sem aplicar?",
                )
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Voltar") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onDismiss()
                    },
                ) { Text("Descartar") }
            },
        )
    }

    Dialog(
        onDismissRequest = { requestClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 960.dp)
                .heightIn(max = 720.dp)
                .onPreviewKeyEvent { evt ->
                    if (evt.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (evt.key == Key.Escape) {
                        requestClose()
                        true
                    } else {
                        false
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "Dicionário de Dados de Objetos",
                    style = TextStyle(
                        fontSize = 17.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Edite o texto de cada campo. \"Pronto\" grava todas as alterações em um único passo do histórico.",
                    style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    itemsIndexed(rows, key = { _, row -> row.key }) { _, row ->
                        val text = draftTexts[row.key].orEmpty()
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                text = row.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = row.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = text,
                                onValueChange = { nv ->
                                    draftTexts = draftTexts + (row.key to nv)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 112.dp),
                                minLines = 4,
                                maxLines = 12,
                                textStyle = TextStyle(fontSize = 13.sp, lineHeight = 16.sp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { requestClose() }) {
                        Text("Cancelar")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val changed = rows.mapNotNull { row ->
                                val v = draftTexts[row.key].orEmpty()
                                if (v != row.initialText) row.key to v else null
                            }
                            onCommit(changed)
                        },
                    ) {
                        Text("Pronto")
                    }
                }
            }
        }
    }
}
