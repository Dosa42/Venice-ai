package com.example.data.adaptive

import android.content.Context
import com.example.data.filesystem.VaultFileSystemManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AdaptiveCategory(val displayName: String, val iconLabel: String) {
    NETWORK_TELEMETRY("Network & Interfaces", "NET"),
    INSTALLED_PACKAGE("Installed Kali Tools", "TOOL"),
    KERNEL_DRIVER("Kernel & Drivers", "KERN"),
    CHROOT_ENVIRONMENT("Chroot & Paths", "PATH"),
    RUNTIME_SERVICE("Active Daemons", "SRV"),
    SYSTEM_CONSTRAINT("Hardware & Memory Safeguards", "MEM"),
    RUNTIME_ERROR_FIX("Error Fixes & Workarounds", "FIX"),
    CUSTOM_DIRECTIVE("User Rules & Directives", "RULE")
}

data class AdaptiveFact(
    val id: String = UUID.randomUUID().toString(),
    val category: AdaptiveCategory,
    val key: String,
    val value: String,
    val confidence: Float = 1.0f,
    val learnedAt: Long = System.currentTimeMillis(),
    val source: String = "Runtime Auto-Learner",
    val isEnabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("category", category.name)
        put("key", key)
        put("value", value)
        put("confidence", confidence.toDouble())
        put("learnedAt", learnedAt)
        put("source", source)
        put("isEnabled", isEnabled)
    }

    companion object {
        fun fromJson(json: JSONObject): AdaptiveFact? {
            return try {
                AdaptiveFact(
                    id = json.optString("id", UUID.randomUUID().toString()),
                    category = AdaptiveCategory.valueOf(json.optString("category", AdaptiveCategory.CUSTOM_DIRECTIVE.name)),
                    key = json.optString("key", "unknown"),
                    value = json.optString("value", ""),
                    confidence = json.optDouble("confidence", 1.0).toFloat(),
                    learnedAt = json.optLong("learnedAt", System.currentTimeMillis()),
                    source = json.optString("source", "Runtime Memory"),
                    isEnabled = json.optBoolean("isEnabled", true)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Dynamic Adaptive Framework (DAF) Engine.
 * Enables runtime learning, continuous environment adaptation, and prompt augmentation
 * based on live terminal outputs, diagnostic traces, and user interactions.
 */
object DynamicAdaptiveEngine {
    private const val PREFS_NAME = "venice_dynamic_adaptive_framework"
    private const val KEY_FACTS = "learned_facts_json"
    private const val KEY_AUTO_LEARN_ENABLED = "auto_learn_enabled"
    private const val FACTS_FILE = ".config/venice_adaptive_facts.json"
    private const val SETTINGS_FILE = ".config/venice_adaptive_settings.json"

    fun isAutoLearningEnabled(context: Context): Boolean {
        return VaultFileSystemManager().readJson(SETTINGS_FILE)
            ?.optBoolean(KEY_AUTO_LEARN_ENABLED, true) ?: true
    }

    fun setAutoLearningEnabled(context: Context, enabled: Boolean) {
        val vault = VaultFileSystemManager()
        val settings = vault.readJson(SETTINGS_FILE) ?: JSONObject()
        settings.put(KEY_AUTO_LEARN_ENABLED, enabled)
        vault.writeJson(SETTINGS_FILE, settings)
    }

    fun loadFacts(context: Context): List<AdaptiveFact> {
        val vault = VaultFileSystemManager()
        var json = vault.readJson(FACTS_FILE)
        if (json == null) {
            // One-time copy of existing private data. An existing shared file always wins.
            val old = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FACTS, null)
            if (!old.isNullOrBlank()) {
                val migrated = JSONObject().put(KEY_FACTS, JSONArray(old))
                vault.writeJson(FACTS_FILE, migrated, overwrite = false)
                json = migrated
            }
        }
        val array = json?.optJSONArray(KEY_FACTS) ?: return emptyList()
        val list = mutableListOf<AdaptiveFact>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            AdaptiveFact.fromJson(obj)?.let { list.add(it) }
        }
        return list
    }

    fun saveFacts(context: Context, facts: List<AdaptiveFact>) {
        val array = JSONArray()
        facts.forEach { array.put(it.toJson()) }
        VaultFileSystemManager().writeJson(
            FACTS_FILE, JSONObject().put(KEY_FACTS, array)
        )
    }

    fun addOrUpdateFact(context: Context, newFact: AdaptiveFact): List<AdaptiveFact> {
        val current = loadFacts(context).toMutableList()
        val existingIndex = current.indexOfFirst { it.key.equals(newFact.key, ignoreCase = true) }
        if (existingIndex >= 0) {
            current[existingIndex] = newFact.copy(id = current[existingIndex].id)
        } else {
            current.add(0, newFact)
        }
        saveFacts(context, current)
        return current
    }

    fun removeFact(context: Context, id: String): List<AdaptiveFact> {
        val updated = loadFacts(context).filter { it.id != id }
        saveFacts(context, updated)
        return updated
    }

    fun toggleFact(context: Context, id: String, isEnabled: Boolean): List<AdaptiveFact> {
        val current = loadFacts(context).map {
            if (it.id == id) it.copy(isEnabled = isEnabled) else it
        }
        saveFacts(context, current)
        return current
    }

    fun resetToDefaults(context: Context): List<AdaptiveFact> {
        return clearAll(context)
    }

    fun clearAll(context: Context): List<AdaptiveFact> {
        val empty = emptyList<AdaptiveFact>()
        saveFacts(context, empty)
        return empty
    }

    /**
     * Analyzes raw terminal output or text to discover and adapt new runtime facts.
     */
    fun autoLearnFromText(
        context: Context,
        rawText: String,
        sourceLabel: String = "Terminal Output"
    ): List<AdaptiveFact> {
        if (!isAutoLearningEnabled(context)) return emptyList()
        if (rawText.isBlank()) return emptyList()

        val discovered = mutableListOf<AdaptiveFact>()

        // 1. Network Interfaces Detection
        val ifaceRegex = Regex("""\b(wlan\d+(?:mon)?|eth\d+|dummy\d+|tun\d+|br\d+|mon\d+)\b""", RegexOption.IGNORE_CASE)
        val ipRegex = Regex("""inet\s+(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(?:/\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val matchedIfaces = ifaceRegex.findAll(rawText).map { it.value }.toSet()
        val matchedIps = ipRegex.findAll(rawText).map { it.groupValues[1] }.toList()

        for (iface in matchedIfaces) {
            if (iface.equals("dummy0", ignoreCase = true) && matchedIfaces.size > 1) continue
            val isMonitor = iface.contains("mon", ignoreCase = true) || rawText.contains("monitor mode", ignoreCase = true)
            val ipDetails = if (matchedIps.isNotEmpty()) " (IP: ${matchedIps.first()})" else ""
            val desc = if (isMonitor) "Monitor mode active on $iface$ipDetails" else "Active network interface: $iface$ipDetails"

            discovered.add(
                AdaptiveFact(
                    category = AdaptiveCategory.NETWORK_TELEMETRY,
                    key = "interface_${iface.lowercase()}",
                    value = desc,
                    confidence = 0.95f,
                    source = sourceLabel
                )
            )
        }

        // 2. Kali Packages & Security Tools Discovery
        val toolsCatalog = listOf(
            "aircrack-ng", "hcxtools", "hcxdumptool", "bettercap", "wireshark",
            "tshark", "nmap", "metasploit", "hydra", "hashcat", "sqlmap",
            "john", "kismet", "responder", "tcpdump", "scapy", "wifite",
            "reaver", "bully", "airmon-ng", "airodump-ng", "aireplay-ng"
        )
        for (tool in toolsCatalog) {
            if (rawText.contains(tool, ignoreCase = true)) {
                discovered.add(
                    AdaptiveFact(
                        category = AdaptiveCategory.INSTALLED_PACKAGE,
                        key = "tool_${tool.replace("-", "_")}",
                        value = "Verified Kali tool: $tool (Binary/Script present)",
                        confidence = 0.90f,
                        source = sourceLabel
                    )
                )
            }
        }

        // 3. Kernel Modules and USB OTG Drivers
        val driverCatalog = listOf("rtl8812au", "rtl8188eu", "rt2800usb", "ath9k_htc", "mac80211", "cfg80211", "mt7601u")
        for (driver in driverCatalog) {
            if (rawText.contains(driver, ignoreCase = true)) {
                discovered.add(
                    AdaptiveFact(
                        category = AdaptiveCategory.KERNEL_DRIVER,
                        key = "driver_$driver",
                        value = "WiFi injection driver active: $driver",
                        confidence = 0.95f,
                        source = sourceLabel
                    )
                )
            }
        }

        // 4. Memory & OOM Protection Detection
        if (rawText.contains("Out of memory", ignoreCase = true) || rawText.contains("Killed process", ignoreCase = true) || rawText.contains("oom-killer", ignoreCase = true)) {
            discovered.add(
                AdaptiveFact(
                    category = AdaptiveCategory.SYSTEM_CONSTRAINT,
                    key = "runtime_oom_safeguard",
                    value = "OOM detected: Force single-job compilation (-j1) and purge chroot caches",
                    confidence = 1.0f,
                    source = "$sourceLabel (OOM Log)"
                )
            )
        }

        // 5. Error Fix Discovery (e.g. broken apt or rfkill)
        if (rawText.contains("apt --fix-broken install", ignoreCase = true) || rawText.contains("unmet dependencies", ignoreCase = true)) {
            discovered.add(
                AdaptiveFact(
                    category = AdaptiveCategory.RUNTIME_ERROR_FIX,
                    key = "fix_broken_dependencies",
                    value = "Run 'apt --fix-broken install -y' inside chroot before continuing installs",
                    confidence = 0.95f,
                    source = sourceLabel
                )
            )
        }
        if (rawText.contains("rfkill", ignoreCase = true) && rawText.contains("blocked", ignoreCase = true)) {
            discovered.add(
                AdaptiveFact(
                    category = AdaptiveCategory.RUNTIME_ERROR_FIX,
                    key = "fix_rfkill_blocked",
                    value = "Execute 'rfkill unblock all' as root (nh -r) to release radio locks",
                    confidence = 0.95f,
                    source = sourceLabel
                )
            )
        }

        // Apply newly discovered facts to stored state
        discovered.forEach { fact ->
            addOrUpdateFact(context, fact)
        }

        return discovered
    }

    /**
     * Synthesizes all enabled learned facts into an adaptive prompt context block.
     */
    fun buildAdaptiveContext(context: Context): String {
        val facts = loadFacts(context).filter { it.isEnabled }
        if (facts.isEmpty()) return ""

        val grouped = facts.groupBy { it.category }
        val sb = StringBuilder()
        sb.append("[DYNAMIC ADAPTIVE RUNTIME CONTEXT (LEARNED IN RUNTIME)]:\n")
        sb.append("Note: The following environment attributes were learned dynamically at runtime from your live NetHunter system and must be respected:\n")

        grouped.forEach { (category, list) ->
            sb.append("• ${category.displayName}:\n")
            list.forEach { fact ->
                sb.append("  - ${fact.key}: ${fact.value}\n")
            }
        }
        return sb.toString().trimEnd()
    }
}
