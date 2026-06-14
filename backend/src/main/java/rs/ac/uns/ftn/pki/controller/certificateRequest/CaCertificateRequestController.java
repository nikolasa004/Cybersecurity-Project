package rs.ac.uns.ftn.pki.controller.certificateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateResponse;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CertificateRequestResponse;
import rs.ac.uns.ftn.pki.dto.certificateRequest.RejectCertificateRequest;
import rs.ac.uns.ftn.pki.service.CertificateRequestService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ca/certificate-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('CA_USER')")
public class CaCertificateRequestController {

    private final CertificateRequestService certificateRequestService;

    @GetMapping("/pending")
    public ResponseEntity<List<CertificateRequestResponse>>
    getPendingRequests() {

        return ResponseEntity.ok(
                certificateRequestService.getPendingRequests()
        );
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<CertificateResponse>
    approveRequest(
            @PathVariable Long requestId
    ) {

        return ResponseEntity.ok(
                certificateRequestService.approveRequest(
                        requestId
                )
        );
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<Map<String, String>>
    rejectRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody RejectCertificateRequest request
    ) {

        certificateRequestService.rejectRequest(
                requestId,
                request.getRejectionReason()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Certificate request rejected."
                )
        );
    }
}