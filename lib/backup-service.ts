import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import * as DocumentPicker from 'expo-document-picker';
import 'react-native-get-random-values';

import * as Crypto from 'expo-crypto';
import { gcm } from '@noble/ciphers/aes';
import { pbkdf2Async } from '@noble/hashes/pbkdf2';
import { sha256 } from '@noble/hashes/sha2';
import { getAllPasswords, createPassword } from './password-service';
import type { CreatePasswordInput } from '@/types/password';

const PBKDF2_ITERATIONS = 200_000;
const SALT_BYTES = 16;
const NONCE_BYTES = 12;

const encoder = new TextEncoder();
const decoder = new TextDecoder();

type BackupFileV2 = {
  version: 2;
  kdf: 'PBKDF2-SHA256';
  iterations: number;
  salt: string;
  nonce: string;
  ciphertext: string;
};

interface BackupData {
  version: 2;
  exported_at: string;
  passwords: CreatePasswordInput[];
}

function randomBytes(length: number): Uint8Array {
  return Crypto.getRandomBytes(length);
}

function ensureNonceSize(nonce: Uint8Array): void {
  if (nonce.length !== NONCE_BYTES) {
    throw new Error(`AES-GCM nonce must be ${NONCE_BYTES} bytes.`);
  }
}

function ensureKeySize(key: Uint8Array): void {
  if (key.length !== 32) {
    throw new Error('AES-256 key must be 32 bytes.');
  }
}

const BASE64_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
const BASE64_REVERSE = BASE64_ALPHABET.split('').reduce<Record<string, number>>((acc, char, index) => {
  acc[char] = index;
  return acc;
}, {});

function encodeBase64(bytes: Uint8Array): string {
  let result = '';
  let i = 0;

  for (; i + 2 < bytes.length; i += 3) {
    const chunk = (bytes[i] << 16) | (bytes[i + 1] << 8) | bytes[i + 2];
    result += BASE64_ALPHABET[(chunk >> 18) & 0x3f];
    result += BASE64_ALPHABET[(chunk >> 12) & 0x3f];
    result += BASE64_ALPHABET[(chunk >> 6) & 0x3f];
    result += BASE64_ALPHABET[chunk & 0x3f];
  }

  const remaining = bytes.length - i;
  if (remaining === 1) {
    const chunk = bytes[i];
    result += BASE64_ALPHABET[(chunk >> 2) & 0x3f];
    result += BASE64_ALPHABET[(chunk << 4) & 0x3f];
    result += '==';
  } else if (remaining === 2) {
    const chunk = (bytes[i] << 8) | bytes[i + 1];
    result += BASE64_ALPHABET[(chunk >> 10) & 0x3f];
    result += BASE64_ALPHABET[(chunk >> 4) & 0x3f];
    result += BASE64_ALPHABET[(chunk << 2) & 0x3f];
    result += '=';
  }

  return result;
}

function decodeBase64(base64: string): Uint8Array {
  const sanitized = base64.replace(/\s+/g, '');
  if (sanitized.length === 0 || sanitized.length % 4 !== 0) {
    throw new Error('Invalid Base64 input.');
  }

  let padding = 0;
  if (sanitized.endsWith('==')) padding = 2;
  else if (sanitized.endsWith('=')) padding = 1;

  const outputLength = (sanitized.length / 4) * 3 - padding;
  const output = new Uint8Array(outputLength);
  let outIndex = 0;

  for (let i = 0; i < sanitized.length; i += 4) {
    const c0 = sanitized[i];
    const c1 = sanitized[i + 1];
    const c2 = sanitized[i + 2];
    const c3 = sanitized[i + 3];

    const b0 = BASE64_REVERSE[c0];
    const b1 = BASE64_REVERSE[c1];
    const b2 = c2 === '=' ? 0 : BASE64_REVERSE[c2];
    const b3 = c3 === '=' ? 0 : BASE64_REVERSE[c3];

    if (
      b0 === undefined ||
      b1 === undefined ||
      (c2 !== '=' && b2 === undefined) ||
      (c3 !== '=' && b3 === undefined)
    ) {
      throw new Error('Invalid Base64 input.');
    }

    const chunk = (b0 << 18) | (b1 << 12) | (b2 << 6) | b3;
    output[outIndex++] = (chunk >> 16) & 0xff;
    if (outIndex < output.length) output[outIndex++] = (chunk >> 8) & 0xff;
    if (outIndex < output.length) output[outIndex++] = chunk & 0xff;
  }

  return output;
}

async function deriveAesKey(password: string, salt: Uint8Array, iterations: number): Promise<Uint8Array> {
  if (!password) {
    throw new Error('Backup password is required.');
  }
  return pbkdf2Async(sha256, encoder.encode(password), salt, {
    c: iterations,
    dkLen: 32,
  );
}

async function encryptBackup(data: string, backupPassword: string): Promise<BackupFileV2> {
  const salt = randomBytes(SALT_BYTES);
  const nonce = randomBytes(NONCE_BYTES);
  const key = await deriveAesKey(backupPassword, salt, PBKDF2_ITERATIONS);
  ensureKeySize(key);
  ensureNonceSize(nonce);
  const encrypted = gcm(key, nonce).encrypt(encoder.encode(data));
  return {
    version: 2,
    kdf: 'PBKDF2-SHA256',
    iterations: PBKDF2_ITERATIONS,
    salt: encodeBase64(salt),
    nonce: encodeBase64(nonce),
    ciphertext: encodeBase64(new Uint8Array(encrypted)),
  };
}

function isBackupFileV2(input: unknown): input is BackupFileV2 {
  if (!input || typeof input !== 'object') return false;
  const data = input as Record<string, unknown>;
  return (
    data.version === 2 &&
    data.kdf === 'PBKDF2-SHA256' &&
    typeof data.iterations === 'number' &&
    data.iterations >= 200_000 &&
    typeof data.salt === 'string' &&
    typeof data.nonce === 'string' &&
    typeof data.ciphertext === 'string'
  );
}

async function decryptBackup(file: BackupFileV2, backupPassword: string): Promise<string> {
  const salt = decodeBase64(file.salt);
  const nonce = decodeBase64(file.nonce);
  if (nonce.length !== NONCE_BYTES) {
    throw new Error('Invalid backup nonce.');
  }
  const ciphertext = decodeBase64(file.ciphertext);
  const key = await deriveAesKey(backupPassword, salt, file.iterations);
  ensureKeySize(key);
  const decrypted = gcm(key, nonce).decrypt(ciphertext);
  return decoder.decode(decrypted);
}

function isValidEntry(input: unknown): input is CreatePasswordInput {
  if (!input || typeof input !== 'object') return false;
  const row = input as Record<string, unknown>;
  return (
    typeof row.title === 'string' &&
    typeof row.username === 'string' &&
    typeof row.password === 'string' &&
    typeof row.website === 'string' &&
    typeof row.notes === 'string' &&
    typeof row.category === 'string'
  );
}

function isValidBackupData(input: unknown): input is BackupData {
  if (!input || typeof input !== 'object') return false;
  const data = input as Record<string, unknown>;
  return (
    data.version === 2 &&
    typeof data.exported_at === 'string' &&
    Array.isArray(data.passwords) &&
    data.passwords.every(isValidEntry)
  );
}

export async function exportBackup(backupPassword: string): Promise<void> {
  const passwords = await getAllPasswords();

  const backupData: BackupData = {
    version: 2,
    exported_at: new Date().toISOString(),
    passwords: passwords.map((entry) => ({
      title: entry.title || '',
      username: entry.username || '',
      password: entry.password || '',
      website: entry.website || '',
      notes: entry.notes || '',
      category: entry.category || 'other',
    })),
  };

  const encryptedEnvelope = await encryptBackup(JSON.stringify(backupData), backupPassword);
  const encrypted = JSON.stringify(encryptedEnvelope);

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

  let envelope: unknown;
  try {
    envelope = JSON.parse(encrypted);
  } catch {
    throw new Error('Invalid backup format');
  }

  if (!isBackupFileV2(envelope)) {
    throw new Error('Unsupported backup version');
  }

  let json = '';
  try {
    json = await decryptBackup(envelope, backupPassword);
  } catch {
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
