#!/usr/bin/env bash
set -euo pipefail

REPO="$GITHUB_REPOSITORY"

runs_json="$(
  gh api --paginate "/repos/${REPO}/actions/runs?per_page=100" |
    jq -s '[.[] | .workflow_runs[]]'
)"

latest_runs="$(
  echo "$runs_json" |
    jq -r '
      group_by(.workflow_id)
      | .[]
      | sort_by(.created_at)
      | reverse
      | .[0]
      | [.workflow_id, .id]
      | @tsv
    '
)"

echo "$runs_json" |
  jq -r '
    group_by(.workflow_id)
    | .[]
    | sort_by(.created_at)
    | reverse
    | .[1:]
    | .[].id
  ' |
  while read -r id; do
    if [[ -n "$id" ]]; then
      gh api --method DELETE "/repos/${REPO}/actions/runs/${id}"
    fi
  done

latest_run_ids="$(echo "$latest_runs" | cut -f2 | paste -sd, -)"

gh api --paginate "/repos/${REPO}/actions/artifacts?per_page=100" |
  jq -s --arg latest "$latest_run_ids" '
    ($latest | split(",") | map(select(length > 0) | tonumber)) as $keep
    | [.[] | .artifacts[]]
    | .[]
    | select(.expired == false)
    | select(
        (.workflow_run.id // 0) as $run
        | ($keep | index($run) | not)
      )
    | .id
  ' |
  while read -r id; do
    if [[ -n "$id" ]]; then
      gh api --method DELETE "/repos/${REPO}/actions/artifacts/${id}"
    fi
  done

gh api --paginate "/repos/${REPO}/actions/caches?per_page=100" |
  jq -r '.actions_caches[] | [.key, .id, .created_at] | @tsv' |
  awk -F '\t' '
    {
      key = $1
      id = $2
      created_at = $3

      if (key ~ /-[0-9a-fA-F]{32,64}$/) {
        sub(/-[0-9a-fA-F]{32,64}$/, "", key)
      }

      print key "\t" id "\t" created_at
    }
  ' |
  sort -t $'\t' -k1,1 -k3,3r |
  awk -F '\t' 'seen[$1]++ { print $2 }' |
  while read -r id; do
    if [[ -n "$id" ]]; then
      gh api --method DELETE "/repos/${REPO}/actions/caches/${id}"
    fi
  done

echo "Cleanup complete."
