#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

COMMAND="${1:-}"

run_build() {
  echo "[/build] Running tests and debug build..."
  (
    cd "$PROJECT_DIR"
    ./gradlew testDebugUnitTest assembleDebug
  )
}

run_deploy() {
  echo "[/deploy] Build + emulator deploy..."
  run_build
  SKIP_BUILD=1 "$SCRIPT_DIR/dev_build_install_emulator.sh"
}

run_deploy_prod() {
  echo "[/deploy-prod] Build + emulator deploy + production deploy..."
  run_build
  SKIP_BUILD=1 "$SCRIPT_DIR/dev_build_install_emulator.sh"
  SKIP_BUILD=1 "$SCRIPT_DIR/production_deploy_xperia5.sh"
}

usage() {
  cat <<'EOF'
Usage:
  ./scripts/custom_command.sh /build
  ./scripts/custom_command.sh /deploy
  ./scripts/custom_command.sh /deploy-prod

Commands:
  /build       Run unit tests and assemble debug APK
  /deploy      /build + install to emulator
  /deploy-prod /build + install to emulator + install to production Xperia
EOF
}

case "$COMMAND" in
  /build)
    run_build
    ;;
  /deploy)
    run_deploy
    ;;
  /deploy-prod)
    run_deploy_prod
    ;;
  *)
    usage
    exit 1
    ;;
esac
