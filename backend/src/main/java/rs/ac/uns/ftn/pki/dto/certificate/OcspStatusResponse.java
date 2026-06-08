package rs.ac.uns.ftn.pki.dto.certificate;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OcspStatusResponse {
    private String serialNumber;
    private String status; // GOOD, REVOKED, UNKNOWN
    private LocalDateTime revokedAt;
    private String revocationReason;
}