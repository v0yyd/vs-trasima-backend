#!/usr/bin/env bash
set -euo pipefail

# Minimal CLI/REPL to call the Aufgabe 10 REST endpoints one-by-one.
#
# Examples:
#   BASE_URL=http://localhost:8080 bash trasima-aufgabe-10/rest_cli.sh list
#   bash trasima-aufgabe-10/rest_cli.sh get 1
#   bash trasima-aufgabe-10/rest_cli.sh create 1 48 9 1.2 90
#   bash trasima-aufgabe-10/rest_cli.sh update 1 48.2 9.2 2 180
#   bash trasima-aufgabe-10/rest_cli.sh delete 1
#   bash trasima-aufgabe-10/rest_cli.sh   # interactive

BASE_URL="${BASE_URL:-http://localhost:8080}"
API="${BASE_URL%/}/api/trasima/vehicles"

usage() {
  cat <<'EOF'
Usage:
  rest_cli.sh list
  rest_cli.sh get <id>
  rest_cli.sh create <id> <lat> <lon> <speed> <direction>
  rest_cli.sh update <id> <lat> <lon> <speed> <direction>
  rest_cli.sh delete <id>
  rest_cli.sh help

Env:
  BASE_URL=http://localhost:8080
EOF
}

req() {
  local method="$1"; shift
  local url="$1"; shift
  local body="${1:-}"

  if [[ -n "$body" ]]; then
    curl -sS -i -X "$method" \
      -H 'Accept: application/json' \
      -H 'Content-Type: application/json' \
      --data-raw "$body" \
      "$url"
  else
    curl -sS -i -X "$method" -H 'Accept: application/json' "$url"
  fi
  printf '\n'
}

json_body_from_flags() {
  local id="$1"; shift
  local lat="48.0"
  local lon="9.0"
  local speed="1.0"
  local direction="0.0"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --lat) lat="${2:-}"; shift 2 ;;
      --lon) lon="${2:-}"; shift 2 ;;
      --speed) speed="${2:-}"; shift 2 ;;
      --direction) direction="${2:-}"; shift 2 ;;
      *) echo "Unknown option: $1" >&2; return 2 ;;
    esac
  done

  printf '{"id":%s,"lat":%s,"lon":%s,"speed":%s,"direction":%s}' \
    "$id" "$lat" "$lon" "$speed" "$direction"
}

json_body_from_args() {
  local id="$1"; shift
  if [[ "${1:-}" == --* ]]; then
    json_body_from_flags "$id" "$@"
    return $?
  fi

  local lat="${1:?missing lat}"
  local lon="${2:?missing lon}"
  local speed="${3:?missing speed}"
  local direction="${4:?missing direction}"
  printf '{"id":%s,"lat":%s,"lon":%s,"speed":%s,"direction":%s}' \
    "$id" "$lat" "$lon" "$speed" "$direction"
}

run_cmd() {
  local cmd="${1:-help}"; shift || true
  case "$cmd" in
    help|-h|--help)
      usage
      ;;
    list)
      req GET "$API"
      ;;
    get)
      local id="${1:?missing id}"
      req GET "$API/$id"
      ;;
    create)
      local id="${1:?missing id}"; shift
      req POST "$API/$id" "$(json_body_from_args "$id" "$@")"
      ;;
    update)
      local id="${1:?missing id}"; shift
      req PUT "$API/$id" "$(json_body_from_args "$id" "$@")"
      ;;
    delete)
      local id="${1:?missing id}"
      req DELETE "$API/$id"
      ;;
    *)
      echo "Unknown command: $cmd" >&2
      usage >&2
      return 2
      ;;
  esac
}

if [[ $# -gt 0 ]]; then
  run_cmd "$@"
  exit $?
fi

echo "Interactive mode. API: $API"
echo "Commands: list | get <id> | create <id> <lat> <lon> <speed> <direction> | update <id> <lat> <lon> <speed> <direction> | delete <id> | help | quit"

while true; do
  printf 'trasima> '
  IFS= read -r line || break
  [[ -z "$line" ]] && continue
  case "$line" in
    quit|exit) break ;;
  esac
  # shellcheck disable=SC2206
  args=($line)
  run_cmd "${args[@]}" || true
done
