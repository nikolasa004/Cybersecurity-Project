package rs.ac.uns.ftn.pki.controller.certificate;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateListResponse;
import rs.ac.uns.ftn.pki.dto.certificate.IssuerOptionResponse;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;
import rs.ac.uns.ftn.pki.service.CertificateService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping
    public ResponseEntity<List<CertificateListResponse>> getAll() {
        return ResponseEntity.ok(certificateService.getAllForAdmin());
    }

    @GetMapping("/issuers")
    public ResponseEntity<List<IssuerOptionResponse>> getAvailableIssuers(@RequestParam CertificateType targetType) {
        return ResponseEntity.ok(certificateService.getAvailableIssuers(targetType));
    }
}