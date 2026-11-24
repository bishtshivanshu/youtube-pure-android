# PureTube - Shorts-Free YouTube Client for Android 16

[![Android 16](https://img.shields.io/badge/Target%20SDK-36%20(Android%2016)-brightgreen.svg)](https://developer.android.com/about/versions/16)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Security](https://img.shields.io/badge/Security-Credential%20Manager%20%2B%20Keystore%20AES--256-red.svg)](#security-architecture)
[![Zero Shorts](https://img.shields.io/badge/Shorts-100%25%20Eliminated-black.svg)](#multi-stage-shorts-eradication-engine)

**PureTube** is a high-performance, security-hardened native YouTube application built specifically for **Android 16 (API 36 / "Baklava")**. It delivers a clean, focused, long-form video experience by **completely eradicating YouTube Shorts** across navigation, recommendation feeds, search results, and deep links.

Developed strictly following the **Matt Pocock real engineering skills methodology** (`mattpocock/skills`) — with formal specifications, architectural decision records (ADRs), tracer-bullet tickets, and seam-based unit testing.

---

## 🌟 Key Features

### 1. 🛡️ 100% Shorts Eradication
- **No Shorts Navigation:** The bottom navigation bar has strictly 4 destinations (`Home`, `Subscriptions`, `Search`, `Library`). The Shorts tab is completely excised.
- **Feed Sanitization (`ShortsFilterEngine`):** Intercepts raw responses to discard `reelItemRenderer`, `shortsShelfRenderer`, `reelShelfRenderer`, and `richSectionRenderer`.
- **Duration & Aspect Heuristics:** Videos under 60 seconds with vertical aspect ratios ($9:16$) or marked with `#shorts` in titles/descriptions are purged from all feeds.
- **Search Filtering:** Quick toggle for long-form content ($> 4$ minutes) to avoid clip clutter.
- **Deep-Link Rewriter:** Any external link to `youtube.com/shorts/{id}` is converted into a standard watch URL, playing inside the standard landscape player with complete controls instead of an addictive vertical swipe-reel.

### 2. 🔐 Defense-in-Depth Security (Google Account Login)
- **Android Credential Manager API (`androidx.credentials`):** Employs modern unified biometric/Google Sign-In with `GetGoogleIdOption`.
- **Replay Attack Defense:** Generates cryptographically secure nonces via `SecureRandom` and SHA-256 for each authentication request.
- **Hardware Keystore Encryption:** OAuth tokens, refresh tokens, user profile info, and API keys are stored in `EncryptedSharedPreferences` backed by `AndroidKeyStore` with **AES-256 GCM** encryption in hardware (StrongBox / TEE).
- **Zero Cleartext Network Traffic:** Enforced via `network_security_config.xml` (`cleartextTrafficPermitted="false"`). All network communication strictly enforces TLS 1.3 / TLS 1.2.
- **Hardened Components & ProGuard:** All internal activities, services, and receivers have `android:exported="false"`. Release builds run through R8 with all `android.util.Log` calls stripped.

### 3. 🎬 Modern Video & Media Engine
- **AndroidX Media3 (ExoPlayer 1.5+):** Adaptive streaming (HLS, DASH, Progressive MP4).
- **Background Audio Playback:** Powered by `PureTubePlaybackService` (`MediaSessionService`) with lock-screen notification controls.
- **Picture-in-Picture (PiP):** Seamless transition to PiP upon home gesture or app switching (`onUserLeaveHint`).
- **Interactive Compose Player Overlay:**
  - Double-tap left/right to seek 10 seconds.
  - Playback speed dialog (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x).
  - Landscape fullscreen toggle with sensor orientation control.
- **Personal Library:** Offline watch history and bookmarks stored locally in Room Database (`PureTubeDatabase`).

### 4. 📱 Android 16 ("Baklava") Native Integration
- Built for `compileSdk = 36` and `targetSdk = 36`.
- Modern **Edge-to-Edge** display handling (`enableEdgeToEdge()`) with dynamic window insets.
- **Predictive Back Gesture** support (`android:enableOnBackInvokedCallback="true"`).
- **16 KB page-size compliance** for modern Android 16 kernels and devices.

---

## 🏗️ Architecture & Matt Pocock Skills

This project adheres strictly to engineering specifications rather than ad-hoc "vibe coding":

```
.
├── AGENTS.md                                # Agent workflow rules
├── GLOSSARY.md                              # Domain terminology
├── docs/
│   ├── agents/
│   │   ├── domain.md                        # ADR and glossary mapping
│   │   ├── issue-tracker.md                 # Ticket tracker mapping (bishtshivanshu)
│   │   └── triage-labels.md                 # Triage label vocabulary
│   ├── adr/
│   │   ├── ADR-001-native-compose-media3.md # Native Compose + Media3
│   │   ├── ADR-002-security-architecture.md # Credential Manager + AES-256
│   │   └── ADR-003-shorts-sanitizer-engine.md # Multi-stage shorts filtering
│   ├── specs/
│   │   └── SPEC-001-youtube-android16-no-shorts.md # Formal system specification
│   └── tickets/
│       ├── TICKET-001-project-scaffold-android16.md
│       ├── TICKET-002-security-layer-auth.md
│       ├── TICKET-003-shorts-sanitizer-engine.md
│       ├── TICKET-004-youtube-data-repository.md
│       ├── TICKET-005-media3-player-engine.md
│       ├── TICKET-006-jetpack-compose-ui.md
│       └── TICKET-007-ci-cd-and-release.md
├── app/
│   ├── src/main/java/com/puretube/app/
│   │   ├── core/
│   │   │   ├── player/                      # Media3 ExoPlayer & PlaybackService
│   │   │   ├── security/                    # CredentialManager & Keystore
│   │   │   └── shorts/                      # Deterministic ShortsFilterEngine
│   │   ├── data/
│   │   │   ├── api/                         # InnerTube & Data API client
│   │   │   ├── db/                          # Room Database (History & Saved)
│   │   │   └── repository/                  # YouTubeRepositoryImpl
│   │   ├── domain/model/                    # VideoItem, StreamInfo, ChannelItem
│   │   └── ui/
│   │       ├── components/                  # VideoCard, VideoPlayerView
│   │       ├── navigation/                  # 4-tab NavHost (No Shorts!)
│   │       ├── screens/                     # Home, Subs, Search, Library, Player
│   │       └── theme/                       # Material 3 Dynamic Theme
│   └── src/test/                            # Seam unit tests (Shorts & Repos)
└── .github/workflows/
    └── android-build.yml                    # Automated APK CI/CD pipeline
```

---

## 📲 How to Install on Your Android 16 Phone

### Method A: Automated GitHub Actions Build (Recommended)
1. Push this repository to your GitHub account:
   ```bash
   git remote add origin https://github.com/bishtshivanshu/youtube-pure-android.git
   git push -u origin main
   ```
2. In your GitHub repository, open the **Actions** tab.
3. The **Build Android 16 APK** workflow runs automatically on commit.
4. Click on the latest workflow run and download the `PureTube-Android16-Debug-APK` zip file.
5. Transfer the `.apk` file to your Android 16 phone (or download it directly on your phone via Chrome).
6. Tap the `.apk` file and tap **Install** (allow "Install unknown apps" if prompted).

### Method B: Build Locally via Android Studio / Gradle
1. Clone or open this folder in **Android Studio Ladybug (or newer)**.
2. Ensure you have **JDK 21** selected (`Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`).
3. Connect your Android 16 phone via USB with **USB Debugging** enabled in Developer Options.
4. Run the Gradle build:
   ```bash
   ./gradlew assembleDebug
   ```
5. Install onto device:
   ```bash
   ./gradlew installDebug
   ```

---

## 🔑 Google Account & Web Client ID Setup

To connect your real Google Account via Android Credential Manager:
1. Open the [Google Cloud Console](https://console.cloud.google.com/).
2. Create an **OAuth 2.0 Client ID** of type **Web application**.
3. In `PureTubeApplication.kt` (or via the in-app Library settings), provide your Web Client ID:
   ```kotlin
   googleAuthManager.setWebClientId("YOUR_CLIENT_ID.apps.googleusercontent.com")
   ```
4. On your Android phone, tapping **Secure Sign In with Google** will invoke the native Android 16 Credential Manager bottom sheet.

---

## 🧪 Seam Testing

Run unit tests locally with:
```bash
./gradlew testDebugUnitTest
```
Tests in `ShortsFilterEngineTest` verify:
- Complete removal of vertical shorts ($\le 60$s).
- Detection and stripping of `#shorts` tags.
- Discarding of InnerTube reel containers.
- Preservation of regular landscape video content.
- URL conversion from shorts endpoints to full watch endpoints.

---

## 👤 Author & Attribution
- **Developer:** Shivanshu Bisht ([@bishtshivanshu](https://github.com/bishtshivanshu))
- **Engineering Discipline:** [Matt Pocock Skills](https://github.com/mattpocock/skills)
- **License:** Apache 2.0
