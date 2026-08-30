package com.willykez.codeorganizer.data

import com.willykez.codeorganizer.model.ParsedProject

sealed class AiResult {
    data class Success(val project: ParsedProject) : AiResult()
    data class Failure(val message: String) : AiResult()
}

/** Implemented once per provider family — Anthropic, OpenAI-compatible, and Gemini. */
interface AiClient {
    suspend fun organize(
        apiKey: String,
        baseUrl: String,
        model: String,
        pastedSource: String
    ): AiResult
}
