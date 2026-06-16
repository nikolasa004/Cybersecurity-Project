package rs.ac.uns.ftn.pki.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.repository.OrganizationRepository;
import rs.ac.uns.ftn.pki.service.crypto.KeyEncryptionService;

import javax.crypto.SecretKey;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MasterKeyRotationJob {

    private final KeyEncryptionService keyEncryptionService;
    private final OrganizationRepository organizationRepository;

    /**
     * Cron expression format: second minute hour day_of_month month day_of_week
     * "0 0 2 1 1,7 *" -> January 1st and July 1st at 02:00 AM
     * For testing (every minute): "0 * * * * *"
     */
    @Scheduled(cron = "0 * * * * *")
    public void performMasterKeyRotation() {
        log.info("Starting master key rotation process...");

        String oldVersion = keyEncryptionService.getCurrentActiveVersion();

        String newVersion = keyEncryptionService.rotateToNewMasterKey();
        log.info("New key version generated: {}. Old version is: {}", newVersion, oldVersion);

        List<Organization> organizationsToMigrate = organizationRepository.findByMasterKeyVersion(oldVersion);
        log.info("Found {} organizations for migration.", organizationsToMigrate.size());

        SecretKey oldSecretKey = keyEncryptionService.getMasterKeyByVersion(oldVersion);
        SecretKey newSecretKey = keyEncryptionService.getMasterKeyByVersion(newVersion);

        int successCount = 0;

        for (Organization org : organizationsToMigrate) {
            try {
                byte[] decryptedOrgKey = keyEncryptionService.decrypt(org.getEncryptedOrgKey(), oldSecretKey);

                byte[] newlyEncryptedOrgKey = keyEncryptionService.encrypt(decryptedOrgKey, newSecretKey);

                org.setEncryptedOrgKey(newlyEncryptedOrgKey);
                org.setMasterKeyVersion(newVersion);
                organizationRepository.save(org);

                successCount++;
            } catch (Exception e) {
                log.error("Error migrating key for organization ID: {}", org.getId(), e);
            }
        }

        if (successCount == organizationsToMigrate.size()) {
            keyEncryptionService.archiveOldMasterKey(oldVersion);
            log.info("Rotation successfully completed. Old key {} has been archived.", oldVersion);
        } else {
            log.warn("Not all organizations were successfully migrated. Old key remains in READ_ONLY status.");
        }
    }
}