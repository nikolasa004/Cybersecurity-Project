package rs.ac.uns.ftn.pki.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.pki.dto.Mfa.MfaSetupResponse;
import rs.ac.uns.ftn.pki.dto.user.UserProfileResponse;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.repository.UserRepository;
import rs.ac.uns.ftn.pki.service.MfaService;
import rs.ac.uns.ftn.pki.service.UserService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final MfaService mfaService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }
    @GetMapping("/me/2fa/setup")
    public ResponseEntity<MfaSetupResponse> getMfaSetup(Authentication authentication) {
        // Iz bezbednosnog konteksta izvlačimo ulogovanog korisnika
        User user = (User) authentication.getPrincipal();
        // Pozivamo servis i prosleđujemo mu tog korisnika
        return ResponseEntity.ok(userService.generateMfaSetup(user));
    }

    @PostMapping("/me/2fa/enable")
    public ResponseEntity<String> enableMfa(@RequestParam String code, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        boolean isEnabled = userService.enableMfa(user, code);
        if (isEnabled) {
            return ResponseEntity.ok("Dvofaktorska autentikacija uspešno aktivirana.");
        }
        return ResponseEntity.badRequest().body("Neispravan kôd. Pokušajte ponovo.");
    }

    @PostMapping("/me/2fa/disable")
    public ResponseEntity<String> disableMfa(org.springframework.security.core.Authentication authentication) {
        // Izvlačimo trenutno ulogovanog korisnika iz bezbednosnog konteksta
        rs.ac.uns.ftn.pki.model.entity.User user = (rs.ac.uns.ftn.pki.model.entity.User) authentication.getPrincipal();

        // Isključujemo 2FA i brišemo tajni ključ iz baze radi bezbednosti
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user); // Osiguraj da ti je userRepository injektovan u kontroler

        return ResponseEntity.ok("Dvofaktorska autentikacija uspešno isključena.");
    }

}