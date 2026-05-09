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
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
internal fun CloseTabUnsavedDialog(
    documentTitle: String,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Salvar alterações?") },
        text = {
            Text(
                "O documento \"$documentTitle\" tem alterações não salvas. " +
                    "Deseja salvá-las antes de fechar?",
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancelar") }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDiscard) { Text("Não salvar") }
                TextButton(onClick = onSave) { Text("Salvar") }
            }
        },
    )
}

@Composable
internal fun QuitApplicationUnsavedDialog(
    showSaveAll: Boolean,
    onSaveAll: () -> Unit,
    onQuitWithoutSaving: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Sair da aplicação?") },
        text = {
            Text(
                "Existem documentos com alterações não salvas. " +
                    "Deseja salvá-las antes de sair?",
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancelar") }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onQuitWithoutSaving) { Text("Sair sem salvar") }
                if (showSaveAll) {
                    TextButton(onClick = onSaveAll) { Text("Salvar todos") }
                }
            }
        },
    )
}
