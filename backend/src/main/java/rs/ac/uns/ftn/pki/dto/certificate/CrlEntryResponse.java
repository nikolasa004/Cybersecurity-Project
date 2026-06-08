package rs.ac.uns.ftn.pki.dto.certificate;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CrlEntryResponse {
    private String serialNumber;
    private String revocationReason;
    private LocalDateTime revokedAt;
}