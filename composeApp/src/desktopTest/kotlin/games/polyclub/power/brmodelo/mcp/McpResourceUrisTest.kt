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

import games.polyclub.power.brmodelo.ui.EditorTabSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class McpResourceUrisTest {

    @Test
    fun `modelResourceUriForSession ends with xml suffix`() {
        // Arrange
        val sessionId = 42L

        // Act
        val uri = modelResourceUriForSession(sessionId)

        // Assert
        assertEquals("brmodelo://model/42.xml", uri)
    }

    @Test
    fun `tabIndexForModelResourceUri resolves session id form`() {
        // Arrange
        val s0 = EditorTabSession.blank(10L)
        val s1 = EditorTabSession.blank(20L)
        val sessions = listOf(s0, s1)
        val uri = modelResourceUriForSession(20L)

        // Act
        val index = tabIndexForModelResourceUri(uri, sessions)

        // Assert
        assertEquals(1, index)
    }

    @Test
    fun `tabIndexForModelResourceUri accepts legacy list index without suffix`() {
        // Arrange
        val s0 = EditorTabSession.blank(10L)
        val s1 = EditorTabSession.blank(20L)
        val sessions = listOf(s0, s1)
        val uri = "brmodelo://model/1"

        // Act
        val index = tabIndexForModelResourceUri(uri, sessions)

        // Assert
        assertEquals(1, index)
    }

    @Test
    fun `tabIndexForModelResourceUri parses path suffix and query before first slash`() {
        // Arrange
        val s0 = EditorTabSession.blank(7L)
        val sessions = listOf(s0)
        val uri = "brmodelo://model/7.xml/extra?x=1"

        // Act
        val index = tabIndexForModelResourceUri(uri, sessions)

        // Assert
        assertEquals(0, index)
    }

    @Test
    fun `tabIndexForModelResourceUri returns null for unknown session id`() {
        // Arrange
        val sessions = listOf(EditorTabSession.blank(1L))
        val uri = modelResourceUriForSession(999L)

        // Act
        val index = tabIndexForModelResourceUri(uri, sessions)

        // Assert
        assertNull(index)
    }

    @Test
    fun `returns null for unrelated uri`() {
        // Arrange
        val uri = "file:///tmp/model.xml"
        val sessions = listOf(EditorTabSession.blank(1L))

        // Act
        val index = tabIndexForModelResourceUri(uri, sessions)

        // Assert
        assertNull(index)
    }

    @Test
    fun `mcpCreatedTabIndexAfterOpen picks new session index when appended`() {
        // Arrange
        val before = listOf(EditorTabSession.blank(1L))
        val after = listOf(EditorTabSession.blank(1L), EditorTabSession.blank(2L))

        // Act
        val created = mcpCreatedTabIndexAfterOpen(before, after, selectedAfter = 1)

        // Assert
        assertEquals(1, created)
    }

    @Test
    fun `mcpCreatedTabIndexAfterOpen falls back to selected when no new session id`() {
        // Arrange
        val tab = EditorTabSession.blank(5L)
        val before = listOf(tab)
        val after = listOf(tab)

        // Act
        val created = mcpCreatedTabIndexAfterOpen(before, after, selectedAfter = 0)

        // Assert
        assertEquals(0, created)
    }

    @Test
    fun `conceptual mer dtd resource uri is stable`() {
        // Act
        val uri = conceptualMerDtdResourceUri()

        // Assert
        assertEquals("brmodelo://schema/conceptual-mer.dtd", uri)
    }
}
