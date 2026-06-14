package rs.ac.uns.ftn.pki.controller.certificateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateResponse;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CertificateRequestResponse;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CreateAutoCertificateRequest;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CreateCertificateRequestRequest;
import rs.ac.uns.ftn.pki.service.CertificateRequestService;
import rs.ac.uns.ftn.pki.service.CertificateService;

import java.util.List;

@RestController
@RequestMapping("/api/users/certificate-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasRole('USER')")
public class CertificateRequestController {

    private final CertificateRequestService certificateRequestService;
    private final CertificateService certificateService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificateRequestResponse> createRequest(@RequestPart("csrFile") MultipartFile csrFile, @RequestPart("request") CreateCertificateRequestRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(certificateRequestService.createRequest(csrFile,request));
    }

    @GetMapping
    public ResponseEntity<List<CertificateRequestResponse>> getMyRequests() {

        return ResponseEntity.ok(certificateRequestService.getMyRequests());
    }
}
