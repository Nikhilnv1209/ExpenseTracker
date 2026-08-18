package com.expensetracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TableAlign { LEFT, CENTER, RIGHT }

sealed class MarkdownBlock {
    data class Text(val content: String) : MarkdownBlock()
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val alignments: List<TableAlign>,
    ) : MarkdownBlock()
}

fun splitMarkdownByTables(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0
    val textBuffer = StringBuilder()

    while (i < lines.size) {
        val line = lines[i]
        val nextLine = lines.getOrNull(i + 1)

        if (isTableLine(line) && nextLine != null && isSeparatorLine(nextLine)) {
            if (textBuffer.isNotBlank()) {
                blocks.add(MarkdownBlock.Text(textBuffer.toString().trimEnd()))
                textBuffer.clear()
            }
            val headers = parseTableRow(line)
            val alignments = parseAlignments(nextLine, headers.size)
            val rows = mutableListOf<List<String>>()
            i += 2
            while (i < lines.size && isTableLine(lines[i])) {
                val row = parseTableRow(lines[i])
                if (row.isNotEmpty()) rows.add(row)
                i++
            }
            blocks.add(MarkdownBlock.Table(headers, rows, alignments))
        } else {
            if (textBuffer.isNotEmpty()) textBuffer.append('\n')
            textBuffer.append(line)
            i++
        }
    }

    if (textBuffer.isNotBlank()) {
        blocks.add(MarkdownBlock.Text(textBuffer.toString().trimEnd()))
    }

    return blocks
}

private fun isTableLine(line: String): Boolean {
    val trimmed = line.trim()
    if (!trimmed.startsWith("|")) return false
    return trimmed.count { it == '|' } >= 2
}

private fun isSeparatorLine(line: String): Boolean {
    val trimmed = line.trim()
    if (!trimmed.startsWith("|")) return false
    val cells = trimmed.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
    return cells.isNotEmpty() && cells.all { cell ->
        val c = cell.replace(":", "")
        c.isNotEmpty() && c.all { it == '-' }
    }
}

private fun parseTableRow(line: String): List<String> {
    val trimmed = line.trim()
    return trimmed
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { it.trim() }
}

private fun parseAlignments(separatorLine: String, columnCount: Int): List<TableAlign> {
    val cells = separatorLine.trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { it.trim() }
    return (0 until columnCount).map { idx ->
        val cell = cells.getOrNull(idx) ?: ""
        when {
            cell.startsWith(":") && cell.endsWith(":") -> TableAlign.CENTER
            cell.endsWith(":") -> TableAlign.RIGHT
            else -> TableAlign.LEFT
        }
    }
}

private fun TableAlign.toTextAlign(): TextAlign = when (this) {
    TableAlign.LEFT -> TextAlign.Start
    TableAlign.CENTER -> TextAlign.Center
    TableAlign.RIGHT -> TextAlign.End
}

@Composable
fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    alignments: List<TableAlign>,
    modifier: Modifier = Modifier,
) {
    val columnCount = headers.size
    val headerStyle = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.95f),
    )
    val cellStyle = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Color.White.copy(alpha = 0.85f),
    )
    val rowDivider = Color.White.copy(alpha = 0.06f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF15121F)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            headers.forEachIndexed { idx, cell ->
                Text(
                    text = cell,
                    style = headerStyle,
                    textAlign = alignments.getOrNull(idx)?.toTextAlign() ?: TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.12f)),
        )

        rows.forEachIndexed { rowIdx, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                (0 until columnCount).forEach { colIdx ->
                    Text(
                        text = row.getOrNull(colIdx) ?: "",
                        style = cellStyle,
                        textAlign = alignments.getOrNull(colIdx)?.toTextAlign() ?: TextAlign.Start,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (rowIdx < rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(rowDivider),
                )
            }
        }
    }
}
