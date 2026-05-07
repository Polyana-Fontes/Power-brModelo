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

package games.polyclub.kbrmodelo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun MainCanvasPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .border(1.dp, Color(0xFF666666))
            .background(Color(0xFFD7D7D7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(Color(0xFFF4F4F4))
                .border(1.dp, Color(0xFFC6C6C6))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Modelos abertos", fontSize = 12.sp, color = Color(0xFF2D2D2D))
            Spacer(modifier = Modifier.width(14.dp))
            Text("▾", fontSize = 11.sp, color = Color(0xFF4D4D4D))
            Spacer(modifier = Modifier.width(20.dp))
            Text("Localizar objeto", fontSize = 12.sp, color = Color(0xFF2D2D2D))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .border(1.dp, Color(0xFF7A7A7A))
                .background(Color(0xFFEDEDED)),
            contentAlignment = Alignment.Center
        ) {
            Text("Área de modelagem (visual)", color = Color(0xFF444444))
        }
    }
}
