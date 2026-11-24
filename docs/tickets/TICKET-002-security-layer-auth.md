# TICKET-002: Hardened Security Layer & Google Credential Manager

- **Status:** ready-for-agent
- **Priority:** p0-critical
- **Spec:** [SPEC-001](../specs/SPEC-001-youtube-android16-no-shorts.md)
- **Dependencies:** TICKET-001

## Description
Implement the security infrastructure for Android 16:
- `SecureStorageManager`: Uses `EncryptedSharedPreferences` backed by `AndroidKeyStore` (AES-256 GCM) to store OAuth access tokens, refresh tokens, user profile info, and API keys.
- `GoogleAuthManager`: Implements Android Credential Manager API (`androidx.credentials.CredentialManager`) with `GetGoogleIdOption`, nonce generation, and SHA-256 state validation.
- `NetworkSecurityConfig`: Strict XML config disabling cleartext traffic and enforcing modern TLS.
- `AuthInterceptor`: Attaches Bearer tokens to authenticated requests while stripping privacy leaks.

## Acceptance Criteria
- Tokens stored only in encrypted storage, never plaintext.
- Login flow handles Credential Manager callbacks gracefully.
- Unit tests verifying encryption/decryption round-trip.
