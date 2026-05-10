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

package games.polyclub.power.brmodelo.domain

/**
 * Builds hover-tooltip text for [SchemaElement.hiddenAttributes]: a header line plus a UTF-8 tree
 * of names only (same branch order as [HiddenAttribute.branchAt]: [HiddenAttribute.children] then
 * [HiddenAttribute.nestedHiddenAttributes]).
 */
fun hiddenAttributesTooltipText(roots: List<HiddenAttribute>): String? {
    if (roots.isEmpty()) return null
    val body = buildString {
        roots.forEachIndexed { index, node ->
            appendTreeLinesForNode(
                node,
                ancestorPrefix = "",
                isLastSibling = index == roots.lastIndex,
                out = this,
            )
        }
    }.trimEnd()
    return "Atributos Ocultos:\n$body"
}

private fun mergedBranches(node: HiddenAttribute): List<HiddenAttribute> =
    node.children + node.nestedHiddenAttributes

private fun appendTreeLinesForNode(
    node: HiddenAttribute,
    ancestorPrefix: String,
    isLastSibling: Boolean,
    out: StringBuilder,
) {
    val connector = if (isLastSibling) "└── " else "├── "
    out.append(ancestorPrefix).append(connector).append(node.name).append('\n')
    val branches = mergedBranches(node)
    if (branches.isEmpty()) return
    val childPrefix = ancestorPrefix + if (isLastSibling) "    " else "│   "
    branches.forEachIndexed { i, child ->
        appendTreeLinesForNode(
            child,
            childPrefix,
            isLastSibling = i == branches.lastIndex,
            out,
        )
    }
}
