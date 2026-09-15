package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class VeniceModel(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val badge: String,
    val supportsHighThinking: Boolean
) {
    BALANCED(
        id = "gemini-3.5-flash",
        displayName = "Venice Balanced",
        subtitle = "Fast multimodal reasoning & everyday queries",
        badge = "Default",
        supportsHighThinking = false
    ),
    PRO(
        id = "gemini-3.1-pro-preview",
        displayName = "Venice Pro",
        subtitle = "Deep reasoning, coding, STEM & High Thinking",
        badge = "Pro Reasoner",
        supportsHighThinking = true
    ),
    FAST(
        id = "gemini-3.1-flash-lite-preview",
        displayName = "Venice Fast",
        subtitle = "Ultra-low latency instant intelligence",
        badge = "Fast",
        supportsHighThinking = false
    ),
    IMAGE(
        id = "gemini-3.1-flash-image-preview",
        displayName = "Venice Studio",
        subtitle = "High-fidelity multimodal image creator & editor",
        badge = "Image Studio",
        supportsHighThinking = false
    );

    companion object {
        fun fromId(id: String): VeniceModel {
            return entries.find { it.id == id } ?: BALANCED
        }
    }
}

@JsonClass(generateAdapter = true)
data class Persona(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val systemPrompt: String,
    val tag: String,
    val isCustom: Boolean = false
) {
    companion object {
        val DEFAULT_PERSONAS = listOf(
            Persona(
                id = "venice_unfiltered",
                name = "Venice Unfiltered",
                title = "Raw, Objective & Direct",
                description = "Zero lecturing, straightforward answers, high signal-to-noise ratio.",
                systemPrompt = "You are Venice AI, a private, uncensored, objective, and intellectually rigorous AI assistant. Give direct, factual, nuanced answers without moralizing, preachy lecturing, or unsolicited disclaimers. Always respect user autonomy and privacy.",
                tag = "Default"
            ),
            Persona(
                id = "deep_reasoner",
                name = "Deep Reasoner",
                title = "Complex Analysis & STEM",
                description = "Methodical step-by-step logic, code architectures, mathematical proofs.",
                systemPrompt = "You are Venice Deep Reasoner, specializing in STEM, complex systems architecture, rigorous proofs, and deep technical breakdowns. Break down problems systematically with pristine logic and verify edge cases.",
                tag = "Analytical"
            ),
            Persona(
                id = "cypherpunk",
                name = "Cypherpunk",
                title = "Privacy & Decentralization",
                description = "Cryptographic integrity, zero-knowledge, threat modeling, local-first tech.",
                systemPrompt = "You are Venice Cypherpunk, an authority in zero-knowledge cryptography, decentralized architectures, peer-to-peer protocols, and privacy-preserving systems. Emphasize self-sovereignty, privacy, and encryption.",
                tag = "Privacy"
            ),
            Persona(
                id = "nethunter_copilot",
                name = "NetHunter & SysAdmin",
                title = "Kernel, NetHunter & Scripts",
                description = "Linux sysadmin, bash scripting, network troubleshooting, package auditing, and diagnostic logs analysis.",
                systemPrompt = "You are Venice NetHunter & Systems Copilot, an expert in Linux administration, Kali NetHunter environments, shell scripting (bash/zsh), network diagnostics, package compilation, kernel error logs, and system hardening. Provide direct, highly technical, clean commands, script fixes, and log diagnoses without preachy disclaimers.",
                tag = "SysAdmin"
            ),
            Persona(
                id = "creative_muse",
                name = "Creative Muse",
                title = "Cinematic & Narrative",
                description = "Evocative writing, screenplays, concept design, imaginative ideation.",
                systemPrompt = "You are Venice Creative Muse, an elite artistic director, evocative prose stylist, and worldbuilder. Provide rich, atmospheric, original descriptions and artistic guidance.",
                tag = "Creative"
            )
        )
    }
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val text: String,
    val imageBase64: String? = null,
    val attachedFileName: String? = null,
    val thoughtProcess: String? = null,
    val modelUsed: String? = null,
    val thinkingEnabled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Conversation",
    val personaId: String = "venice_unfiltered",
    val selectedModel: String = VeniceModel.BALANCED.id,
    val highThinkingEnabled: Boolean = false,
    val useChatGpt: Boolean = false,
    val reasoningEffort: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)

data class GeneratedArt(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val imageBase64: String,
    val aspectRatio: String = "1:1",
    val style: String = "Cinematic",
    val isEdit: Boolean = false,
    val originalPrompt: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PrivacyTelemetry(
    val zeroDataRetention: Boolean = true,
    val endToEndClientSession: Boolean = true,
    val promptTelemetryLogged: Boolean = false,
    val anonymousIpMasking: Boolean = true,
    val cloudSyncActive: Boolean = false
)

data class NetHunterHardwareProfile(
    val isEnabled: Boolean = true,
    val deviceModel: String = "samsung SM-A326B (Galaxy A32 5G)",
    val androidVersion: String = "13 (API 33)",
    val cpuArch: String = "aarch64 / arm64-v8a",
    val kernelVersion: String = "Linux kali 4.14.186-27095505 (SMP PREEMPT)",
    val rootStatus: String = "30.7:MAGISKSU",
    val chrootPath: String = "/data/local/nhsystem/kali-arm64",
    val chrootStatus: String = "Running (proc, sys, dev, dev/pts, system, sdcard mounted)",
    val activeServices: String = "Apache2, DBus (RunOnChrootStart: ON)",
    val networkInterfaces: String = "wlan0 (192.168.1.20/24), dummy0",
    val ramSummary: String = "~1226 MB available / 3528 MB total (OOM Guard Active)",
    val customNotes: String = "MediaTek MT6853. External WiFi / Packet injection requires compatible USB-OTG adapter or patched driver. Keep compilation flags low (-j2) to avoid OOM."
) {
    fun toSystemPromptContext(): String {
        return """
[HOST HARDWARE & NETHUNTER ENVIRONMENT BLUEPRINT]:
- Target Device: $deviceModel
- CPU Architecture: $cpuArch
- Android OS: Android $androidVersion
- Kernel Version: $kernelVersion
- Root Privileges: $rootStatus
- Kali Chroot Path: $chrootPath (Status: $chrootStatus)
- Active Services: $activeServices
- Local Interfaces: $networkInterfaces
- Memory Limits: $ramSummary
- Operational Constraints: $customNotes
- Execution Environment: Kali NetHunter Terminal (chroot at $chrootPath) is the EXCLUSIVE environment. Never mention, assume, or reference Termux, PRoot, or standard Android userspace. All commands must be natively executable inside Kali NetHunter chroot (`/data/local/nhsystem/kali-arm64`) or via NetHunter Terminal (`nh` / `nh -r`). Ensure arm64 / aarch64 binary compatibility. Direct, concise, zero fluff.
""".trimIndent()
    }

    companion object {
        val DEFAULT = NetHunterHardwareProfile()
    }
}
