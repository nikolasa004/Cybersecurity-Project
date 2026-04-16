package rs.ac.uns.ftn.pki.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String password;
}