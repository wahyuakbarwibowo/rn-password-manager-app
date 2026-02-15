import 'react-native-get-random-values';
import * as Crypto from 'expo-crypto';
import * as SecureStore from 'expo-secure-store';
import { gcm } from '@noble/ciphers/aes';
import { pbkdf2Async } from '@noble/hashes/pbkdf2';
import { sha256 } from '@noble/hashes/sha2';
import { getDatabase } from './database';

const PBKDF2_ITERATIONS = 200_000;
const SALT_BYTES = 16;
const NONCE_BYTES = 12;
const VAULT_KEY_BYTES = 32;
const APP_MASTER_PASSWORD_KEY = 'pm_app_master_password';

const encoder = new TextEncoder();
const decoder = new TextDecoder();

type VaultMetaRow = {
  salt: string;
  encrypted_vault_key: string;
  vault_key_nonce: string;
};

const BASE64_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
const BASE64_REVERSE = BASE64_ALPHABET.split('').reduce<Record<string, number>>((acc, char, index) => {
  acc[char] = index;
  return acc;
}, {});

function randomBytes(length: number): Uint8Array {
  return Crypto.getRandomBytes(length);
}

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

function ensureNonceSize(nonce: Uint8Array): void {
  if (nonce.length !== NONCE_BYTES) {
    throw new Error(`AES-GCM nonce must be ${NONCE_BYTES} bytes.`);
  }
}

function ensureVaultKeySize(vaultKey: Uint8Array): void {
  if (vaultKey.length !== VAULT_KEY_BYTES) {
    throw new Error(`Vault key must be ${VAULT_KEY_BYTES} bytes.`);
  }
}

let activeVaultKey: Uint8Array | null = null;

async function ensureVaultMetaTable(): Promise<void> {
  const db = await getDatabase();
  await db.execAsync(`
    CREATE TABLE IF NOT EXISTS vault_meta (
      id INTEGER PRIMARY KEY,
      salt TEXT NOT NULL,
      encrypted_vault_key TEXT NOT NULL,
      vault_key_nonce TEXT NOT NULL
    );
  `);
}

async function getVaultMeta(): Promise<VaultMetaRow | null> {
  await ensureVaultMetaTable();
  const db = await getDatabase();
  return db.getFirstAsync<VaultMetaRow>(
    'SELECT salt, encrypted_vault_key, vault_key_nonce FROM vault_meta ORDER BY id DESC LIMIT 1'
  );
}

async function saveVaultMeta(salt: Uint8Array, encryptedVaultKey: Uint8Array, vaultKeyNonce: Uint8Array): Promise<void> {
  await ensureVaultMetaTable();
  const db = await getDatabase();
  await db.runAsync('DELETE FROM vault_meta');
  await db.runAsync(
    'INSERT INTO vault_meta (salt, encrypted_vault_key, vault_key_nonce) VALUES (?, ?, ?)',
    [encodeBase64(salt), encodeBase64(encryptedVaultKey), encodeBase64(vaultKeyNonce)]
  );
}

async function deriveMasterAesKey(masterPassword: string, salt: Uint8Array): Promise<Uint8Array> {
  if (!masterPassword) {
    throw new Error('Master password is required.');
  }
  return pbkdf2Async(sha256, encoder.encode(masterPassword), salt, {
    c: PBKDF2_ITERATIONS,
    dkLen: VAULT_KEY_BYTES,
  });
}

function ensureKeySize(key: Uint8Array): void {
  if (key.length !== VAULT_KEY_BYTES) {
    throw new Error(`AES-256 key must be ${VAULT_KEY_BYTES} bytes.`);
  }
}

async function aesGcmEncrypt(key: Uint8Array, plaintext: Uint8Array, nonce: Uint8Array): Promise<Uint8Array> {
  ensureKeySize(key);
  ensureNonceSize(nonce);
  return gcm(key, nonce).encrypt(plaintext);
}

async function aesGcmDecrypt(key: Uint8Array, ciphertext: Uint8Array, nonce: Uint8Array): Promise<Uint8Array> {
  ensureKeySize(key);
  ensureNonceSize(nonce);
  return gcm(key, nonce).decrypt(ciphertext);
}

async function importVaultAesKey(vaultKey: Uint8Array): Promise<Uint8Array> {
  ensureVaultKeySize(vaultKey);
  return vaultKey;
}

async function deriveMasterAesKeyLegacy(masterPassword: string, salt: Uint8Array): Promise<Uint8Array> {
  if (!masterPassword) {
    throw new Error('Master password is required.');
  }
  return deriveMasterAesKey(masterPassword, salt);
}

export async function createVault(masterPassword: string): Promise<void> {
  const salt = randomBytes(SALT_BYTES);
  const masterKey = await deriveMasterAesKeyLegacy(masterPassword, salt);

  const vaultKey = randomBytes(VAULT_KEY_BYTES);
  const vaultKeyNonce = randomBytes(NONCE_BYTES);
  const encryptedVaultKey = await aesGcmEncrypt(masterKey, vaultKey, vaultKeyNonce);

  await saveVaultMeta(salt, encryptedVaultKey, vaultKeyNonce);
}

async function getOrCreateAppMasterPassword(): Promise<string> {
  const existing = await SecureStore.getItemAsync(APP_MASTER_PASSWORD_KEY);
  if (existing) {
    return existing;
  }
  const generated = encodeBase64(randomBytes(32));
  await SecureStore.setItemAsync(APP_MASTER_PASSWORD_KEY, generated);
  return generated;
}

async function getActiveVaultKey(): Promise<Uint8Array> {
  if (!activeVaultKey) {
    await ensureEncryptionKey();
  }
  if (!activeVaultKey) {
    throw new Error('Vault key is not initialized.');
  }
  return activeVaultKey;
}

export async function ensureEncryptionKey(): Promise<void> {
  await ensureVaultMetaTable();
  const masterPassword = await getOrCreateAppMasterPassword();
  const meta = await getVaultMeta();
  if (!meta) {
    await createVault(masterPassword);
  }
  activeVaultKey = await unlockVault(masterPassword);
}

export async function unlockVault(masterPassword: string): Promise<Uint8Array> {
  const meta = await getVaultMeta();
  if (!meta) {
    throw new Error('Vault is not initialized.');
  }

  const salt = decodeBase64(meta.salt);
  const encryptedVaultKey = decodeBase64(meta.encrypted_vault_key);
  const vaultKeyNonce = decodeBase64(meta.vault_key_nonce);

  const masterKey = await deriveMasterAesKeyLegacy(masterPassword, salt);
  const vaultKey = await aesGcmDecrypt(masterKey, encryptedVaultKey, vaultKeyNonce);
  ensureVaultKeySize(vaultKey);
  return vaultKey;
}

export async function encryptEntry(
  vaultKey: Uint8Array,
  data: object
): Promise<{ ciphertext: string; nonce: string }> {
  const entryKey = await importVaultAesKey(vaultKey);
  const nonce = randomBytes(NONCE_BYTES);
  const plaintext = encoder.encode(JSON.stringify(data));
  const ciphertext = await aesGcmEncrypt(entryKey, plaintext, nonce);

  return {
    ciphertext: encodeBase64(ciphertext),
    nonce: encodeBase64(nonce),
  };
}

export async function decryptEntry<T extends object>(
  vaultKey: Uint8Array,
  ciphertext: string,
  nonce: string
): Promise<T> {
  const entryKey = await importVaultAesKey(vaultKey);
  const ciphertextBytes = decodeBase64(ciphertext);
  const nonceBytes = decodeBase64(nonce);
  const plaintext = await aesGcmDecrypt(entryKey, ciphertextBytes, nonceBytes);
  return JSON.parse(decoder.decode(plaintext)) as T;
}

export async function changeMasterPassword(oldPassword: string, newPassword: string): Promise<void> {
  const currentVaultKey = await unlockVault(oldPassword);

  const newSalt = randomBytes(SALT_BYTES);
  const newMasterKey = await deriveMasterAesKeyLegacy(newPassword, newSalt);
  const newVaultKeyNonce = randomBytes(NONCE_BYTES);
  const rewrappedVaultKey = await aesGcmEncrypt(newMasterKey, currentVaultKey, newVaultKeyNonce);

  await saveVaultMeta(newSalt, rewrappedVaultKey, newVaultKeyNonce);
  activeVaultKey = currentVaultKey;
}

export async function encryptSecrets(payload: { password: string; notes: string }): Promise<{
  ciphertext: string;
  nonce: string;
}> {
  const vaultKey = await getActiveVaultKey();
  return encryptEntry(vaultKey, payload);
}

export async function decryptSecrets(ciphertext: string, nonce: string): Promise<{
  password: string;
  notes: string;
}> {
  const vaultKey = await getActiveVaultKey();
  return decryptEntry<{ password: string; notes: string }>(vaultKey, ciphertext, nonce);
}

export function generatePassword(length: number = 16): string {
  if (!Number.isInteger(length) || length <= 0) {
    throw new Error('Password length must be a positive integer.');
  }
  const charset = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{}|;:,.<>?';
  const random = randomBytes(length);
  let output = '';
  for (let i = 0; i < length; i++) {
    output += charset[random[i] % charset.length];
  }
  return output;
}
