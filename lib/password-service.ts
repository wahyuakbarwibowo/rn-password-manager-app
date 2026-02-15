import { getDatabase } from './database';
import { decryptSecrets, encryptSecrets, ensureEncryptionKey } from './crypto';
import type { PasswordEntry, PasswordRow, CreatePasswordInput, UpdatePasswordInput } from '@/types/password';

async function rowToEntry(row: PasswordRow): Promise<PasswordEntry> {
  let password = row.password ?? '';
  let notes = row.notes ?? '';

  if (row.ciphertext && row.nonce) {
    const decrypted = await decryptSecrets(row.ciphertext, row.nonce);
    password = decrypted.password;
    notes = decrypted.notes;
  }

  return {
    ...row,
    password,
    notes,
  };
}

export async function getAllPasswords(): Promise<PasswordEntry[]> {
  const db = await getDatabase();
  await ensureEncryptionKey();
  const rows = await db.getAllAsync<PasswordRow>(
    'SELECT * FROM passwords ORDER BY updated_at DESC'
  );
  return Promise.all(rows.map(rowToEntry));
}

export async function searchPasswords(query: string): Promise<PasswordEntry[]> {
  const db = await getDatabase();
  await ensureEncryptionKey();
  const like = `%${query}%`;
  const rows = await db.getAllAsync<PasswordRow>(
    'SELECT * FROM passwords WHERE title LIKE ? OR username LIKE ? OR website LIKE ? OR category LIKE ? ORDER BY updated_at DESC',
    [like, like, like, like]
  );
  return Promise.all(rows.map(rowToEntry));
}

export async function getPasswordById(id: number): Promise<PasswordEntry | null> {
  const db = await getDatabase();
  await ensureEncryptionKey();
  const row = await db.getFirstAsync<PasswordRow>(
    'SELECT * FROM passwords WHERE id = ?',
    [id]
  );
  return row ? rowToEntry(row) : null;
}

export async function createPassword(input: CreatePasswordInput): Promise<number> {
  await ensureEncryptionKey();
  const db = await getDatabase();
  const { ciphertext, nonce } = await encryptSecrets({
    password: input.password,
    notes: input.notes,
  });
  const result = await db.runAsync(
    'INSERT INTO passwords (title, username, password, website, notes, category, ciphertext, nonce) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
    [
      input.title,
      input.username,
      '',
      input.website,
      '',
      input.category,
      ciphertext,
      nonce,
    ]
  );
  return result.lastInsertRowId;
}

export async function updatePassword(input: UpdatePasswordInput): Promise<void> {
  await ensureEncryptionKey();
  const db = await getDatabase();
  const existing = await getPasswordById(input.id);
  if (!existing) throw new Error('Password not found');

  const title = input.title ?? existing.title;
  const username = input.username ?? existing.username;
  const password = input.password ?? existing.password;
  const website = input.website ?? existing.website;
  const notes = input.notes ?? existing.notes;
  const category = input.category ?? existing.category;
  const { ciphertext, nonce } = await encryptSecrets({ password, notes });

  await db.runAsync(
    `UPDATE passwords SET title = ?, username = ?, password = '', website = ?, notes = '', category = ?, ciphertext = ?, nonce = ?, updated_at = datetime('now') WHERE id = ?`,
    [title, username, website, category, ciphertext, nonce, input.id]
  );
}

export async function deletePassword(id: number): Promise<void> {
  const db = await getDatabase();
  await db.runAsync('DELETE FROM passwords WHERE id = ?', [id]);
}
