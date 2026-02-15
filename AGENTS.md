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
- Only `password` and `notes` fields are encrypted with AES (see `lib/crypto.ts`). Be cautious when touching the hard-coded encryption key—future work will replace it with a master password or biometric-derived secret.
