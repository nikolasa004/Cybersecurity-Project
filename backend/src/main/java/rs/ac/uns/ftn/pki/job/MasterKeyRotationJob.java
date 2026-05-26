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
     * Cron izraz format: sekunda minut sat dan_u_mesecu mesec dan_u_nedelji
     * "0 0 2 1 1,7 *" -> 1. Januara i 1. Jula u 02:00 noću
     * Za testiranje ( svaki minut ): "0 * * * * *"
     */
    @Scheduled(cron = "0 0 2 1 1,7 *")
    public void performMasterKeyRotation() {
        log.info("Započinjem proces rotacije master ključa...");

        String oldVersion = keyEncryptionService.getCurrentActiveVersion();

        String newVersion = keyEncryptionService.rotateToNewMasterKey();
        log.info("Generisana nova verzija ključa: {}. Stara verzija je: {}", newVersion, oldVersion);

        List<Organization> organizationsToMigrate = organizationRepository.findByMasterKeyVersion(oldVersion);
        log.info("Pronađeno {} organizacija za migraciju.", organizationsToMigrate.size());

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
                log.error("Greška pri migraciji ključa za organizaciju ID: {}", org.getId(), e);
            }
        }

        if (successCount == organizationsToMigrate.size()) {
            keyEncryptionService.archiveOldMasterKey(oldVersion);
            log.info("Rotacija uspešno završena. Stari ključ {} je arhiviran.", oldVersion);
        } else {
            log.warn("Nisu sve organizacije uspešno migrirane. Stari ključ ostaje u READ_ONLY statusu.");
        }
    }
}