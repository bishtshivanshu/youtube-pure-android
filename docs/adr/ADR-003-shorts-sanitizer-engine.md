# ADR-003: Multi-Stage Shorts Sanitization Engine

- **Status:** Accepted
- **Context:**
  The user explicitly specified: "I don't want the ability to see shorts or get any recommendation of shorts. We can also hide the button shorts and we can limit all the recommendation or any things about the shorts."
- **Decision:**
  Shorts filtering is implemented across 4 distinct architectural layers:
  1. **UI Navigation Barrier:**
     - The bottom navigation bar contains strictly 4 destinations: `Home`, `Subscriptions`, `Search`, `Library`.
     - The standard YouTube "Shorts" tab is completely removed from the navigation graph.
  2. **Feed Ingestion Barrier (`ShortsFilterEngine`):**
     - Incoming feed payloads from InnerTube / YouTube API are intercepted by a pure domain filter.
     - Strips any item matching:
       - InnerTube renderers: `reelItemRenderer`, `shortsShelfRenderer`, `reelShelfRenderer`, `richSectionRenderer` containing reel content.
       - Metadata heuristics: Title or tags containing `#shorts`, `#short`, `Shorts`.
       - Duration heuristic: Video duration $\le 60$ seconds with vertical aspect ratio ($height > width$ or 9:16).
       - Navigation endpoints: Endpoints directing to `reelWatchEndpoint` or `/shorts/`.
  3. **Search Results Filter:**
     - Pure long-form search filtering.
     - Search results apply duration filters and strip short-form reels.
  4. **Deep-Link / Intent Interceptor:**
     - If the user clicks an external link to `youtube.com/shorts/{id}`, the app intercepts the intent.
     - Instead of opening an addictive vertical swipe-reel, the app translates the ID into a standard video ID and plays it inside the standard horizontal video player with complete scrubbing controls, descriptions, and comments, or allows the user to disable Shorts playback altogether via a setting toggle.
- **Consequences:**
  - Complete elimination of algorithmically addictive short-form content.
  - Zero accidental triggers or recommendations of Shorts throughout Home, Subscriptions, Search, and Related Videos feeds.
