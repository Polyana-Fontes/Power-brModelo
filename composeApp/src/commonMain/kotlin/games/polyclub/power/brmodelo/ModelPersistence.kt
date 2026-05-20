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

package games.polyclub.power.brmodelo

import games.polyclub.power.brmodelo.domain.ConceptualSchema

/**
 * Writes [schema] as brModelo XML to disk.
 *
 * @param suggestedBaseName Filename stem suggested when a save dialog is shown.
 * @param pickLocation When true, always asks for a path (Save As / XML migration).
 * @param explicitPath When set, writes to this absolute path without a save dialog (desktop).
 * @return Schema updated with [games.polyclub.power.brmodelo.domain.ConceptualSchema.filePath] and [games.polyclub.power.brmodelo.domain.ConceptualSchema.openedFromBrm] cleared,
 *         or null if cancelled / unsupported (e.g. WASM).
 */
internal expect suspend fun saveConceptualSchemaXml(
    schema: ConceptualSchema,
    suggestedBaseName: String,
    pickLocation: Boolean,
    explicitPath: String? = null,
): ConceptualSchema?
