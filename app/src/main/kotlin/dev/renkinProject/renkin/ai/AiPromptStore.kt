package dev.renkinProject.renkin.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.renkinProject.renkin.dataStore
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Per-profile/component instructions. Local only; full Renkin backups include preferences. */
@Singleton
class AiPromptStore @Inject constructor(@ApplicationContext private val context: Context) {
    private fun key(target: String) = stringPreferencesKey("ai_app_prompt_" +
        MessageDigest.getInstance("SHA-256").digest(target.toByteArray()).joinToString("") { "%02x".format(it) })
    suspend fun load(target: String): String = context.dataStore.data.first()[key(target)].orEmpty()
    suspend fun save(target: String, value: String) {
        require(value.length <= 2000) { "Use at most 2000 characters." }
        context.dataStore.edit { if (value.isBlank()) it.remove(key(target)) else it[key(target)] = value }
    }
}
