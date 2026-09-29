package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.ui.components.TerminalExecutionCard
import com.example.ui.components.readPickedTextFile
import com.example.ui.components.rememberMiXplorerPicker
import com.example.ui.theme.VeniceBackground
import com.example.ui.theme.VeniceCyan
import com.example.ui.theme.VeniceRose
import com.example.ui.theme.VeniceSurface
import com.example.ui.theme.VeniceTextPrimary
import com.example.ui.theme.VeniceTextSecondary
import com.example.ui.theme.VeniceTheme
import com.example.ui.viewmodel.VeniceNavTab
import com.example.ui.viewmodel.VeniceUiState
import com.example.ui.viewmodel.VeniceViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: VeniceViewModel by viewModels()
    private val storagePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshVault() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.initialize(this)
        handleIncomingIntent(intent)
        setContent { VeniceTheme { VeniceApp(viewModel, ::requestVaultAccess) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshVault()
    }

    private fun requestVaultAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        } else {
            storagePermissions.launch(arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let(viewModel::showSharedText)
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
        try {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val buffer = CharArray(65_536)
                val count = reader.read(buffer)
                if (count > 0) viewModel.showSharedText(String(buffer, 0, count))
            }
        } catch (e: Exception) {
            viewModel.showError(e.message ?: "Shared file could not be read")
        }
    }
}

@Composable
fun VeniceApp(viewModel: VeniceViewModel, requestVaultAccess: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            Surface(color = VeniceSurface, modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("VENICE AI", color = VeniceTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Shared vault · No model backend connected", color = VeniceTextSecondary, fontSize = 12.sp)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = VeniceSurface) {
                listOf(
                    VeniceNavTab.WORKSPACE to "Workspace",
                    VeniceNavTab.TERMINAL to "Terminal",
                    VeniceNavTab.VAULT to "Vault"
                ).forEach { (tab, title) ->
                    NavigationBarItem(selected = state.currentTab == tab,
                        onClick = { viewModel.selectTab(tab) }, icon = { Text(title.take(1)) },
                        label = { Text(title) })
                }
            }
        },
        containerColor = VeniceBackground
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.error?.let { Text(it, color = VeniceRose, modifier = Modifier.padding(16.dp)) }
            when (state.currentTab) {
                VeniceNavTab.WORKSPACE -> WorkspacePanel(viewModel, state)
                VeniceNavTab.TERMINAL -> TerminalPanel(viewModel, state)
                VeniceNavTab.VAULT -> VaultPanel(viewModel, state, requestVaultAccess)
            }
        }
    }
}

@Composable
private fun WorkspacePanel(viewModel: VeniceViewModel, state: VeniceUiState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chooseFile = rememberMiXplorerPicker(
        onFile = { uri -> scope.launch {
            try {
                val file = readPickedTextFile(context, uri)
                viewModel.showPickedFile(file.name, file.text)
            } catch (e: Exception) {
                viewModel.showError(e.message ?: "File could not be read")
            }
        } },
        onError = viewModel::showError
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Workspace", color = VeniceTextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("No AI backend is connected. Shared vault data, files and terminal remain available.",
            color = VeniceTextSecondary)
        Button(onClick = chooseFile) { Text("Open file with MiXplorer") }
        state.sharedText?.let { ContentCard("Shared text", it) }
        state.selectedFileContent?.let { ContentCard(state.selectedFileName ?: "Selected file", it) }
    }
}

@Composable
private fun TerminalPanel(viewModel: VeniceViewModel, state: VeniceUiState) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
        Text("NetHunter terminal", color = VeniceTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp))
        OutlinedTextField(value = state.terminalInput, onValueChange = viewModel::setTerminalInput,
            label = { Text("Command") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.setTerminalEnvironment("nethunter") },
                enabled = state.terminalEnvironment != "nethunter") { Text("NetHunter") }
            Button(onClick = { viewModel.setTerminalEnvironment("android") },
                enabled = state.terminalEnvironment != "android") { Text("Android") }
            Button(onClick = viewModel::runTerminalCommand, enabled = state.terminalInput.isNotBlank()) {
                Text("Run")
            }
        }
        TerminalExecutionCard(state.terminalCommands, viewModel::stopTerminalCommand,
            viewModel::stopAllTerminalCommands)
        if (state.terminalCommands.isEmpty()) {
            Text("No commands run in this session.", color = VeniceTextSecondary,
                modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun VaultPanel(viewModel: VeniceViewModel, state: VeniceUiState, requestVaultAccess: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Shared Obsidian Vault", color = VeniceTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(state.vaultPath.ifEmpty { "/storage/emulated/0/Download/ObsidianVault" },
            color = VeniceTextSecondary, fontFamily = FontFamily.Monospace)
        state.vaultError?.let {
            Text(it, color = VeniceRose)
            Button(onClick = requestVaultAccess) { Text("Grant storage access") }
        }
        Button(onClick = viewModel::refreshVault) { Text("Reload vault") }
        if (state.vaultPath.isNotEmpty()) {
            val sessionStatus = when {
                state.sharedSession == null -> "No shared session file"
                state.sharedSession.isValid -> "Token loaded; expires at ${state.sharedSession.expiresAt}"
                else -> "Session file loaded, but token is expired or incomplete"
            }
            ContentCard("Shared session", sessionStatus)
            state.sharedConfig?.let { config ->
                ContentCard("Shared config",
                    "Provider: ${config.provider.ifEmpty { "(unset)" }}\n" +
                    "Model: ${config.activeModel.ifEmpty { "(unset)" }}\n" +
                    "Persona: ${config.personaName.ifEmpty { "(unset)" }}\n" +
                    "API key: ${if (config.apiKey.isNotEmpty()) "present" else "absent"}\n" +
                    "System prompt: ${config.systemPrompt}")
            } ?: ContentCard("Shared config", "No vault_auth_config.json file")
            Text("Markdown notes (${state.vaultNotes.size})", color = VeniceTextPrimary,
                fontWeight = FontWeight.Bold)
            Text("Room index: ${state.indexedNotes} notes", color = VeniceTextSecondary)
            state.indexError?.let { Text("Index error: $it", color = VeniceRose) }
            state.vaultNotes.forEach { note ->
                Button(onClick = { viewModel.selectNote(note.relativePath) }) {
                    Text(note.relativePath)
                }
            }
            state.selectedNotePath?.let { path ->
                Text(path, color = VeniceCyan, fontFamily = FontFamily.Monospace)
                OutlinedTextField(
                    value = state.selectedNoteText,
                    onValueChange = viewModel::setNoteText,
                    label = { Text("Markdown") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6
                )
                Button(onClick = viewModel::saveSelectedNote, enabled = state.noteDirty) {
                    Text("Save note")
                }
            }
            OutlinedTextField(
                value = state.newNoteTitle,
                onValueChange = viewModel::setNewNoteTitle,
                label = { Text("New note title (Concepts/)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = viewModel::createNote, enabled = state.newNoteTitle.isNotBlank()) {
                Text("Create Markdown note")
            }
        }
        Text("Shared adaptive facts (${state.adaptiveFacts.size})", color = VeniceTextPrimary,
            fontWeight = FontWeight.Bold)
        state.adaptiveFacts.forEach { fact ->
            ContentCard(fact.key, fact.value)
        }
    }
}

@Composable
private fun ContentCard(title: String, content: String) {
    Card(colors = CardDefaults.cardColors(containerColor = VeniceSurface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = VeniceCyan, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(content, color = VeniceTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}
