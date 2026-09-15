package com.example.data.skills

/**
 * Adaptive Dynamic Prompt Engine that intelligently matches user queries,
 * active screen context, or explicit flags to relevant Android Native Skills.
 * Prevents prompt bloat and rate limits by injecting only the necessary skill recipes.
 */
object AdaptivePromptEngine {

    enum class InjectionMode(val title: String, val description: String) {
        ADAPTIVE("Adaptive Intent Match", "Dynamically injects only skills relevant to your prompt to optimize context and token speed."),
        ALL_LEGENDARY("Full Legendary Mode", "Always injects all 6 core Android skills for maximum system knowledge."),
        CUSTOM_ONLY("Selected Skills", "Injects only manually toggled skills from your settings.")
    }

    private val SKILL_INTENT_KEYWORDS: Map<String, List<String>> = mapOf(
        "android-cli" to listOf(
            "cli", "command", "android docs", "search docs", "fetch docs", "terminal",
            "shell", "knowledge base", "documentation search", "official docs"
        ),
        "android-secret-management" to listOf(
            "secret", "api key", "apikey", "token", "env", ".env", "buildconfig",
            "security", "decompile", "apk leak", "credential", "hide key", "bearer"
        ),
        "android-typography" to listOf(
            "font", "typography", "ttf", "font-util", "google fonts", "textstyle",
            "fontfamily", "lineheight", "sp", "letterspacing", "custom font", "res/font"
        ),
        "app-icon-generation" to listOf(
            "icon", "app icon", "adaptive icon", "launcher", "ic_launcher", "foreground",
            "background", "safe zone", "layer-list", "mipmap", "108dp", "66dp"
        ),
        "design-guidelines" to listOf(
            "design", "ui", "compose", "material 3", "m3", "insets", "edgetoedge",
            "padding", "scaffold", "navigationbar", "touch target", "48dp", "spring",
            "animation", "theme", "surface", "elevation", "color scheme", "slop"
        ),
        "focus-mode" to listOf(
            "focus", "selector", "css", "target element", "metadata", "surgical",
            "ais_metadata", "specific button", "selected element", "component id"
        )
    )

    /**
     * Resolves matching skills for a given query based on semantic keyword density.
     */
    fun detectRelevantSkillIds(query: String): Set<String> {
        val lower = query.lowercase()
        val matches = mutableSetOf<String>()

        for ((skillId, keywords) in SKILL_INTENT_KEYWORDS) {
            if (keywords.any { keyword -> lower.contains(keyword) }) {
                matches.add(skillId)
            }
        }

        // If general Android/Compose query without specific skill match, include design-guidelines as baseline
        if (matches.isEmpty() && (lower.contains("android") || lower.contains("compose") || lower.contains("app") || lower.contains("code"))) {
            matches.add("design-guidelines")
        }

        return matches
    }

    /**
     * Builds the dynamic prompt hook context tailored to the user's intent.
     */
    fun synthesizePromptHook(
        userQuery: String,
        mode: InjectionMode = InjectionMode.ADAPTIVE,
        manualSelectedSkills: Set<String> = NativeSkillsEngine.SKILLS.map { it.id }.toSet()
    ): PromptHookResult {
        val activeSkillIds = when (mode) {
            InjectionMode.ALL_LEGENDARY -> NativeSkillsEngine.SKILLS.map { it.id }.toSet()
            InjectionMode.CUSTOM_ONLY -> manualSelectedSkills
            InjectionMode.ADAPTIVE -> {
                val detected = detectRelevantSkillIds(userQuery)
                // Intersect with manual allowed skills or fallback to detected
                if (detected.isNotEmpty()) {
                    detected.filter { manualSelectedSkills.contains(it) }.toSet()
                } else {
                    emptySet()
                }
            }
        }

        val promptBlock = if (activeSkillIds.isNotEmpty()) {
            NativeSkillsEngine.buildNativeSkillsPromptContext(activeSkillIds)
        } else {
            ""
        }

        return PromptHookResult(
            mode = mode,
            matchedSkillIds = activeSkillIds,
            promptContext = promptBlock,
            injectedSkillCount = activeSkillIds.size
        )
    }
}

data class PromptHookResult(
    val mode: AdaptivePromptEngine.InjectionMode,
    val matchedSkillIds: Set<String>,
    val promptContext: String,
    val injectedSkillCount: Int
)
