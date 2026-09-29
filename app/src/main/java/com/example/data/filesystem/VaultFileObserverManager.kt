package com.example.data.filesystem

import android.os.Build
import android.os.FileObserver
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

enum class VaultChangeKind { AUTH, FACTS, NOTES, CHAT, SCRIPTS, OTHER }
data class VaultFileEvent(val kind: VaultChangeKind, val path: String)

/** Watches the real filesystem; keeps observers alive until stopWatching(). */
class VaultFileObserverManager(
    private val root: File,
    private val scope: CoroutineScope
) {
    private val lock = Any()
    private val observers = mutableMapOf<String, FileObserver>()
    private val pending = mutableMapOf<VaultChangeKind, Job>()
    private val _events = MutableSharedFlow<VaultFileEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<VaultFileEvent> = _events.asSharedFlow()
    private var running = false

    fun startWatching() {
        synchronized(lock) {
            if (running) return
            running = true
            registerDirectories()
        }
    }

    private fun registerDirectories() {
        root.walkTopDown()
            .onEnter { dir -> dir == root || !dir.name.equals(".database", ignoreCase = true) }
            .filter { it.isDirectory }
            .forEach { dir ->
                val path = dir.absolutePath
                if (observers.containsKey(path)) return@forEach
                val mask = FileObserver.CLOSE_WRITE or FileObserver.CREATE or FileObserver.DELETE or
                    FileObserver.MOVED_TO or FileObserver.MOVED_FROM or FileObserver.DELETE_SELF or
                    FileObserver.MOVE_SELF
                val observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    object : FileObserver(dir, mask) {
                        override fun onEvent(event: Int, name: String?) = handle(dir, name, event)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    object : FileObserver(path, mask) {
                        override fun onEvent(event: Int, name: String?) = handle(dir, name, event)
                    }
                }
                observer.startWatching()
                observers[path] = observer
            }
    }

    private fun handle(parent: File, name: String?, event: Int) {
        if (name == null || name.endsWith(".tmp") || name.endsWith(".db-wal") ||
            name.endsWith(".db-shm") || name.endsWith(".db-journal")) return
        val changed = File(parent, name)
        val relative = changed.relativeTo(root).invariantSeparatorsPath
        val kind = when {
            relative.startsWith(".auth/") -> VaultChangeKind.AUTH
            relative.startsWith(".config/") -> VaultChangeKind.FACTS
            relative.startsWith(".chat/") -> VaultChangeKind.CHAT
            relative.startsWith(".scripts/") -> VaultChangeKind.SCRIPTS
            name.endsWith(".md", ignoreCase = true) || changed.isDirectory -> VaultChangeKind.NOTES
            else -> VaultChangeKind.OTHER
        }
        synchronized(lock) {
            if (!running) return
            if (event and (FileObserver.CREATE or FileObserver.MOVED_TO) != 0) {
                registerDirectories()
            }
            pending.remove(kind)?.cancel()
            pending[kind] = scope.launch(Dispatchers.IO) {
                delay(150)
                _events.emit(VaultFileEvent(kind, changed.absolutePath))
            }
        }
    }

    fun stopWatching() {
        synchronized(lock) {
            running = false
            pending.values.forEach { it.cancel() }
            pending.clear()
            observers.values.forEach { it.stopWatching() }
            observers.clear()
        }
    }
}
