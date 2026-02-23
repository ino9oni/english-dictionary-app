#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

WIN_SDK_ROOT="${WIN_SDK_ROOT:-/mnt/c/Users/ino9o/AppData/Local/Android/Sdk}"
WIN_ADB="${WIN_ADB:-$WIN_SDK_ROOT/platform-tools/adb.exe}"
BUILD_TASK="${BUILD_TASK:-assembleDebug}"
APK_PATH="${APK_PATH:-$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk}"
DEVICE_SERIAL="${DEVICE_SERIAL:-}"
TARGET_PATH="${TARGET_PATH:-}"
PACKAGE_NAME="${PACKAGE_NAME:-com.example.englishdictionary}"
SKIP_BUILD="${SKIP_BUILD:-0}"

if [[ ! -f "$WIN_ADB" ]]; then
  echo "ERROR: adb.exe not found: $WIN_ADB" >&2
  exit 1
fi

connect_wireless_xperia_if_discovered() {
  mapfile -t ENDPOINTS < <(
    "$WIN_ADB" mdns services 2>/dev/null |
      tr -d '\r' |
      awk '/_adb-tls-connect\._tcp/ {print $3}'
  )
  if [[ ${#ENDPOINTS[@]} -eq 0 ]]; then
    return 0
  fi

  echo "Discovered wireless adb endpoints:"
  printf '  %s\n' "${ENDPOINTS[@]}"
  for endpoint in "${ENDPOINTS[@]}"; do
    echo "Trying adb connect $endpoint"
    "$WIN_ADB" connect "$endpoint" >/dev/null 2>&1 || true
  done
}

list_physical_candidates() {
  "$WIN_ADB" devices -l |
    tr -d '\r' |
    awk '
      NR>1 &&
      $2=="device" &&
      $1 !~ /^emulator-/ &&
      $1 !~ /_adb-tls-connect\._tcp$/ { print $0 }
    '
}

select_device_serial() {
  mapfile -t CANDIDATES < <(list_physical_candidates)
  if [[ ${#CANDIDATES[@]} -eq 0 ]]; then
    echo ""
    return 0
  fi

  mapfile -t XPERIA < <(printf '%s\n' "${CANDIDATES[@]}" | grep -Ei 'xperia|xq-|so-5|sog0|so_' || true)
  if [[ ${#XPERIA[@]} -ge 1 ]]; then
    mapfile -t XPERIA_NET < <(printf '%s\n' "${XPERIA[@]}" | awk '$1 ~ /^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:[0-9]+$/ {print $0}')
    if [[ ${#XPERIA_NET[@]} -ge 1 ]]; then
      printf '%s' "${XPERIA_NET[0]}" | awk '{print $1}'
      return 0
    fi
    printf '%s' "${XPERIA[0]}" | awk '{print $1}'
    return 0
  fi

  if [[ ${#CANDIDATES[@]} -eq 1 ]]; then
    printf '%s' "${CANDIDATES[0]}" | awk '{print $1}'
    return 0
  fi

  echo "MULTIPLE"
}

if [[ "$SKIP_BUILD" != "1" ]]; then
  echo "Building APK with task: $BUILD_TASK"
  (
    cd "$PROJECT_DIR"
    ./gradlew "$BUILD_TASK"
  )
else
  echo "Skipping build (SKIP_BUILD=1)"
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "ERROR: APK not found: $APK_PATH" >&2
  exit 1
fi

if [[ -z "$DEVICE_SERIAL" ]]; then
  connect_wireless_xperia_if_discovered
  DEVICE_SERIAL="$(select_device_serial)"
  if [[ -z "$DEVICE_SERIAL" ]]; then
    echo "ERROR: No physical device detected." >&2
    echo "Hint: connect USB or enable Wireless debugging (paired) on Xperia." >&2
    exit 1
  fi
  if [[ "$DEVICE_SERIAL" == "MULTIPLE" ]]; then
    echo "ERROR: Multiple physical devices found. Set DEVICE_SERIAL explicitly." >&2
    list_physical_candidates >&2
    exit 1
  fi
fi

if [[ -z "$TARGET_PATH" ]]; then
  TARGET_PATH="/sdcard/Download/english-dictionary-app-$(date +%Y%m%d-%H%M%S).apk"
fi

WIN_APK_PATH="$(wslpath -w "$APK_PATH")"

echo "Pushing APK to Xperia device: $DEVICE_SERIAL"
"$WIN_ADB" -s "$DEVICE_SERIAL" push "$WIN_APK_PATH" "$TARGET_PATH"
"$WIN_ADB" -s "$DEVICE_SERIAL" shell ls -l "$TARGET_PATH"

echo "Installing APK to Xperia device: $DEVICE_SERIAL"
"$WIN_ADB" -s "$DEVICE_SERIAL" install -r "$WIN_APK_PATH"
"$WIN_ADB" -s "$DEVICE_SERIAL" shell pm path "$PACKAGE_NAME"

echo "Production deploy completed: $TARGET_PATH (installed)"
