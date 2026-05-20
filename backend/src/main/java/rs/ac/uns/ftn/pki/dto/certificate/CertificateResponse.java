package rs.ac.uns.ftn.pki.dto.certificate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rs.ac.uns.ftn.pki.model.enums.CertificateStatus;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CertificateResponse {
    private Long id;
    private String serialNumber;
    private CertificateType type;
    private CertificateStatus status;
    private String subjectCommonName;
    private String subjectOrganization;
    private String issuerCommonName;
    private String organizationName;
    private boolean ca;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String signatureAlgorithm;
    private String certificateData;
}