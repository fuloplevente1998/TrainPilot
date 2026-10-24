# TrainPilot Codex Cloud environment

The reusable Linux toolchain setup is `scripts/codex-cloud-setup.sh`; the
verification/build entry point is `scripts/codex-cloud-verify.sh`.

In Codex Cloud, create an environment for `fuloplevente1998/TrainPilot` using
`feat/native-android-finalization`, select the setup script above, and enable
outbound network access for the official package hosts below. The scripts do
not use release credentials, create signing keys, install an APK, or modify
application data.

The setup installs Temurin JDK 21, Node.js 22 with npm, Android SDK command-line
tools, platform-tools, Android API 36 and Build Tools 36.0.0. Gradle 8.13,
Android Gradle Plugin 8.13.0, and Kotlin/Compose 2.2.21 are resolved through
the checked-in Android Gradle wrapper and active build files. Java/Kotlin
bytecode stays at 17. `ANDROID_HOME` and `ANDROID_SDK_ROOT` point to the same
SDK; the shell profile loads these variables in later commands.

Gradle uses the reusable `~/.gradle` cache; npm uses `~/.npm` unless the
environment overrides `GRADLE_USER_HOME` or `NPM_CONFIG_CACHE`. This migration
branch currently has no root npm manifest or lockfile, so `npm ci` is
intentionally skipped rather than inventing a dependency manifest. The
Node-based checks use Node's built-in test runner and require no npm packages.

Allow outbound HTTPS to:

- `api.adoptium.net` for Temurin 21
- `nodejs.org` for Node.js 22
- `dl.google.com` and `maven.google.com` for Android SDK and Google Maven artifacts
- `services.gradle.org` and `plugins.gradle.org` for Gradle distributions/plugins
- `repo.maven.apache.org` for Maven Central
- `registry.npmjs.org` when a locked npm project is later present
- Ubuntu package mirrors when base Linux packages are missing

Run `bash scripts/codex-cloud-verify.sh` after setup. It checks tool versions
and SDK variables, confirms `:app`, `:wear`, and `:shared`, builds phone/Wear
debug APKs, runs the three native unit-test tasks, and executes the Node-only
checks. Signed release APKs remain a GitHub Actions operation using the
existing repository secrets.

Codex Cloud environment records and their network-access policy are managed in
the Codex Cloud UI; this repository supplies the repeatable setup and
verification scripts but cannot create or authorize that cloud resource.
