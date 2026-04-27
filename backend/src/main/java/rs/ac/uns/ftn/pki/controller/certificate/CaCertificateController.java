package rs.ac.uns.ftn.pki.controller.certificate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateListResponse;
import rs.ac.uns.ftn.pki.dto.certificate.IssueCertificateRequest;
import rs.ac.uns.ftn.pki.dto.certificate.IssuerOptionResponse;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;
import rs.ac.uns.ftn.pki.service.CertificateService;

import java.util.List;

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
    public ResponseEntity<CertificateListResponse> issue(@Valid @RequestBody IssueCertificateRequest request) {
        return ResponseEntity.status(201).body(certificateService.issueCertificateAsCa(request));
    }
}