package com.example.data.local

import org.json.JSONArray
import org.json.JSONObject

internal class TerminalTools(private val terminal: NetHunterTerminal, private val chrootPath: String) {
    suspend fun execute(name: String, args: JSONObject): String {
        return when (name) {
            "exec_command" -> {
                val environment = args.optString("environment").takeUnless { it.isBlank() || it == "null" } ?: "nethunter"
                terminal.start(
                    command = args.getString("command"),
                    workdir = args.optString("workdir").takeUnless { it.isBlank() || it == "null" }
                        ?: if (environment == "android") "/" else "/root",
                    environment = environment, chroot = chrootPath
                ).toJson().toString()
            }
            "read_command_output" -> terminal.poll(args.getString("session_id"),
                args.optLong("stdout_offset", 0), args.optLong("stderr_offset", 0)).toJson().toString()
            "write_stdin" -> {
                terminal.write(args.getString("session_id"), args.optString("chars", ""), args.optBoolean("close_stdin", false))
                JSONObject().put("session_id", args.getString("session_id")).put("stdin_written", true).toString()
            }
            "stop_command" -> {
                terminal.stop(args.getString("session_id"))
                JSONObject().put("session_id", args.getString("session_id")).put("cancellation_requested", true).toString()
            }
            else -> error("Unknown terminal tool: $name")
        }
    }

    private fun TerminalOutput.toJson(): JSONObject = JSONObject()
        .put("session_id", command.id).put("command", command.command)
        .put("environment", command.environment).put("workdir", command.workdir)
        .put("status", command.status).put("exit_code", command.exitCode ?: JSONObject.NULL)
        .put("error", command.error ?: JSONObject.NULL)
        .put("stdout", stdout.text).put("stderr", stderr.text)
        .put("next_stdout_offset", stdout.nextOffset).put("next_stderr_offset", stderr.nextOffset)
        .put("stdout_bytes", stdout.totalBytes).put("stderr_bytes", stderr.totalBytes)
        .put("more_output", stdout.nextOffset < stdout.totalBytes || stderr.nextOffset < stderr.totalBytes)
        .put("stdout_file", command.stdoutPath).put("stderr_file", command.stderrPath)

    companion object {
        val instructions = """
            [LIVE TERMINAL TOOLS]:
            exec_command runs an actual root shell command on the phone. The default environment is the configured NetHunter chroot; environment=android selects the Android root shell. workdir is a path inside the selected environment. Commands have full read/write/execute access provided by su, without app command allowlists or approval dialogs. Use the tools for requested filesystem inspection, edits, scripts, builds and installed CLI tools.
            A running session is not a completed command. Use read_command_output with the returned byte offsets until status is completed/failed/cancelled and more_output is false. Full stdout/stderr are retained at the returned Android filesystem paths; responses deliver pages without deleting output. write_stdin sends input or EOF. stop_command requests cancellation. Report actual output and exit status; do not claim a command ran from instructions alone.
        """.trimIndent()

        fun definitions(): JSONArray = JSONArray()
            .put(function("exec_command", "Run a shell command with root access. Supports scripts, file writes, package tools and builds. Returns a session for commands still running. This is a pipe-based shell, with stdin available through write_stdin.",
                JSONObject().put("command", type("string"))
                    .put("workdir", nullable("string", "Directory inside the selected environment; null uses /root in NetHunter and / in Android."))
                    .put("environment", nullable("string", "nethunter (default) or android.").put("enum", JSONArray().put("nethunter").put("android").put(JSONObject.NULL)))))
            .put(function("read_command_output", "Read more stdout/stderr from a command, using byte offsets returned by its preceding result. Returns its actual status and exit code. Repeat while running or more_output is true.",
                JSONObject().put("session_id", type("string"))
                    .put("stdout_offset", type("integer")).put("stderr_offset", type("integer"))))
            .put(function("write_stdin", "Write text to a running command's stdin; close_stdin sends EOF after the text. Include a newline when answering a line-oriented prompt.",
                JSONObject().put("session_id", type("string")).put("chars", type("string")).put("close_stdin", type("boolean"))))
            .put(function("stop_command", "Cancel a running command session.", JSONObject().put("session_id", type("string"))))

        private fun type(name: String) = JSONObject().put("type", name)
        private fun nullable(name: String, description: String) = JSONObject()
            .put("type", JSONArray().put(name).put("null")).put("description", description)
        private fun function(name: String, description: String, properties: JSONObject): JSONObject {
            val required = JSONArray()
            properties.keys().forEach { required.put(it) }
            return JSONObject().put("type", "function").put("name", name).put("description", description)
                .put("strict", true).put("parameters", JSONObject().put("type", "object")
                    .put("properties", properties).put("required", required).put("additionalProperties", false))
        }
    }
}
