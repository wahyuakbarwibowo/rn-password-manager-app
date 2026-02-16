import 'react-native-get-random-values';
import * as Crypto from 'expo-crypto';
import * as SecureStore from 'expo-secure-store';
import { getDatabase } from './database';

const APP_MASTER_PASSWORD_KEY = 'pm_app_master_password';

// Fungsi sederhana untuk enkripsi/dekripsi tanpa enkripsi sebenarnya (hanya untuk testing)
function encodeBase64(str: string): string {
  return btoa(unescape(encodeURIComponent(str)));
}

function decodeBase64(str: string): string {
  return decodeURIComponent(escape(atob(str)));
}

// Fungsi sederhana untuk menyimpan dan mengambil password utama
async function getOrCreateAppMasterPassword(): Promise<string> {
  const existing = await SecureStore.getItemAsync(APP_MASTER_PASSWORD_KEY);
  if (existing) {
    return existing;
  }
  const newPassword = 'temp_default_password'; // Hanya untuk testing
  await SecureStore.setItemAsync(APP_MASTER_PASSWORD_KEY, newPassword);
  return newPassword;
}

// Fungsi untuk memastikan kunci enkripsi siap (tanpa enkripsi sebenarnya)
export async function ensureEncryptionKey(): Promise<void> {
  // Hanya untuk memastikan password utama tersedia
  await getOrCreateAppMasterPassword();
}

// Fungsi untuk mengenkripsi secrets (sementara tanpa enkripsi sebenarnya)
export async function encryptSecrets(payload: { password: string; notes: string }): Promise<{
  ciphertext: string;
  nonce: string;
}> {
  // Untuk sementara, hanya encode ke base64 tanpa enkripsi sebenarnya
  const data = JSON.stringify(payload);
  const ciphertext = encodeBase64(data);
  const nonce = encodeBase64(Math.random().toString()); // Dummy nonce
  
  return {
    ciphertext,
    nonce,
  };
}

// Fungsi untuk mendekripsi secrets (sementara tanpa dekripsi sebenarnya)
export async function decryptSecrets(ciphertext: string, nonce: string): Promise<{
  password: string;
  notes: string;
}> {
  // Untuk sementara, hanya decode dari base64 tanpa dekripsi sebenarnya
  const data = JSON.parse(decodeBase64(ciphertext));
  return data;
}

export function generatePassword(length: number = 16): string {
  if (!Number.isInteger(length) || length <= 0) {
    throw new Error('Password length must be a positive integer.');
  }
  const charset = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{}|;:,.<>?';
  const random = Crypto.getRandomBytes(length);
  let output = '';
  for (let i = 0; i < length; i++) {
    output += charset[random[i] % charset.length];
  }
  return output;
}