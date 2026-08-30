package com.willykez.codeorganizer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.willykez.codeorganizer.model.AiProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "code_organizer_settings")

data class ProviderSettings(
    val apiKey: String,
    val model: String,
    val baseUrl: String
)

/**
 * Holds everything Settings screen edits, per provider — switching providers doesn't
 * lose the key/model/base URL you had set for the others.
 *
 * NOTE on API keys: these are stored in plain DataStore preferences for simplicity.
 * For anything beyond personal/local use, swap this for EncryptedSharedPreferences /
 * the Jetpack Security library so keys aren't sitting in a plaintext file on disk.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val TREE_URI = stringPreferencesKey("tree_uri")
        fun apiKey(p: AiProvider) = stringPreferencesKey("apikey_${p.name}")
        fun model(p: AiProvider) = stringPreferencesKey("model_${p.name}")
        fun baseUrl(p: AiProvider) = stringPreferencesKey("baseurl_${p.name}")
    }

    suspend fun getSelectedProvider(): AiProvider {
        val name = context.dataStore.data.map { it[Keys.SELECTED_PROVIDER] }.first()
        return AiProvider.entries.find { it.name == name } ?: AiProvider.ANTHROPIC
    }

    suspend fun saveSelectedProvider(provider: AiProvider) {
        context.dataStore.edit { it[Keys.SELECTED_PROVIDER] = provider.name }
    }

    suspend fun getProviderSettings(provider: AiProvider): ProviderSettings {
        val prefs = context.dataStore.data.first()
        return ProviderSettings(
            apiKey = prefs[Keys.apiKey(provider)] ?: "",
            model = prefs[Keys.model(provider)] ?: provider.defaultModel,
            baseUrl = prefs[Keys.baseUrl(provider)] ?: provider.defaultBaseUrl
        )
    }

    suspend fun saveApiKey(provider: AiProvider, key: String) {
        context.dataStore.edit { it[Keys.apiKey(provider)] = key.trim() }
    }

    suspend fun saveModel(provider: AiProvider, model: String) {
        context.dataStore.edit { it[Keys.model(provider)] = model.trim() }
    }

    suspend fun saveBaseUrl(provider: AiProvider, baseUrl: String) {
        context.dataStore.edit { it[Keys.baseUrl(provider)] = baseUrl.trim() }
    }

    val treeUri = context.dataStore.data.map { it[Keys.TREE_URI] ?: "" }

    suspend fun saveTreeUri(uri: String) {
        context.dataStore.edit { it[Keys.TREE_URI] = uri }
    }
}
