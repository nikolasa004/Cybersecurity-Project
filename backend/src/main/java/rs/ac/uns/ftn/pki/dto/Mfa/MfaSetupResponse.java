package rs.ac.uns.ftn.pki.dto.Mfa;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // <-- Ova anotacija će automatski napraviti konstruktor sa argumentima
public class MfaSetupResponse {
    private String secretKey;
    private String qrCodeUrl;
}