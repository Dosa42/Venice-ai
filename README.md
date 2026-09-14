# Venice AI Android

Kotlin/Jetpack Compose Android application. The existing model backend is Google's
Gemini API; the Venice branding does not change the configured API provider.

## Local Files

1. Open **Files** and enter an absolute device path. The initial path is `/root`.
2. Tap **Open directory** or **Read text**, then grant `su` access in your device's
   root manager if prompted.
3. Open a directory entry or preview a text file. Symlinks have separate **Open**
   and **Read** actions. **Up** navigates to the entered path's parent.
4. Tap **Attach listing to chat** or **Attach text to chat**. Review the attachment
   in Chat, add your question, and press **Send** to submit it to Gemini.

Browsing never sends a network request. Only the selected snapshot is attached;
directory listings do not recursively read files. One pending attachment is shown
at a time. Selecting another replaces it. It can be removed before sending.
Previously sent snapshots remain part of that conversation's subsequent Gemini
requests. They are held in app memory and omitted from the explicit Firebase
message serialization; user questions and model answers still follow the app's
existing cloud-sync setting. Model answers can quote attached data.

The module uses `Runtime.getRuntime().exec("su")` with fixed read-only scripts.
Each invocation verifies UID 0. It does not expose terminal command entry or
execute text from files or model responses. Paths are single-argument shell
quoted, and directory records are NUL framed to preserve spaces, quotes and
newlines. Snapshot text is JSON encoded as a separate Gemini content part, with
system instructions identifying it as untrusted observed data.

Limits: 20 seconds per operation, 512 captured directory entries, 2 MiB retained
directory output, 32 KiB per text file, 8 KiB retained error output. Truncation is
visible. Binary/NUL-containing or invalid UTF-8 data is rejected. A partial final
UTF-8 character is omitted at the capture boundary. stdout and stderr are drained
while the process runs. Cancellation requests root-process termination and closes its
pipes; a root manager may manage child-process lifetimes separately. Reads capture a point-in-time observation, not an atomic filesystem image.

`/root` must actually exist in the filesystem visible to the app's `su` process.
Android root access does not automatically enter a NetHunter chroot or discover
its root directory. Enter its real accessible path if needed. Root denial,
missing `su`, missing paths, command errors and timeouts are shown directly;
there is no alternate-path fallback.

## Build

Required: JDK 17 or newer, Android SDK platform **36.1**, Android Build Tools
**36.0.0**, and access to the configured Gradle/Maven repositories for uncached
dependencies. The checked-in Gradle **9.3.1** wrapper validates its distribution
SHA-256. Configure `ANDROID_HOME` or `sdk.dir` in an ignored `local.properties`.

```sh
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. Debug signing uses Android's
standard automatically generated debug keystore. Release signing still requires
the existing `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` configuration.

For real Gemini requests, create an ignored `.env` file containing
`GEMINI_API_KEY=your_actual_key` before building. Without a configured key, chat
reports an error. This build-time key is packaged in the APK; keep keyed builds
private. Optional Firebase authentication/sync requires your own
`app/google-services.json` and provider configuration.

Focused verification:

```sh
./gradlew :app:testDebugUnitTest --tests 'com.example.data.local.*' --tests 'com.example.data.api.GeminiLocalContextTest' --tests 'com.example.ui.viewmodel.LocalContextLifecycleTest'
```

The local backend tests execute real host `/bin/sh` processes and filesystem
operations. They do not establish that Magisk/KernelSU has granted access on a
phone. On-device root and live Gemini integration require the target rooted
device and a valid API key.
