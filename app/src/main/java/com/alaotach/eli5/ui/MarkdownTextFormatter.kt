package com.alaotach.eli5.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

object MarkdownTextFormatter {

    fun formatMarkdown(text: String, primaryColor: Color, accentColor: Color): AnnotatedString {
        return buildAnnotatedString {
            val lines = text.split("\n")
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()

                when {
                    // Headings: ### or ## or #
                    trimmed.startsWith("### ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor)) {
                            append(trimmed.removePrefix("### "))
                        }
                    }
                    trimmed.startsWith("## ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = accentColor)) {
                            append(trimmed.removePrefix("## "))
                        }
                    }
                    trimmed.startsWith("# ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accentColor)) {
                            append(trimmed.removePrefix("# "))
                        }
                    }
                    // Bullet points: * or -
                    trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                        append("  •  ")
                        formatInline(trimmed.substring(2), primaryColor, accentColor)
                    }
                    // Numbered lists: 1. or 2.
                    trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                        val num = trimmed.substringBefore(".")
                        val rest = trimmed.substringAfter(".").trim()
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accentColor)) {
                            append("$num. ")
                        }
                        formatInline(rest, primaryColor, accentColor)
                    }
                    else -> {
                        formatInline(line, primaryColor, accentColor)
                    }
                }

                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    private fun AnnotatedString.Builder.formatInline(line: String, primaryColor: Color, accentColor: Color) {
        // Parse bold: **bold text**
        val regex = Regex("\\*\\*(.*?)\\*\\*")
        var cursor = 0
        val matches = regex.findAll(line)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > cursor) {
                withStyle(SpanStyle(color = primaryColor)) {
                    append(line.substring(cursor, start))
                }
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor)) {
                append(match.groupValues[1])
            }
            cursor = end
        }

        if (cursor < line.length) {
            withStyle(SpanStyle(color = primaryColor)) {
                append(line.substring(cursor))
            }
        }
    }
}
