package com.willykez.codeorganizer.data

import com.willykez.codeorganizer.data.providers.AnthropicClient
import com.willykez.codeorganizer.data.providers.GeminiClient
import com.willykez.codeorganizer.data.providers.OpenAiCompatibleClient
import com.willykez.codeorganizer.model.AiProvider

object AiClientFactory {
    fun create(provider: AiProvider): AiClient = when (provider) {
        AiProvider.ANTHROPIC -> AnthropicClient()
        AiProvider.OPENAI -> OpenAiCompatibleClient(providerLabel = "ChatGPT")
        AiProvider.DEEPSEEK -> OpenAiCompatibleClient(providerLabel = "DeepSeek")
        AiProvider.QWEN -> OpenAiCompatibleClient(providerLabel = "Qwen")
        AiProvider.GEMINI -> GeminiClient()
    }
}
