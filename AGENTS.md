# Repository Guidelines

## Project Structure & Module Organization
- `app/` is the Expo Router entry point: file-based routing keeps each screen (and modal) inside `app/` or nested folders such as `app/(tabs)/`.
- Reusable UI lives under `components/`, theme constants in `constants/`, behavior hooks in `hooks/`, and persistence logic in `lib/` (crypto, database, service layers). TypeScript models go in `types/`.
- Assets, configuration, and platform metadata are rooted at the project root (`app.json`, `assets/`, `bun.lock`).

## Build, Test, and Development Commands
- `bun start` – launches Expo’s Metro server; handles web, Android, and iOS previews via the CLI menu.
- `bun run android` / `bun run ios` – boot the corresponding simulator/emulator, reusing the same script used by `bun start`.
- `bun run lint` – runs ESLint (exports styled for Expo/React Native).
- `bunx tsc --noEmit` – type-checks the strict TypeScript configuration without writing files.

## Coding Style & Naming Conventions
- TypeScript strict mode everywhere; prefer `const`/`let` over `var`, explicitly type props, and leverage `@/*` aliases to reference project paths.
- File names use `kebab-case` (e.g., `password-list-item.tsx`). Screen components under `app/` mirror their route names.
- Layouts and lists avoid external UI frameworks; use React Native primitives + `useThemeColor` to adapt to light/dark palettes. Keep components small and focused.
- Run `bun run lint` before committing to catch formatting issues.

## Testing Guidelines
- No automated test suite yet; rely on manual smoke checks via the Expo client.
- Smoke-test modal flows (add/edit passwords), the biometric lock screen, and navigation between dashboard, passwords, and settings whenever relevant.

## Commit & Pull Request Guidelines
- Follow conventional prefixes (`feat:`, `fix:`, `chore:`, etc.) as seen in recent history.
- Commit messages should include scope when helpful (e.g., `feat: add biometric blur overlay`).
- PR descriptions should summarize changes, list commands executed (`bun run lint`, screen flow verifications), and mention linked issues or follow-up work.
- Include screenshots or short videos for UI changes, especially if they affect navigation or biometric flows.

## Security & Configuration Notes

- This repository is **educational only**: the current crypto and storage design is meant for learning, not for production use. Do not use it to store real secrets.
- Secrets are “encrypted” via a very simple XOR-based scheme in `lib/crypto.ts`, using the user’s master password bytes as the key. This is cryptographically weak and trivially breakable; it exists only to illustrate end-to-end flows (master password, lock screen, backup/restore).
- `passwords` rows rely on `ciphertext` + `nonce` (Base64) for secret data; `nonce` is currently unused (schema compatibility only). Legacy `password`/`notes` columns are compatibility fields.
- The master password hash is stored in `settings` under `master_password_hash` (SHA-256 via `expo-crypto`) to check unlock attempts; this is not hardened against offline guessing.
- Backup/export uses a JSON envelope (`version: 1`) in `lib/backup-service.ts` that XOR-encrypts the serialized payload with a backup password (also weak). There is **no integrity protection** and no formal security audit; all data should be considered non-secure.
