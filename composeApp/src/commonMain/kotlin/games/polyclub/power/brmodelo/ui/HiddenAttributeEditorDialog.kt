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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import games.polyclub.power.brmodelo.domain.AttributeCardinality
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.HiddenAttribute
import games.polyclub.power.brmodelo.domain.hiddenAttributeForestNamesValid

private val ATTR_TYPE_PRESETS = listOf(
    "VARCHAR( )",
    "INTEGER",
    "NUMERIC",
    "BOOLEAN",
    "DATE",
    "TIME",
    "TEXT",
)

private val HIDDEN_ATTR_DIALOG_PAD = 10.dp
private val HIDDEN_ATTR_FIELD_TEXT = TextStyle(fontSize = 12.sp, lineHeight = 14.sp)

internal fun defaultNewHiddenAttribute(suggestedName: String): HiddenAttribute =
    HiddenAttribute(
        name = suggestedName,
        type = "VARCHAR( )",
        isIdentifier = false,
        cardinality = AttributeCardinality(1, 0),
        position = ElementPosition(x = -1, y = -1, width = 0, height = 0),
        children = emptyList(),
        nestedHiddenAttributes = emptyList(),
        isOptional = false,
        observations = "",
        dictionary = "",
    )

private fun readHiddenAtPath(root: HiddenAttribute, path: List<Int>): HiddenAttribute? {
    if (path.isEmpty()) return root
    var n = root
    for (i in path) {
        n = n.branchAt(i) ?: return null
    }
    return n
}

private fun updateHiddenAtPath(root: HiddenAttribute, path: List<Int>, f: (HiddenAttribute) -> HiddenAttribute): HiddenAttribute? {
    if (path.isEmpty()) return f(root)
    val idx = path[0]
    val child = root.branchAt(idx) ?: return null
    val updatedChild = updateHiddenAtPath(child, path.drop(1), f) ?: return null
    return root.withBranchReplaced(idx, updatedChild)
}

private fun breadcrumbLabels(root: HiddenAttribute, path: List<Int>): List<String> {
    if (path.isEmpty()) return listOf(root.name.ifBlank { "(raiz)" })
    val out = mutableListOf<String>()
    var n = root
    out.add(n.name.ifBlank { "(raiz)" })
    for (i in path) {
        n = n.branchAt(i) ?: break
        out.add(n.name.ifBlank { "—" })
    }
    return out
}

@Composable
internal fun HiddenAttributeEditorDialog(
    title: String,
    initialSubtree: HiddenAttribute,
    onDismiss: () -> Unit,
    onConfirm: (HiddenAttribute) -> Unit,
    extraValid: (HiddenAttribute) -> Boolean = { true },
) {
    var draft by remember(initialSubtree) { mutableStateOf(initialSubtree) }
    var navPath by remember { mutableStateOf(emptyList<Int>()) }
    var typeMenu by remember { mutableStateOf(false) }
    var minMenu by remember { mutableStateOf(false) }
    var maxMenu by remember { mutableStateOf(false) }
    var multilineField by remember { mutableStateOf<MultilineField?>(null) }

    val current = readHiddenAtPath(draft, navPath) ?: draft
    val crumbs = breadcrumbLabels(draft, navPath)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    multilineField?.let { field ->
        val label = when (field) {
            MultilineField.Observations -> "Observação"
            MultilineField.Dictionary -> "Dicionário de dados"
        }
        val text = when (field) {
            MultilineField.Observations -> current.observations
            MultilineField.Dictionary -> current.dictionary
        }
        MultilineInspectorDialog(
            label = label,
            initialText = text,
            onLiveDraftChange = { live ->
                draft = updateHiddenAtPath(draft, navPath) { node ->
                    when (field) {
                        MultilineField.Observations -> node.copy(observations = live)
                        MultilineField.Dictionary -> node.copy(dictionary = live)
                    }
                } ?: draft
            },
            onCancel = { multilineField = null },
            onConfirm = { finalText ->
                draft = updateHiddenAtPath(draft, navPath) { node ->
                    when (field) {
                        MultilineField.Observations -> node.copy(observations = finalText)
                        MultilineField.Dictionary -> node.copy(dictionary = finalText)
                    }
                } ?: draft
                multilineField = null
            },
            compact = true,
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 300.dp, max = 440.dp)
                .onPreviewKeyEvent { evt ->
                    if (evt.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (evt.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                },
        ) {
            Column(
                Modifier.padding(HIDDEN_ATTR_DIALOG_PAD).heightIn(max = 520.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = crumbs.joinToString(" › "),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    OutlinedTextField(
                        value = current.name,
                        onValueChange = { v ->
                            draft = updateHiddenAtPath(draft, navPath) { node -> node.copy(name = v) } ?: draft
                        },
                        label = { Text("Nome", fontSize = 11.sp) },
                        singleLine = true,
                        textStyle = HIDDEN_ATTR_FIELD_TEXT,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.heightIn(min = 26.dp),
                    ) {
                        Checkbox(
                            checked = current.isIdentifier,
                            onCheckedChange = { c ->
                                draft = updateHiddenAtPath(draft, navPath) { node -> node.copy(isIdentifier = c) } ?: draft
                            },
                            modifier = Modifier.padding(0.dp),
                        )
                        Text("Identificador", fontSize = 11.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.heightIn(min = 26.dp),
                    ) {
                        Checkbox(
                            checked = current.isOptional,
                            onCheckedChange = { c ->
                                draft = updateHiddenAtPath(draft, navPath) { node -> node.copy(isOptional = c) } ?: draft
                            },
                            modifier = Modifier.padding(0.dp),
                        )
                        Text("Opcional", fontSize = 11.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.heightIn(min = 26.dp),
                    ) {
                        Checkbox(
                            checked = current.isMultiValued,
                            onCheckedChange = { mv ->
                                draft = updateHiddenAtPath(draft, navPath) { node ->
                                    if (mv) {
                                        val min = if (node.isOptional) 0 else 1
                                        node.copy(cardinality = AttributeCardinality(min, 21))
                                    } else {
                                        node.copy(cardinality = AttributeCardinality(1, 0))
                                    }
                                } ?: draft
                            },
                            modifier = Modifier.padding(0.dp),
                        )
                        Text("Multivalorado", fontSize = 11.sp)
                    }
                    if (current.isMultiValued) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Min:", fontSize = 10.sp)
                            BoxDropdown(
                                label = current.cardinality.minCardinality.toString(),
                                expanded = minMenu,
                                onExpand = { minMenu = it },
                            ) {
                                listOf(0, 1).forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m.toString(), fontSize = 12.sp) },
                                        onClick = {
                                            minMenu = false
                                            draft = updateHiddenAtPath(draft, navPath) { node ->
                                                val max = node.cardinality.maxCardinality.coerceAtLeast(1)
                                                node.copy(cardinality = AttributeCardinality(m, max))
                                            } ?: draft
                                        },
                                    )
                                }
                            }
                            Text("Max:", fontSize = 10.sp)
                            BoxDropdown(
                                label = maxLabel(current.cardinality.maxCardinality),
                                expanded = maxMenu,
                                onExpand = { maxMenu = it },
                            ) {
                                (1..20).forEach { mx ->
                                    DropdownMenuItem(
                                        text = { Text(mx.toString(), fontSize = 12.sp) },
                                        onClick = {
                                            maxMenu = false
                                            draft = updateHiddenAtPath(draft, navPath) { node ->
                                                val min = node.cardinality.minCardinality.coerceAtMost(mx)
                                                node.copy(cardinality = AttributeCardinality(min, mx))
                                            } ?: draft
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("n", fontSize = 12.sp) },
                                    onClick = {
                                        maxMenu = false
                                        draft = updateHiddenAtPath(draft, navPath) { node ->
                                            val min = node.cardinality.minCardinality
                                            node.copy(cardinality = AttributeCardinality(min, 21))
                                        } ?: draft
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = current.type,
                            onValueChange = { t ->
                                draft = updateHiddenAtPath(draft, navPath) { node -> node.copy(type = t) } ?: draft
                            },
                            label = { Text("Tipo", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = HIDDEN_ATTR_FIELD_TEXT,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = { typeMenu = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("Presets", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                            ATTR_TYPE_PRESETS.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset, fontSize = 12.sp) },
                                    onClick = {
                                        typeMenu = false
                                        draft = updateHiddenAtPath(draft, navPath) { node -> node.copy(type = preset) } ?: draft
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    MultilineOpenRow(
                        label = "Observação",
                        preview = firstLinePreview(current.observations),
                        onOpen = { multilineField = MultilineField.Observations },
                    )
                    MultilineOpenRow(
                        label = "Dicionário",
                        preview = firstLinePreview(current.dictionary),
                        onOpen = { multilineField = MultilineField.Dictionary },
                    )
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Estrutura composta",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                    Text(
                        "Filhos no diagrama ao revelar; ocultos aninhados só no modelo.",
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (current.children.isEmpty() && current.nestedHiddenAttributes.isEmpty()) {
                        Text("(nenhum sub-atributo)", fontSize = 10.sp, color = Color(0xFF7A8A9A))
                    } else {
                        current.children.forEachIndexed { i, ch ->
                            BranchRow(
                                label = "Filho ${i + 1}",
                                name = ch.name,
                                onOpen = { navPath = navPath + i },
                                onRemove = {
                                    draft = updateHiddenAtPath(draft, navPath) { parent ->
                                        parent.withBranchRemoved(i)?.second ?: parent
                                    } ?: draft
                                },
                            )
                        }
                        current.nestedHiddenAttributes.forEachIndexed { j, ch ->
                            val merged = current.children.size + j
                            BranchRow(
                                label = "Oculto aninhado ${j + 1}",
                                name = ch.name,
                                onOpen = { navPath = navPath + merged },
                                onRemove = {
                                    draft = updateHiddenAtPath(draft, navPath) { parent ->
                                        parent.withBranchRemoved(merged)?.second ?: parent
                                    } ?: draft
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                val n = suggestChildName(current, prefix = "atributo")
                                val leaf = defaultNewHiddenAttribute(n)
                                draft = updateHiddenAtPath(draft, navPath) { parent ->
                                    parent.copy(children = parent.children + leaf)
                                } ?: draft
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        ) {
                            Text("+ Filho", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = {
                                val n = suggestChildName(current, prefix = "oculto")
                                val leaf = defaultNewHiddenAttribute(n)
                                draft = updateHiddenAtPath(draft, navPath) { parent ->
                                    parent.copy(nestedHiddenAttributes = parent.nestedHiddenAttributes + leaf)
                                } ?: draft
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        ) {
                            Text("+ Oculto aninhado", fontSize = 11.sp)
                        }
                    }
                }
                if (navPath.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { navPath = navPath.dropLast(1) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Text("↑ Voltar ao pai", fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Cancelar", fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (hiddenAttributeForestNamesValid(listOf(draft)) && extraValid(draft)) {
                                onConfirm(draft)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("Salvar", fontSize = 11.sp)
                    }
                }
                if (!hiddenAttributeForestNamesValid(listOf(draft))) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Corrija: nomes não podem ficar vazios nem repetidos entre irmãos.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                    )
                } else if (!extraValid(draft)) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Este nome conflita com outro atributo oculto no mesmo nível da árvore do elemento.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

private enum class MultilineField { Observations, Dictionary }

@Composable
private fun BoxDropdown(
    label: String,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Box {
        Text(
            text = label,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
                .clickable { onExpand(true) }
                .padding(horizontal = 6.dp, vertical = 3.dp),
            fontSize = 11.sp,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpand(false) }) {
            content()
        }
    }
}

@Composable
private fun MultilineOpenRow(
    label: String,
    preview: String,
    onOpen: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .clickable(onClick = onOpen)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = preview.ifBlank { "Clique para editar…" },
                fontSize = 10.sp,
                color = if (preview.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("…", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BranchRow(
    label: String,
    name: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name.ifBlank { "—" }, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        TextButton(
            onClick = onRemove,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        ) {
            Text("Remover", fontSize = 10.sp)
        }
    }
}

private fun maxLabel(maxCardinality: Int): String =
    if (maxCardinality > 20) "n" else maxCardinality.toString()

private fun firstLinePreview(s: String): String =
    s.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        .let { if (it.length > 120) it.take(120) + "…" else it }

internal fun suggestNewRootHiddenAttributeName(roots: List<HiddenAttribute>): String {
    val used = roots.map { it.name.trim() }.toSet()
    var i = 1
    while (true) {
        val c = "oculto$i"
        if (c !in used) return c
        i++
    }
}

private fun suggestChildName(parent: HiddenAttribute, prefix: String): String {
    val used = buildSet {
        parent.children.forEach { add(it.name.trim()) }
        parent.nestedHiddenAttributes.forEach { add(it.name.trim()) }
    }
    var i = 1
    while (true) {
        val candidate = "$prefix$i"
        if (candidate !in used && candidate.isNotBlank()) return candidate
        i++
    }
}
