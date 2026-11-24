# ADR-001: Android 16 Native Architecture with Jetpack Compose & Media3

- **Status:** Accepted
- **Context:**
  The user requested an Android client specifically built for Android 16 with high security, full video playback capabilities, and installability on a physical device.
- **Decision:**
  We choose Native Android (Kotlin) targeting `compileSdk = 36` (Android 16 "Baklava") using:
  1. **UI Layer:** Jetpack Compose with Material 3, Dynamic Theming, Edge-to-Edge window insets (`enableEdgeToEdge()`).
  2. **Media Engine:** AndroidX Media3 (ExoPlayer 1.5+) for adaptive streaming, background audio playback with `MediaSessionService`, and Picture-in-Picture (PiP).
  3. **Architecture:** Clean Architecture + MVI/MVVM with unidirectional data flow (StateFlow & SharedFlow).
  4. **Persistence:** Room Database with SQLCipher capability for local history and cached feeds.
- **Consequences:**
  - Optimal performance on Android 16 without cross-platform overhead.
  - Native integration with Android 16 system APIs (Predictive back, Credential Manager, Picture-in-Picture).
  - Clean separation allowing deterministic unit testing on JVM without Android emulator.
