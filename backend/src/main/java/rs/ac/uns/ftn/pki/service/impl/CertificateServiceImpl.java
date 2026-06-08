package rs.ac.uns.ftn.pki.service.impl;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import rs.ac.uns.ftn.pki.dto.certificate.*;
import rs.ac.uns.ftn.pki.exception.BadRequestException;
import rs.ac.uns.ftn.pki.exception.CertificateFileReadException;
import rs.ac.uns.ftn.pki.exception.NotFoundException;
import rs.ac.uns.ftn.pki.exception.CreatePkcs12Exception;
import rs.ac.uns.ftn.pki.mapper.CertificateMapper;
import rs.ac.uns.ftn.pki.mapper.CertificateSubjectMapper;
import rs.ac.uns.ftn.pki.model.entity.Certificate;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.model.entity.PrivateKeyEntity;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.model.entity.X500SubjectData;
import rs.ac.uns.ftn.pki.model.enums.CertificateStatus;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;
import rs.ac.uns.ftn.pki.repository.CertificateRepository;
import rs.ac.uns.ftn.pki.repository.OrganizationRepository;
import rs.ac.uns.ftn.pki.repository.PrivateKeyRepository;
import rs.ac.uns.ftn.pki.service.AuthService;
import rs.ac.uns.ftn.pki.service.CertificateService;
import rs.ac.uns.ftn.pki.service.crypto.CertificateEntityFactory;
import rs.ac.uns.ftn.pki.service.crypto.CertificateGeneratorService;
import rs.ac.uns.ftn.pki.service.crypto.KeyEncryptionService;
import rs.ac.uns.ftn.pki.service.crypto.KeyPairGeneratorService;
import rs.ac.uns.ftn.pki.service.crypto.X500NameBuilderService;

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
    private final KeyPairGeneratorService keyPairGeneratorService;
    private final X500NameBuilderService x500NameBuilderService;
    private final CertificateGeneratorService certificateGeneratorService;
    private final AuthService authService;

    @Value("${app.public-base-url:https://localhost:8443}")
    private String publicBaseUrl;

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
                .filter(this::isChainUsableForIssuing)
                .map(certificateMapper::toIssuerOptionResponse)
                .toList();
    }

    @Override
    @Transactional
    public CertificateResponse issueCertificate(IssueCertificateRequest request) {
        validateIssueRequest(request);

        Certificate issuerCertificate = resolveIssuerCertificate(request);
        validateIssuerIfNeeded(issuerCertificate, request);
        
        Organization organization = resolveOrganization(request.getOrganizationId());

        X500SubjectData subjectData = certificateSubjectMapper.fromIssueRequest(request);
        subjectData.setOrganization(organization.getName());

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
                    keyEncryptionService.getMasterKeyByVersion(issuerCertificate.getOrganization().getMasterKeyVersion())
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
        
        String crlIssuerSerial = request.getType() == CertificateType.ROOT
                ? serialNumber.toString()
                : issuerCertificate.getSerialNumber();

        String crlUrl = buildCrlUrl(crlIssuerSerial);
        String ocspUrl = buildOcspUrl(serialNumber.toString());

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
                request.getSubjectAlternativeName(),
                "SHA256withRSA",
                crlUrl,
                ocspUrl
        );

        byte[] orgKeyBytes = keyEncryptionService.decrypt(
                organization.getEncryptedOrgKey(),
                keyEncryptionService.getMasterKeyByVersion(organization.getMasterKeyVersion())
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
        certificate.setSubjectAlternativeName(request.getSubjectAlternativeName());

        Certificate saved = certificateRepository.save(certificate);

        saveCertificateToFile(saved);
        byte[] pkcs12 = null;
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
        } else {
            try {
                pkcs12 = createPkcs12(
                        subjectKeyPair.getPrivate(),
                        certificate,
                        null
                );
            } catch (Exception e) {
                throw new CreatePkcs12Exception("Failed to create PKCS12 for End Entity", e);
            }
        }
        return certificateMapper.toResponse(saved, pkcs12);
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
        
        if (request.getValidFrom().isBefore(issuer.getValidFrom())) {
            throw new BadRequestException("Issued certificate cannot start before its issuer.");
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
                .filter(this::isChainUsableForIssuing)
                .map(certificateMapper::toIssuerOptionResponse)
                .toList();
    }

    @Override
    @Transactional
    public CertificateResponse issueCertificateAsCa(IssueCertificateRequest request) {
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
                    .orElseThrow(() -> new NotFoundException("Private key not found"));

            byte[] orgKeyBytes = keyEncryptionService.decrypt(
                    issuer.getOrganization().getEncryptedOrgKey(),
                    keyEncryptionService.getMasterKeyByVersion(issuer.getOrganization().getMasterKeyVersion())
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
            throw new NotFoundException("Failed to load issuer private key");
        }

        BigInteger serialNumber = generateSerialNumber();
        
        String crlUrl = buildCrlUrl(issuer.getSerialNumber());
        String ocspUrl = buildOcspUrl(serialNumber.toString());
        
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
                request.getSubjectAlternativeName(),
                "SHA256withRSA",
                crlUrl,
                ocspUrl
        );

        Certificate certificate = certificateEntityFactory.create(
                request,
                serialNumber.toString(),
                subjectData,
                issuer,
                currentUser.getOrganization(),
                toPem(x509Certificate)
        );
        certificate.setSubjectAlternativeName(request.getSubjectAlternativeName());

        Certificate saved = certificateRepository.save(certificate);

        saveCertificateToFile(saved);
        byte[] pkcs12 = null;
        if (request.getType() != CertificateType.END_ENTITY) {
            byte[] orgKeyBytes = keyEncryptionService.decrypt(
                    issuer.getOrganization().getEncryptedOrgKey(),
                    keyEncryptionService.getMasterKeyByVersion(issuer.getOrganization().getMasterKeyVersion())
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

        } else {
            try {
            pkcs12 = createPkcs12(
                subjectKeyPair.getPrivate(),
                certificate,
                    null);
            } catch (Exception e) {
            throw new CreatePkcs12Exception("Failed to create PKCS12 for End Entity", e);
            }
        }
        return certificateMapper.toResponse(saved, pkcs12);
    }

    private void validateCertificateForCa(Certificate issuer, IssueCertificateRequest request, User currentUser) {
        LocalDateTime now = LocalDateTime.now();

        if (request.getValidFrom().isBefore(now.minusMinutes(5))) {
            throw new BadRequestException("The certificate start date cannot be in the past.");
        }
        
        if (request.getValidFrom().isBefore(issuer.getValidFrom())) {
            throw new BadRequestException("The new certificate cannot start before the issuer's validity starts.");
        }

        if (!request.getValidTo().isAfter(request.getValidFrom())) {
            throw new BadRequestException("The expiration date must be after the start date.");
        }

        if (issuer.getOrganization() != null && !issuer.getOrganization().getId().equals(currentUser.getOrganization().getId()) && !issuer.getOrganization().getId().equals(1L)) {
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

        verifyCertificateChain(issuer);
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

    private void verifyCertificateChain(Certificate certificate) {

        try {

            Certificate current = certificate;

            while (current != null) {
            	
            	if (current.getStatus() == CertificateStatus.REVOKED) {
            	    throw new BadRequestException("Certificate chain contains a revoked certificate.");
            	}

                X509Certificate currentX509 =
                        convertToX509(current.getEncodedCertificate());

                PublicKey publicKeyToVerifyWith;

                // ROOT self-signed certificate
                if (current.getIssuerCertificate() == null) {

                    publicKeyToVerifyWith =
                            currentX509.getPublicKey();

                } else {

                    Certificate parent =
                            current.getIssuerCertificate();

                    X509Certificate parentX509 =
                            convertToX509(
                                    parent.getEncodedCertificate()
                            );

                    publicKeyToVerifyWith =
                            parentX509.getPublicKey();
                }

                currentX509.verify(publicKeyToVerifyWith);
                // stop after verifying ROOT
                if (current.getType() == CertificateType.ROOT) {
                    break;
                }

                current = current.getIssuerCertificate();
            }

        } catch (java.security.SignatureException
                 | java.security.InvalidKeyException e) {

            throw new BadRequestException(
                    "Certificate chain signature is invalid."
            );

        } catch (Exception e) {

            throw new BadRequestException(
                    "Error verifying certificate chain: "
                            + e.getMessage()
            );
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
            throw new CertificateFileReadException("Failed to save certificate to file", e);
        }
    }
    @Transactional(readOnly = true)
    public List<CertificateListResponse> getAllForUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return certificateRepository
                .findAllBySubjectDataEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(certificateMapper::toListResponse)
                .toList();
    }


    private String readCertificateFromFile(Certificate certificate) {
        try {
            Path dir = Paths.get("certs");
            String fileName = certificate.getSubjectData().getCommonName()
                    + "_" + certificate.getSerialNumber() + ".pem";
            Path filePath = dir.resolve(fileName);
            return Files.readString(filePath);
        } catch (Exception e) {
            throw new CertificateFileReadException("Failed to read certificate from file", e);
        }
    }

    private java.security.cert.Certificate[]  buildCertificateChain(Certificate certificate) {

        List<java.security.cert.Certificate> chain =
                new ArrayList<>();

        Certificate current = certificate;

        while (current != null) {

            String pem = readCertificateFromFile(current);

            X509Certificate x509 =
                    convertToX509(pem);

            chain.add(x509);

            if (current.getType() == CertificateType.ROOT) {
                break;
            }

            current = current.getIssuerCertificate();
        }

        return chain.toArray(
                new java.security.cert.Certificate[0]
        );
    }

    private byte[] createPkcs12(
            PrivateKey privateKey,
            Certificate certificate,
            String password) {

        try (ByteArrayOutputStream baos =
                     new ByteArrayOutputStream()) {

            KeyStore pkcs12 =
                    KeyStore.getInstance("PKCS12");

            pkcs12.load(null, null);

            java.security.cert.Certificate[] chain =
                    buildCertificateChain(certificate);

            String alias =
                    certificate.getSubjectData()
                            .getCommonName()
                            + "_"
                            + certificate.getSerialNumber();
            char[] passwordChars =
                    password == null
                            ? new char[0]
                            : password.toCharArray();
            pkcs12.setKeyEntry(
                    alias,
                    privateKey,
                    passwordChars,
                    chain
            );

            pkcs12.store(baos, passwordChars);

            return baos.toByteArray();

        } catch (Exception e) {

            throw new CreatePkcs12Exception(
                    "Failed to create PKCS12 keystore",
                    e
            );
        }
    }


    @Transactional(readOnly = true)
    public byte[] downloadPkcs12WithKey(Long certificateId, DownloadCertificateRequest request) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("Certificate not found."));
        
        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            throw new BadRequestException("Private key export is not allowed for revoked certificates.");
        }

        if (certificate.getType() == CertificateType.END_ENTITY) {
            throw new BadRequestException("Download with private key is not allowed for End Entity certificates.");
        }

        PrivateKeyEntity privateKeyEntity = privateKeyRepository.findByCertificate(certificate)
                .orElseThrow(() -> new NotFoundException("Private key not found for certificate."));

        if (certificate.getOrganization() == null) {
            throw new BadRequestException("Certificate organization missing.");
        }

        byte[] orgKeyBytes = keyEncryptionService.decrypt(
                certificate.getOrganization().getEncryptedOrgKey(),
                keyEncryptionService.getMasterKeyByVersion(certificate.getOrganization().getMasterKeyVersion())
        );
        SecretKey orgKey = new SecretKeySpec(orgKeyBytes, "AES");
        byte[] decryptedKey = keyEncryptionService.decrypt(
                privateKeyEntity.getEncryptedPrivateKey(),
                orgKey
        );
        PrivateKey privateKey;
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decryptedKey));
        } catch (Exception e) {
            throw new NotFoundException("Failed to load private key");
        }
        return createPkcs12(privateKey, certificate, request.getPassword());
    }

    @Override
    @Transactional(readOnly = true)
    public String getCertificateAsPem(Long certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("Certificate not found with id: " + certificateId));

        return readCertificateFromFile(certificate);
    }

    private String buildCrlUrl(String issuerSerialNumber) {
        return publicBaseUrl + "/api/revocation/crl/" + issuerSerialNumber;
    }

    private String buildOcspUrl(String serialNumber) {
        return publicBaseUrl + "/api/revocation/ocsp/" + serialNumber;
    }

    private boolean isChainUsableForIssuing(Certificate certificate) {
        Certificate current = certificate;

        while (current != null) {
            if (current.getStatus() != CertificateStatus.ACTIVE) {
                return false;
            }
            current = current.getIssuerCertificate();
        }

        return true;
    }

    private void ensureCertificateCanBeRevoked(Certificate certificate) {
        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            throw new BadRequestException("Certificate is already revoked.");
        }

        if (certificate.getStatus() != CertificateStatus.ACTIVE) {
            throw new BadRequestException("Only active certificates can be revoked.");
        }
    }

    private void applyRevocation(Certificate certificate, RevokeCertificateRequest request) {
        ensureCertificateCanBeRevoked(certificate);
        certificate.setStatus(CertificateStatus.REVOKED);
        certificate.setRevokedAt(LocalDateTime.now());
        certificate.setRevocationReason(request.getReason().name());
        certificateRepository.save(certificate);
    }

    @Override
    @Transactional
    public void revokeCertificateAsAdmin(Long certificateId, RevokeCertificateRequest request) {
        Certificate certificate = certificateRepository.findWithIssuerCertificateById(certificateId)
                .orElseThrow(() -> new NotFoundException("Certificate not found."));

        applyRevocation(certificate, request);
    }

    @Override
    @Transactional
    public void revokeCertificateAsCa(Long certificateId, RevokeCertificateRequest request) {
        User currentUser = authService.getLoggedInUser();

        Certificate certificate = certificateRepository.findWithIssuerCertificateById(certificateId)
                .orElseThrow(() -> new NotFoundException("Certificate not found."));

        if (certificate.getOrganization() == null ||
                !certificate.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new BadRequestException("You can revoke only certificates from your organization.");
        }

        applyRevocation(certificate, request);
    }

    @Override
    @Transactional(readOnly = true)
    public CrlResponse getCrl(String issuerSerialNumber) {
        List<CrlEntryResponse> entries = certificateRepository.findRevokedByIssuerSerialNumber(issuerSerialNumber)
                .stream()
                .map(c -> new CrlEntryResponse(
                        c.getSerialNumber(),
                        c.getRevocationReason(),
                        c.getRevokedAt()
                ))
                .toList();

        return new CrlResponse(issuerSerialNumber, entries);
    }

    @Override
    @Transactional(readOnly = true)
    public OcspStatusResponse getOcspStatus(String serialNumber) {
        Certificate certificate = certificateRepository.findBySerialNumber(serialNumber)
                .orElse(null);

        if (certificate == null) {
            return new OcspStatusResponse(serialNumber, "UNKNOWN", null, null);
        }

        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            return new OcspStatusResponse(
                    serialNumber,
                    "REVOKED",
                    certificate.getRevokedAt(),
                    certificate.getRevocationReason()
            );
        }

        return new OcspStatusResponse(serialNumber, "GOOD", null, null);
    }
    
    @Override
    @Transactional
    public void revokeCertificateAsUser(Long certificateId, RevokeCertificateRequest request) {
        User currentUser = authService.getLoggedInUser();

        Certificate certificate = certificateRepository.findWithIssuerCertificateById(certificateId)
                .orElseThrow(() -> new NotFoundException("Certificate not found."));

        if (certificate.getSubjectData() == null || certificate.getSubjectData().getEmail() == null) {
            throw new BadRequestException("Certificate is not linked to a user.");
        }

        if (!certificate.getSubjectData().getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            throw new BadRequestException("You can revoke only your own certificates.");
        }

        if (certificate.getType() != CertificateType.END_ENTITY) {
            throw new BadRequestException("Ordinary users can revoke only end-entity certificates.");
        }

        applyRevocation(certificate, request);
    }
}