package rs.ac.uns.ftn.pki.controller.certificate;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import rs.ac.uns.ftn.pki.dto.certificate.*;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CreateAutoCertificateRequest;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;
import rs.ac.uns.ftn.pki.service.CertificateService;

@RestController
@RequestMapping("/api/admin/certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CertificateController {

    private final CertificateService certificateService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<CertificateListResponse>> getAll() {
        return ResponseEntity.ok(certificateService.getAllForAdmin());
    }

    @GetMapping("/issuers")
    public ResponseEntity<List<IssuerOptionResponse>> getAvailableIssuers(@RequestParam CertificateType targetType) {
        return ResponseEntity.ok(certificateService.getAvailableIssuers(targetType));
    }

    @PostMapping("/issue")
    public ResponseEntity<CertificateResponse> issue(@Valid @RequestBody IssueCertificateRequest request) {
        return ResponseEntity.status(201).body(certificateService.issueCertificate(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/revoke/{certificateId}")
    public ResponseEntity<Map<String, String>> revoke(@PathVariable Long certificateId, @Valid @RequestBody RevokeCertificateRequest request) {
        certificateService.revokeCertificateAsAdmin(certificateId, request);
        return ResponseEntity.ok(Map.of("message", "Certificate revoked successfully."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/download-with-key/{certificateId}")
    public ResponseEntity<byte[]> downloadWithKey(@PathVariable Long certificateId, @RequestBody DownloadCertificateRequest request) {
        byte[] pkcs12 = certificateService.downloadPkcs12WithKey(certificateId, request);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=certificate_" + certificateId + ".p12")
                .header("Content-Type", "application/x-pkcs12")
                .body(pkcs12);
    }

    @GetMapping("/user/certificates")
    public List<CertificateListResponse> getUserCertificates() {
        return certificateService.getAllForUser();
    }

    @GetMapping("/download-pem/{id}")
    public ResponseEntity<Map<String, String>> downloadPem(@PathVariable Long id) {
        String pemContent = certificateService.getCertificateAsPem(id);
        return ResponseEntity.ok(Map.of("pemContent", pemContent, "fileName", "certificate_" + id + ".pem"));
    }
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/autogenerate")
    public ResponseEntity<CertificateResponse> autoGenerateCertificate(
            @Valid @RequestBody CreateAutoCertificateRequest request
    ) {

        return ResponseEntity.ok(
                certificateService.autoGenerateCertificate(
                        request
                )
        );
    }
}