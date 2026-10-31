package com.alaotach.eli5

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TextProcessorViewModel : ViewModel() {
    private val _processingState: MutableStateFlow<ProcessingState> = MutableStateFlow(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    fun processText(text: String, action: TextAction) {
        _processingState.value = ProcessingState.Loading
        try {
            val result = when (action) {
                TextAction.EXPLAIN -> explainText(text)
                TextAction.SIMPLIFY -> simplifyText(text)
                TextAction.SUMMARIZE -> summarizeText(text)
                TextAction.TRANSLATE -> translateText(text)
                TextAction.KEY_POINTS -> extractKeyPoints(text)
            }
            _processingState.value = ProcessingState.Success(result)
        } catch (e: Exception) {
            _processingState.value = ProcessingState.Error(e.message ?: "Unknown error")
        }
    }

    fun resetState() {
        _processingState.value = ProcessingState.Idle
    }

    private fun explainText(text: String): String {
        val wordCount = text.split(" ").size
        return buildString {
            append("📚 Explanation\n\n")
            append("This text discusses: \"${text.take(100)}${if (text.length > 100) "..." else ""}\"\n\n")
            append("Key Concepts:\n")
            append("• This is a demo explanation that would normally come from an AI model\n")
            append("• In production, integrate with ML Kit, Gemini API, or your preferred solution\n")
            append("• The explanation breaks down complex concepts into understandable parts\n\n")
            append("Context:\n")
            append("The original text appears to be ${detectTextType(text)}. It contains $wordCount words and discusses topics that would benefit from detailed explanation.\n")
        }
    }

    private fun simplifyText(text: String): String {
        val wordCount = text.split(" ").size
        return buildString {
            append("✨ Simplified Version\n\n")
            append("Original text ($wordCount words):\n")
            append("\"${text.take(150)}${if (text.length > 150) "..." else ""}\"\n\n")
            append("In simple terms:\n")
            append("This is a plain-language version of the text. In production, an AI model would convert complex jargon and technical terms into everyday language that anyone can understand.\n\n")
            append("💡 Tip: The simplified version maintains the core meaning while using common words.\n")
        }
    }

    private fun summarizeText(text: String): String {
        val estLength = (text.length * 0.3).toInt()
        return buildString {
            append("📝 Summary\n\n")
            append("Main Points:\n")
            append("• This is a concise summary of the original text\n")
            append("• In production, AI would identify the most important information\n")
            append("• Only essential details would be preserved\n\n")
            append("Original length: ${text.length} characters\n")
            append("Summary length: ~$estLength characters (70% reduction)\n\n")
            append("The summary captures the essence while removing redundant information.\n")
        }
    }

    private fun translateText(text: String): String {
        return buildString {
            append("🌍 Translation\n\n")
            append("Original (detected: English):\n")
            append("\"${text.take(100)}${if (text.length > 100) "..." else ""}\"\n\n")
            append("Translated to Spanish (demo):\n")
            append("\"Esta es una traducción de demostración. En producción, integraría ML Kit Translation API o Google Cloud Translation.\"\n\n")
            append("Available languages: In production, support 100+ languages with on-device or cloud translation.\n")
        }
    }

    private fun extractKeyPoints(text: String): String {
        val words = text.split(" ")
        val wordCount = words.size
        val readingTime = wordCount / 200
        return buildString {
            append("🎯 Key Points\n\n")
            append("1. Main Idea\n")
            append("   The primary concept or argument presented in the text\n\n")
            append("2. Supporting Details\n")
            append("   Important facts or examples that back up the main idea\n\n")
            append("3. Conclusion\n")
            append("   The final takeaway or call to action\n\n")
            append("📊 Analysis:\n")
            append("• Word count: $wordCount\n")
            append("• Text type: ${detectTextType(text)}\n")
            append("• Reading time: ~$readingTime min\n\n")
            append("In production, AI would intelligently extract the actual key points from your specific text.\n")
        }
    }

    private fun detectTextType(text: String): String {
        return when {
            text.contains("http") || text.contains("www") -> "web content or link"
            text.contains("?") && text.length < 200 -> "question or query"
            text.contains("\n\n") -> "article or document"
            text.split(" ").size < 20 -> "short message or snippet"
            else -> "general text"
        }
    }
}
