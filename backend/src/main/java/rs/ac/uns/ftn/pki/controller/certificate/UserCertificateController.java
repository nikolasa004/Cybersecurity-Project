package rs.ac.uns.ftn.pki.controller.certificate;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateListResponse;
import rs.ac.uns.ftn.pki.dto.certificate.RevokeCertificateRequest;
import rs.ac.uns.ftn.pki.service.CertificateService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class UserCertificateController {

    private final CertificateService certificateService;

    @GetMapping
    public ResponseEntity<List<CertificateListResponse>> getUserCertificates() {
        return ResponseEntity.ok(certificateService.getAllForUser());
    }

    @PostMapping("/revoke/{certificateId}")
    public ResponseEntity<Map<String, String>> revoke(@PathVariable Long certificateId, @Valid @RequestBody RevokeCertificateRequest request) {
        certificateService.revokeCertificateAsUser(certificateId, request);
        return ResponseEntity.ok(Map.of("message", "Certificate revoked successfully."));
    }

    @GetMapping("/download-pem/{id}")
    public ResponseEntity<Map<String, String>> downloadPem(@PathVariable Long id) {
        String pemContent = certificateService.getCertificateAsPem(id);
        Map<String, String> response = new java.util.HashMap<>();
        response.put("pemContent", pemContent);
        response.put("fileName", "certificate_" + id + ".pem");
        return ResponseEntity.ok(response);
    }
}