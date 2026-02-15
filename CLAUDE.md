# Aminmart Password Manager

Offline password manager app built with React Native Expo.

## Tech Stack

- **Framework**: React Native with Expo (SDK 54, Expo Router v6)
- **Language**: TypeScript (strict mode)
- **Database**: expo-sqlite (local SQLite)
- **Encryption**: crypto-js (AES-256) for password & notes fields
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

## Architecture Decisions

- Only `password` and `notes` fields are encrypted. Other fields (title, username, website, category) remain plaintext for SQL search.
- Encryption key is hardcoded in `lib/crypto.ts` — marked for future replacement with master password or biometric-derived key.
- No state management library — uses `useState` + `useFocusEffect` for simplicity.
- No UI library — all components are custom with React Native `StyleSheet`.
- Dark mode supported via `useColorScheme` hook and themed color constants.

## Conventions

- File naming: kebab-case (e.g. `password-list-item.tsx`)
- Path alias: `@/*` maps to project root
- Use `bun` as package manager, `bunx` instead of `npx`
