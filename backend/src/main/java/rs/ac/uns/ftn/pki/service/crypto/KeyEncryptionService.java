package rs.ac.uns.ftn.pki.service.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.pki.model.entity.MasterKeyEntity;
import rs.ac.uns.ftn.pki.model.enums.MasterKeyStatus;
import rs.ac.uns.ftn.pki.repository.MasterKeyRepository;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KeyEncryptionService {

    private final SecretKey rootKek;

    private final MasterKeyRepository masterKeyRepository;

    private final Map<String, SecretKey> activeMasterKeysCache = new ConcurrentHashMap<>();

    private String currentActiveVersion;

    public KeyEncryptionService(
            @Value("${root.key}") String kekValue,
            MasterKeyRepository masterKeyRepository) {

        this.rootKek = new SecretKeySpec(kekValue.getBytes(), "AES");
        this.masterKeyRepository = masterKeyRepository;
    }

    @PostConstruct
    @Transactional
    public void initializeKeys() {
        List<MasterKeyEntity> activeKeys = masterKeyRepository.findByStatusIn(
                List.of(MasterKeyStatus.ACTIVE_FOR_ALL, MasterKeyStatus.ACTIVE_READ_ONLY)
        );

        if (activeKeys.isEmpty()) {
            MasterKeyEntity firstKey = generateAndSaveNewMasterKey("v1");
            activeKeys.add(firstKey);
        }

        for (MasterKeyEntity entity : activeKeys) {
            byte[] decryptedKeyBytes = decrypt(entity.getEncryptedKey(), rootKek);
            SecretKey secretKey = new SecretKeySpec(decryptedKeyBytes, "AES");

            activeMasterKeysCache.put(entity.getVersion(), secretKey);

            if (entity.getStatus() == MasterKeyStatus.ACTIVE_FOR_ALL) {
                this.currentActiveVersion = entity.getVersion();
            }
        }
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

    public SecretKey generateOrgKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate organization key", e);
        }
    }

    public SecretKey getCurrentActiveMasterKey() {
        return activeMasterKeysCache.get(currentActiveVersion);
    }

    public String getCurrentActiveVersion() {
        return currentActiveVersion;
    }

    public SecretKey getMasterKeyByVersion(String version) {
        SecretKey key = activeMasterKeysCache.get(version);
        if (key == null) {
            throw new IllegalStateException("Master key version not found in cache: " + version);
        }
        return key;
    }


    private MasterKeyEntity generateAndSaveNewMasterKey(String version) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            SecretKey newDek = keyGenerator.generateKey();

            byte[] encryptedDek = encrypt(newDek.getEncoded(), rootKek);

            MasterKeyEntity entity = new MasterKeyEntity();
            entity.setVersion(version);
            entity.setEncryptedKey(encryptedDek);
            entity.setStatus(MasterKeyStatus.ACTIVE_FOR_ALL);
            entity.setCreatedAt(LocalDateTime.now());

            return masterKeyRepository.save(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate initial master key", e);
        }
    }
}