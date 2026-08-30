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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Calls the OpenAI-compatible `/chat/completions` endpoint. This same shape (Bearer auth,
 * {"messages":[...]} request, {"choices":[{"message":{"content":...}}]} response) is used
 * as-is by OpenAI, DeepSeek, and Qwen's DashScope "compatible mode" — so one client covers
 * all three; only [providerLabel] (for error messages) and baseUrl/model differ.
 *
 * baseUrl example: "https://api.openai.com/v1" — this client appends "/chat/completions".
 *
 * NOTE: some newer reasoning-style models on these providers expect
 * "max_completion_tokens" instead of "max_tokens". If a chosen model rejects this request,
 * that's the first thing to check.
 */
class OpenAiCompatibleClient(private val providerLabel: String) : AiClient {

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
        if (apiKey.isBlank()) return@withContext AiResult.Failure("No API key set for $providerLabel. Add one in Settings.")

        try {
            val body = buildJsonObject {
                put("model", model)
                put("max_tokens", 8192)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "system")
                        put("content", AiPrompts.SYSTEM)
                    }
                    addJsonObject {
                        put("role", "user")
                        put("content", pastedSource)
                    }
                }
            }.toString()

            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext AiResult.Failure(
                        "$providerLabel API error ${response.code}: ${extractError(bodyText) ?: bodyText.take(300)}"
                    )
                }
                runCatchingParse(extractText(bodyText))
            }
        } catch (e: Exception) {
            AiResult.Failure("Request to $providerLabel failed: ${e.message}")
        }
    }

    private fun extractText(rawBody: String): String? {
        val obj = json.parseToJsonElement(rawBody).jsonObject
        val choices = obj["choices"]?.jsonArray ?: return null
        val first = choices.firstOrNull()?.jsonObject ?: return null
        return first["message"]?.jsonObject?.get("content")?.jsonPrimitive?.content
    }

    private fun extractError(rawBody: String): String? = try {
        json.parseToJsonElement(rawBody).jsonObject["error"]
            ?.jsonObject?.get("message")?.jsonPrimitive?.content
    } catch (e: Exception) {
        null
    }
}
