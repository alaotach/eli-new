package com.alaotach.eli5

enum class TextAction {
    EXPLAIN,
    SIMPLIFY,
    SUMMARIZE,
    TRANSLATE,
    KEY_POINTS;

    fun getDisplayName(): String = when (this) {
        EXPLAIN -> "Explain"
        SIMPLIFY -> "Simplify"
        SUMMARIZE -> "Summarize"
        TRANSLATE -> "Translate"
        KEY_POINTS -> "Key Points"
    }

    fun getDescription(): String = when (this) {
        EXPLAIN -> "Get a detailed explanation"
        SIMPLIFY -> "Convert to plain language"
        SUMMARIZE -> "Get a brief summary"
        TRANSLATE -> "Translate to another language"
        KEY_POINTS -> "Extract main points"
    }
}
