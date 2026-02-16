// Type definitions for noble libraries used in the app
declare module '@noble/ciphers/aes' {
  export function gcm(key: Uint8Array, nonce: Uint8Array): {
    encrypt(plaintext: Uint8Array): Uint8Array;
    decrypt(ciphertext: Uint8Array): Uint8Array;
  };
}

declare module '@noble/hashes/pbkdf2' {
  export function pbkdf2Async(
    hash: (message: Uint8Array) => Uint8Array,
    password: Uint8Array,
    salt: Uint8Array,
    options: { c: number; dkLen: number }
  ): Promise<Uint8Array>;
}

declare module '@noble/hashes/sha2' {
  export function sha256(message: Uint8Array): Uint8Array;
}

