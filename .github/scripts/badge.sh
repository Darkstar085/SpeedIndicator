#!/usr/bin/env bash
set -euo pipefail

releases_url="https://api.github.com/repos/Darkstar085/SpeedIndicator/releases"
response_file="$(mktemp)"
trap 'rm -f "$response_file"' EXIT

page=1
total=0

while true; do
  curl --fail --silent --show-error \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "${releases_url}?per_page=100&page=${page}" \
    -o "$response_file"

  jq -e 'type == "array"' "$response_file" >/dev/null
  release_count="$(jq 'length' "$response_file")"
  page_total="$(
    jq '[.[] | .assets[]? | select((.name | ascii_downcase) | endswith(".apk")) | (.download_count // 0)] | add // 0' \
      "$response_file"
  )"

  total=$((total + page_total))
  (( release_count < 100 )) && break
  page=$((page + 1))
done

mkdir -p .github/badges
jq -n --arg total "$total" '{
  schemaVersion: 1,
  label: "APK Downloads",
  message: $total,
  color: "00ACC1"
}' > .github/badges/downloads.json

echo "Updated APK-only download count: $total"
