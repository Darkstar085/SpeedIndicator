#!/usr/bin/env bash
set -euo pipefail

RELEASE_VERSION="$1"

CURRENT_VERSION="$(
  sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts |
    head -n 1
)"

if [[ -z "$CURRENT_VERSION" ]]; then
  echo "Could not determine versionName."
  exit 1
fi

if [[ "$CURRENT_VERSION" != "$RELEASE_VERSION" ]]; then
  echo "Requested version $RELEASE_VERSION does not match app version $CURRENT_VERSION."
  exit 1
fi

VERSION="$CURRENT_VERSION"
TAG="v$CURRENT_VERSION"
APK_NAME="SpeedIndicator_v$CURRENT_VERSION.apk"

chmod +x ./gradlew
./gradlew assembleRelease --stacktrace

APK="$(
  find app/build/outputs/apk/release \
    -maxdepth 1 \
    -type f \
    -name '*.apk' |
    head -n 1
)"

if [[ -z "$APK" ]]; then
  echo "No release APK was produced."
  exit 1
fi

cp "$APK" "$APK_NAME"
apksigner verify --verbose "$APK"

PREVIOUS_TAG="$(
  gh release list \
    --repo "$GITHUB_REPOSITORY" \
    --limit 100 \
    --json tagName \
    --jq '.[].tagName' |
    grep -vxF "$TAG" |
    head -n 1 || true
)"

{
  echo "## What's new"
  echo

  if [[ -n "$PREVIOUS_TAG" ]]; then
    RANGE="$PREVIOUS_TAG..HEAD"
  else
    RANGE="HEAD"
  fi

  git log "$RANGE" \
    --no-merges \
    --pretty=format:'%s' |
    sed -nE 's/^(feat|style|ui|fix|perf|security)(\([^)]*\))?:[[:space:]]*//p' |
    sed '/^[[:space:]]*$/d' |
    awk '!seen[$0]++ {
      first = toupper(substr($0, 1, 1))
      print "- " first substr($0, 2)
    }'

  if [[ -n "$PREVIOUS_TAG" ]]; then
    echo
    echo "**Full Changelog:** https://github.com/${GITHUB_REPOSITORY}/compare/${PREVIOUS_TAG}...${TAG}"
  fi
} > RELEASE_NOTES.md

if gh release view "$TAG" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
  echo "Release $TAG already exists."
  exit 0
fi

gh release create "$TAG" "$APK_NAME" \
  --repo "$GITHUB_REPOSITORY" \
  --title "Speed Indicator v$VERSION" \
  --notes-file RELEASE_NOTES.md \
  --latest

python3 .github/scripts/telegram.py "$APK_NAME"
