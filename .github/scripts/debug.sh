#!/usr/bin/env bash
set -euo pipefail

mkdir -p ~/.android

if [[ ! -f ~/.android/debug.keystore ]]; then
  keytool -genkeypair -v \
    -keystore ~/.android/debug.keystore \
    -storepass android \
    -keypass android \
    -alias androiddebugkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
fi

chmod +x ./gradlew
./gradlew assembleDebug --stacktrace

APK="app/build/outputs/apk/debug/app-debug.apk"
NAMED_APK="SpeedIndicator-${GITHUB_SHA::7}.apk"

test -f "$APK"
mv "$APK" "$NAMED_APK"

SIZE=$(stat -c%s "$NAMED_APK")
echo "APK size: $SIZE bytes"

if [[ "$SIZE" -gt 52428800 ]]; then
  echo "Debug APK exceeds 50 MiB; Telegram upload skipped."
else
  echo "Debug APK is within threshold; Telegram upload disabled."
fi
