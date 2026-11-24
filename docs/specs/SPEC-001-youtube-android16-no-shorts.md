# SPEC-001: PureTube - Android 16 Shorts-Free YouTube Client

- **Author:** Shivanshu Bisht (`bishtshivanshu`) & AI Assistant
- **Target OS:** Android 16 (API Level 36 - "Baklava")
- **Status:** Approved / In Progress

---

## 1. Problem Statement

Modern YouTube applications are inundated with short-form algorithmically addictive vertical content ("Shorts"). For users wanting a productive, focused media consumption experience on their modern Android 16 devices, Shorts create distraction, dopamine hijacking, and cognitive clutter. Furthermore, users require authentic Google Account login to sync their real subscriptions and playlists, but demand top-tier Android 16 cryptographic security without exposing credentials or falling victim to token theft.

## 2. Solution Overview

**PureTube** is a native Android 16 client built with Kotlin, Jetpack Compose, Material 3, and Media3 ExoPlayer.
It provides:
1. **Total Shorts Eradication:** The Shorts tab is completely excised from the UI. A deterministic multi-stage `ShortsFilterEngine` strips all Shorts shelves, reels, short vertical videos ($\le 60$s), and `#shorts` metadata from Home, Subscriptions, Search, and Related recommendations.
2. **Hardened Security Architecture:** Authenticates via modern Android Credential Manager API (`androidx.credentials`) and Google Identity Services with PKCE. Tokens are AES-256 GCM encrypted in hardware Keystore (`EncryptedSharedPreferences`). Network traffic strictly enforces TLS 1.3 with cleartext disabled.
3. **Full Standard YouTube Capabilities:** Full-screen adaptive video streaming (HLS/DASH/MP4), gesture controls, playback speed selector, resolution selector, audio-only background playback, Picture-in-Picture (PiP), search with auto-suggestions, subscription feeds, and personal library (history & playlists).
4. **Android 16 Native Integration:** Edge-to-Edge display with dynamic system bar insets, predictive back gestures, and 16 KB page-size compliance.

---

## 3. User Stories

1. **US-001 (Shorts-Free Navigation):** As a user, I want a bottom navigation bar with only Home, Subscriptions, Search, and Library, so that I am never tempted or forced to view a Shorts tab.
2. **US-002 (Clean Home Feed):** As a user, I want the Home feed to display only regular long-form videos without any Shorts carousels or reels, so that I can browse content peacefully.
3. **US-003 (Clean Subscriptions Feed):** As a user, I want my Subscriptions feed to show standard uploads from my subscribed channels and purge all Short uploads, so that my feed is high quality.
4. **US-004 (Clean Search Experience):** As a user, I want search queries to filter out Shorts reels and allow duration filtering, so that I only find full videos.
5. **US-005 (Secure Google Login):** As a user, I want to securely log in with my Google Account using Android Credential Manager and PKCE, so that my account credentials and tokens are cryptographically protected in hardware.
6. **US-006 (ExoPlayer Media Playback):** As a user, I want high-definition video playback with double-tap seek, brightness/volume gestures, quality picking, and speed selection.
7. **US-007 (Background Playback & PiP):** As a user, I want audio playback to continue when locking the screen or switching apps, and smooth Picture-in-Picture support.
8. **US-008 (Personal Library & History):** As a user, I want to access my watch history and playlists stored locally and synchronized with my account.
9. **US-009 (Android 16 Edge-to-Edge):** As a user, I want the app to utilize full Android 16 edge-to-edge system insets with Material You dynamic theming.

---

## 4. Implementation Decisions & Seams

### Architecture Modules
- `:app`
  - `com.puretube.app.core.security` (Credential Manager, Keystore, EncryptedPrefs, NetworkSecurity)
  - `com.puretube.app.core.shorts` (`ShortsFilterEngine`, `ShortsFilterRules`)
  - `com.puretube.app.core.player` (Media3 ExoPlayer manager, `PlaybackService`, PiP controller)
  - `com.puretube.app.data.api` (InnerTube client, YouTube Data API v3 client, OkHttp with TLS 1.3)
  - `com.puretube.app.data.db` (Room Database, VideoEntity, HistoryEntity)
  - `com.puretube.app.data.repository` (YouTubeRepository, AuthRepository)
  - `com.puretube.app.ui.navigation` (NavHost, BottomNavBar - Home, Subs, Search, Library)
  - `com.puretube.app.ui.screens` (Home, Subscriptions, Search, Player, Library, Auth)
  - `com.puretube.app.ui.theme` (Material 3 Dynamic Theme, Typography, Shape)

### Testing Seams
1. **`ShortsFilterEngineTest`:** Pure Kotlin unit tests validating that Shorts reels, `reelItemRenderer`, durations $\le 60$s with vertical aspect, and `#shorts` titles are 100% stripped from input feeds.
2. **`SecurityStorageTest`:** Validates AES-256 GCM token encryption and decryption.
3. **`FeedParserTest`:** Validates deserialization of video cards, channels, and pagination tokens.
4. **`NavigationGraphTest`:** Verifies that no route or bottom navigation item exists for Shorts.
