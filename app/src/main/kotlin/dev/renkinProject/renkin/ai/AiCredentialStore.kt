package dev.renkinProject.renkin.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Ciphertext only, outside Android backup AND Renkin's exported backup allowlist. */
@Singleton
class AiCredentialStore @Inject constructor(@ApplicationContext private val context: Context) {
    private fun file(provider: String) = AtomicFile(File(context.noBackupFilesDir,
        if (provider == AiSettings.STABILITY) "stability-key.enc" else "openai-key.enc"))
    private fun alias(provider: String) = if (provider == AiSettings.STABILITY)
        "renkin-ai-stability-v1" else "renkin-ai-openai-v1"

    private fun key(provider: String): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias(provider), null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias(provider), KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }

    @Synchronized fun hasKey(provider: String): Boolean = file(provider).baseFile.exists()

    @Synchronized fun read(provider: String): String {
        val file = file(provider)
        if (!hasKey(provider)) error("Add a $provider API key in AI settings first.")
        try {
            val bytes = file.readFully()
            require(bytes.size in 29..4096)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(provider), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            error("The saved API key cannot be decrypted. Remove it and enter it again in AI settings.")
        }
    }

    @Synchronized fun save(provider: String, value: String) {
        val file = file(provider)
        val token = value.trim()
        require(token.length in 20..2048 && token.all { it.code in 33..126 }) { "Invalid API key format." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(provider))
        val stream = file.startWrite()
        try {
            stream.write(cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8)))
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    @Synchronized fun clear(provider: String) { file(provider).delete() }
}
