package rs.ac.uns.ftn.pki.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordChallengeRequest {

    @NotBlank
    private String token;
}