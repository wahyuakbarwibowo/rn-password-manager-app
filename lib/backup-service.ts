import * as FileSystem from 'expo-file-system';
import * as DocumentPicker from 'expo-document-picker';
import * as Sharing from 'expo-sharing';
import { Buffer } from 'buffer';

import { getDatabase } from './database';
import type { PasswordRow } from '@/types/password';

const BACKUP_VERSION = 1;

interface BackupFileV1 {
  version: 1;
  createdAt: string;
  ciphertext: string; // base64 (XOR of JSON payload)
}

interface BackupPayloadV1 {
  passwords: Array<{
    title: string;
    username: string;
    website: string;
    category: string;
    created_at: string;
    updated_at: string;
    ciphertext: string;
    nonce: string;
  }>;
}

function stringToBytes(str: string): Uint8Array {
  const encoder = new TextEncoder();
  return encoder.encode(str);
}

function bytesToString(bytes: Uint8Array): string {
  const decoder = new TextDecoder();
  return decoder.decode(bytes);
}

function bytesToBase64(bytes: Uint8Array): string {
  return Buffer.from(bytes).toString('base64');
}

function base64ToBytes(base64: string): Uint8Array {
  return new Uint8Array(Buffer.from(base64, 'base64'));
}

function xorBytes(data: Uint8Array, key: Uint8Array): Uint8Array {
  const out = new Uint8Array(data.length);
  for (let i = 0; i < data.length; i++) {
    out[i] = data[i] ^ key[i % key.length];
  }
  return out;
}

async function getAllPasswordRows(): Promise<PasswordRow[]> {
  const db = await getDatabase();
  return db.getAllAsync<PasswordRow>('SELECT * FROM passwords ORDER BY updated_at DESC');
}

function buildBackupPayload(rows: PasswordRow[]): BackupPayloadV1 {
  return {
    passwords: rows
      .filter((row) => row.ciphertext && row.nonce)
      .map((row) => ({
        title: row.title,
        username: row.username,
        website: row.website,
        category: row.category,
        created_at: row.created_at,
        updated_at: row.updated_at,
        ciphertext: row.ciphertext ?? '',
        nonce: row.nonce ?? '',
      })),
  };
}

export async function exportBackup(masterPassword: string): Promise<string> {
  if (!masterPassword) {
    throw new Error('Backup password is required');
  }

  const rows = await getAllPasswordRows();
  const payload: BackupPayloadV1 = buildBackupPayload(rows);

  const json = JSON.stringify(payload);
  const dataBytes = stringToBytes(json);
  const keyBytes = stringToBytes(masterPassword);
  const xored = xorBytes(dataBytes, keyBytes);

  const backup: BackupFileV1 = {
    version: BACKUP_VERSION as 1,
    createdAt: new Date().toISOString(),
    ciphertext: bytesToBase64(xored),
  };

  const backupJson = JSON.stringify(backup, null, 2);
  const backupDir = FileSystem.documentDirectory ?? FileSystem.cacheDirectory!;
  const backupPath = `${backupDir}aminmart-backup-${Date.now()}.aminmartbackup`;

  await FileSystem.writeAsStringAsync(backupPath, backupJson, {
    encoding: FileSystem.EncodingType.UTF8,
  });

  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(backupPath, {
      mimeType: 'application/json',
      dialogTitle: 'Share Aminmart Backup',
    });
  }

  return backupPath;
}

async function pickBackupFile(): Promise<string> {
  const result = await DocumentPicker.getDocumentAsync({
    type: 'application/json',
    copyToCacheDirectory: true,
  });

  if (result.canceled || !result.assets || result.assets.length === 0) {
    throw new Error('User cancelled');
  }

  return result.assets[0].uri;
}

async function readBackupFile(uri: string): Promise<BackupFileV1> {
  const content = await FileSystem.readAsStringAsync(uri, {
    encoding: FileSystem.EncodingType.UTF8,
  });
  const parsed = JSON.parse(content) as BackupFileV1;

  if (parsed.version !== BACKUP_VERSION) {
    throw new Error('Unsupported backup version');
  }

  return parsed;
}

async function decryptBackup(
  backup: BackupFileV1,
  masterPassword: string
): Promise<BackupPayloadV1> {
  const keyBytes = stringToBytes(masterPassword);
  const cipherBytes = base64ToBytes(backup.ciphertext);
  const plainBytes = xorBytes(cipherBytes, keyBytes);

  const json = bytesToString(plainBytes);
  const payload = JSON.parse(json) as BackupPayloadV1;
  return payload;
}

async function mergePasswordsFromBackup(payload: BackupPayloadV1): Promise<number> {
  const db = await getDatabase();

  let inserted = 0;

  await db.execAsync('BEGIN TRANSACTION;');
  try {
    for (const item of payload.passwords) {
      const existing = await db.getFirstAsync<{ id: number }>(
        `
          SELECT id FROM passwords
          WHERE title = ? AND username = ? AND website = ? AND category = ?
        `,
        [item.title, item.username, item.website, item.category]
      );

      if (existing) {
        continue;
      }

      await db.runAsync(
        `
          INSERT INTO passwords
          (title, username, password, website, notes, category, ciphertext, nonce, created_at, updated_at)
          VALUES (?, ?, '', ?, '', ?, ?, ?, ?, ?)
        `,
        [
          item.title,
          item.username,
          item.website,
          item.category,
          item.ciphertext,
          item.nonce,
          item.created_at,
          item.updated_at,
        ]
      );
      inserted += 1;
    }

    await db.execAsync('COMMIT;');
  } catch (e) {
    await db.execAsync('ROLLBACK;');
    throw e;
  }

  return inserted;
}

async function overwritePasswordsFromBackup(payload: BackupPayloadV1): Promise<number> {
  const db = await getDatabase();

  await db.execAsync('BEGIN TRANSACTION;');
  try {
    await db.runAsync('DELETE FROM passwords;');

    for (const item of payload.passwords) {
      await db.runAsync(
        `
          INSERT INTO passwords
          (title, username, password, website, notes, category, ciphertext, nonce, created_at, updated_at)
          VALUES (?, ?, '', ?, '', ?, ?, ?, ?, ?)
        `,
        [
          item.title,
          item.username,
          item.website,
          item.category,
          item.ciphertext,
          item.nonce,
          item.created_at,
          item.updated_at,
        ]
      );
    }

    const countRow = await db.getFirstAsync<{ count: number }>(
      'SELECT COUNT(*) as count FROM passwords;'
    );

    await db.execAsync('COMMIT;');

    return countRow?.count ?? 0;
  } catch (e) {
    await db.execAsync('ROLLBACK;');
    throw e;
  }
}

export async function importBackup(masterPassword: string): Promise<number> {
  if (!masterPassword) {
    throw new Error('Backup password is required');
  }

  const uri = await pickBackupFile();
  const backup = await readBackupFile(uri);
  const payload = await decryptBackup(backup, masterPassword);

  const inserted = await mergePasswordsFromBackup(payload);
  return inserted;
}

export async function importBackupOverwrite(masterPassword: string): Promise<number> {
  if (!masterPassword) {
    throw new Error('Backup password is required');
  }

  const uri = await pickBackupFile();
  const backup = await readBackupFile(uri);
  const payload = await decryptBackup(backup, masterPassword);

  const count = await overwritePasswordsFromBackup(payload);
  return count;
}

