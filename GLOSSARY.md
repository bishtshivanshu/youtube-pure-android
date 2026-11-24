# GLOSSARY

### PureTube Domain Terms

- **Android 16 (API 36 / "Baklava"):** The target platform version. Enforces Edge-to-Edge window rendering, predictive back gestures, 16 KB native library alignment, and modern Credential Manager APIs.
- **Shorts:** Short-form vertical video content on YouTube (typically $\le 60$ seconds, aspect ratio 9:16 or tagged with `#shorts`). The primary goal of PureTube is total zero-exposure to Shorts.
- **ShortsFilterEngine:** The deterministic multi-stage filtering pipeline residing in the Data/Domain boundary that inspects raw API payloads, metadata, aspect ratios, endpoints, and durations to strip Shorts completely before reaching UI state.
- **Credential Manager API (`androidx.credentials`):** The modern Android authentication API combining Passkeys, Google Sign-In, and password autofill under a unified biometric/secure prompt.
- **Hardware-Backed Keystore:** The Android hardware security module (TEE / StrongBox) used to generate and isolate AES-256 GCM keys for encrypting OAuth tokens.
- **Media3 ExoPlayer:** Google's canonical media playback engine for Android providing adaptive HLS/DASH streaming, audio focus management, and background media sessions.
- **InnerTube:** The internal YouTube JSON RPC API used for feed fetching, metadata, and stream formats without Google Play Services telemetry.
- **Tracer-Bullet Ticket:** An end-to-end vertical slice touching database, repository, domain use case, UI component, and test, which leaves the codebase in an always-deployable state.
