package rs.ac.uns.ftn.pki.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.pki.dto.user.CaUserResponse;
import rs.ac.uns.ftn.pki.dto.user.CreateCaUserRequest;
import rs.ac.uns.ftn.pki.dto.user.SetPasswordRequest;
import rs.ac.uns.ftn.pki.service.CaUserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ca-users")
@RequiredArgsConstructor
public class CaUserController {

    private final CaUserService caUserService;

    @PostMapping
    public ResponseEntity<CaUserResponse> create(@Valid @RequestBody CreateCaUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caUserService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CaUserResponse>> getAll() {
        return ResponseEntity.ok(caUserService.getAllCaUsers());
    }

    @PostMapping("/activate")
    public ResponseEntity<Map<String, String>> activate(@Valid @RequestBody SetPasswordRequest request) {
        caUserService.activateAndSetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password set successfully."));
    }
}