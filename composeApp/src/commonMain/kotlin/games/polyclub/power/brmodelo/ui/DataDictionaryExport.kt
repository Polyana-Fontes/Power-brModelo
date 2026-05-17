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

import games.polyclub.power.brmodelo.domain.ConceptualSchemaDictionaryEntry

/**
 * Opens a native save dialog and writes UTF-8 Markdown (Pascal `TbrFmDicFull.btnSalvarClick` / `SaveToFile`, extension `.md`).
 *
 * @param suggestedBaseFileName Model name without extension, used as default file name stem.
 * @param markdown Full report as produced by [games.polyclub.power.brmodelo.domain.formatConceptualDataDictionaryMarkdown].
 * @return true if the file was written; false if the user cancelled or an error occurred.
 */
expect suspend fun saveConceptualDataDictionaryTextFile(suggestedBaseFileName: String, markdown: String): Boolean

/**
 * Opens a native print dialog and prints [markdown] (Pascal `TRichEdit.Print('[Dicionário de dados]')` after `TPrintDialog`).
 *
 * @param documentTitle Job / document title (Pascal passes `'[Dicionário de dados]'`).
 * @return true if printing was started; false if the user cancelled.
 */
expect suspend fun printConceptualDataDictionary(markdown: String, documentTitle: String): Boolean

/** True when [saveConceptualDataDictionaryPdfFile] can write a real PDF (desktop); false on WASM/browser. */
expect fun conceptualDataDictionaryPdfExportSupported(): Boolean

/**
 * Opens a native save dialog and writes a PDF built from the same logical content as Markdown export
 * ([games.polyclub.power.brmodelo.domain.formatConceptualDataDictionaryPlainText]).
 *
 * @return true if a file was written; false if the user cancelled, the platform does not support PDF export,
 * or an error was handled by returning false.
 */
expect suspend fun saveConceptualDataDictionaryPdfFile(
    suggestedBaseFileName: String,
    entries: List<ConceptualSchemaDictionaryEntry>,
    schemaName: String,
): Boolean
