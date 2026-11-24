# TICKET-001: Project Scaffolding & Android 16 Architecture

- **Status:** ready-for-agent
- **Priority:** p0-critical
- **Spec:** [SPEC-001](../specs/SPEC-001-youtube-android16-no-shorts.md)
- **Dependencies:** None

## Description
Scaffold the native Android project configured for Android 16 (API 36 - "Baklava"):
- Gradle Wrapper and root `build.gradle.kts`
- `settings.gradle.kts` with dependency resolution
- `gradle/libs.versions.toml` Version Catalog (Kotlin 2.0+, Compose BOM, Media3 1.5+, Room, Credentials, Crypto)
- `app/build.gradle.kts` targeting `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`
- Android Manifest with `android:enableOnBackInvokedCallback="true"`, exported component hardening, and Network Security Configuration
- ProGuard rules with R8 optimizations and logging stripping in release

## Acceptance Criteria
- Valid Gradle project structure ready to compile with `./gradlew assembleDebug`
- Manifest enforces security boundaries and Android 16 edge-to-edge support
