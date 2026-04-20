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
import rs.ac.uns.ftn.pki.model.entity.Certificate;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.model.entity.X500SubjectData;
import rs.ac.uns.ftn.pki.model.enums.CertificateStatus;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;
import rs.ac.uns.ftn.pki.repository.CertificateRepository;
import rs.ac.uns.ftn.pki.repository.OrganizationRepository;
import rs.ac.uns.ftn.pki.service.CertificateService;
import rs.ac.uns.ftn.pki.service.crypto.CertificateEntityFactory;
import rs.ac.uns.ftn.pki.service.crypto.CertificateKeyMaterialService;
import rs.ac.uns.ftn.pki.service.crypto.CertificateGeneratorService;
import rs.ac.uns.ftn.pki.service.crypto.KeyPairGeneratorService;
import rs.ac.uns.ftn.pki.service.crypto.X500NameBuilderService;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final OrganizationRepository organizationRepository;
    private final CertificateMapper certificateMapper;
    private final CertificateSubjectMapper certificateSubjectMapper;
    private final CertificateEntityFactory certificateEntityFactory;
    private final CertificateKeyMaterialService certificateKeyMaterialService;
    private final KeyPairGeneratorService keyPairGeneratorService;
    private final X500NameBuilderService x500NameBuilderService;
    private final CertificateGeneratorService certificateGeneratorService;

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
            issuerPrivateKey = certificateKeyMaterialService.getIssuerPrivateKey(issuerCertificate);
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

        Certificate certificate = certificateEntityFactory.create(
                request,
                serialNumber.toString(),
                subjectData,
                issuerCertificate,
                organization,
                toPem(x509Certificate)
        );

        Certificate saved = certificateRepository.save(certificate);

        /*
         * Temporary runtime key material retention for Student 1 scope only.
         * Functionality 6 should replace this with secure encrypted private key storage.
         */
        certificateKeyMaterialService.rememberPrivateKey(saved.getSerialNumber(), subjectKeyPair.getPrivate());

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
            String base64 = java.util.Base64.getMimeEncoder(64, "\n".getBytes())
                    .encodeToString(certificate.getEncoded());

            return "-----BEGIN CERTIFICATE-----\n"
                    + base64
                    + "\n-----END CERTIFICATE-----";
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode certificate to PEM.", e);
        }
    }
}