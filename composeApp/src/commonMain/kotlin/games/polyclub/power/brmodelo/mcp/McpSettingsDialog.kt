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

package games.polyclub.power.brmodelo.mcp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
internal fun McpSettingsDialog(
    initialBindHost: String,
    initialPort: Int,
    initialAllowLanHosts: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (bindHost: String, port: Int, allowLanHosts: Boolean) -> Unit,
) {
    var bindHost by remember { mutableStateOf(initialBindHost) }
    var portText by remember { mutableStateOf(initialPort.toString()) }
    var allowLanHosts by remember { mutableStateOf(initialAllowLanHosts) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
            Column(
                modifier = Modifier.padding(20.dp).width(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Servidor MCP embarcado",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Endereço e porta onde o servidor HTTP MCP escuta. " +
                        "Use 127.0.0.1 para apenas esta máquina; 0.0.0.0 para aceitar conexões de outras interfaces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = bindHost,
                    onValueChange = { bindHost = it.trim() },
                    label = { Text("Endereço (bind)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("Porta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = allowLanHosts,
                        onCheckedChange = { allowLanHosts = it == true },
                    )
                    Text(
                        text = "Permitir cabeçalhos Host de outras máquinas (rede local)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = "Desative em redes não confiáveis. O cliente MCP envia Host como \"endereço:porta\"; " +
                        "o validador aceita padrões compatíveis com o bind e com localhost.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val port = portText.toIntOrNull()?.coerceIn(1, 65535) ?: 8765
                            onConfirm(bindHost.ifBlank { "127.0.0.1" }, port, allowLanHosts)
                        },
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}
