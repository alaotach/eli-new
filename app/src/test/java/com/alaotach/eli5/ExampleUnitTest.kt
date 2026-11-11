package com.alaotach.eli5

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTtsHelperCleansMarkdownFormatting() {
        val markdown = """
            # Heading 1
            ### Subheading
            **Bold Text** and *Italic Text* and ***Bold Italic***.
            * Bullet point 1
            * Bullet point 2 with `code snippet`
            - Dash bullet
            1. Numbered item
            Check [this link](https://example.com/guide) for more.
            > Blockquote here
            ~~Strikethrough text~~
            ---
            Table: | col1 | col2 |
        """.trimIndent()

        val cleaned = TtsHelper.cleanTextForSpeech(markdown)

        assertFalse("Should not contain asterisks", cleaned.contains("*"))
        assertFalse("Should not contain hashtags", cleaned.contains("#"))
        assertFalse("Should not contain backticks", cleaned.contains("`"))
        assertFalse("Should not contain brackets", cleaned.contains("[") || cleaned.contains("]"))
        assertFalse("Should not contain URLs", cleaned.contains("https://"))
        assertFalse("Should not contain blockquote symbols", cleaned.contains(">"))
        assertFalse("Should not contain tildes", cleaned.contains("~"))
        assertFalse("Should not contain table pipes", cleaned.contains("|"))

        assertTrue("Should contain bold text content", cleaned.contains("Bold Text"))
        assertTrue("Should contain italic text content", cleaned.contains("Italic Text"))
        assertTrue("Should contain bullet text", cleaned.contains("Bullet point 1"))
        assertTrue("Should contain code content", cleaned.contains("code snippet"))
        assertTrue("Should contain link anchor text", cleaned.contains("this link"))
    }
}