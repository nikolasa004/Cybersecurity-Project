package rs.ac.uns.ftn.pki.dto.certificate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import rs.ac.uns.ftn.pki.model.enums.RevocationReason;

@Getter
@Setter
public class RevokeCertificateRequest {

    @NotNull(message = "Revocation reason is required.")
    private RevocationReason reason;
}