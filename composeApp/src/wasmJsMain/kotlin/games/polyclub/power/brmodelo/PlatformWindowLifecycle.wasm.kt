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

internal actual fun registerDesktopMainWindowCloseHandler(handler: (() -> Unit)?) = Unit

internal actual fun setBrowserUnloadWarningEnabled(enabled: Boolean): Unit = js(
    """
    (function(on) {
        if (!window._kbrUnloadWarn) {
            window._kbrUnloadWarn = function(e) {
                e.preventDefault();
                e.returnValue = ' ';
            };
        }
        if (on) window.addEventListener('beforeunload', window._kbrUnloadWarn);
        else window.removeEventListener('beforeunload', window._kbrUnloadWarn);
    })(enabled)
    """
)

internal actual fun quitApplicationCompletely() = Unit
