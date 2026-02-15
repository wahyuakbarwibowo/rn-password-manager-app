import * as SQLite from 'expo-sqlite';

let db: SQLite.SQLiteDatabase | null = null;

const DB_SCHEMA_VERSION = 2;

async function ensurePasswordColumn(
  database: SQLite.SQLiteDatabase,
  columnName: 'ciphertext' | 'nonce'
): Promise<void> {
  const tableInfo = await database.getAllAsync<{ name: string }>('PRAGMA table_info(passwords);');
  const hasColumn = tableInfo.some((column) => column.name === columnName);
  if (!hasColumn) {
    await database.execAsync(`ALTER TABLE passwords ADD COLUMN ${columnName} TEXT NOT NULL DEFAULT '';`);
  }
}

async function runMigrations(database: SQLite.SQLiteDatabase): Promise<void> {
  const versionRow = await database.getFirstAsync<{ user_version: number }>('PRAGMA user_version;');
  const currentVersion = versionRow?.user_version ?? 0;
  if (currentVersion >= DB_SCHEMA_VERSION) {
    return;
  }

  await database.execAsync('BEGIN TRANSACTION;');
  try {
    await database.execAsync(`
      CREATE TABLE IF NOT EXISTS vault_meta (
        id INTEGER PRIMARY KEY,
        salt TEXT NOT NULL,
        encrypted_vault_key TEXT NOT NULL,
        vault_key_nonce TEXT NOT NULL
      );
    `);

    await ensurePasswordColumn(database, 'ciphertext');
    await ensurePasswordColumn(database, 'nonce');

    await database.execAsync(`PRAGMA user_version = ${DB_SCHEMA_VERSION};`);
    await database.execAsync('COMMIT;');
  } catch (error) {
    await database.execAsync('ROLLBACK;');
    throw error;
  }
}

export async function getDatabase(): Promise<SQLite.SQLiteDatabase> {
  if (db) return db;

  db = await SQLite.openDatabaseAsync('passwords.db');

  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS passwords (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT NOT NULL,
      username TEXT NOT NULL DEFAULT '',
      password TEXT NOT NULL DEFAULT '',
      website TEXT NOT NULL DEFAULT '',
      notes TEXT NOT NULL DEFAULT '',
      category TEXT NOT NULL DEFAULT 'other',
      created_at TEXT NOT NULL DEFAULT (datetime('now')),
      updated_at TEXT NOT NULL DEFAULT (datetime('now'))
    );
  `);

  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS settings (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL
    );
  `);

  await runMigrations(db);

  return db;
}
