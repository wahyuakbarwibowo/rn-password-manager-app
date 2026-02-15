import CryptoJS from 'crypto-js';
import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import * as DocumentPicker from 'expo-document-picker';

import { getAllPasswords, createPassword } from './password-service';
import type { CreatePasswordInput, PasswordEntry } from '@/types/password';

function encryptBackup(data: string, password: string): string {
  return CryptoJS.AES.encrypt(data, password).toString();
}

function decryptBackup(ciphertext: string, password: string): string {
  const bytes = CryptoJS.AES.decrypt(ciphertext, password);
  return bytes.toString(CryptoJS.enc.Utf8);
}

interface BackupData {
  version: 1;
  exported_at: string;
  passwords: Omit<PasswordEntry, 'id'>[];
}

function isValidBackupData(data: unknown): data is BackupData {
  if (!data || typeof data !== 'object') return false;
  const d = data as Record<string, unknown>;
  return (
    d.version === 1 &&
    typeof d.exported_at === 'string' &&
    Array.isArray(d.passwords)
  );
}

export async function exportBackup(backupPassword: string): Promise<void> {
  const passwords = await getAllPasswords();

  const backupData: BackupData = {
    version: 1,
    exported_at: new Date().toISOString(),
    passwords: passwords.map(({ id, ...rest }) => rest),
  };

  const json = JSON.stringify(backupData);
  const encrypted = encryptBackup(json, backupPassword);

  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const fileName = `aminmart-backup-${timestamp}.aminmart`;
  const file = new File(Paths.cache, fileName);

  file.create();
  file.write(encrypted);
  await Sharing.shareAsync(file.uri, {
    mimeType: 'application/octet-stream',
    dialogTitle: 'Save Backup File',
  });
}

export async function importBackup(backupPassword: string): Promise<number> {
  const result = await DocumentPicker.getDocumentAsync({
    type: '*/*',
    copyToCacheDirectory: true,
  });

  if (result.canceled || result.assets.length === 0) {
    throw new Error('No file selected');
  }

  const fileUri = result.assets[0].uri;
  const pickedFile = new File(fileUri);
  const encrypted = await pickedFile.text();

  const json = decryptBackup(encrypted, backupPassword);
  if (!json) {
    throw new Error('Wrong backup password or corrupted file');
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(json);
  } catch {
    throw new Error('Wrong backup password or corrupted file');
  }

  if (!isValidBackupData(parsed)) {
    throw new Error('Invalid backup file format');
  }

  let count = 0;
  for (const entry of parsed.passwords) {
    const input: CreatePasswordInput = {
      title: entry.title || '',
      username: entry.username || '',
      password: entry.password || '',
      website: entry.website || '',
      notes: entry.notes || '',
      category: entry.category || 'other',
    };
    await createPassword(input);
    count++;
  }

  return count;
}
