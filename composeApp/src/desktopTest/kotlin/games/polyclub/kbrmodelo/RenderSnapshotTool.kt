/*
 * KbrModelo - Kotlin port of brModelo 3.0 originally written in Pascal
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

package games.polyclub.kbrmodelo

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import games.polyclub.kbrmodelo.domain.serialization.ConceptualSchemaXmlParser
import games.polyclub.kbrmodelo.ui.canvas.computeSchemaBounds
import games.polyclub.kbrmodelo.ui.canvas.drawSchema
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Test-suite-driven snapshot generator for the schema renderer.
 *
 * Each `@Test` loads one of the XML fixtures from `desktopTest/resources/`,
 * renders it through [drawSchema] into an off-screen [ImageBitmap] and writes a
 * PNG to `build/render-snapshots/<fixture>.png`. The PNGs are then compared
 * (visually, by the developer or downstream tooling) against the original
 * brModelo Pascal screenshots that live under `assets/` in the project.
 *
 * The generated images are *not* asserted automatically — the tool exists so
 * we can iterate on the renderer without round-tripping through the GUI app
 * each time.
 */
class RenderSnapshotTool {

    private val outputDir = File("build/render-snapshots").apply { mkdirs() }

    /** Tight crop, white background — easy diff against the Pascal export PNG. */
    private fun render(xmlFixtureName: String, paddingPx: Float = 20f): File {
        // Arrange — load the fixture from the test classpath
        val bytes = checkNotNull(javaClass.classLoader.getResourceAsStream(xmlFixtureName)) {
            "Test resource '$xmlFixtureName' not found on classpath"
        }.readBytes()
        val schema = ConceptualSchemaXmlParser.parse(bytes)

        // Build a TextMeasurer that does not require a Compose UI context. The
        // default font family resolver is sufficient — we only use Tahoma-like
        // text and the renderer never asks for non-system fonts.
        val density = Density(density = 1f, fontScale = 1f)
        val resolver = createFontFamilyResolver()
        val textMeasurer = TextMeasurer(resolver, density, LayoutDirection.Ltr)

        val bounds = computeSchemaBounds(schema)
        val width  = (bounds.width  + 2f * paddingPx).toInt().coerceAtLeast(1)
        val height = (bounds.height + 2f * paddingPx).toInt().coerceAtLeast(1)
        val bitmap = ImageBitmap(width, height)

        // Act — draw the schema onto the off-screen bitmap
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(bitmap),
            size = Size(width.toFloat(), height.toFloat()),
        ) {
            drawRect(Color.White)
            translate(-bounds.left + paddingPx, -bounds.top + paddingPx) {
                drawSchema(schema, textMeasurer)
            }
        }

        // Persist as PNG
        val outFile = File(outputDir, xmlFixtureName.removeSuffix(".xml") + ".png")
        ImageIO.write(bitmap.toAwtImage(), "PNG", outFile)
        println("[render-snapshot] ${xmlFixtureName} -> ${outFile.absolutePath}")
        return outFile
    }

    @Test
    fun `snapshot - altamente-personalizado`() {
        // Act & Assert — generation must not throw and must produce a non-empty file.
        val out = render("altamente-personalizado.xml")
        check(out.length() > 0) { "Generated snapshot is empty: $out" }
    }

    @Test
    fun `snapshot - teste-varios-componentes`() {
        // Act & Assert
        val out = render("teste-varios-componentes.xml")
        check(out.length() > 0) { "Generated snapshot is empty: $out" }
    }

    @Test
    fun `snapshot - MER-PousadaSolDaManha`() {
        // Act & Assert
        val out = render("MER-PousadaSolDaManha.xml")
        check(out.length() > 0) { "Generated snapshot is empty: $out" }
    }

    @Test
    fun `snapshot - exemplo-simples`() {
        // Act & Assert
        val out = render("exemplo-simples.xml")
        check(out.length() > 0) { "Generated snapshot is empty: $out" }
    }
}
