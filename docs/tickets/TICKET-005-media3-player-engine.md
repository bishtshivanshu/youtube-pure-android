# TICKET-005: Media3 ExoPlayer Engine & Background Playback

- **Status:** ready-for-agent
- **Priority:** p0-critical
- **Spec:** [SPEC-001](../specs/SPEC-001-youtube-android16-no-shorts.md)
- **Dependencies:** TICKET-004

## Description
Implement the high-performance media playback engine using AndroidX Media3:
- `ExoPlayerManager`: Configures adaptive HLS/DASH/MP4 playback, audio attributes for media playback, and state management.
- `PureTubePlaybackService`: Android `MediaSessionService` supporting continuous audio background playback with lock screen media notifications.
- Picture-in-Picture (PiP) controller with Android 16 auto-enter PiP (`setPictureInPictureParams`).
- Player gestures: Double-tap seek (10s), brightness/volume swipes, playback speed changer, quality picker dialog.

## Acceptance Criteria
- Seamless video playback with background playback audio service.
- Smooth PiP entry upon home gesture or back invocation.
