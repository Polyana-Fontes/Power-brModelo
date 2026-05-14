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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ConceptualSearchHit
import games.polyclub.power.brmodelo.domain.ConceptualSearchOutcome
import games.polyclub.power.brmodelo.domain.ConceptualSearchTextScope
import games.polyclub.power.brmodelo.domain.ConceptualSearchTypeFilters
import games.polyclub.power.brmodelo.domain.searchConceptualModel

@Composable
internal fun ConceptualSearchDialog(
    schema: ConceptualSchema,
    onDismiss: () -> Unit,
    onNavigate: (ConceptualSearchNavigateAction) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var includeEntities by remember { mutableStateOf(false) }
    var includeRelationships by remember { mutableStateOf(false) }
    var includeAssociative by remember { mutableStateOf(false) }
    var includeSpecializations by remember { mutableStateOf(false) }
    var includeCanvasAttributes by remember { mutableStateOf(false) }
    var includeHiddenAttributes by remember { mutableStateOf(false) }
    var includeCardinality by remember { mutableStateOf(false) }
    var includeObservationBoxes by remember { mutableStateOf(false) }
    var searchDictionary by remember { mutableStateOf(true) }
    var searchObservations by remember { mutableStateOf(true) }
    var lastOutcome by remember { mutableStateOf<ConceptualSearchOutcome?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val typeFilters = ConceptualSearchTypeFilters(
        includeEntities = includeEntities,
        includeRelationships = includeRelationships,
        includeAssociativeEntities = includeAssociative,
        includeSpecializations = includeSpecializations,
        includeCanvasAttributes = includeCanvasAttributes,
        includeHiddenAttributes = includeHiddenAttributes,
        includeCardinalityLabels = includeCardinality,
        includeObservationBoxes = includeObservationBoxes,
    )
    val textScope = ConceptualSearchTextScope(
        searchDictionary = searchDictionary,
        searchObservations = searchObservations,
    )

    fun runSearch() {
        lastOutcome = schema.searchConceptualModel(query, typeFilters, textScope)
    }

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(horizontal = 12.dp, vertical = 16.dp),
        ) {
            val scrollState = rememberScrollState()
            val maxDialogHeight = (maxHeight - 32.dp).coerceAtLeast(220.dp)
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .width(480.dp)
                    .heightIn(max = maxDialogHeight),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Localizar no esquema conceitual", style = MaterialTheme.typography.titleSmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Texto") },
                        )
                        Button(onClick = { runSearch() }) {
                            Text("Buscar")
                        }
                    }
                    Text("Tipos (nenhum = todos)", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            TypeCheckRow("Entidades", includeEntities) { includeEntities = it }
                            TypeCheckRow("Relações", includeRelationships) { includeRelationships = it }
                            TypeCheckRow("Ent. associativas", includeAssociative) { includeAssociative = it }
                            TypeCheckRow("Especializações", includeSpecializations) { includeSpecializations = it }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            TypeCheckRow("Atributos (canvas)", includeCanvasAttributes) { includeCanvasAttributes = it }
                            TypeCheckRow("Atributos ocultos", includeHiddenAttributes) { includeHiddenAttributes = it }
                            TypeCheckRow("Cardinalidades", includeCardinality) { includeCardinality = it }
                            TypeCheckRow("Caixa de Observação", includeObservationBoxes) { includeObservationBoxes = it }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = searchDictionary, onCheckedChange = { searchDictionary = it ?: false })
                            Text("Dicionário", modifier = Modifier.padding(start = 2.dp), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = searchObservations, onCheckedChange = { searchObservations = it ?: false })
                            Text("Observações", modifier = Modifier.padding(start = 2.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                    when (val o = lastOutcome) {
                        null -> Text(
                            "Digite um texto ou deixe em branco para listar todos os itens dos tipos marcados, depois clique em Buscar.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        is ConceptualSearchOutcome.Err -> {
                            Text(o.code, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                        is ConceptualSearchOutcome.Ok -> {
                            val r = o.result
                            if (r.truncated) {
                                Text(
                                    "Mostrando ${r.hits.size} de pelo menos ${r.totalMatched} ocorrências (limite 400).",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                Text("${r.hits.size} ocorrência(s).", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                for (hit in r.hits) {
                                    HitRow(
                                        hit = hit,
                                        onGoTo = {
                                            val action = conceptualSearchNavigateAction(schema, hit, textMeasurer)
                                            if (action != null) {
                                                onNavigate(action)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Fechar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onCheckedChange(it ?: false) })
        Text(label, modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HitRow(hit: ConceptualSearchHit, onGoTo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(hitTitle(hit), style = MaterialTheme.typography.bodyMedium)
            val fields = when (hit) {
                is ConceptualSearchHit.ElementHit -> hit.matchedIn
                is ConceptualSearchHit.CardinalityHit -> hit.matchedIn
                is ConceptualSearchHit.HiddenHit -> hit.matchedIn
            }
            if (fields.isNotEmpty()) {
                Text(
                    fields.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onGoTo) { Text("Ir para") }
    }
}

private fun hitTitle(hit: ConceptualSearchHit): String =
    when (hit) {
        is ConceptualSearchHit.ElementHit -> hit.title
        is ConceptualSearchHit.CardinalityHit -> "Cardinalidade: ${hit.title}"
        is ConceptualSearchHit.HiddenHit -> "Oculto: ${hit.displayName}"
    }
