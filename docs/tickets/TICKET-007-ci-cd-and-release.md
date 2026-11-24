# TICKET-007: GitHub Actions CI/CD & Android 16 APK Build Pipeline

- **Status:** ready-for-agent
- **Priority:** p1-high
- **Spec:** [SPEC-001](../specs/SPEC-001-youtube-android16-no-shorts.md)
- **Dependencies:** TICKET-006

## Description
Create GitHub Actions CI/CD workflows and local build scripts for the user's repository (`bishtshivanshu/youtube-pure-android`):
- `.github/workflows/android-build.yml`:
  - Sets up JDK 21 (required for Android 16 / AGP 8.7+)
  - Runs unit tests (`./gradlew testDebugUnitTest`)
  - Assembles Debug and Release APK (`./gradlew assembleDebug assembleRelease`)
  - Uploads APK artifacts to GitHub Releases / workflow run artifacts for 1-click download and installation on physical Android 16 device.
- Comprehensive `README.md` with complete installation instructions, sideloading guide, Google OAuth client setup, and security architecture documentation.

## Acceptance Criteria
- Valid GitHub Actions workflow ready to build APK in cloud.
- Clear setup and sideloading documentation for Android 16.
