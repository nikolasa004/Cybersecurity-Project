package rs.ac.uns.ftn.pki.dto.Mfa;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
        @NotBlank String email,
        @NotBlank String code
) {}