package rs.ac.uns.ftn.pki.dto.certificateRequest;

import lombok.Getter;
import lombok.Setter;

import java.security.PublicKey;

@Getter
@Setter
public class ParsedCsrData {

    private String commonName;
    private String country;
    private String state;
    private String locality;
    private String organization;
    private String organizationalUnit;
    private String email;
    private PublicKey publicKey;
}