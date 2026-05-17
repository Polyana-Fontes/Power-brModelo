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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.PrinterJob
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.max
import kotlin.math.min
import java.util.concurrent.CountDownLatch

actual suspend fun saveConceptualDataDictionaryTextFile(suggestedBaseFileName: String, plainText: String): Boolean =
    withContext(Dispatchers.IO) {
        val written = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        SwingUtilities.invokeLater {
            try {
                val stem = suggestedBaseFileName.ifBlank { "modelo" }
                val chooser = JFileChooser().apply {
                    dialogTitle = "Salvar dicionário de dados"
                    selectedFile = File("$stem.txt")
                    fileFilter = FileNameExtensionFilter("Texto (*.txt)", "txt")
                }
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    var file = chooser.selectedFile
                    if (!file.name.lowercase().endsWith(".txt")) {
                        file = File("${file.path}.txt")
                    }
                    file.writeText(plainText, Charsets.UTF_8)
                    written.set(true)
                }
            } finally {
                latch.countDown()
            }
        }
        latch.await()
        written.get()
    }

actual suspend fun printConceptualDataDictionary(plainText: String, documentTitle: String): Boolean =
    withContext(Dispatchers.IO) {
        val accepted = AtomicBoolean(false)
        val errorRef = AtomicReference<Throwable?>(null)
        val latch = CountDownLatch(1)
        SwingUtilities.invokeLater {
            try {
                val job = PrinterJob.getPrinterJob()
                job.jobName = documentTitle
                val printable = PlainTextPrintable(plainText)
                job.setPrintable(printable)
                if (job.printDialog()) {
                    job.print()
                    accepted.set(true)
                }
            } catch (t: Throwable) {
                errorRef.set(t)
            } finally {
                latch.countDown()
            }
        }
        latch.await()
        errorRef.get()?.let { throw it }
        accepted.get()
    }

/**
 * Paginated plain-text [Printable] (wraps long lines to the imageable width).
 */
private class PlainTextPrintable(private val text: String) : Printable {
    override fun print(graphics: Graphics, pf: PageFormat, pageIndex: Int): Int {
        val g2 = graphics as Graphics2D
        val margin = 48.0
        val x = pf.imageableX + margin
        var y = pf.imageableY + margin
        val maxW = (pf.imageableWidth - 2 * margin).coerceAtLeast(24.0)

        g2.font = Font(Font.MONOSPACED, Font.PLAIN, 10)
        val fm = g2.fontMetrics
        val lineHeight = fm.height + 1
        val wrapped = wrapAllLines(text.split('\n'), fm, maxW.toInt())
        val usableH = pf.imageableHeight - 2 * margin
        val linesPerPage = max(1, (usableH / lineHeight).toInt())
        val pageCount = max(1, (wrapped.size + linesPerPage - 1) / linesPerPage)
        if (pageIndex >= pageCount) {
            return Printable.NO_SUCH_PAGE
        }
        val start = pageIndex * linesPerPage
        val end = min(start + linesPerPage, wrapped.size)
        for (i in start until end) {
            g2.drawString(wrapped[i], x.toFloat(), y.toFloat() + fm.ascent)
            y += lineHeight
        }
        return Printable.PAGE_EXISTS
    }

    private fun wrapAllLines(rawLines: List<String>, fm: FontMetrics, maxW: Int): List<String> {
        val out = ArrayList<String>(rawLines.size * 2)
        for (raw in rawLines) {
            if (raw.isEmpty()) {
                out.add("")
                continue
            }
            var rest = raw
            while (rest.isNotEmpty()) {
                if (fm.stringWidth(rest) <= maxW) {
                    out.add(rest)
                    break
                }
                var cut = rest.length
                while (cut > 0 && fm.stringWidth(rest.substring(0, cut)) > maxW) {
                    cut--
                }
                if (cut == 0) cut = 1
                var br = rest.lastIndexOf(' ', min(cut, rest.length))
                if (br <= 0) br = cut
                val chunk = rest.substring(0, br).trimEnd()
                out.add(chunk)
                rest = rest.substring(br).trimStart()
            }
        }
        return out
    }
}
