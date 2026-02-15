import { getDatabase } from './database';

export async function getSetting(key: string): Promise<string | null> {
  const db = await getDatabase();
  const row = await db.getFirstAsync<{ value: string }>(
    'SELECT value FROM settings WHERE key = ?',
    [key]
  );
  return row?.value ?? null;
}

export async function setSetting(key: string, value: string): Promise<void> {
  const db = await getDatabase();
  await db.runAsync(
    'INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)',
    [key, value]
  );
}

export async function isBiometricEnabled(): Promise<boolean> {
  const value = await getSetting('biometric_enabled');
  return value === 'true';
}

export async function setBiometricEnabled(enabled: boolean): Promise<void> {
  await setSetting('biometric_enabled', enabled ? 'true' : 'false');
}
