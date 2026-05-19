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
import rs.ac.uns.ftn.pki.service.CertificateService;

@RestController
@RequestMapping("/api/ca/certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('CA_USER')")
public class CaCertificateController {

    private final CertificateService certificateService;

    @GetMapping
    public ResponseEntity<List<CertificateListResponse>> getAll() {
        return ResponseEntity.ok(certificateService.getAllForCa());
    }

    @GetMapping("/issuers")
    public ResponseEntity<List<IssuerOptionResponse>> getAvailableIssuers() {
        return ResponseEntity.ok(certificateService.getAvailableIssuersForCa());
    }

    @PostMapping("/issue")
    public ResponseEntity<CertificateResponse> issue(@Valid @RequestBody IssueCertificateRequest request) {
        return ResponseEntity.status(201).body(certificateService.issueCertificateAsCa(request));
    }

    @GetMapping("/download-with-key/{certificateId}")
    public ResponseEntity<byte[]> downloadWithKey(@PathVariable Long certificateId) {
        byte[] pkcs12 = certificateService.downloadPkcs12WithKey(certificateId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=certificate_" + certificateId + ".p12")
                .header("Content-Type", "application/x-pkcs12")
                .body(pkcs12);
    }
}