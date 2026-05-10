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

package games.polyclub.power.brmodelo.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolVariant
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool
import java.awt.AWTEvent
import java.awt.Cursor
import java.awt.EventQueue
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.MouseEvent
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Skiko/Compose Desktop often resets the custom pointer to the default on mouse down until the
 * next move. Mouse targets are not always classes whose simple name contains "Skia" (events may
 * hit child components under [org.jetbrains.skiko.SkiaLayer] / [org.jetbrains.skiko.HardwareLayer]).
 * Re-applying the AWT [Cursor] on press, release, and drag over that subtree fixes it.
 */
private object DesktopConceptualToolCursorFix {
    @Volatile
    var toolAwtCursor: Cursor? = null

    private fun isUnderSkikoLayer(component: java.awt.Component): Boolean {
        var c: java.awt.Component? = component
        repeat(48) {
            val cur = c ?: return@repeat
            val n = cur.javaClass.name
            if (n.contains("org.jetbrains.skiko.") &&
                (n.contains("SkiaLayer") || n.contains("HardwareLayer"))) {
                return true
            }
            c = cur.parent
        }
        return false
    }

    private fun applyCursorAlongPathToSkikoLayer(leaf: java.awt.Component, cursor: Cursor) {
        val chain = ArrayList<java.awt.Component>(24)
        var comp: java.awt.Component? = leaf
        repeat(40) {
            if (comp == null) return@repeat
            chain.add(comp)
            comp = comp.parent
        }
        val skikoIdx = chain.indexOfFirst { candidate ->
            val n = candidate.javaClass.name
            n.contains("SkiaLayer") || n.contains("HardwareLayer")
        }
        val endIdx = if (skikoIdx >= 0) skikoIdx else 0
        for (i in 0..endIdx) {
            chain[i].cursor = cursor
        }
    }

    private fun reapplyAfterSkikoMouseEvent(ev: AWTEvent) {
        val me = ev as? MouseEvent ?: return
        if (me.id != MouseEvent.MOUSE_PRESSED &&
            me.id != MouseEvent.MOUSE_RELEASED &&
            me.id != MouseEvent.MOUSE_DRAGGED
        ) {
            return
        }
        if (!isUnderSkikoLayer(me.component)) return
        val cursor = toolAwtCursor ?: Cursor.getDefaultCursor()
        fun apply() = applyCursorAlongPathToSkikoLayer(me.component, cursor)
        EventQueue.invokeLater {
            apply()
            EventQueue.invokeLater { apply() }
        }
    }

    /**
     * After the active tool changes, Compose may not refresh the native cursor until the pointer
     * moves. Applies [cursor] (or the default) on the Skiko subtree under the current screen
     * position, if any.
     */
    fun applyToolCursorUnderMouse(cursor: Cursor?) {
        fun run() {
            val c = cursor ?: Cursor.getDefaultCursor()
            val screenPt = runCatching { MouseInfo.getPointerInfo().location }.getOrNull() ?: return
            for (w in Window.getWindows()) {
                if (!w.isShowing) continue
                val local = Point(screenPt)
                SwingUtilities.convertPointFromScreen(local, w)
                if (!w.contains(local.x, local.y)) continue
                val deepest = SwingUtilities.getDeepestComponentAt(w, local.x, local.y) ?: continue
                if (!isUnderSkikoLayer(deepest)) continue
                applyCursorAlongPathToSkikoLayer(deepest, c)
                return
            }
        }
        if (EventQueue.isDispatchThread()) {
            run()
        } else {
            EventQueue.invokeLater { run() }
        }
    }

    private val listener = AWTEventListener(::reapplyAfterSkikoMouseEvent)

    private var listenerInstalled = false

    fun ensureListener() {
        synchronized(this) {
            if (!listenerInstalled) {
                val mask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
                Toolkit.getDefaultToolkit().addAWTEventListener(listener, mask)
                listenerInstalled = true
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal actual fun rememberConceptualCanvasToolCursorModifier(tool: ConceptualCanvasTool): Modifier {
    DesktopConceptualToolCursorFix.ensureListener()

    var icon by remember(tool) { mutableStateOf<PointerIcon?>(null) }
    LaunchedEffect(tool) {
        val (ptr, awt) = loadAwtPointerIconAndCursor(tool)
        icon = ptr
        DesktopConceptualToolCursorFix.toolAwtCursor = awt
        EventQueue.invokeLater {
            DesktopConceptualToolCursorFix.applyToolCursorUnderMouse(awt)
            EventQueue.invokeLater {
                DesktopConceptualToolCursorFix.applyToolCursorUnderMouse(awt)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            DesktopConceptualToolCursorFix.toolAwtCursor = null
            DesktopConceptualToolCursorFix.applyToolCursorUnderMouse(null)
        }
    }
    return icon?.let { Modifier.pointerHoverIcon(it, overrideDescendants = true) } ?: Modifier
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadAwtPointerIconAndCursor(tool: ConceptualCanvasTool): Pair<PointerIcon?, Cursor?> {
    val path = when (tool) {
        is ConceptualCanvasTool.Entity.Plain ->
            "files/brmodelo_cursors/png/cursor_entidade.png"
        is ConceptualCanvasTool.Entity.Relation ->
            "files/brmodelo_cursors/png/cursor_relacao.png"
        is ConceptualCanvasTool.Entity.Associative ->
            "files/brmodelo_cursors/png/cursor_entassoss.png"
        is ConceptualCanvasTool.Observation ->
            "files/brmodelo_cursors/png/cursor_textoii.png"
        is ConceptualCanvasTool.AutoSelfRelationship ->
            "files/brmodelo_cursors/png/cursor_autorel.png"
        is ConceptualCanvasTool.Specialization ->
            when (tool.variant) {
                ConceptualSpecializationToolVariant.Basic ->
                    "files/brmodelo_cursors/png/cursor_especializacao.png"
                ConceptualSpecializationToolVariant.ExclusiveWithEntityCreation ->
                    "files/brmodelo_cursors/png/cursor_especializacaoa.png"
                ConceptualSpecializationToolVariant.NonExclusiveWithEntityCreation ->
                    "files/brmodelo_cursors/png/cursor_especializacaob.png"
            }
        is ConceptualCanvasTool.LinkObjects.AwaitingFirst ->
            "files/brmodelo_cursors/png/cursor_ligacao.png"
        is ConceptualCanvasTool.LinkObjects.AwaitingSecond ->
            "files/brmodelo_cursors/png/cursor_ligacao2.png"
        else -> return null to null
    }
    return withContext(Dispatchers.IO) {
        runCatching {
            val bytes = Res.readBytes(path)
            val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@runCatching null to null
            val tk = Toolkit.getDefaultToolkit()
            val cursor = tk.createCustomCursor(image, java.awt.Point(0, 0), "brmodelo_tool_cursor")
            PointerIcon(cursor) to cursor
        }.getOrNull() ?: (null to null)
    }
}
