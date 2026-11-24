# TICKET-006: Jetpack Compose UI & Shorts-Free Navigation

- **Status:** ready-for-agent
- **Priority:** p0-critical
- **Spec:** [SPEC-001](../specs/SPEC-001-youtube-android16-no-shorts.md)
- **Dependencies:** TICKET-005

## Description
Build the complete Jetpack Compose UI with Material 3:
- Navigation bar with strictly 4 tabs: `Home`, `Subscriptions`, `Search`, `Library`. (No Shorts tab).
- Screens:
  - `HomeScreen`: Video cards with thumbnails, channel avatars, view count, publish time, pull-to-refresh.
  - `SubscriptionsScreen`: Channel chips horizontally scrollable, feed of latest long-form uploads.
  - `SearchScreen`: Search bar with instant query suggestions, search filters, and video list.
  - `LibraryScreen`: User profile header with login/logout button, watch history, liked videos, and settings.
  - `PlayerScreen`: Seamless inline/fullscreen player with video details, channel subscribe button, like button, comments drawer, and related videos list (filtered).
- Strict Android 16 Edge-to-Edge window insets handling (`WindowInsets.systemBars`).

## Acceptance Criteria
- Complete Compose UI tree without any Shorts entrypoint.
- Seamless navigation between feeds and active video player.
