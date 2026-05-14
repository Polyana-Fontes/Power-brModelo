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

import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class McpClasspathXmlExamplesClasspathTest {

    @Test
    fun `each mcp example xml exists on the desktop classpath`() {
        // Arrange
        val loader = McpClasspathXmlExamplesClasspathTest::class.java.classLoader

        // Act & Assert
        for (ex in mcpClasspathXmlExamples) {
            val stream = loader.getResourceAsStream(ex.classpathPath)
            assertNotNull(stream, "Missing ${ex.classpathPath} for ${ex.resourceUri}")
            val prefix = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText().take(400) }
            assertTrue(
                prefix.contains("<MER") || prefix.contains("<?xml"),
                "Expected MER root in ${ex.classpathPath}",
            )
        }
    }
}
