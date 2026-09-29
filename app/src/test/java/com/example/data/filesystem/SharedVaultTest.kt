package com.example.data.filesystem

import android.content.Context
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import com.example.data.adaptive.DynamicAdaptiveEngine
import com.example.data.auth.ChatGPTAuthManager
import com.example.data.config.VaultAuthConfigManager
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SharedVaultTest {
    private lateinit var context: Context
    private lateinit var vault: VaultFileSystemManager
    private lateinit var root: File

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("venice_dynamic_adaptive_framework", Context.MODE_PRIVATE)
            .edit().clear().commit()
        vault = VaultFileSystemManager()
        root = vault.resolveLocation().root
    }

    @After fun cleanup() {
        listOf(
            "Concepts/existing.md", "Wiki/test.md", ".config/venice_adaptive_facts.json",
            ".auth/chatgpt_session.json"
        ).forEach { File(root, it).delete() }
        context.getSharedPreferences("venice_dynamic_adaptive_framework", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun topologyKeepsExistingMarkdownAndNeverSeedsFacts() {
        val original = File(root, "Concepts/existing.md")
        original.parentFile!!.mkdirs()
        original.writeText("# Existing\nuser-owned")
        vault.ensureVaultTopology()
        assertEquals("# Existing\nuser-owned", original.readText())
        assertEquals(listOf("Concepts/existing.md"), vault.listNotes().map { it.relativePath })
        assertTrue(DynamicAdaptiveEngine.loadFacts(context).isEmpty())
        assertFalse(File(root, ".config/venice_adaptive_facts.json").exists())
    }

    @Test fun vaultUsesOnlyDownloadAnchor() {
        val location = vault.resolveLocation()
        assertEquals(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "ObsidianVault").canonicalFile,
            location.root
        )
    }

    @Test fun explicitNoteWriteRejectsCollisionAndStaleEdit() {
        vault.ensureVaultTopology()
        vault.saveNote("Wiki/test.md", "# first")
        val collision = runCatching { vault.saveNote("Wiki/test.md", "replacement") }
        assertTrue(collision.isFailure)
        File(root, "Wiki/test.md").writeText("# external edit")
        val stale = runCatching {
            vault.saveNote("Wiki/test.md", "# my edit", overwrite = true, expectedContent = "# first")
        }
        assertTrue(stale.isFailure)
        assertEquals("# external edit", vault.readNote("Wiki/test.md"))
        assertTrue(runCatching { vault.saveNote("../outside.md", "no") }.isFailure)
    }

    @Test fun oldPrivateFactsMigrateOnlyWhenSharedFactsAreAbsent() {
        val oldFact = JSONObject()
            .put("category", "CUSTOM_DIRECTIVE")
            .put("key", "owned")
            .put("value", "real data")
        val oldJson = JSONArray().put(oldFact).toString()
        context.getSharedPreferences("venice_dynamic_adaptive_framework", Context.MODE_PRIVATE)
            .edit().putString("learned_facts_json", oldJson).commit()
        assertEquals("real data", DynamicAdaptiveEngine.loadFacts(context).single().value)
        val shared = vault.readJson(".config/venice_adaptive_facts.json")!!
        assertEquals("owned", shared.getJSONArray("learned_facts_json").getJSONObject(0).getString("key"))
        vault.writeJson(".config/venice_adaptive_facts.json",
            JSONObject().put("learned_facts_json", JSONArray()))
        assertTrue(DynamicAdaptiveEngine.loadFacts(context).isEmpty())
    }

    @Test fun missingSessionAndConfigAreNotCreated() = runBlocking {
        vault.ensureVaultTopology()
        assertNull(ChatGPTAuthManager().reloadFromDisk())
        assertNull(VaultAuthConfigManager().reloadFromDisk())
        assertFalse(File(root, ".auth/chatgpt_session.json").exists())
        assertFalse(File(root, ".auth/vault_auth_config.json").exists())
        vault.writeJson(".auth/chatgpt_session.json",
            JSONObject().put("accessToken", "token").put("expiresAt", System.currentTimeMillis() + 60_000))
        assertTrue(ChatGPTAuthManager().reloadFromDisk()!!.isValid)
    }
}
