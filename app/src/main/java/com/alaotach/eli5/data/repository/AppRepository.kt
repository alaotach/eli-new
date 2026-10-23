package com.alaotach.eli5.data.repository

import com.alaotach.eli5.data.local.HistoryDao
import com.alaotach.eli5.data.local.HistoryEntity
import com.alaotach.eli5.data.model.ChatRequest
import com.alaotach.eli5.data.model.Message
import com.alaotach.eli5.data.remote.OpenRouterApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import com.alaotach.eli5.data.model.StreamResponse

class AppRepository(
    private val api: OpenRouterApi,
    private val historyDao: HistoryDao
) {
    suspend fun getExplanation(text: String, mode: String): String {
        return withContext(Dispatchers.IO) {
            val systemPrompt = when (mode) {
                "ELI5" -> "You are an expert at explaining complex topics simply. Explain the following text as if I am 5 years old. Use analogies and simple language. Only output the explanation, do not include pleasantries."
                "Direct" -> "You are a concise assistant. Provide a direct, factual, and brief summary or definition of the following text. Do not add filler."
                "Detailed" -> "You are a knowledgeable assistant. Provide a detailed explanation of the following text, including context, examples, and nuances."
                "Proofread" -> "You are a professional proofreader. Fix any grammatical, spelling, or stylistic errors in the following text. Output ONLY the corrected text. If no corrections are needed, output the original text."
                else -> "Explain the following text."
            }

            val request = ChatRequest(
                model = "openrouter/owl-alpha",
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = text)
                )
            )

            try {
                val response = api.getCompletion(
                    request = request
                )
                
                val resultText = response.choices.firstOrNull()?.message?.content ?: "Could not generate a response."
                
                // Save to history
                historyDao.insertHistory(
                    HistoryEntity(
                        sourceText = text,
                        resultText = resultText,
                        type = mode,
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                resultText
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
            }
        }
    }

    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun getExplanationStream(text: String, mode: String): Flow<String> = flow {
        val systemPrompt = when (mode) {
            "ELI5" -> "You are an expert at explaining complex topics simply. Explain the following text as if I am 5 years old. Use analogies and simple language. Only output the explanation, do not include pleasantries."
            "Direct" -> "You are a concise assistant. Provide a direct, factual, and brief summary or definition of the following text. Do not add filler."
            "Detailed" -> "You are a knowledgeable assistant. Provide a detailed explanation of the following text, including context, examples, and nuances."
            "Proofread" -> "You are a professional proofreader. Fix any grammatical, spelling, or stylistic errors in the following text. Output ONLY the corrected text. If no corrections are needed, output the original text."
            else -> "Explain the following text."
        }

        val request = ChatRequest(
            model = "openrouter/owl-alpha",
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = text)
            ),
            stream = true
        )

        try {
            val responseBody = api.getCompletionStream(request)
            val reader = responseBody.charStream().buffered()
            var result = ""
            
            reader.useLines { lines ->
                for (line in lines) {
                    if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                        val jsonStr = line.substring(6)
                        try {
                            val streamResponse = jsonParser.decodeFromString<StreamResponse>(jsonStr)
                            val chunk = streamResponse.choices.firstOrNull()?.delta?.content ?: ""
                            result += chunk
                            emit(result)
                        } catch (e: Exception) {
                            // Ignore parse errors on partial streams
                        }
                    }
                }
            }
            
            // Save to history
            historyDao.insertHistory(
                HistoryEntity(
                    sourceText = text,
                    resultText = result,
                    type = mode,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emit("Error: ${e.message}")
        }
    }
}
