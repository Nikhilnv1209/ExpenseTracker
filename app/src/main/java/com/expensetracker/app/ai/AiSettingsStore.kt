package com.expensetracker.app.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiSettingsStore(context: Context) {

    private val masterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        "ai_settings_encrypted",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var provider: String
        get() = prefs.getString(KEY_PROVIDER, PROVIDER_OPENAI_COMPATIBLE) ?: PROVIDER_OPENAI_COMPATIBLE
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "https://api.openai.com/v1/") ?: "https://api.openai.com/v1/"
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isConfigured(): Boolean = isEnabled && apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()

    suspend fun isConfiguredAsync(): Boolean = withContext(Dispatchers.IO) { isConfigured() }

    companion object {
        const val PROVIDER_OPENAI_COMPATIBLE = "OpenAI-Compatible"
        const val KEY_ENABLED = "enabled"
        const val KEY_PROVIDER = "provider"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_API_KEY = "api_key"
    }
}
