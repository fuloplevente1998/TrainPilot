#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
if [[ "$(uname -s)" != "Linux" ]]; then
  echo "TrainPilot Codex Cloud setup requires Linux." >&2
  exit 1
fi

case "$(uname -m)" in
  x86_64)
    ADOPTIUM_ARCH="x64"
    NODE_ARCH="x64"
    ;;
  aarch64|arm64)
    ADOPTIUM_ARCH="aarch64"
    NODE_ARCH="arm64"
    ;;
  *)
    echo "Unsupported Linux architecture: $(uname -m)" >&2
    exit 1
    ;;
esac

if ! command -v curl >/dev/null || ! command -v unzip >/dev/null || ! command -v python3 >/dev/null || ! command -v git >/dev/null; then
  if command -v apt-get >/dev/null; then
    if [[ "${EUID}" -eq 0 ]]; then
      APT=(apt-get)
    elif command -v sudo >/dev/null; then
      APT=(sudo apt-get)
    else
      echo "Install curl, unzip, python3 and git, or provide apt-get with sudo access." >&2
      exit 1
    fi
    "${APT[@]}" update
    "${APT[@]}" install -y --no-install-recommends bash ca-certificates curl git python3 tar unzip xz-utils
  else
    echo "Missing curl, unzip or python3, and apt-get is unavailable." >&2
    exit 1
  fi
fi

TOOL_HOME="${HOME}/.local/share/trainpilot"
CACHE_HOME="${XDG_CACHE_HOME:-${HOME}/.cache}/trainpilot"
TEMURIN_HOME="${TOOL_HOME}/temurin-21"
ANDROID_HOME="${ANDROID_HOME:-${HOME}/.android/sdk}"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-${HOME}/.gradle}"
NPM_CONFIG_CACHE="${NPM_CONFIG_CACHE:-${HOME}/.npm}"
mkdir -p "$TOOL_HOME" "$CACHE_HOME" "$ANDROID_HOME" "$GRADLE_USER_HOME" "$NPM_CONFIG_CACHE"

if [[ ! -x "${TEMURIN_HOME}/bin/java" ]] || ! "${TEMURIN_HOME}/bin/java" -version 2>&1 | grep -Eq 'version "21[.]|openjdk 21'; then
  if [[ -e "$TEMURIN_HOME" ]]; then
    echo "Unexpected existing Temurin directory: $TEMURIN_HOME" >&2
    exit 1
  fi
  archive="${CACHE_HOME}/temurin-21-linux-${ADOPTIUM_ARCH}.tar.gz"
  curl -fL --retry 3 \
    "https://api.adoptium.net/v3/binary/latest/21/ga/linux/${ADOPTIUM_ARCH}/jdk/hotspot/normal/eclipse" \
    -o "$archive"
  install_dir="$(mktemp -d "${TOOL_HOME}/temurin-21.XXXXXX")"
  tar -xzf "$archive" --strip-components=1 -C "$install_dir"
  mv "$install_dir" "$TEMURIN_HOME"
fi
JAVA_HOME="$TEMURIN_HOME"

if ! command -v node >/dev/null || ! command -v npm >/dev/null || [[ "$(node -p 'process.versions.node.split(".")[0]')" != "22" ]]; then
  node_version="$(curl -fsSL --retry 3 https://nodejs.org/dist/index.json | python3 -c 'import json,sys; print(next(item["version"] for item in json.load(sys.stdin) if item["version"].startswith("v22.")))')"
  NODE_HOME="${TOOL_HOME}/node-${node_version}"
  if [[ ! -x "${NODE_HOME}/bin/node" ]]; then
    archive="${CACHE_HOME}/${node_version}-linux-${NODE_ARCH}.tar.xz"
    curl -fL --retry 3 "https://nodejs.org/dist/${node_version}/node-${node_version}-linux-${NODE_ARCH}.tar.xz" -o "$archive"
    install_dir="$(mktemp -d "${TOOL_HOME}/node.XXXXXX")"
    tar -xJf "$archive" --strip-components=1 -C "$install_dir"
    mv "$install_dir" "$NODE_HOME"
  fi
  PATH="${NODE_HOME}/bin:${PATH}"
fi

if [[ ! -x "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" ]]; then
  if [[ -e "${ANDROID_HOME}/cmdline-tools/latest" ]]; then
    echo "Android command-line tools directory exists but sdkmanager is missing." >&2
    exit 1
  fi
  archive="${CACHE_HOME}/commandlinetools-linux-13114758_latest.zip"
  curl -fL --retry 3 \
    https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip \
    -o "$archive"
  install_dir="$(mktemp -d "${CACHE_HOME}/android-cmdline-tools.XXXXXX")"
  unzip -q "$archive" -d "$install_dir"
  mkdir -p "${ANDROID_HOME}/cmdline-tools"
  mv "${install_dir}/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest"
  rmdir "$install_dir"
fi

ANDROID_SDK_ROOT="$ANDROID_HOME"
PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${JAVA_HOME}/bin:${PATH}"
export JAVA_HOME ANDROID_HOME ANDROID_SDK_ROOT GRADLE_USER_HOME NPM_CONFIG_CACHE PATH

SDKMANAGER="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
set +o pipefail
yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null
license_status="${PIPESTATUS[1]}"
set -o pipefail
if [[ "$license_status" -ne 0 ]]; then
  echo "Android SDK license acceptance failed." >&2
  exit "$license_status"
fi

"$SDKMANAGER" --sdk_root="$ANDROID_HOME" \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0"

ENV_DIR="${HOME}/.config/trainpilot"
ENV_FILE="${ENV_DIR}/codex-cloud-env.sh"
mkdir -p "$ENV_DIR"
cat > "$ENV_FILE" <<EOF
export JAVA_HOME="${JAVA_HOME}"
export ANDROID_HOME="${ANDROID_HOME}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME}"
export NPM_CONFIG_CACHE="${NPM_CONFIG_CACHE}"
export PATH="\${JAVA_HOME}/bin:\${ANDROID_HOME}/cmdline-tools/latest/bin:\${ANDROID_HOME}/platform-tools:${PATH}"
EOF

for startup_file in "${HOME}/.bashrc" "${HOME}/.profile"; do
  touch "$startup_file"
  if ! grep -Fq 'trainpilot/codex-cloud-env.sh' "$startup_file"; then
    printf '\n[ -f "$HOME/.config/trainpilot/codex-cloud-env.sh" ] && source "$HOME/.config/trainpilot/codex-cloud-env.sh"\n' >> "$startup_file"
  fi
done

cd "$ROOT"
if [[ -f package.json && -f package-lock.json ]]; then
  npm ci --cache "$NPM_CONFIG_CACHE" --prefer-offline
elif [[ -e package.json || -e package-lock.json ]]; then
  echo "An npm manifest exists without its lockfile; refusing a non-reproducible npm install." >&2
  exit 1
else
  echo "No npm manifest on this native migration branch; Node checks use built-in Node.js modules."
fi

cd "${ROOT}/android"
chmod +x gradlew
./gradlew --no-daemon --version
./gradlew --no-daemon projects
dependency_log="${CACHE_HOME}/gradle-dependencies.log"
if ! ./gradlew --no-daemon :app:dependencies :wear:dependencies :shared:dependencies >"$dependency_log" 2>&1; then
  cat "$dependency_log" >&2
  exit 1
fi

echo "TrainPilot Android toolchain installed. Run scripts/codex-cloud-verify.sh for builds and tests."
