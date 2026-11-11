package com.alaotach.eli5

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ExplainRepository {

    suspend fun fetchExplanation(
        text: String,
        mode: String = "ELI5",
        language: String = "English",
        extraModifier: String? = null,
        isFromSnip: Boolean = false,
        onChunk: suspend (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        try {
            val isProofread = mode.startsWith("Proofread", ignoreCase = true)
            val endpoint = if (isProofread) {
                "${BuildConfig.BACKEND_URL}/api/proofread"
            } else {
                "${BuildConfig.BACKEND_URL}/api/explain"
            }
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 ELI5-Android/2.2.1")
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doOutput = true

            val payload = if (isProofread) {
                val instruction = extraModifier ?: if (mode.contains("(") && mode.contains(")")) {
                    mode.substringAfter("(").substringBefore(")").trim()
                } else {
                    "Fix grammar, improve clarity, make it sound professional while keeping original meaning"
                }
                JSONObject().apply {
                    put("text", text)
                    put("instructions", instruction)
                    if (!language.equals("English", ignoreCase = true) && !language.equals("Auto", ignoreCase = true)) {
                        put("language", language)
                    }
                }
            } else {
                val resolvedMode = when (mode) {
                    "Direct" -> "Straightforward"
                    "Detailed" -> "Detailed"
                    else -> "ELI5"
                }

                var promptText = text
                if (isFromSnip) {
                    promptText = """
                        [Context: The user selected this text by circling/snipping on their mobile screen.]
                        [Instruction: Focus exclusively on explaining the central entity, concept, punchline, jargon, or question the user circled. Disregard any peripheral timestamps, battery/status text, buttons, or accidental edge clippings. Give a clear, direct, and insightful explanation.]

                        $text
                    """.trimIndent()
                }
                if (!extraModifier.isNullOrEmpty()) {
                    promptText = "$promptText\n\n[$extraModifier]"
                }
                if (language.equals("Hinglish", ignoreCase = true)) {
                    promptText = "$promptText\n\n[Please explain in natural, casual Hinglish (Hindi written using the English/Latin alphabet, mixing everyday conversational Hindi and English words naturally, e.g. 'Yeh concept bohot simple hai, basically...'). Do NOT use Devanagari script; write entirely in Latin/English letters.]"
                } else if (!language.equals("English", ignoreCase = true)) {
                    promptText = "$promptText\n\n[Please explain in clear, natural $language]"
                }

                JSONObject().apply {
                    put("text", promptText)
                    put("mode", resolvedMode)
                    put("language", language)
                }
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val fullResponse = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        try {
                            val json = JSONObject(data)
                            val delta = json.getJSONArray("choices").getJSONObject(0).getJSONObject("delta")
                            val content = delta.optString("content", "")
                            if (content.isNotEmpty()) {
                                fullResponse.append(content)
                                onChunk(fullResponse.toString())
                            }
                        } catch (_: Exception) {}
                    }
                }
                reader.close()
                val result = fullResponse.toString().trim()
                if (result.isNotEmpty()) result else "Could not generate an explanation. Please try again."
            } else {
                "Error: Server returned HTTP ${conn.responseCode}. Please check your connection."
            }
        } catch (e: Exception) {
            "Network error: ${e.localizedMessage ?: "Unable to connect"}. Please try again."
        }
    }
}
