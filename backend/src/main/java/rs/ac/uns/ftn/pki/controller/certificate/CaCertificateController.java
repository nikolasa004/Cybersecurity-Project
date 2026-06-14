package rs.ac.uns.ftn.pki.controller.certificate;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import rs.ac.uns.ftn.pki.dto.certificate.*;
import rs.ac.uns.ftn.pki.service.CertificateService;

@RestController
@RequestMapping("/api/ca/certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CaCertificateController {

    private final CertificateService certificateService;
    @PreAuthorize("hasRole('CA_USER')")
    @GetMapping
    public ResponseEntity<List<CertificateListResponse>> getAll() {
        return ResponseEntity.ok(certificateService.getAllForCa());
    }
    @PreAuthorize("hasAnyRole('CA_USER','USER')")
    @GetMapping("/issuers")
    public ResponseEntity<List<IssuerOptionResponse>> getAvailableIssuers() {
        return ResponseEntity.ok(certificateService.getAvailableIssuersForCa());
    }
    @PreAuthorize("hasRole('CA_USER')")
    @PostMapping("/issue")
    public ResponseEntity<CertificateResponse> issue(@Valid @RequestBody IssueCertificateRequest request) {
        return ResponseEntity.status(201).body(certificateService.issueCertificateAsCa(request));
    }
    @PreAuthorize("hasRole('CA_USER')")
    @PostMapping("/revoke/{certificateId}")
    public ResponseEntity<Map<String, String>> revoke(@PathVariable Long certificateId, @Valid @RequestBody RevokeCertificateRequest request) {
        certificateService.revokeCertificateAsCa(certificateId, request);
        return ResponseEntity.ok(Map.of("message", "Certificate revoked successfully."));
    }
    @PreAuthorize("hasRole('CA_USER')")
    @PostMapping("/download-with-key/{certificateId}")
    public ResponseEntity<byte[]> downloadWithKey(@PathVariable Long certificateId, @RequestBody DownloadCertificateRequest request) {
        byte[] pkcs12 = certificateService.downloadPkcs12WithKey(certificateId, request);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=certificate_" + certificateId + ".p12")
                .header("Content-Type", "application/x-pkcs12")
                .body(pkcs12);
    }
    @PreAuthorize("hasRole('CA_USER')")
    @GetMapping("/download-pem/{id}")
    public ResponseEntity<Map<String, String>> downloadPem(@PathVariable Long id) {
        String pemContent = certificateService.getCertificateAsPem(id);
        return ResponseEntity.ok(Map.of("pemContent", pemContent, "fileName", "certificate_" + id + ".pem"));
    }
}