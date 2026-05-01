package rs.ac.uns.ftn.pki.controller.certificate;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateListResponse;
import rs.ac.uns.ftn.pki.service.CertificateService;

import java.util.List;

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

}