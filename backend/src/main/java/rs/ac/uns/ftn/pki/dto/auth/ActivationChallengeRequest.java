package rs.ac.uns.ftn.pki.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivationChallengeRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String publicKey;
}