package rs.ac.uns.ftn.pki.controller.auth;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.uns.ftn.pki.dto.Mfa.MfaVerifyRequest;
import rs.ac.uns.ftn.pki.dto.auth.*;
import rs.ac.uns.ftn.pki.dto.organization.OrganizationResponse;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.security.JwtService;
import rs.ac.uns.ftn.pki.security.RefreshTokenService;
import rs.ac.uns.ftn.pki.service.OrganizationService;
import rs.ac.uns.ftn.pki.service.PublicUserAuthService;
import rs.ac.uns.ftn.pki.service.impl.MfaServiceImpl;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final MfaServiceImpl mfaService;
    private final UserDetailsService userDetailsService;
    private final PublicUserAuthService publicUserAuthService;
    private final OrganizationService organizationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = (User) authentication.getPrincipal();

        if (user.isTwoFactorEnabled()) {
            return ResponseEntity.ok(LoginResponse.builder()
                    .accessToken(null)
                    .refreshToken(null)
                    .role(user.getRole().name())
                    .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                    .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : "System Admin")
                    .isTwoFactorRequired(true)
                    .build());
        }

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

    @PostMapping("/login/verify-2fa")
    public ResponseEntity<LoginResponse> verifyMfaLogin(@RequestBody MfaVerifyRequest request) {
        User user = (User) userDetailsService.loadUserByUsername(request.email());

        boolean isCodeValid = mfaService.verifyCode(user.getTwoFactorSecret(), request.code());

        if (!isCodeValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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
                .isTwoFactorRequired(false)
                .build());
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterUserRequest request) {
        publicUserAuthService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful. Check your email for activation link."));
    }

    @PostMapping("/activation/challenge")
    public ResponseEntity<ActivationChallengeResponse> createActivationChallenge(@RequestBody ActivationChallengeRequest request) {
        return ResponseEntity.ok(publicUserAuthService.createActivationChallenge(request));
    }

    @PostMapping("/activation/complete")
    public ResponseEntity<?> completeActivation(@RequestBody ActivationCompleteRequest request) {
        publicUserAuthService.completeActivation(request);
        return ResponseEntity.ok(Map.of("message", "Account activated successfully."));
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody ForgotPasswordRequest request) {
        publicUserAuthService.requestPasswordReset(request);
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a reset link has been sent."));
    }

    @PostMapping("/forgot-password/challenge")
    public ResponseEntity<ActivationChallengeResponse> createPasswordResetChallenge(@RequestBody ForgotPasswordChallengeRequest request) {
        return ResponseEntity.ok(publicUserAuthService.createPasswordResetChallenge(request));
    }

    @PostMapping("/forgot-password/complete")
    public ResponseEntity<?> completePasswordReset(@RequestBody ForgotPasswordCompleteRequest request) {
        publicUserAuthService.completePasswordReset(request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
    
    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationResponse>> getPublicOrganizations() {
        return ResponseEntity.ok(organizationService.getAll());
    }
}