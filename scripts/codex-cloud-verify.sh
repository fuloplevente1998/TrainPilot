#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$ROOT"

for tool in java node npm git adb sdkmanager; do
  if ! command -v "$tool" >/dev/null; then
    echo "Required tool is missing: $tool" >&2
    exit 1
  fi
done

java_major="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.specification.version = //p' | head -n 1)"
node_major="$(node -p 'process.versions.node.split(".")[0]')"
if [[ "$java_major" != "21" ]]; then
  echo "Expected Temurin JDK 21, found Java ${java_major:-unknown}." >&2
  exit 1
fi
if [[ "$node_major" != "22" ]]; then
  echo "Expected Node.js 22, found Node.js $node_major." >&2
  exit 1
fi
if [[ -z "${ANDROID_HOME:-}" || "${ANDROID_SDK_ROOT:-}" != "$ANDROID_HOME" ]]; then
  echo "ANDROID_HOME and ANDROID_SDK_ROOT must point to the same SDK directory." >&2
  exit 1
fi

printf 'Java: '; java -version 2>&1 | head -n 1
printf 'Node.js: '; node --version
printf 'npm: '; npm --version
printf 'Git: '; git --version
printf 'Android SDK: %s\n' "$ANDROID_HOME"
adb version | head -n 1

cd "${ROOT}/android"
chmod +x gradlew
./gradlew --no-daemon --version
projects="$(./gradlew --no-daemon projects)"
for module in app wear shared; do
  if ! grep -Fq "Project ':${module}'" <<<"$projects"; then
    echo "Gradle module is missing: :$module" >&2
    exit 1
  fi
done

./gradlew --continue --no-daemon --stacktrace \
  :app:assembleDebug \
  :wear:assembleDebug \
  :app:testDebugUnitTest \
  :wear:testDebugUnitTest \
  :shared:testDebugUnitTest

cd "$ROOT"
node --test scripts/ui-runner.test.cjs
node --test android/wear/ci/release-gates.test.cjs

if [[ -f package.json && -f package-lock.json ]]; then
  npm test
else
  echo "npm test skipped: this branch has no package.json/package-lock.json."
fi
