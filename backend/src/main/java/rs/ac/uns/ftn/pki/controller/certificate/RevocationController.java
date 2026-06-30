package rs.ac.uns.ftn.pki.controller.certificate;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.pki.dto.certificate.CrlResponse;
import rs.ac.uns.ftn.pki.dto.certificate.OcspStatusResponse;
import rs.ac.uns.ftn.pki.service.CertificateService;

@RestController
@RequestMapping("/api/revocation")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class RevocationController {

    private final CertificateService certificateService;

    @GetMapping("/crl/{issuerSerialNumber}")
    public ResponseEntity<CrlResponse> getCrl(@PathVariable String issuerSerialNumber) {
        return ResponseEntity.ok(certificateService.getCrl(issuerSerialNumber));
    }

    @GetMapping("/ocsp/{serialNumber}")
    public ResponseEntity<OcspStatusResponse> getOcspStatus(@PathVariable String serialNumber) {
        return ResponseEntity.ok(certificateService.getOcspStatus(serialNumber));
    }
    
    @GetMapping(value = "/crl/{issuerSerialNumber}/download", produces = "application/pkix-crl")
    public ResponseEntity<byte[]> downloadCrl(@PathVariable String issuerSerialNumber) {
        byte[] crlBytes = certificateService.downloadStandardCrl(issuerSerialNumber);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"crl_" + issuerSerialNumber + ".crl\"")
                .header("Content-Type", "application/pkix-crl")
                .body(crlBytes);
    }
}