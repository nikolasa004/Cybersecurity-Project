package rs.ac.uns.ftn.pki.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.uns.ftn.pki.dto.auth.*;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.security.JwtService;
import rs.ac.uns.ftn.pki.security.RefreshTokenService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = (User) authentication.getPrincipal();

        String accessToken = jwtService.generateToken(user);

        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();

        String orgName = "System Admin";
        if (user.getOrganization() != null) {
            orgName = user.getOrganization().getName();
        }

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(orgName)
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        String newRefreshToken = refreshTokenService.rotateToken(request.refreshToken());

        User user = refreshTokenService.findByToken(newRefreshToken).getUser();
        String newAccessToken = jwtService.generateToken(user);

        return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, newRefreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        if (request.refreshToken() != null) {
            refreshTokenService.deleteByToken(request.refreshToken());
        }
        return ResponseEntity.ok("Logged out successfully");
    }
}