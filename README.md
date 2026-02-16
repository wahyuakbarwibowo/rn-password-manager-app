# Aminmart Password Manager

Offline password manager app built with React Native Expo. **This project is for learning purposes only and must not be used to store real passwords or sensitive data.**

## Security Disclaimer

- **Educational only**: The encryption/decryption logic in this repository is designed to be understandable for learning, not to meet production-grade security standards.
- **Not for real secrets**: Do not use this app to store actual passwords, banking information, or any sensitive personal data.
- **No security guarantees**: The author and contributors provide no guarantees about the strength of the cryptography, protection against attacks, or safety of the stored data.
- **Use at your own risk**: Treat all secrets managed by this app as disposable test data.

## Getting Started

Install dependencies:

```bash
bun install
```

Run the app in development:

```bash
bun start
```

Then open it in:

- Android emulator
- iOS simulator
- Expo Go on a physical device

## Project Overview

Key pieces:

- `app/` – screens and navigation (Expo Router)
- `lib/crypto.ts` – master-password-based vault encryption (for learning)
- `lib/backup-service.ts` – encrypted backup and restore (for learning)
- `lib/password-service.ts` – CRUD access to the `passwords` table in SQLite

See `CLAUDE.md` and `AGENTS.md` for more implementation details.
