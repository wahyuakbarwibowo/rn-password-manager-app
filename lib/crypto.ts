import 'react-native-get-random-values';
import * as Crypto from 'expo-crypto';
import { Buffer } from 'buffer';
import { getDatabase } from './database';
import { getSetting, setSetting } from './settings-service';

type SecretsPayload = { password: string; notes: string };

const MASTER_PASSWORD_KEY = 'master_password_hash';

let cachedKeyBytes: Uint8Array | null = null;

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

async function hashPassword(password: string): Promise<string> {
  // Hash sederhana pakai SHA-256 untuk cek kecocokan (masih untuk belajar)
  const digest = await Crypto.digestStringAsync(
    Crypto.CryptoDigestAlgorithm.SHA256,
    password
  );
  return digest;
}

export async function isVaultInitialized(): Promise<boolean> {
  const existing = await getSetting(MASTER_PASSWORD_KEY);
  return existing !== null;
}

export function isVaultUnlocked(): boolean {
  return cachedKeyBytes !== null;
}

export function lockVault(): void {
  cachedKeyBytes = null;
}

export async function initializeVault(masterPassword: string): Promise<void> {
  if (!masterPassword) {
    throw new Error('Master password is required');
  }
  const exists = await isVaultInitialized();
  if (exists) {
    throw new Error('Vault is already initialized');
  }

  const hash = await hashPassword(masterPassword);
  await setSetting(MASTER_PASSWORD_KEY, hash);
  cachedKeyBytes = stringToBytes(masterPassword);
}

export async function unlockVault(masterPassword: string): Promise<void> {
  if (!masterPassword) {
    throw new Error('Master password is required');
  }

  const storedHash = await getSetting(MASTER_PASSWORD_KEY);
  if (!storedHash) {
    throw new Error('Vault is not initialized');
  }

  const hash = await hashPassword(masterPassword);
  if (hash !== storedHash) {
    throw new Error('Wrong password');
  }

  cachedKeyBytes = stringToBytes(masterPassword);
}

// Hanya memastikan DB siap; tidak lagi bergantung pada WebCrypto.
export async function ensureEncryptionKey(): Promise<void> {
  await getDatabase();
}

export async function encryptSecrets(payload: SecretsPayload): Promise<{
  ciphertext: string;
  nonce: string;
}> {
  if (!cachedKeyBytes) {
    throw new Error('Vault not unlocked');
  }

  const json = JSON.stringify(payload);
  const dataBytes = stringToBytes(json);
  const xored = xorBytes(dataBytes, cachedKeyBytes);

  // Nonce dummy hanya supaya schema DB tetap terisi; tidak dipakai di skema sederhana ini.
  const nonceBytes = Crypto.getRandomBytes(12);

  return {
    ciphertext: bytesToBase64(xored),
    nonce: bytesToBase64(nonceBytes),
  };
}

export async function decryptSecrets(
  ciphertext: string,
  _nonce: string
): Promise<SecretsPayload> {
  if (!cachedKeyBytes) {
    throw new Error('Vault not unlocked');
  }

  const cipherBytes = base64ToBytes(ciphertext);
  const plainBytes = xorBytes(cipherBytes, cachedKeyBytes);
  const json = bytesToString(plainBytes);
  const data = JSON.parse(json) as SecretsPayload;
  return data;
}

export function generatePassword(length: number = 16): string {
  if (!Number.isInteger(length) || length <= 0) {
    throw new Error('Password length must be a positive integer.');
  }
  const charset =
    'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{}|;:,.<>?';
  const random = Crypto.getRandomBytes(length);
  let output = '';
  for (let i = 0; i < length; i++) {
    output += charset[random[i] % charset.length];
  }
  return output;
}