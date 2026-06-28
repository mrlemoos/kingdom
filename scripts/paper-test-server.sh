#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERVER_DIR="$ROOT/test-server"
PAPER_VERSION="${PAPER_VERSION:-1.21.11}"
PAPER_BUILD="${PAPER_BUILD:-latest}"
PLUGIN_JAR="${PLUGIN_JAR:-$ROOT/target/kingdom-0.1.0-SNAPSHOT.jar}"
TIMEOUT_SECS="${TIMEOUT_SECS:-90}"

if [[ ! -f "$PLUGIN_JAR" ]]; then
  echo "Plugin JAR missing; run: mvn -q package" >&2
  exit 1
fi

mkdir -p "$SERVER_DIR/plugins"

if [[ "$PAPER_BUILD" == "latest" ]]; then
  PAPER_BUILD="$(curl -fsSL "https://api.papermc.io/v2/projects/paper/versions/${PAPER_VERSION}/builds" \
    | python3 -c "import json,sys; print(json.load(sys.stdin)['builds'][-1]['build'])")"
fi

PAPER_JAR="$SERVER_DIR/paper-${PAPER_VERSION}-${PAPER_BUILD}.jar"
if [[ ! -f "$PAPER_JAR" ]]; then
  JAR_NAME="$(curl -fsSL "https://api.papermc.io/v2/projects/paper/versions/${PAPER_VERSION}/builds/${PAPER_BUILD}" \
    | python3 -c "import json,sys; print(json.load(sys.stdin)['downloads']['application']['name'])")"
  DOWNLOAD_URL="https://api.papermc.io/v2/projects/paper/versions/${PAPER_VERSION}/builds/${PAPER_BUILD}/downloads/${JAR_NAME}"
  echo "Downloading Paper ${PAPER_VERSION} build ${PAPER_BUILD}..."
  curl -fsSL "$DOWNLOAD_URL" -o "$PAPER_JAR"
fi

cp "$PLUGIN_JAR" "$SERVER_DIR/plugins/kingdom-0.1.0-SNAPSHOT.jar"

if [[ ! -f "$SERVER_DIR/eula.txt" ]]; then
  echo "eula=true" > "$SERVER_DIR/eula.txt"
fi

if [[ ! -f "$SERVER_DIR/server.properties" ]]; then
  cat > "$SERVER_DIR/server.properties" <<'EOF'
online-mode=false
max-players=2
spawn-protection=0
motd=Kingdom plugin enable test
EOF
fi

LOG_FILE="$SERVER_DIR/latest.log"
rm -f "$LOG_FILE"

echo "Starting Paper test server (${PAPER_VERSION} build ${PAPER_BUILD})..."
(
  cd "$SERVER_DIR"
  java -Xms512M -Xmx512M -jar "$(basename "$PAPER_JAR")" nogui
) > "$LOG_FILE" 2>&1 &
SERVER_PID=$!

cleanup() {
  if kill -0 "$SERVER_PID" 2>/dev/null; then
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

for _ in $(seq 1 "$TIMEOUT_SECS"); do
  if grep -q "Done (" "$LOG_FILE" 2>/dev/null; then
    break
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "Paper exited early. Log tail:" >&2
    tail -n 40 "$LOG_FILE" >&2 || true
    exit 1
  fi
  sleep 1
done

if grep -q "Ambiguous command chains detected" "$LOG_FILE" || \
   grep -q "Error occurred while enabling Kingdom" "$LOG_FILE"; then
  echo "Kingdom failed to enable. Log tail:" >&2
  grep -E "Kingdom|Ambiguous|Exception|Error occurred" "$LOG_FILE" | tail -n 30 >&2 || true
  exit 1
fi

if grep -q "Enabling Kingdom" "$LOG_FILE" && ! grep -q "Disabling Kingdom" "$LOG_FILE"; then
  echo "OK: Kingdom enabled on Paper ${PAPER_VERSION} build ${PAPER_BUILD}."
  exit 0
fi

echo "Timed out or Kingdom did not enable. Log tail:" >&2
tail -n 40 "$LOG_FILE" >&2 || true
exit 1
