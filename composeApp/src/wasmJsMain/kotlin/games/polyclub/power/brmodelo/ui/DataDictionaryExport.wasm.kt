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

actual suspend fun saveConceptualDataDictionaryTextFile(suggestedBaseFileName: String, plainText: String): Boolean {
    val stem = suggestedBaseFileName.ifBlank { "modelo" }
    val filename = "$stem.txt"
    triggerUtf8TextDownload(plainText, filename)
    return true
}

actual suspend fun printConceptualDataDictionary(plainText: String, documentTitle: String): Boolean {
    printPlainTextInNewWindow(plainText, documentTitle)
    return true
}

private fun triggerUtf8TextDownload(text: String, filename: String): Unit = js(
    """
    (function(t, name) {
        var blob = new Blob([t], { type: 'text/plain;charset=utf-8' });
        var url = URL.createObjectURL(blob);
        var link = document.createElement('a');
        link.href = url;
        link.download = name;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    })(text, filename)
    """
)

private fun printPlainTextInNewWindow(text: String, title: String): Unit = js(
    """
    (function(t, docTitle) {
        var w = window.open('', '_blank');
        if (!w) return;
        var esc = function(s) {
            return String(s)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;');
        };
        w.document.open();
        w.document.write('<!DOCTYPE html><html><head><meta charset="utf-8"><title>' + esc(docTitle) +
            '</title></head><body><pre style="font:12px monospace;white-space:pre-wrap;margin:12px">' +
            esc(t) + '</pre></body></html>');
        w.document.close();
        w.focus();
        w.print();
        w.close();
    })(text, title)
    """
)
