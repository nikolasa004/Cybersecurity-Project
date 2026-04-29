package rs.ac.uns.ftn.pki.service.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Service
public class KeyEncryptionService {

    private final SecretKey masterKey;

    public KeyEncryptionService(@Value("${master.key}") String masterKeyValue) {
        this.masterKey = new SecretKeySpec(masterKeyValue.getBytes(), "AES");
    }

    public byte[] encrypt(byte[] data, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    public SecretKey getMasterKey() {
        return masterKey;
    }

    public SecretKey generateOrgKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate organization key", e);
        }
    }
}