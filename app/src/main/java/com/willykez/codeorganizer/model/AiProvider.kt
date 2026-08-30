package com.willykez.codeorganizer.model

/**
 * One selectable AI backend. [defaultBaseUrl] already includes whatever path prefix
 * that provider's client expects to append its final path segment to — see the
 * matching class under data/providers for the exact endpoint each one builds.
 *
 * [sampleModels] are shown as tap-to-fill suggestions in Settings, NOT a locked list —
 * model names/versions change often, so the model field always stays freely editable.
 */
enum class AiProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val sampleModels: List<String>,
    val apiKeyHelp: String
) {
    ANTHROPIC(
        displayName = "Claude (Anthropic)",
        defaultBaseUrl = "https://api.anthropic.com",
        defaultModel = "claude-sonnet-5",
        sampleModels = listOf(
            "claude-opus-5", "claude-sonnet-5", "claude-haiku-4-5-20251001", "claude-fable-5"
        ),
        apiKeyHelp = "From console.anthropic.com"
    ),
    OPENAI(
        displayName = "ChatGPT (OpenAI)",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4.1",
        sampleModels = listOf("gpt-4.1", "gpt-4.1-mini", "gpt-4o", "o4-mini"),
        apiKeyHelp = "From platform.openai.com/api-keys"
    ),
    GEMINI(
        displayName = "Gemini (Google AI Studio)",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "gemini-2.5-flash",
        sampleModels = listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.0-flash"),
        apiKeyHelp = "From aistudio.google.com/app/apikey"
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        sampleModels = listOf("deepseek-chat", "deepseek-reasoner"),
        apiKeyHelp = "From platform.deepseek.com"
    ),
    QWEN(
        displayName = "Qwen (Alibaba DashScope)",
        defaultBaseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
        defaultModel = "qwen-plus",
        sampleModels = listOf("qwen-max", "qwen-plus", "qwen-turbo"),
        apiKeyHelp = "From dashscope.console.aliyun.com " +
            "(mainland China accounts may need the non-intl dashscope.aliyuncs.com base URL instead)"
    )
}
