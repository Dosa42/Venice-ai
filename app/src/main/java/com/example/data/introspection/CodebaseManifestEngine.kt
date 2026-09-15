package com.example.data.introspection

import android.content.Context

data class SourceFileEntry(
    val path: String,
    val displayName: String,
    val category: String,
    val description: String,
    val content: String
)

/**
 * Codebase Self-Introspection Engine.
 * Gives the AI model verbatim access to its own critical source code, build scripts,
 * and step-by-step GitHub Actions CI/CD rebuild instructions.
 */
object CodebaseManifestEngine {

    val REGISTERED_FILES: List<SourceFileEntry> = listOf(
        SourceFileEntry(
            path = ".github/workflows/build-apk.yml",
            displayName = "build-apk.yml",
            category = "CI/CD Pipeline",
            description = "GitHub Actions adaptive workflow for assembling debug & release APKs",
            content = """
name: Build Android Debug APK (Adaptive CI/CD Runner)

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:
    inputs:
      build_variant:
        description: "Build variant to assemble"
        required: true
        default: "debug"
        type: choice
        options:
          - "debug"
          - "release"
      clean_build:
        description: "Perform clean build (--clean)"
        required: false
        default: false
        type: boolean
      run_lint:
        description: "Run Android Lint checks"
        required: false
        default: false
        type: boolean
      java_version:
        description: "JDK version to use"
        required: false
        default: "17"
        type: choice
        options:
          - "17"
          - "21"

concurrency:
  group: ${'$'}{{ github.workflow }}-${'$'}{{ github.ref }}
  cancel-in-progress: true

jobs:
  build-apk:
    name: Build ${'$'}{{ inputs.build_variant || 'debug' }} APK
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: Checkout Source Code
        uses: actions/checkout@v4
        with:
          fetch-depth: 1

      - name: Set up Java JDK ${'$'}{{ inputs.java_version || '17' }}
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: ${'$'}{{ inputs.java_version || '17' }}

      - name: Setup Android SDK Tools
        uses: android-actions/setup-android@v3

      - name: Cache Gradle Dependencies & Wrapper
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${'$'}{{ runner.os }}-gradle-${'$'}{{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${'$'}{{ runner.os }}-gradle-

      - name: Assemble Debug APK
        if: ${'$'}{{ (inputs.build_variant || 'debug') == 'debug' }}
        run: |
          ./gradlew assembleDebug --stacktrace --no-daemon \
            -Dorg.gradle.jvmargs="-Xmx3072m -XX:+UseParallelGC"

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: VeniceAI-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
            """.trimIndent()
        ),
        SourceFileEntry(
            path = "build-debug-apk.sh",
            displayName = "build-debug-apk.sh",
            category = "Local Build Runner",
            description = "Adaptive shell script for compiling APK directly in NetHunter chroot with RAM safeguard",
            content = """
#!/usr/bin/env bash
set -e
echo "=== [Venice AI] Dynamic Adaptive Build Runner ==="
TOTAL_MEM_KB=$(grep MemTotal /proc/meminfo | awk '{print ${'$'}2}' || echo "4000000")
if [ "${'$'}TOTAL_MEM_KB" -lt 4500000 ]; then
    GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx2048m -Dorg.gradle.parallel=false --max-workers=2"
else
    GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx3072m -Dorg.gradle.parallel=true"
fi
BUILD_TYPE="${'$'}{1:-assembleDebug}"
./gradlew ${'$'}BUILD_TYPE --stacktrace --no-daemon ${'$'}GRADLE_OPTS
APK_FILE=$(find app/build/outputs/apk -type f -name "*.apk" | head -n 1)
echo "APK Output: ${'$'}APK_FILE"
            """.trimIndent()
        ),
        SourceFileEntry(
            path = "app/src/main/java/com/example/data/adaptive/DynamicAdaptiveEngine.kt",
            displayName = "DynamicAdaptiveEngine.kt",
            category = "Adaptive Engine",
            description = "Runtime learning framework that extracts network interfaces, tools, and error fixes",
            content = """
package com.example.data.adaptive

enum class AdaptiveCategory {
    NETWORK_TELEMETRY, INSTALLED_PACKAGE, KERNEL_DRIVER,
    CHROOT_ENVIRONMENT, RUNTIME_SERVICE, SYSTEM_CONSTRAINT,
    RUNTIME_ERROR_FIX, CUSTOM_DIRECTIVE
}

data class AdaptiveFact(
    val id: String,
    val category: AdaptiveCategory,
    val key: String,
    val value: String,
    val confidence: Float,
    val source: String,
    val isEnabled: Boolean
)

object DynamicAdaptiveEngine {
    fun autoLearnFromText(context: Context, rawText: String, sourceLabel: String): List<AdaptiveFact>
    fun buildAdaptiveContext(context: Context): String
    fun addOrUpdateFact(context: Context, newFact: AdaptiveFact): List<AdaptiveFact>
}
            """.trimIndent()
        ),
        SourceFileEntry(
            path = "app/src/main/java/com/example/data/auth/OpenAIOAuthManager.kt",
            displayName = "OpenAIOAuthManager.kt",
            category = "OAuth PKCE Engine",
            description = "PKCE generator, SHA-256 verifier, local loopback listener on port 1455, and token exchange",
            content = """
package com.example.data.auth

data class OpenAIOAuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
    val accountId: String,
    val expiresInSeconds: Long,
    val issuedAtMillis: Long
)

object OpenAIOAuthManager {
    const val DEFAULT_CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
    const val REDIRECT_URI = "http://localhost:1455/auth/callback"
    const val LOOPBACK_PORT = 1455

    suspend fun startPkceLogin(context: Context, clientId: String, onStatusUpdate: (String) -> Unit): Result<OpenAIOAuthSession>
    suspend fun refreshToken(context: Context, refreshToken: String, clientId: String): Result<OpenAIOAuthSession>
}
            """.trimIndent()
        ),
        SourceFileEntry(
            path = "app/src/main/java/com/example/ui/viewmodel/VeniceViewModel.kt",
            displayName = "VeniceViewModel.kt",
            category = "Core State Engine",
            description = "Central ViewModel orchestrating chat state, DAF runtime memory, and OAuth sessions",
            content = """
package com.example.ui.viewmodel

data class VeniceUiState(
    val messages: List<ChatMessage>,
    val selectedPersona: Persona,
    val hardwareProfile: NetHunterHardwareProfile,
    val openAiSession: OpenAIOAuthSession?,
    val adaptiveFacts: List<AdaptiveFact>,
    val dynamicAdaptivePromptContext: String
)

class VeniceViewModel : ViewModel() {
    fun sendMessage(userText: String)
    fun learnFromText(context: Context, text: String, source: String)
    fun startOpenAiPkceLogin(context: Context)
}
            """.trimIndent()
        )
    )

    fun getRebuildRunbook(): String {
        return """
# === VENICE AI REBUILD & DEPLOYMENT RUNBOOK ===

## OPTION 1: Remote Build via GitHub Actions (Recommended)
1. Push changes to your repository:
   git add .
   git commit -m "feat: adaptive update"
   git push origin main

2. Trigger the workflow manually via GitHub CLI:
   gh workflow run build-apk.yml -f build_variant=debug

3. Download the generated artifact:
   gh run download -n VeniceAI-debug-apk

## OPTION 2: Local Compilation in Kali NetHunter Terminal (chroot)
1. Launch NetHunter Terminal as root:
   nh -r

2. Navigate to project root:
   cd /path/to/venice-ai-android

3. Run the adaptive memory-aware build runner:
   bash ./build-debug-apk.sh

4. Install the debug APK:
   pm install -r app/build/outputs/apk/debug/app-debug.apk
        """.trimIndent()
    }

    /**
     * Synthesizes the AI model's self-awareness prompt context, providing its own
     * architecture map and exact rebuild steps.
     */
    fun buildSelfIntrospectionContext(selectedFilePath: String? = null): String {
        val sb = StringBuilder()
        sb.append("[METACGNITIVE SELF-AWARENESS & CODEBASE MANIFEST]:\n")
        sb.append("You are inspecting your own Android application codebase (Venice AI for Kali NetHunter SM-A326B).\n")
        sb.append("You have direct visibility into your architecture, source files, and GitHub Actions build pipeline.\n\n")

        sb.append("### Project Architecture Tree:\n")
        REGISTERED_FILES.forEach { file ->
            sb.append("• ${file.path} [${file.category}]: ${file.description}\n")
        }
        sb.append("\n")

        if (selectedFilePath != null) {
            val file = REGISTERED_FILES.find { it.path.equals(selectedFilePath, ignoreCase = true) }
            if (file != null) {
                sb.append("### Focused Source File: ${file.path}\n```\n${file.content}\n```\n\n")
            }
        }

        sb.append("### How to Rebuild Yourself:\n")
        sb.append("To patch and rebuild this APK:\n")
        sb.append("1. Provide surgical patches using standard diff syntax.\n")
        sb.append("2. Output the exact NetHunter terminal commands or GitHub Actions dispatch command:\n")
        sb.append("   `gh workflow run build-apk.yml -f build_variant=debug`\n")
        sb.append("3. Once the workflow finishes, the new debug APK is downloadable from GitHub artifacts.\n")

        return sb.toString().trimEnd()
    }
}
