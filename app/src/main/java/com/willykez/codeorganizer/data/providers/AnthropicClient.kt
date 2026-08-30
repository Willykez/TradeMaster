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

/** Calls Anthropic's /v1/messages endpoint. baseUrl example: "https://api.anthropic.com" */
class AnthropicClient : AiClient {

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
        if (apiKey.isBlank()) return@withContext AiResult.Failure("No API key set for Claude. Add one in Settings.")

        try {
            val body = buildJsonObject {
                put("model", model)
                put("max_tokens", 8192)
                put("system", AiPrompts.SYSTEM)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", pastedSource)
                    }
                }
            }.toString()

            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext AiResult.Failure(
                        "Claude API error ${response.code}: ${extractError(bodyText) ?: bodyText.take(300)}"
                    )
                }
                runCatchingParse(extractText(bodyText))
            }
        } catch (e: Exception) {
            AiResult.Failure("Request to Claude failed: ${e.message}")
        }
    }

    private fun extractText(rawBody: String): String? {
        val obj = json.parseToJsonElement(rawBody).jsonObject
        val content = obj["content"]?.jsonArray ?: return null
        val sb = StringBuilder()
        for (block in content) {
            val blockObj = block.jsonObject
            if (blockObj["type"]?.jsonPrimitive?.content == "text") {
                sb.append(blockObj["text"]?.jsonPrimitive?.content.orEmpty())
            }
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
