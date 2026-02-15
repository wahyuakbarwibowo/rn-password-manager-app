import { getDatabase } from './database';
import { encrypt, decrypt } from './crypto';
import type { PasswordEntry, PasswordRow, CreatePasswordInput, UpdatePasswordInput } from '@/types/password';

function rowToEntry(row: PasswordRow): PasswordEntry {
  return {
    ...row,
    password: decrypt(row.password),
    notes: decrypt(row.notes),
  };
}

export async function getAllPasswords(): Promise<PasswordEntry[]> {
  const db = await getDatabase();
  const rows = await db.getAllAsync<PasswordRow>(
    'SELECT * FROM passwords ORDER BY updated_at DESC'
  );
  return rows.map(rowToEntry);
}

export async function searchPasswords(query: string): Promise<PasswordEntry[]> {
  const db = await getDatabase();
  const like = `%${query}%`;
  const rows = await db.getAllAsync<PasswordRow>(
    'SELECT * FROM passwords WHERE title LIKE ? OR username LIKE ? OR website LIKE ? OR category LIKE ? ORDER BY updated_at DESC',
    [like, like, like, like]
  );
  return rows.map(rowToEntry);
}

export async function getPasswordById(id: number): Promise<PasswordEntry | null> {
  const db = await getDatabase();
  const row = await db.getFirstAsync<PasswordRow>(
    'SELECT * FROM passwords WHERE id = ?',
    [id]
  );
  return row ? rowToEntry(row) : null;
}

export async function createPassword(input: CreatePasswordInput): Promise<number> {
  const db = await getDatabase();
  const result = await db.runAsync(
    'INSERT INTO passwords (title, username, password, website, notes, category) VALUES (?, ?, ?, ?, ?, ?)',
    [
      input.title,
      input.username,
      encrypt(input.password),
      input.website,
      encrypt(input.notes),
      input.category,
    ]
  );
  return result.lastInsertRowId;
}

export async function updatePassword(input: UpdatePasswordInput): Promise<void> {
  const db = await getDatabase();
  const existing = await getPasswordById(input.id);
  if (!existing) throw new Error('Password not found');

  const title = input.title ?? existing.title;
  const username = input.username ?? existing.username;
  const password = input.password ?? existing.password;
  const website = input.website ?? existing.website;
  const notes = input.notes ?? existing.notes;
  const category = input.category ?? existing.category;

  await db.runAsync(
    `UPDATE passwords SET title = ?, username = ?, password = ?, website = ?, notes = ?, category = ?, updated_at = datetime('now') WHERE id = ?`,
    [title, username, encrypt(password), website, encrypt(notes), category, input.id]
  );
}

export async function deletePassword(id: number): Promise<void> {
  const db = await getDatabase();
  await db.runAsync('DELETE FROM passwords WHERE id = ?', [id]);
}
