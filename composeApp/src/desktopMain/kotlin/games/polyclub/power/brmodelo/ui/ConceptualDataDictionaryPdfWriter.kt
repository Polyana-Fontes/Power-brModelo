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

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Writes multi-page A4 PDF with paginated plain text (line wrapping similar to the desktop print path).
 */
internal object ConceptualDataDictionaryPdfWriter {

    fun write(file: File, plainText: String) {
        PDDocument().use { doc ->
            val font = resolveBodyFont(doc)
            val margin = 48f
            val fontSize = 10f
            val leading = fontSize * 1.2f
            val pageH = PDRectangle.A4.height
            val pageW = PDRectangle.A4.width
            val contentWidth = pageW - 2 * margin
            val wrapped = wrapAllLines(plainText.split('\n'), font, fontSize, contentWidth)
            val usableHeight = pageH - 2 * margin
            val linesPerPage = max(1, (usableHeight / leading).toInt())
            if (wrapped.isEmpty()) {
                val page = PDPage(PDRectangle.A4)
                doc.addPage(page)
                PDPageContentStream(doc, page).use { cs ->
                    cs.beginText()
                    cs.setFont(font, fontSize)
                    val baseline = pageH - margin - fontSize * 0.85f
                    cs.newLineAtOffset(margin, baseline)
                    cs.showText(safeForFont(font, "(empty)"))
                    cs.endText()
                }
            } else {
                val pageCount = max(1, (wrapped.size + linesPerPage - 1) / linesPerPage)
                for (pageIndex in 0 until pageCount) {
                    val start = pageIndex * linesPerPage
                    val end = min(start + linesPerPage, wrapped.size)
                    val chunk = wrapped.subList(start, end)
                    val page = PDPage(PDRectangle.A4)
                    doc.addPage(page)
                    PDPageContentStream(doc, page).use { cs ->
                        cs.beginText()
                        cs.setFont(font, fontSize)
                        val baseline = pageH - margin - fontSize * 0.85f
                        cs.newLineAtOffset(margin, baseline)
                        chunk.forEachIndexed { idx, line ->
                            if (idx > 0) {
                                cs.newLineAtOffset(0f, -leading)
                            }
                            cs.showText(safeForFont(font, line))
                        }
                        cs.endText()
                    }
                }
            }
            doc.save(file)
        }
    }

    private fun resolveBodyFont(doc: PDDocument): PDFont {
        val candidates = listOf(
            File("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
            File("/usr/share/fonts/dejavu/DejaVuSans.ttf"),
            File("/usr/share/fonts/TTF/DejaVuSans.ttf"),
            File("C:\\Windows\\Fonts\\arial.ttf"),
            File("/System/Library/Fonts/Supplemental/Arial.ttf"),
        )
        for (path in candidates) {
            if (path.isFile) {
                try {
                    return PDType0Font.load(doc, path)
                } catch (_: Exception) {
                    // try next candidate
                }
            }
        }
        return PDType1Font.HELVETICA
    }

    private fun safeForFont(font: PDFont, line: String): String {
        if (font is PDType0Font) {
            return line
        }
        return buildString(line.length) {
            for (ch in line) {
                when {
                    ch == '\r' -> {}
                    ch == '\n' -> {}
                    ch.code < 32 -> append(' ')
                    ch.code <= 255 -> append(ch)
                    else -> append('?')
                }
            }
        }
    }

    private fun stringWidth(font: PDFont, fontSize: Float, s: String): Float =
        font.getStringWidth(s) / 1000f * fontSize

    private fun wrapAllLines(rawLines: List<String>, font: PDFont, fontSize: Float, maxW: Float): List<String> {
        val out = ArrayList<String>(rawLines.size * 2)
        for (raw in rawLines) {
            if (raw.isEmpty()) {
                out.add("")
                continue
            }
            var rest = raw
            while (rest.isNotEmpty()) {
                if (stringWidth(font, fontSize, rest) <= maxW) {
                    out.add(rest)
                    break
                }
                var cut = rest.length
                while (cut > 0 && stringWidth(font, fontSize, rest.substring(0, cut)) > maxW) {
                    cut--
                }
                if (cut == 0) {
                    cut = 1
                }
                var br = rest.lastIndexOf(' ', min(cut, rest.length))
                if (br <= 0) {
                    br = cut
                }
                val chunk = rest.substring(0, br).trimEnd()
                out.add(chunk)
                rest = rest.substring(br).trimStart()
            }
        }
        return out
    }
}
