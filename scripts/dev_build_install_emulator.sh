#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

WIN_SDK_ROOT="${WIN_SDK_ROOT:-/mnt/c/Users/ino9o/AppData/Local/Android/Sdk}"
WIN_ADB="${WIN_ADB:-$WIN_SDK_ROOT/platform-tools/adb.exe}"
WIN_EMULATOR="${WIN_EMULATOR:-$WIN_SDK_ROOT/emulator/emulator.exe}"
AVD_NAME="${AVD_NAME:-Medium_Phone_API_36.1}"
PACKAGE_NAME="${PACKAGE_NAME:-com.example.englishdictionary}"
SKIP_BUILD="${SKIP_BUILD:-0}"

if [[ ! -f "$WIN_ADB" ]]; then
  echo "ERROR: adb.exe not found: $WIN_ADB" >&2
  exit 1
fi
if [[ ! -f "$WIN_EMULATOR" ]]; then
  echo "ERROR: emulator.exe not found: $WIN_EMULATOR" >&2
  exit 1
fi

find_running_emulator() {
  "$WIN_ADB" devices | tr -d '\r' | awk 'NR>1 && $2=="device" && $1 ~ /^emulator-/ {print $1; exit}'
}

ensure_emulator_running() {
  local serial
  serial="$(find_running_emulator)"
  if [[ -n "$serial" ]]; then
    echo "Emulator already running: $serial" >&2
    printf '%s' "$serial"
    return 0
  fi

  if ! "$WIN_EMULATOR" -list-avds | tr -d '\r' | grep -Fxq "$AVD_NAME"; then
    echo "ERROR: AVD not found: $AVD_NAME" >&2
    echo "Available AVDs:" >&2
    "$WIN_EMULATOR" -list-avds | tr -d '\r' >&2
    exit 1
  fi

  echo "Starting emulator: $AVD_NAME" >&2
  nohup "$WIN_EMULATOR" -avd "$AVD_NAME" -netdelay none -netspeed full >/tmp/emulator_${AVD_NAME}.log 2>&1 &

  local i
  for i in $(seq 1 180); do
    serial="$(find_running_emulator)"
    if [[ -n "$serial" ]]; then
      break
    fi
    sleep 1
  done

  if [[ -z "$serial" ]]; then
    echo "ERROR: Emulator did not appear in adb devices within timeout" >&2
    exit 1
  fi

  "$WIN_ADB" -s "$serial" wait-for-device >/dev/null
  for i in $(seq 1 180); do
    local boot
    boot="$("$WIN_ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if [[ "$boot" == "1" ]]; then
      break
    fi
    sleep 1
  done

  echo "Emulator ready: $serial" >&2
  printf '%s' "$serial"
}

if [[ "$SKIP_BUILD" != "1" ]]; then
  echo "Building debug APK..."
  (
    cd "$PROJECT_DIR"
    ./gradlew assembleDebug
  )
else
  echo "Skipping build (SKIP_BUILD=1)"
fi

APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK_PATH" ]]; then
  echo "ERROR: APK not found: $APK_PATH" >&2
  exit 1
fi
WIN_APK_PATH="$(wslpath -w "$APK_PATH")"

SERIAL="$(ensure_emulator_running)"

echo "Installing to $SERIAL ..."
"$WIN_ADB" -s "$SERIAL" install -r "$WIN_APK_PATH"
"$WIN_ADB" -s "$SERIAL" shell pm enable "$PACKAGE_NAME" >/dev/null 2>&1 || true

LAUNCH_COMPONENT="$("$WIN_ADB" -s "$SERIAL" shell cmd package resolve-activity --brief "$PACKAGE_NAME" 2>/dev/null | tr -d '\r' | tail -n 1)"
if [[ -n "$LAUNCH_COMPONENT" && "$LAUNCH_COMPONENT" != "No activity found" ]]; then
  "$WIN_ADB" -s "$SERIAL" shell am start -n "$LAUNCH_COMPONENT" >/dev/null
else
  "$WIN_ADB" -s "$SERIAL" shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 >/dev/null
fi

echo "Done. Installed and launched on $SERIAL"
