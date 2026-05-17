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

/**
 * Opens a native save dialog and writes UTF-8 plain text (Pascal `TbrFmDicFull.btnSalvarClick` / `SaveToFile`).
 *
 * @param suggestedBaseFileName Model name without extension, used as default file name stem.
 * @param plainText Full report as produced by [games.polyclub.power.brmodelo.domain.formatConceptualDataDictionaryPlainText].
 * @return true if the file was written; false if the user cancelled or an error occurred.
 */
expect suspend fun saveConceptualDataDictionaryTextFile(suggestedBaseFileName: String, plainText: String): Boolean

/**
 * Opens a native print dialog and prints [plainText] (Pascal `TRichEdit.Print('[Dicionário de dados]')` after `TPrintDialog`).
 *
 * @param documentTitle Job / document title (Pascal passes `'[Dicionário de dados]'`).
 * @return true if printing was started; false if the user cancelled.
 */
expect suspend fun printConceptualDataDictionary(plainText: String, documentTitle: String): Boolean
