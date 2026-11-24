# ADR-002: Security Architecture (Credential Manager, Keystore, TLS 1.3)

- **Status:** Accepted
- **Context:**
  The user requested the highest possible login and app security for their Google Account integration on Android 16.
- **Decision:**
  We adopt a multi-layer defense-in-depth security model:
  1. **Authentication:**
     - Use modern `androidx.credentials:credentials` & `androidx.credentials:credentials-play-services-auth` (Credential Manager API).
     - Support Google Identity OAuth 2.0 with PKCE (Proof Key for Code Exchange).
     - Protect login flows with cryptographic nonce verification against replay attacks.
  2. **Encrypted Storage:**
     - Use `androidx.security:security-crypto` (`EncryptedSharedPreferences`).
     - Backed by Android Keystore with `MasterKey.KeyScheme.AES256_GCM` (utilizing StrongBox / TEE hardware security module).
     - Plaintext tokens, refresh tokens, and user credentials are never written to disk or logged.
  3. **Network Security:**
     - Strict `network_security_config.xml` disabling cleartext HTTP (`cleartextTrafficPermitted="false"`).
     - TLS 1.3/1.2 enforced.
     - Custom OkHttp interceptor stripping unnecessary tracking headers and sanitizing outgoing telemetry.
  4. **Component Isolation:**
     - All activities, services, content providers, and receivers have `android:exported="false"` unless explicitly acting as app launcher or deep-link handler.
     - Deep-link intent filters are strictly constrained to verified video URLs.
  5. **Code Protection:**
     - Release builds run through R8 minifier with logging calls (`Log.d`, `Log.v`, `Log.i`) stripped via `-assumenosideeffects`.
- **Consequences:**
  - Token theft via disk inspection is mitigated even on rooted devices without physical keystore compromise.
  - Zero MITM risk over untrusted Wi-Fi.
