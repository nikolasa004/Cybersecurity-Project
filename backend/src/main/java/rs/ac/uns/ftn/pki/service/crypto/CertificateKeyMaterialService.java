package rs.ac.uns.ftn.pki.service.crypto;

import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.exception.NotFoundException;
import rs.ac.uns.ftn.pki.model.entity.Certificate;

import java.security.PrivateKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary key material holder for Student 1 scope.
 *
 * This service exists only so certificate issuing and issuer validation
 * can be tested before functionality 6 (secure certificate/private key storage)
 * is implemented.
 *
 * Later, only the internals of this service should be replaced with the real
 * storage/encryption logic from functionality 6.
 */
@Service
public class CertificateKeyMaterialService {

    private final Map<String, PrivateKey> privateKeysBySerialNumber = new ConcurrentHashMap<>();

    public void rememberPrivateKey(String certificateSerialNumber, PrivateKey privateKey) {
        privateKeysBySerialNumber.put(certificateSerialNumber, privateKey);
    }

    public PrivateKey getIssuerPrivateKey(Certificate issuerCertificate) {
        PrivateKey privateKey = privateKeysBySerialNumber.get(issuerCertificate.getSerialNumber());

        if (privateKey == null) {
            throw new NotFoundException(
                    "Private key for issuer certificate is not available in temporary runtime storage. " +
                    "Functionality 6 should replace this implementation."
            );
        }

        return privateKey;
    }
}