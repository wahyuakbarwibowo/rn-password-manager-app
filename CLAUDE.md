# Aminmart Password Manager

Offline password manager app built with React Native Expo.

> ⚠️ **Security Disclaimer**: This project is for learning and experimentation only.  
> The encryption/decryption logic and overall architecture have not been audited and **must not** be used to protect real passwords or sensitive data in production.

## Tech Stack

- **Framework**: React Native with Expo (SDK 54, Expo Router v6)
- **Language**: TypeScript (strict mode)
- **Database**: expo-sqlite (local SQLite)
- **Encryption**: Simple XOR-based scheme driven by a user master password (for learning only; cryptographically weak and not suitable for production)
- **Navigation**: Stack navigator (expo-router)

## Project Structure

```
app/                    — Screens (file-based routing)
  index.tsx             — Password list (home)
  passwords/add.tsx     — Add password (modal)
  passwords/[id].tsx    — View password detail
  passwords/edit/[id].tsx — Edit password (modal)

components/             — Reusable UI components
constants/              — Theme colors, category definitions
hooks/                  — useColorScheme, useThemeColor
lib/                    — Core logic
  crypto.ts             — AES-256 encrypt/decrypt, password generator
  database.ts           — SQLite init (singleton)
  password-service.ts   — CRUD operations
types/                  — TypeScript interfaces
```

## Commands

- `bun start` — Start Expo dev server
- `bun run android` — Start on Android
- `bun run ios` — Start on iOS
- `bun run lint` — Run ESLint
- `bunx tsc --noEmit` — Type check

## Recent Changes

- Added safe-area wrapping to the add-password modal so the input form avoids notches and on-screen cameras, matching the new tab layout.
- Dashboard now exposes categorical summaries and fixes the biometric warning banner route so it navigates to `/settings`.
- Lock screen blur: `expo-blur` now darkens the view while the biometric/fallback prompt is active, improving focus and privacy.

## Architecture Decisions

- Only `password` and `notes` fields are “encrypted”. Other fields (title, username, website, category) remain plaintext for SQL search.
- Vault model (educational): `lib/crypto.ts` uses a user-provided master password to:
  - store a SHA-256 hash of the master password in the `settings` table (`master_password_hash`) for unlock checks,
  - derive a simple in-memory key (the password bytes) used for a XOR-based transform of the `{ password, notes }` JSON payload.
- `ciphertext` holds the Base64-encoded XOR result; `nonce` is filled with random bytes but **not used** in the scheme (kept only for schema compatibility).
- Master password is entered on first launch to “initialize” the vault, and on subsequent launches to unlock it; the raw password is not stored, but the hash allows offline guessing and must be considered weak.
- Biometric unlock gates access to the app UI but does not replace the master password (it only acts as a convenience lock screen).
- Backup/export is a JSON envelope (`version: 1`) in `lib/backup-service.ts` that XOR-encrypts the serialized `passwords` payload with a backup password (also weak and for learning only).
- No state management library — uses `useState` + `useFocusEffect` for simplicity.
- No UI library — all components are custom with React Native `StyleSheet` and `react-native-paper` for basic theming.
- Dark mode supported via `useColorScheme` hook and themed color constants.

## Conventions

- File naming: kebab-case (e.g. `password-list-item.tsx`)
- Path alias: `@/*` maps to project root
- Use `bun` as package manager, `bunx` instead of `npx`
