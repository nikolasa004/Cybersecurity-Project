package rs.ac.uns.ftn.pki.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordCompleteRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String decryptedChallenge;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}