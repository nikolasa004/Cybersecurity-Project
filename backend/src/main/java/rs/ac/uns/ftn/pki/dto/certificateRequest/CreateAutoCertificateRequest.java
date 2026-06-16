package rs.ac.uns.ftn.pki.dto.certificateRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CreateAutoCertificateRequest {

    private Long issuerCertificateId;

    private String commonName;
    private String organization;
    private String organizationalUnit;
    private String country;
    private String state;
    private String locality;
    private String email;

    private String subjectAlternativeName;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}