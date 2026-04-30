package rs.ac.uns.ftn.pki.dto.certificate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;

import java.time.LocalDateTime;

@Getter
@Setter
public class IssueCertificateRequest {

    @NotNull
    private CertificateType type;

    private Long issuerCertificateId;

    private Long organizationId;

    @NotBlank
    private String commonName;

    private String country;
    private String state;
    private String locality;
    private String organization;
    private String organizationalUnit;
    private String email;
    
    private String subjectAlternativeName;

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    @Future
    private LocalDateTime validTo;

    private boolean ca;
    private boolean keyCertSign;
    private boolean digitalSignature;
    private boolean keyEncipherment;
}