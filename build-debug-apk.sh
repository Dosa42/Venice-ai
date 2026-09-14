#!/usr/bin/env bash
# ==============================================================================
# Dynamic Adaptive APK Build Runner for Venice AI
# Compatible with: GitHub Actions, Linux CI, and Kali NetHunter Terminal (chroot)
# ==============================================================================

set -e

echo "=== [Venice AI] Dynamic Adaptive Build Runner ==="
echo "Host Kernel: $(uname -a)"
echo "Host Arch:   $(uname -m)"

# Detect Gradle executable
if [ -f "./gradlew" ]; then
    GRADLE_BIN="./gradlew"
    chmod +x "$GRADLE_BIN"
elif command -v gradle >/dev/null 2>&1; then
    GRADLE_BIN="gradle"
else
    echo "[-] Error: Neither ./gradlew nor gradle binary was found in PATH."
    exit 1
fi

echo "[+] Using Gradle: $GRADLE_BIN"

# Check available memory to dynamically adapt worker threads and heap
TOTAL_MEM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}' || echo "4000000")
if [ "$TOTAL_MEM_KB" -lt 4500000 ]; then
    echo "[!] Low RAM detected ($((TOTAL_MEM_KB / 1024)) MB). Applying SM-A326B safe heap limits."
    GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx2048m -Dorg.gradle.parallel=false --max-workers=2"
else
    echo "[+] Standard RAM detected ($((TOTAL_MEM_KB / 1024)) MB). Applying optimal build flags."
    GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx3072m -Dorg.gradle.parallel=true"
fi

BUILD_TYPE="${1:-assembleDebug}"

echo "[+] Executing: $GRADLE_BIN $BUILD_TYPE --stacktrace --no-daemon $GRADLE_OPTS"
$GRADLE_BIN $BUILD_TYPE --stacktrace --no-daemon $GRADLE_OPTS

echo "[+] Discovering output APK..."
APK_FILE=$(find app/build/outputs/apk -type f -name "*.apk" | head -n 1)

if [ -n "$APK_FILE" ]; then
    echo "=============================================================================="
    echo " BUILD SUCCESSFUL"
    echo " APK Path:   $APK_FILE"
    echo " APK Size:   $(du -h "$APK_FILE" | awk '{print $1}')"
    echo " SHA-256:    $(sha256sum "$APK_FILE" | awk '{print $1}')"
    echo "=============================================================================="
else
    echo "[-] Build completed but output APK was not found in app/build/outputs/apk"
fi
