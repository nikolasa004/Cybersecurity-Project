package rs.ac.uns.ftn.pki.dto.certificateRequest;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CreateCertificateRequestRequest {

    @NotNull
    private Long issuerCertificateId;

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    @Future
    private LocalDateTime validTo;
}
