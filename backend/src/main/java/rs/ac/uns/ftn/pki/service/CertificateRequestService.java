package rs.ac.uns.ftn.pki.service;

import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateResponse;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CertificateRequestResponse;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CreateAutoCertificateRequest;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CreateCertificateRequestRequest;

import java.util.List;

public interface CertificateRequestService {

    CertificateRequestResponse createRequest(
            MultipartFile csrFile,
            CreateCertificateRequestRequest request
    );

    List<CertificateRequestResponse> getMyRequests();

    List<CertificateRequestResponse> getPendingRequests();

    CertificateResponse approveRequest(Long requestId);

    void rejectRequest(
            Long requestId,
            String rejectionReason
    );


}