# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Native Android password manager. Kotlin + Jetpack Compose (Material 3), MVVM over Clean Architecture, Hilt DI. Single Gradle module `:app`. Package root `com.aminmart.passwordmanager`. minSdk 26, targetSdk/compileSdk 35, JDK 17, Kotlin 2.0.21.

## Build & Test

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release (signs only if keystore.properties + storeFile exist, else unsigned)
./gradlew installDebug           # install on connected device (applicationId suffix .debug)
./gradlew test                   # JVM unit tests (src/test — none exist yet; create the dir when adding the first test)
./gradlew connectedAndroidTest   # instrumented tests (src/androidTest — none exist yet; needs device/emulator)
./gradlew lint
```

Run a single unit test:
```bash
./gradlew test --tests "com.aminmart.passwordmanager.SomeTest.methodName"
```

Release builds produce a single universal APK with R8 minify + resource shrink (no ABI splits — the app has almost no native code). A `Makefile` wraps the common Gradle/adb commands (`make help`).

## Architecture

Layered, dependencies point inward; Hilt wires everything (`@HiltAndroidApp` on `PasswordManagerApplication`, `@AndroidEntryPoint` on `MainActivity`).

- **data/local** — Persistence. `PasswordDatabase` is a hand-written `SQLiteOpenHelper` (NOT Room — README is stale on this; Room deps in `libs.versions.toml` are unused). Two tables: `passwords`, `settings` (key/value). DB version 1; `onUpgrade` drops + recreates (destructive). `PasswordEntity`, `SettingsEntity`, `PasswordCategory` enum live here.
- **data/security** — Crypto services, each `@Singleton @Inject`:
  - `EncryptionService` — AES-256-GCM via Android Keystore (hardware-backed key, unique nonce per op).
  - `SecretEncryptionService` — wraps EncryptionService to encrypt the `{password, notes}` payload → ciphertext + nonce columns.
  - `PasswordHashingService` — PBKDF2WithHmacSHA256, 100k iterations, 256-bit salt/hash. Master-password verification only.
  - `BiometricAuthService` — BiometricPrompt wrapper.
  - `PasswordGeneratorService` — random password generation + strength.
- **data/repository** — `PasswordRepository` (CRUD + encrypt/decrypt, maps Entity↔domain `PasswordEntry`), `VaultRepository` (vault init state + master-password hashing via settings table), `BackupService` (encrypted export/import).
- **domain/model** — `PasswordEntry`, `CreatePasswordInput`, `UpdatePasswordInput`. Plaintext domain types; repos do the crypto boundary.
- **di** — `DatabaseModule` (provides `PasswordDatabase` singleton), `SharedPreferencesModule`.
- **ui/screens/<feature>** — Each feature = `XxxScreen` (Compose) + `XxxViewModel` (`@HiltViewModel`, exposes state via `StateFlow`). Features: `auth`, `passwordlist`, `passworddetail`, `addeditpassword` (shared add+edit), `settings`.
- **ui/navigation** — `Screen` sealed class holds routes (`PasswordDetail`/`EditPassword` take `passwordId` arg via `createRoute`). `AppNavigation` is the NavHost; `startDestination` chosen by `VaultRepository.isVaultInitialized()` (Auth vs list flow).

### Data flow

`Master password → PasswordHashingService (PBKDF2) → hash stored in settings table` for verification. Separately `EncryptionService` holds a Keystore AES key used by `SecretEncryptionService` to encrypt each entry's secret payload into `ciphertext`+`nonce` columns. Plaintext never persisted.

## Conventions

- New screen: add `XxxScreen` + `@HiltViewModel XxxViewModel`, register route in `Screen`, wire `composable` in `AppNavigation`.
- New persisted field: edit `CREATE_*_TABLE` SQL + cursor mappers in `PasswordDatabase`, the matching `*Entity`, and bump `DATABASE_VERSION` (current onUpgrade is destructive — write a real migration if data must survive).
- Hilt uses KSP (not kapt). DI singletons go in a `di/` module.
- Crypto stays inside repositories/security services; ViewModels and UI handle only plaintext domain models.

## Notes

- `keystore.properties` + the `.keystore`/`.jks` file gate release signing; absent → unsigned release.
- No cloud backup (excluded in manifest); backups are app-managed encrypted files via `BackupService`.
