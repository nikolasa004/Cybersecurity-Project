package rs.ac.uns.ftn.pki.dto.certificate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class IssuerOptionResponse {
    private Long id;
    private String label;
    private CertificateType type;
    private String serialNumber;
    private LocalDateTime validTo;
}