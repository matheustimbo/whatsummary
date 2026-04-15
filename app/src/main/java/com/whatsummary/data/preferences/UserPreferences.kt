package com.whatsummary.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "whatsummary_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var summaryTimeHour: Int
        get() = prefs.getInt(KEY_SUMMARY_HOUR, 22)
        set(value) = prefs.edit().putInt(KEY_SUMMARY_HOUR, value).apply()

    var summaryTimeMinute: Int
        get() = prefs.getInt(KEY_SUMMARY_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_SUMMARY_MINUTE, value).apply()

    var llmModel: String
        get() = prefs.getString(KEY_LLM_MODEL, MODEL_HAIKU) ?: MODEL_HAIKU
        set(value) = prefs.edit().putString(KEY_LLM_MODEL, value).apply()

    var retentionDays: Int
        get() = prefs.getInt(KEY_RETENTION_DAYS, 30)
        set(value) = prefs.edit().putInt(KEY_RETENTION_DAYS, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    val dbPassphrase: String
        get() {
            val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
            if (existing != null) return existing
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DB_PASSPHRASE, generated).apply()
            return generated
        }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SUMMARY_HOUR = "summary_hour"
        private const val KEY_SUMMARY_MINUTE = "summary_minute"
        private const val KEY_LLM_MODEL = "llm_model"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"

        const val MODEL_HAIKU = "claude-haiku-4-5-20251001"
        const val MODEL_SONNET = "claude-sonnet-4-5-20241022"
    }
}
