package rs.ac.uns.ftn.pki.dto.certificate;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CrlResponse {
    private String issuerSerialNumber;
    private List<CrlEntryResponse> revokedCertificates;
}