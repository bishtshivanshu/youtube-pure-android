# TICKET-004: YouTube Data Layer & Stream Resolution

- **Status:** ready-for-agent
- **Priority:** p0-critical
- **Spec:** [SPEC-001](../specs/SPEC-001-youtube-android16-no-shorts.md)
- **Dependencies:** TICKET-002, TICKET-003

## Description
Implement the YouTube data extraction and repository layer:
- `YouTubeApiService`: InnerTube API client + YouTube Data API v3 fallback.
- `YouTubeRepository`: Interface and implementation exposing:
  - `getHomeFeed(pageToken: String?): Flow<FeedResult>`
  - `getSubscriptionsFeed(): Flow<FeedResult>`
  - `searchVideos(query: String, filter: SearchFilter): Flow<List<VideoItem>>`
  - `getVideoDetails(videoId: String): Flow<VideoDetails>`
  - `getStreamUrls(videoId: String): Flow<StreamInfo>`
- Automated routing of all incoming feed items through `ShortsFilterEngine`.
- Local Room Database (`PureTubeDatabase`) for caching offline watch history, liked videos, and subscriptions.

## Acceptance Criteria
- Feeds emit clean, Shorts-sanitized streams of `VideoItem`.
- Room DAO handles watch history and bookmarking.
