package rs.ac.uns.ftn.pki.controller.certificate;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateListResponse;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateResponse;
import rs.ac.uns.ftn.pki.dto.certificate.IssueCertificateRequest;
import rs.ac.uns.ftn.pki.dto.certificate.IssuerOptionResponse;
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
    @GetMapping("/download-with-key/{certificateId}")
    public ResponseEntity<byte[]> downloadWithKey(@PathVariable Long certificateId) {
        byte[] pkcs12 = certificateService.downloadPkcs12WithKey(certificateId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=certificate_" + certificateId + ".p12")
                .header("Content-Type", "application/x-pkcs12")
                .body(pkcs12);
    }

    @GetMapping("/user/certificates")
    public List<CertificateListResponse> getUserCertificates() {
        return certificateService.getAllForUser();
    }

}