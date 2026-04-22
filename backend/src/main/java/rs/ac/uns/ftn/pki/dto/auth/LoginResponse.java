package rs.ac.uns.ftn.pki.dto.auth;

import lombok.Builder;

@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String role,
        Long organizationId
) {}
