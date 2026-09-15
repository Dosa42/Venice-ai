package com.example.data.skills

data class NativeSkill(
    val id: String,
    val name: String,
    val category: String,
    val tag: String,
    val summary: String,
    val coreDirectives: List<String>,
    val codeRecipe: String
)

/**
 * Native Android Engineering Skills Knowledge Base.
 * Injects legendary, battle-tested Android engineering standards into Venice AI's system prompt:
 * - android-cli: Official docs search and knowledge retrieval
 * - android-secret-management: Secrets Gradle Plugin, .env/.env.example, BuildConfig, APK security
 * - android-typography: Local static .ttf font bundling via font-util CLI, Compose FontFamily
 * - app-icon-generation: Material You Adaptive Icon specifications (66dp safe zone, layer-list)
 * - design-guidelines: High-fidelity M3 Compose polish, edge-to-edge insets, 48dp touch targets
 * - focus-mode: Surgical UI element selection and targeted diff manipulation
 */
object NativeSkillsEngine {

    val SKILLS: List<NativeSkill> = listOf(
        NativeSkill(
            id = "android-cli",
            name = "Android CLI Knowledge Base",
            category = "Documentation & Tooling",
            tag = "Docs",
            summary = "Authoritative Android Knowledge Base retrieval via `android docs search` and `android docs fetch`.",
            coreDirectives = listOf(
                "Use `android docs search \"<query>\"` to locate official, authoritative documentation.",
                "Use `android docs fetch \"<document-id>\"` to retrieve comprehensive technical specifications.",
                "Never attempt `android create`, `android run`, or `android sdk` in sandboxed headless environments.",
                "Rely on standard Gradle commands (`./gradlew`) for compiling and testing."
            ),
            codeRecipe = """
# Search Android Knowledge Base
android docs search "Jetpack Compose WindowInsets"

# Fetch full specification
android docs fetch "compose/layouts/insets"
            """.trimIndent()
        ),
        NativeSkill(
            id = "android-secret-management",
            name = "Android Secret & Credential Management",
            category = "Security & Gradle",
            tag = "Security",
            summary = "Secrets Gradle Plugin, .env.example placeholders, BuildConfig injection, and APK extraction warnings.",
            coreDirectives = listOf(
                "ALWAYS define placeholders in `.env.example` (e.g. `API_KEY=KEY_PLACEHOLDER`).",
                "ALWAYS access secrets through `BuildConfig.<NAME>` rather than hardcoding in source code.",
                "NEVER commit `.env` containing real keys to version control; verify `.gitignore` covers `.env`.",
                "NEVER instruct users to use `local.properties` for API keys.",
                "Warn users that decompiled APKs expose all embedded strings/BuildConfig keys; recommend backend proxies for production."
            ),
            codeRecipe = """
// 1. In .env.example:
// VENICE_API_KEY=PLACEHOLDER

// 2. In app/build.gradle.kts:
// secrets {
//     propertiesFileName = ".env"
//     defaultPropertiesFileName = ".env.example"
// }

// 3. Access in Kotlin:
val apiKey: String = BuildConfig.VENICE_API_KEY
if (apiKey.isBlank()) {
    // Graceful fallback or user alert
}
            """.trimIndent()
        ),
        NativeSkill(
            id = "android-typography",
            name = "Static Typography & Font Bundling",
            category = "UI & Typography",
            tag = "Fonts",
            summary = "Local static .ttf bundling in res/font/ via font-util CLI, eliminating runtime downloadable font failures.",
            coreDirectives = listOf(
                "NEVER use `androidx.compose.ui:ui-text-google-fonts` (downloadable fonts fail in sandboxes/offline).",
                "ALWAYS bundle static `.ttf` fonts in `app/src/main/res/font/` using `font-util install`.",
                "Font names are automatically sanitized to `[a-z0-9_]+.ttf` (e.g. 'Roboto Mono' -> `R.font.roboto_mono`).",
                "Declare `FontFamily(Font(R.font.<font_name>, FontWeight.Normal))` in Theme.kt."
            ),
            codeRecipe = """
// Install font via shell:
// font-util install "Roboto Mono" --target app/src/main/res/font

// Compose Declaration in Theme.kt:
val RobotoMonoFamily = FontFamily(
    Font(R.font.roboto_mono, FontWeight.Normal),
    Font(R.font.roboto_mono_bold, FontWeight.Bold)
)

val CustomTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = RobotoMonoFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
)
            """.trimIndent()
        ),
        NativeSkill(
            id = "app-icon-generation",
            name = "Material You Adaptive App Icons",
            category = "Graphics & Manifest",
            tag = "Adaptive Icons",
            summary = "108dp canvas with strict 66dp safe zone layer-list, custom background gradients, and mipmap adaptive wrappers.",
            coreDirectives = listOf(
                "Never ship default green Android launcher icons.",
                "Foreground asset (`res/drawable/ic_launcher_foreground.xml`) MUST use `<layer-list>` centering a 66dp item in 108dp.",
                "Background asset (`res/drawable/ic_launcher_background.xml`) uses subtle linear diagonal gradients matching brand colors.",
                "Adaptive wrappers in `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` combine foreground and background.",
                "Manifest `android:icon` and `android:roundIcon` MUST target `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`."
            ),
            codeRecipe = """
<!-- res/drawable/ic_launcher_foreground.xml -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:width="66dp"
        android:height="66dp"
        android:drawable="@drawable/ic_brand_logo"
        android:gravity="center" />
</layer-list>

<!-- res/mipmap-anydpi-v26/ic_launcher.xml -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
            """.trimIndent()
        ),
        NativeSkill(
            id = "design-guidelines",
            name = "Material 3 High-Fidelity Design Guidelines",
            category = "UI Architecture",
            tag = "M3 Polish",
            summary = "Edge-to-edge system insets, 48dp minimum touch targets, spring animations, tonal depth, and zero AI slop.",
            coreDirectives = listOf(
                "Strictly avoid generic AI slop: no default gray card backgrounds, unstyled lists, or default system fonts.",
                "Always call `enableEdgeToEdge()` and handle `WindowInsets.safeDrawing` or `WindowInsets.navigationBars`.",
                "Enforce minimum touch targets of 48.dp (`Modifier.minimumInteractiveComponentSize()`).",
                "Always use `.dp` for dimensions and `.sp` for typography.",
                "Provide tactile ripple feedback on all clickable elements (`Modifier.clickable`).",
                "Use `spring()` physics-based animations for fluid reveals under 300ms."
            ),
            codeRecipe = """
Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    bottomBar = {
        NavigationBar(
            windowInsets = WindowInsets.navigationBars
        ) {
            // Navigation items with minimum 48dp touch target
        }
    }
) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // High fidelity content
    }
}
            """.trimIndent()
        ),
        NativeSkill(
            id = "focus-mode",
            name = "Focus Mode Surgical UI Targeting",
            category = "Precision Engineering",
            tag = "Focus Mode",
            summary = "Contextual CSS/Composable selector parsing for surgical element modifications without collateral regressions.",
            coreDirectives = listOf(
                "When selector metadata is provided (`[AIS_METADATA_SECTION_START]`), target modifications strictly to that element.",
                "Apply CSS / style overrides first before interpreting any open-ended text requests.",
                "Do NOT introduce unrequested global re-theming, re-formatting, or peripheral rewrites during focused edits.",
                "Ensure parent layouts accommodate modified dimensions without clipping or layout shifts."
            ),
            codeRecipe = """
// Surgical modification pattern:
// 1. Identify specific target composable / modifier
// 2. Adjust strictly the requested visual attribute (e.g. padding, background, corner radius)
// 3. Keep surrounding hierarchy untouched
Card(
    shape = RoundedCornerShape(16.dp), // targeted adjustment
    modifier = Modifier.testTag("focused_target_element")
)
            """.trimIndent()
        )
    )

    /**
     * Synthesizes the full native skills prompt context for LLM prompt injection.
     */
    fun buildNativeSkillsPromptContext(enabledSkillIds: Set<String> = SKILLS.map { it.id }.toSet()): String {
        val activeSkills = SKILLS.filter { enabledSkillIds.contains(it.id) }
        if (activeSkills.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append("[NATIVE ANDROID EXPERT SKILLS & LEGENDARY DIRECTIVES]:\n")
        sb.append("You possess native mastery over the following 6 core Android development skills:\n\n")

        activeSkills.forEach { skill ->
            sb.append("### SKILL: ${skill.name} (${skill.id})\n")
            sb.append("• Category: ${skill.category} [${skill.tag}]\n")
            sb.append("• Summary: ${skill.summary}\n")
            sb.append("• Core Mandates:\n")
            skill.coreDirectives.forEach { directive ->
                sb.append("  - $directive\n")
            }
            sb.append("\n")
        }

        return sb.toString().trimEnd()
    }
}
