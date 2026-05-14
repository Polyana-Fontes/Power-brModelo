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

import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userRoot().node("games/polyclub/power/brmodelo/mcp")

private const val PREF_BIND = "bindHost"
private const val PREF_PORT = "port"
private const val PREF_ALLOW_LAN_HOSTS = "allowLanHosts"

internal actual object McpSettingsStore {
    actual fun load(): Triple<String, Int, Boolean> {
        val host = prefs.get(PREF_BIND, "127.0.0.1").trim().ifBlank { "127.0.0.1" }
        val port = prefs.getInt(PREF_PORT, 8765).coerceIn(1, 65535)
        val allow = prefs.getBoolean(PREF_ALLOW_LAN_HOSTS, false)
        return Triple(host, port, allow)
    }

    actual fun save(bindHost: String, port: Int, allowLanHosts: Boolean) {
        prefs.put(PREF_BIND, bindHost.trim().ifBlank { "127.0.0.1" })
        prefs.putInt(PREF_PORT, port.coerceIn(1, 65535))
        prefs.putBoolean(PREF_ALLOW_LAN_HOSTS, allowLanHosts)
        prefs.flush()
    }
}
