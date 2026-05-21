package rs.ac.uns.ftn.pki.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.pki.dto.Mfa.MfaSetupResponse;
import rs.ac.uns.ftn.pki.dto.user.UserProfileResponse;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.repository.UserRepository;
import rs.ac.uns.ftn.pki.service.UserService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }
    @GetMapping("/me/2fa/setup")
    public ResponseEntity<MfaSetupResponse> getMfaSetup(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userService.generateMfaSetup(user));
    }

    @PostMapping("/me/2fa/enable")
    public ResponseEntity<String> enableMfa(@RequestParam String code, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        boolean isEnabled = userService.enableMfa(user, code);
        if (isEnabled) {
            return ResponseEntity.ok("Two factor authentication enabled successfully.");
        }
        return ResponseEntity.badRequest().body("Invalid 2FA code. Please try again.");
    }

    @PostMapping("/me/2fa/disable")
    public ResponseEntity<String> disableMfa(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);

        return ResponseEntity.ok("Two factor authentication disabled successfully.");
    }

}