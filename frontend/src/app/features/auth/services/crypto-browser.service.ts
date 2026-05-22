import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class CryptoBrowserService {
  async decryptChallengeWithPrivateKey(
    privateKeyPem: string,
    encryptedChallengeBase64: string
  ): Promise<string> {
    const privateKey = await this.importPrivateKey(privateKeyPem);
    const encryptedBytes = this.base64ToArrayBuffer(encryptedChallengeBase64);

    const decrypted = await window.crypto.subtle.decrypt(
      {
        name: 'RSA-OAEP'
      },
      privateKey,
      encryptedBytes
    );

    return new TextDecoder().decode(decrypted);
  }

  private async importPrivateKey(privateKeyPem: string): Promise<CryptoKey> {
    const cleaned = privateKeyPem
      .replace('-----BEGIN PRIVATE KEY-----', '')
      .replace('-----END PRIVATE KEY-----', '')
      .replace(/\s/g, '');

    const binaryDer = this.base64ToArrayBuffer(cleaned);

    return window.crypto.subtle.importKey(
      'pkcs8',
      binaryDer,
      {
        name: 'RSA-OAEP',
        hash: 'SHA-256'
      },
      false,
      ['decrypt']
    );
  }

  private base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binaryString = atob(base64);
    const bytes = new Uint8Array(binaryString.length);

    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }

    return bytes.buffer;
  }
}