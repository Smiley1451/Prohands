package com.anand.prohands.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "prohands_secure_session"
        const val KEY_TOKEN = "jwt_token"
        const val KEY_USER_ID = "user_id"
    }

    init {
        // SECURITY IMPROVEMENT: Use EncryptedSharedPreferences to store sensitive data
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAuthToken(token: String?) {
        if (token == null) {
            prefs.edit().remove(KEY_TOKEN).apply()
        } else {
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }
    }

    fun getAuthToken(): String? {
        // Note: EncryptedSharedPreferences can throw exceptions, 
        // but for robustness, we rely on the framework to handle common access failures.
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUserId(userId: String?) {
        if (userId == null) {
            prefs.edit().remove(KEY_USER_ID).apply()
        } else {
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
