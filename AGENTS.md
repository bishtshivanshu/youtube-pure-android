# AGENTS.md - Agent Workflow & Engineering Guidelines

This repository follows the real engineering practices from Matt Pocock's skills collection (`mattpocock/skills`).

## Core Rules

1. **No Vibe Coding:** Every feature and module must be backed by formal specifications, tracer-bullet tickets, and testable seams.
2. **Shorts Elimination:** The absolute core constraint of this application is zero YouTube Shorts. Any UI element, recommendation shelf, reel, search result, or feed item representing a Short must be filtered out at the domain/data layer before reaching the UI.
3. **Android 16 & Security First:**
   - Target API 36 (Android 16).
   - Use Android Credential Manager API and Google Identity Services for Google OAuth login.
   - Enforce cryptographic protection for all tokens and credentials using Android Keystore and `EncryptedSharedPreferences` (AES-256 GCM).
   - Zero cleartext network traffic via strict Network Security Configuration (`network_security_config.xml`).
   - Maintain strict Activity/Service export controls (`android:exported="false"` unless explicitly intended).
4. **Clean Architecture & Separation of Concerns:**
   - Domain layer has no Android framework dependencies.
   - Data layer handles InnerTube / YouTube API v3 and Room database with the `ShortsFilterEngine` boundary.
   - UI layer uses Jetpack Compose with Material 3 and strict edge-to-edge system bar handling.
5. **Testing at the Seams:**
   - Every filter rule, extractor parser, and security manager must have deterministic unit tests.

## Agent Skills Configuration

Configured files:
- Issue Tracker: `docs/agents/issue-tracker.md`
- Domain & ADRs: `docs/agents/domain.md`
- Triage Labels: `docs/agents/triage-labels.md`
- Specs: `docs/specs/`
- Tickets: `docs/tickets/`
- ADRs: `docs/adr/`
