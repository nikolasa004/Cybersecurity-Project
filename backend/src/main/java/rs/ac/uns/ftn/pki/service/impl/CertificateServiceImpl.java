package rs.ac.uns.ftn.pki.service.impl;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateListResponse;
import rs.ac.uns.ftn.pki.dto.certificate.IssueCertificateRequest;
import rs.ac.uns.ftn.pki.dto.certificate.IssuerOptionResponse;
import rs.ac.uns.ftn.pki.exception.BadRequestException;
import rs.ac.uns.ftn.pki.exception.NotFoundException;
import rs.ac.uns.ftn.pki.mapper.CertificateMapper;
import rs.ac.uns.ftn.pki.mapper.CertificateSubjectMapper;
import rs.ac.uns.ftn.pki.model.entity.*;
import rs.ac.uns.ftn.pki.model.enums.CertificateStatus;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;
import rs.ac.uns.ftn.pki.repository.CertificateRepository;
import rs.ac.uns.ftn.pki.repository.OrganizationRepository;
import rs.ac.uns.ftn.pki.repository.PrivateKeyRepository;
import rs.ac.uns.ftn.pki.service.AuthService;
import rs.ac.uns.ftn.pki.service.CertificateService;
import rs.ac.uns.ftn.pki.service.crypto.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {
    private final PrivateKeyRepository privateKeyRepository;
    private final KeyEncryptionService keyEncryptionService;
    private final CertificateRepository certificateRepository;
    private final OrganizationRepository organizationRepository;
    private final CertificateMapper certificateMapper;
    private final CertificateSubjectMapper certificateSubjectMapper;
    private final CertificateEntityFactory certificateEntityFactory;
    private final CertificateKeyMaterialService certificateKeyMaterialService;
    private final KeyPairGeneratorService keyPairGeneratorService;
    private final X500NameBuilderService x500NameBuilderService;
    private final CertificateGeneratorService certificateGeneratorService;
    private final AuthService authService;

    @Override
    @Transactional(readOnly = true)
    public List<CertificateListResponse> getAllForAdmin() {
        return certificateRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(certificateMapper::toListResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssuerOptionResponse> getAvailableIssuers(CertificateType targetType) {
        if (targetType == CertificateType.ROOT) {
            return List.of();
        }

        List<CertificateType> allowedIssuerTypes = switch (targetType) {
            case ROOT -> List.of();
            case INTERMEDIATE, END_ENTITY -> List.of(CertificateType.ROOT, CertificateType.INTERMEDIATE);
        };

        return certificateRepository
                .findAllByTypeInAndStatusAndValidToAfterAndCaTrueAndKeyCertSignTrueOrderByCreatedAtDesc(
                        allowedIssuerTypes,
                        CertificateStatus.ACTIVE,
                        LocalDateTime.now()
                )
                .stream()
                .map(certificateMapper::toIssuerOptionResponse)
                .toList();
    }

    @Override
    @Transactional
    public CertificateListResponse issueCertificate(IssueCertificateRequest request) {
        validateIssueRequest(request);

        Certificate issuerCertificate = resolveIssuerCertificate(request);
        validateIssuerIfNeeded(issuerCertificate, request);

        X500SubjectData subjectData = certificateSubjectMapper.fromIssueRequest(request);
        X500Name subjectX500Name = x500NameBuilderService.build(subjectData);

        KeyPair subjectKeyPair = keyPairGeneratorService.generateRsaKeyPair();

        PrivateKey issuerPrivateKey;
        X500Name issuerX500Name;

        if (request.getType() == CertificateType.ROOT) {
            issuerPrivateKey = subjectKeyPair.getPrivate();
            issuerX500Name = subjectX500Name;
        } else {
            PrivateKeyEntity keyEntity = privateKeyRepository
                    .findByCertificate(issuerCertificate)
                    .orElseThrow(() -> new RuntimeException("Private key not found"));

            byte[] orgKeyBytes = keyEncryptionService.decrypt(
                    issuerCertificate.getOrganization().getEncryptedOrgKey(),
                    keyEncryptionService.getMasterKey()
            );

            SecretKey orgKey = new SecretKeySpec(orgKeyBytes, "AES");

            byte[] decryptedKey = keyEncryptionService.decrypt(
                    keyEntity.getEncryptedPrivateKey(),
                    orgKey
            );

            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");

                issuerPrivateKey = keyFactory.generatePrivate(
                        new PKCS8EncodedKeySpec(decryptedKey)
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to load issuer private key", e);
            }
            issuerX500Name = x500NameBuilderService.build(issuerCertificate.getSubjectData());
        }

        BigInteger serialNumber = generateSerialNumber();

        X509Certificate x509Certificate = certificateGeneratorService.generateCertificate(
                issuerX500Name,
                issuerPrivateKey,
                subjectX500Name,
                subjectKeyPair.getPublic(),
                serialNumber,
                toDate(request.getValidFrom()),
                toDate(request.getValidTo()),
                request.isCa(),
                request.isKeyCertSign(),
                request.isDigitalSignature(),
                request.isKeyEncipherment(),
                "SHA256withRSA"
        );

        Organization organization = resolveOrganization(request.getOrganizationId());

        byte[] orgKeyBytes = keyEncryptionService.decrypt(
                organization.getEncryptedOrgKey(),
                keyEncryptionService.getMasterKey()
        );

        SecretKey orgKey = new SecretKeySpec(orgKeyBytes, "AES");

        Certificate certificate = certificateEntityFactory.create(
                request,
                serialNumber.toString(),
                subjectData,
                issuerCertificate,
                organization,
                toPem(x509Certificate)
        );

        Certificate saved = certificateRepository.save(certificate);

        saveCertificateToFile(saved);

        if (request.getType() != CertificateType.END_ENTITY) {

            byte[] encryptedPrivateKey = keyEncryptionService.encrypt(
                    subjectKeyPair.getPrivate().getEncoded(),
                    orgKey
            );

            PrivateKeyEntity privateKeyEntity = new PrivateKeyEntity();
            privateKeyEntity.setCertificate(saved);
            privateKeyEntity.setEncryptedPrivateKey(encryptedPrivateKey);
            privateKeyEntity.setOrganization(organization);

            privateKeyRepository.save(privateKeyEntity);
        }

        return certificateMapper.toListResponse(saved);
    }

    private void validateIssueRequest(IssueCertificateRequest request) {
        if (request.getValidTo().isBefore(request.getValidFrom()) || request.getValidTo().isEqual(request.getValidFrom())) {
            throw new BadRequestException("Valid to must be after valid from.");
        }

        if (request.getType() != CertificateType.ROOT && request.getIssuerCertificateId() == null) {
            throw new BadRequestException("Issuer certificate is required for non-root certificates.");
        }

        if (request.getType() == CertificateType.ROOT && request.getIssuerCertificateId() != null) {
            throw new BadRequestException("Root certificate must not have an issuer certificate.");
        }
    }

    private Certificate resolveIssuerCertificate(IssueCertificateRequest request) {
        if (request.getType() == CertificateType.ROOT) {
            return null;
        }

        return certificateRepository.findById(request.getIssuerCertificateId())
                .orElseThrow(() -> new NotFoundException("Issuer certificate not found."));
    }

    private void validateIssuerIfNeeded(Certificate issuer, IssueCertificateRequest request) {
        if (request.getType() == CertificateType.ROOT) {
            return;
        }

        if (issuer.getStatus() != CertificateStatus.ACTIVE) {
            throw new BadRequestException("Issuer certificate is not active.");
        }

        if (issuer.getValidTo().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Issuer certificate is expired.");
        }

        if (!issuer.isCa()) {
            throw new BadRequestException("Issuer certificate is not CA capable.");
        }

        if (!issuer.isKeyCertSign()) {
            throw new BadRequestException("Issuer certificate cannot sign other certificates.");
        }

        if (request.getValidTo().isAfter(issuer.getValidTo())) {
            throw new BadRequestException("Issued certificate cannot outlive its issuer.");
        }
    }

    private Organization resolveOrganization(Long organizationId) {
        if (organizationId == null) {
            return null;
        }

        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found."));
    }

    private BigInteger generateSerialNumber() {
        return BigInteger.valueOf(System.currentTimeMillis()).abs();
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private String toPem(X509Certificate certificate) {
        try {
            String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                    .encodeToString(certificate.getEncoded());

            return "-----BEGIN CERTIFICATE-----\n"
                    + base64
                    + "\n-----END CERTIFICATE-----";
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode certificate to PEM.", e);
        }
    }

    // FOR CA USER

    @Override
    @Transactional(readOnly = true)
    public List<IssuerOptionResponse> getAvailableIssuersForCa() {
        User currentUser = authService.getLoggedInUser();

        return certificateRepository.findAvailableIssuersForCa(
                        currentUser.getOrganization(),
                        LocalDateTime.now()
                )
                .stream()
                .map(certificateMapper::toIssuerOptionResponse)
                .toList();
    }

    @Override
    @Transactional
    public CertificateListResponse issueCertificateAsCa(IssueCertificateRequest request) {
        User currentUser = authService.getLoggedInUser();

        Certificate issuer = certificateRepository.findById(request.getIssuerCertificateId())
                .orElseThrow(() -> new NotFoundException("Issuer not found"));

        validateCertificateForCa(issuer, request, currentUser);

        X500SubjectData subjectData = certificateSubjectMapper.fromIssueRequest(request);
        X500Name subjectX500Name = x500NameBuilderService.build(subjectData);
        X500Name issuerX500Name = x500NameBuilderService.build(issuer.getSubjectData());

        KeyPair subjectKeyPair = keyPairGeneratorService.generateRsaKeyPair();

        PrivateKey issuerPrivateKey;

        try {
            PrivateKeyEntity keyEntity = privateKeyRepository
                    .findByCertificate(issuer)
                    .orElseThrow(() -> new RuntimeException("Private key not found"));

            byte[] orgKeyBytes = keyEncryptionService.decrypt(
                    issuer.getOrganization().getEncryptedOrgKey(),
                    keyEncryptionService.getMasterKey()
            );

            SecretKey orgKey = new SecretKeySpec(orgKeyBytes, "AES");

            byte[] decryptedKey = keyEncryptionService.decrypt(
                    keyEntity.getEncryptedPrivateKey(),
                    orgKey
            );

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            issuerPrivateKey = keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(decryptedKey)
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to load issuer private key", e);
        }

        BigInteger serialNumber = generateSerialNumber();
        X509Certificate x509Certificate = certificateGeneratorService.generateCertificate(
                issuerX500Name,
                issuerPrivateKey,
                subjectX500Name,
                subjectKeyPair.getPublic(),
                serialNumber,
                toDate(request.getValidFrom()),
                toDate(request.getValidTo()),
                request.isCa(),
                request.isKeyCertSign(),
                request.isDigitalSignature(),
                request.isKeyEncipherment(),
                "SHA256withRSA"
        );

        Certificate certificate = certificateEntityFactory.create(
                request,
                serialNumber.toString(),
                subjectData,
                issuer,
                currentUser.getOrganization(),
                toPem(x509Certificate)
        );

        Certificate saved = certificateRepository.save(certificate);

        saveCertificateToFile(saved);

        byte[] orgKeyBytes = keyEncryptionService.decrypt(
                currentUser.getOrganization().getEncryptedOrgKey(),
                keyEncryptionService.getMasterKey()
        );

        SecretKey orgKey = new SecretKeySpec(orgKeyBytes, "AES");

        byte[] encryptedPrivateKey = keyEncryptionService.encrypt(
                subjectKeyPair.getPrivate().getEncoded(),
                orgKey
        );

        PrivateKeyEntity privateKeyEntity = new PrivateKeyEntity();
        privateKeyEntity.setCertificate(saved);
        privateKeyEntity.setEncryptedPrivateKey(encryptedPrivateKey);
        privateKeyEntity.setOrganization(currentUser.getOrganization());

        privateKeyRepository.save(privateKeyEntity);

        return certificateMapper.toListResponse(saved);
    }

    private void validateCertificateForCa(Certificate issuer, IssueCertificateRequest request, User currentUser) {
        LocalDateTime now = LocalDateTime.now();

        if (request.getValidFrom().isBefore(now.minusMinutes(5))) {
            throw new BadRequestException("The certificate start date cannot be in the past.");
        }

        if (!request.getValidTo().isAfter(request.getValidFrom())) {
            throw new BadRequestException("The expiration date must be after the start date.");
        }

        if (issuer.getOrganization() != null && !issuer.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new BadRequestException("The issuer must belong to your organization or be RootCA.");
        }

        if (request.getType() == CertificateType.ROOT) {
            throw new BadRequestException("CA users are not authorized to create Root certificates.");
        }

        if (issuer.getStatus() != CertificateStatus.ACTIVE) {
            throw new BadRequestException("The issuer certificate is not active.");
        }

        if (issuer.getValidTo().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("The issuer certificate has expired.");
        }

        if (!issuer.isCa()) {
            throw new BadRequestException("The selected certificate is not a CA.");
        }

        if (!issuer.isKeyCertSign()) {
            throw new BadRequestException("The selected certificate does not have permission to sign other certificates.");
        }

        if (request.getValidTo().isAfter(issuer.getValidTo())) {
            throw new BadRequestException("The new certificate validity cannot exceed the issuer's validity.");
        }

        verifyDigitalSignature(issuer);
        validateExtensions(request);
    }

    private void validateExtensions(IssueCertificateRequest request) {
        if (request.getType() == CertificateType.END_ENTITY) {
            if (request.isCa() || request.isKeyCertSign()) {
                throw new BadRequestException("End-entity certificates cannot have CA or KeyCertSign capabilities.");
            }
        }

        if (request.getType() == CertificateType.INTERMEDIATE || request.getType() == CertificateType.ROOT) {
            if (!request.isCa() || !request.isKeyCertSign()) {
                throw new BadRequestException("CA certificates must have CA and KeyCertSign capabilities enabled.");
            }
        }
    }


    private void verifyDigitalSignature(Certificate issuer) {
        try {
            X509Certificate issuerX509 = convertToX509(issuer.getEncodedCertificate());
            PublicKey publicKeyToVerifyWith;

            if (issuer.getIssuerCertificate() == null) {
                publicKeyToVerifyWith = issuerX509.getPublicKey();
            } else {
                X509Certificate parentX509 = convertToX509(issuer.getIssuerCertificate().getEncodedCertificate());
                publicKeyToVerifyWith = parentX509.getPublicKey();
            }

            issuerX509.verify(publicKeyToVerifyWith);

        } catch (java.security.SignatureException | java.security.InvalidKeyException e) {
            throw new BadRequestException("Issuer digital signature is invalid.");
        } catch (Exception e) {
            throw new BadRequestException("Error verifying digital signature: " + e.getMessage());
        }
    }

    private X509Certificate convertToX509(String pemCertificate) {
        try {
            byte[] decoded = java.util.Base64.getMimeDecoder().decode(
                    pemCertificate.replace("-----BEGIN CERTIFICATE-----", "")
                            .replace("-----END CERTIFICATE-----", "")
                            .replace("\n", "")
            );
            java.security.cert.CertificateFactory factory = java.security.cert.CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new java.io.ByteArrayInputStream(decoded));
        } catch (Exception e) {
            throw new BadRequestException("Failed to convert issuer certificate.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateListResponse> getAllForCa() {
        User currentUser = authService.getLoggedInUser();

        return certificateRepository.findAllByOrganizationOrderByCreatedAtDesc(currentUser.getOrganization())
                .stream()
                .map(certificateMapper::toListResponse)
                .toList();
    }
    private void saveCertificateToFile(Certificate certificate) {
        try {
            Path dir = Paths.get("certs");

            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String fileName = certificate.getSubjectData().getCommonName()
                    + "_" + certificate.getSerialNumber() + ".pem";

            Path filePath = dir.resolve(fileName);

            Files.write(filePath, certificate.getEncodedCertificate().getBytes());

        } catch (Exception e) {
            throw new RuntimeException("Failed to save certificate to file", e);
        }
    }
}