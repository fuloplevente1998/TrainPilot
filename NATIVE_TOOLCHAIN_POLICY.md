# TrainPilot native build toolchain policy

Recorded: 2026-10-22
Branch: `feat/native-android-migration`

## Pinned baseline (until an explicit tested upgrade)

- GitHub Actions build JDK: **Temurin 21 LTS**.
- Java/Kotlin Android bytecode target: **17** (not the same as the JDK used to run Gradle).
- Android Gradle Plugin: **8.13.0**.
- Gradle wrapper: **8.13**.
- Kotlin Gradle plugin, serialization and Compose compiler plugin: **2.4.21**, aligned.
- Android compile SDK: **35** (unchanged pending API/permissions audit).
- Phone target SDK: **35**, Wear target SDK: **34** (unchanged pending platform behavior tests).
- Existing AndroidX, Compose BOM, Room, Wear and Health Connect dependencies remain pinned until individually verified.

## Rules for future changes

1. Use stable releases only; do not use preview, beta or RC versions for the release pipeline.
2. Update the Gradle wrapper and Android Gradle Plugin as a compatible pair, using the official Android compatibility table.
3. Keep Kotlin and Compose compiler plugin on matching versions.
4. Prefer Java 21 LTS to execute Gradle, but preserve JVM bytecode 17 until runtime/API compatibility has been validated.
5. Do not upgrade AGP to 9.x without migrating the Kotlin/AGP plugin configuration and checking KAPT/KSP/Compose support.
6. Never change compileSdk/targetSdk as a side effect of build tool upgrades; test Android and Wear behavior changes first.
7. Every dependency upgrade requires green `:app:assembleDebug`, `:wear:assembleDebug`, unit tests and signed release gates before merging into `main`.
8. Preserve the known-working build baseline and make upgrades on this feature branch.
9. Revisit versions periodically, but never automatically float versions in Gradle builds.

## References

- https://developer.android.com/build/releases/about-agp
- https://docs.gradle.org/current/userguide/compatibility.html
- https://kotlinlang.org/docs/releases.html

## Status

Configuration committed; successful GitHub Actions build and test verification **pending**. Do not mark this baseline as validated until CI passes.
