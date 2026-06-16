package rs.ac.uns.ftn.pki.dto.certificateRequest;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectCertificateRequest {

    @NotBlank
    private String rejectionReason;
}
