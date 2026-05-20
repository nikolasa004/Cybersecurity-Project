package rs.ac.uns.ftn.pki.mapper;

import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateListResponse;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateResponse;
import rs.ac.uns.ftn.pki.dto.certificate.IssuerOptionResponse;
import rs.ac.uns.ftn.pki.model.entity.Certificate;

import java.util.Base64;

@Component
public class CertificateMapper {

    public CertificateListResponse toListResponse(Certificate certificate) {
        return new CertificateListResponse(
                certificate.getId(),
                certificate.getSerialNumber(),
                certificate.getType(),
                certificate.getStatus(),
                certificate.getSubjectData() != null ? certificate.getSubjectData().getCommonName() : null,
                certificate.getSubjectData() != null ? certificate.getSubjectData().getOrganization() : null,
                resolveIssuerCommonName(certificate),
                certificate.getOrganization() != null ? certificate.getOrganization().getName() : null,
                certificate.isCa(),
                certificate.getValidFrom(),
                certificate.getValidTo(),
                certificate.getSignatureAlgorithm()
        );
    }

    public CertificateResponse toResponse(Certificate certificate, byte[] certificateBytes) {
        if (certificateBytes == null) {
            certificateBytes = new byte[0];
        }
        String base64 =
                Base64.getEncoder()
                        .encodeToString(certificateBytes);

        CertificateResponse response =  new CertificateResponse(
                certificate.getId(),
                certificate.getSerialNumber(),
                certificate.getType(),
                certificate.getStatus(),
                certificate.getSubjectData() != null ? certificate.getSubjectData().getCommonName() : null,
                certificate.getSubjectData() != null ? certificate.getSubjectData().getOrganization() : null,
                resolveIssuerCommonName(certificate),
                certificate.getOrganization() != null ? certificate.getOrganization().getName() : null,
                certificate.isCa(),
                certificate.getValidFrom(),
                certificate.getValidTo(),
                certificate.getSignatureAlgorithm(),
                base64
        );
        return response;
    }

    public IssuerOptionResponse toIssuerOptionResponse(Certificate certificate) {
        String commonName = certificate.getSubjectData() != null
                ? certificate.getSubjectData().getCommonName()
                : "Unknown";

        String label = commonName + " (" + certificate.getType().name() + ")";

        return new IssuerOptionResponse(
                certificate.getId(),
                label,
                certificate.getType(),
                certificate.getSerialNumber(),
                certificate.getValidTo()
        );
    }

    private String resolveIssuerCommonName(Certificate certificate) {
        if (certificate.isSelfSigned()) {
            return certificate.getSubjectData() != null
                    ? certificate.getSubjectData().getCommonName()
                    : "Self-signed";
        }

        if (certificate.getIssuerCertificate() != null && certificate.getIssuerCertificate().getSubjectData() != null) {
            return certificate.getIssuerCertificate().getSubjectData().getCommonName();
        }

        return certificate.getIssuerCommonNameSnapshot();
    }
}