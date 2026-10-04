package dev.renkinProject.renkin.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.renkinProject.renkin.dataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Global, deliberately independent of Renkin's per-profile rendering preferences. No secrets. */
data class AiSettings(
    val provider: String = PROVIDERS.first(),
    val style: String = DEFAULT_STYLE,
    val model: String = MODELS.first(),
    val quality: String = "medium",
    val referenceNames: List<String> = emptyList(),
    val appInstruction: String = ""
) {
    fun validate() {
        require(appInstruction.length <= 2000) { "Use at most 2000 characters for the app instruction." }
        require(provider in PROVIDERS) { "Select a supported AI provider." }
        require(style.isNotBlank() && style.length <= 6000) { "A style prompt of 1–6000 characters is required." }
        require(model in MODELS) { "Select a supported image model." }
        require(quality in QUALITIES) { "Select a supported quality." }
        require(referenceNames.size <= 5) { "Use at most five reference icons." }
        require(referenceNames.all { it.matches(Regex("img_[0-9]+_[0-9]+\\.png")) }) { "Invalid reference file." }
    }
    companion object {
        const val OPENAI = "OpenAI"
        const val STABILITY = "Stability AI"
        val PROVIDERS = listOf(OPENAI, STABILITY)
        val MODELS = listOf("gpt-image-2.5-flare", "gpt-image-2.5-sunburst", "gpt-image-1-mini")
        val QUALITIES = listOf("low", "medium", "high")
        const val DEFAULT_STYLE = "Minimal white outline icon. Consistent rounded caps and joins, " +
            "approximately 24 px stroke at 1024 px canvas. Center the recognizable symbol inside " +
            "a 720 × 720 px optical box, leaving generous balanced padding. Simple silhouette, " +
            "no gradients, no shadows, no lettering, no surrounding tile."
    }
}

@Singleton
class AiSettingsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val providerKey = stringPreferencesKey("ai_provider_v1")
    private val styleKey = stringPreferencesKey("ai_style_v1")
    private val modelKey = stringPreferencesKey("ai_model_v1")
    private val qualityKey = stringPreferencesKey("ai_quality_v1")
    private val referencesKey = stringPreferencesKey("ai_references_v1")

    suspend fun load(): AiSettings {
        val prefs = context.dataStore.data.first()
        return AiSettings(
            prefs[providerKey]?.takeIf { it in AiSettings.PROVIDERS } ?: AiSettings.PROVIDERS.first(),
            prefs[styleKey] ?: AiSettings.DEFAULT_STYLE,
            prefs[modelKey]?.takeIf { it in AiSettings.MODELS } ?: AiSettings.MODELS.first(),
            prefs[qualityKey]?.takeIf { it in AiSettings.QUALITIES } ?: "medium",
            prefs[referencesKey].orEmpty().split('|').filter { it.isNotBlank() }.take(5)
        )
    }

    suspend fun save(settings: AiSettings) {
        settings.validate()
        context.dataStore.edit {
            it[providerKey] = settings.provider
            it[styleKey] = settings.style
            it[modelKey] = settings.model
            it[qualityKey] = settings.quality
            it[referencesKey] = settings.referenceNames.joinToString("|")
        }
    }
}

/** Identity is conveyed by image 1; Android package/activity/profile identifiers stay local. */
internal fun iconPrompt(style: String, referenceCount: Int, appInstruction: String = ""): String = """
    Create ONE Android launcher icon. Image 1 is the original app icon: preserve its distinctive
    visual identity and symbol, while redrawing it in the requested style.
    ${if (referenceCount == 0) "No style reference images were supplied; follow the specification below."
      else "Images 2 through ${referenceCount + 1} are STYLE REFERENCES ONLY. Match their stroke weight, " +
        "optical size, padding, palette and complexity. Do not copy their app symbols."}
    STYLE SPECIFICATION:
    $style
    APP-SPECIFIC INSTRUCTION (optional; subject to the shared style and output contract):
    ${appInstruction.ifBlank { "No additional instruction; inherit the shared style." }}
    OUTPUT CONTRACT:
    One centered icon, square canvas, genuine transparent alpha outside the artwork.
    No text, watermark, checkerboard, solid background, device mockup, collage or contact sheet.
    Preserve recognizability at 48 px. Return only the image.
""".trimIndent()
