package com.willykez.codeorganizer.data.providers

import com.willykez.codeorganizer.data.AiClient
import com.willykez.codeorganizer.data.AiPrompts
import com.willykez.codeorganizer.data.AiResult
import com.willykez.codeorganizer.data.runCatchingParse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Calls Google AI Studio's generateContent endpoint.
 * baseUrl example: "https://generativelanguage.googleapis.com/v1beta" — this client builds
 * "$baseUrl/models/$model:generateContent?key=$apiKey" (Gemini takes the key as a query
 * param rather than an Authorization header).
 */
class GeminiClient : AiClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun organize(
        apiKey: String,
        baseUrl: String,
        model: String,
        pastedSource: String
    ): AiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext AiResult.Failure("No API key set for Gemini. Add one in Settings.")

        try {
            val body = buildJsonObject {
                putJsonObject("system_instruction") {
                    putJsonArray("parts") {
                        addJsonObject { put("text", AiPrompts.SYSTEM) }
                    }
                }
                putJsonArray("contents") {
                    addJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            addJsonObject { put("text", pastedSource) }
                        }
                    }
                }
            }.toString()

            val url = "${baseUrl.trimEnd('/')}/models/$model:generateContent".toHttpUrl()
                .newBuilder()
                .addQueryParameter("key", apiKey)
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext AiResult.Failure(
                        "Gemini API error ${response.code}: ${extractError(bodyText) ?: bodyText.take(300)}"
                    )
                }
                runCatchingParse(extractText(bodyText))
            }
        } catch (e: Exception) {
            AiResult.Failure("Request to Gemini failed: ${e.message}")
        }
    }

    private fun extractText(rawBody: String): String? {
        val obj = json.parseToJsonElement(rawBody).jsonObject
        val candidates = obj["candidates"]?.jsonArray ?: return null
        val firstContent = candidates.firstOrNull()?.jsonObject?.get("content")?.jsonObject ?: return null
        val parts = firstContent["parts"]?.jsonArray ?: return null
        val sb = StringBuilder()
        for (part in parts) {
            sb.append(part.jsonObject["text"]?.jsonPrimitive?.content.orEmpty())
        }
        return sb.toString().ifBlank { null }
    }

    private fun extractError(rawBody: String): String? = try {
        json.parseToJsonElement(rawBody).jsonObject["error"]
            ?.jsonObject?.get("message")?.jsonPrimitive?.content
    } catch (e: Exception) {
        null
    }
}
