package rs.ac.uns.ftn.pki.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateResponse;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CertificateRequestResponse;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CreateAutoCertificateRequest;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CreateCertificateRequestRequest;
import rs.ac.uns.ftn.pki.dto.certificateRequest.ParsedCsrData;
import rs.ac.uns.ftn.pki.exception.NotFoundException;
import rs.ac.uns.ftn.pki.model.entity.*;
import rs.ac.uns.ftn.pki.model.enums.CertificateRequestStatus;
import rs.ac.uns.ftn.pki.repository.CertificateRepository;
import rs.ac.uns.ftn.pki.repository.CertificateRequestRepository;
import rs.ac.uns.ftn.pki.repository.UserRepository;
import rs.ac.uns.ftn.pki.service.CertificateRequestService;
import rs.ac.uns.ftn.pki.service.UserService;
import rs.ac.uns.ftn.pki.service.crypto.CsrGeneratorService;
import rs.ac.uns.ftn.pki.service.crypto.CsrParserServiceImpl;
import rs.ac.uns.ftn.pki.service.crypto.KeyEncryptionService;
import rs.ac.uns.ftn.pki.service.crypto.KeyPairGeneratorService;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateRequestServiceImpl implements CertificateRequestService {

    private final CertificateRequestRepository certificateRequestRepository;
    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final CsrParserServiceImpl csrParserServiceImpl;
    private final CertificateServiceImpl certificateService;
    private final KeyPairGeneratorService keyPairGeneratorService;
    private final CsrGeneratorService csrGeneratorService;
    private final KeyEncryptionService keyEncryptionService;
    @Transactional
    @Override
    public CertificateRequestResponse createRequest(MultipartFile csrFile,CreateCertificateRequestRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User requester = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found."));
        Certificate issuerCertificate = certificateRepository.findWithIssuerCertificateById(request.getIssuerCertificateId()).orElseThrow(() -> new NotFoundException("Issuer certificate not found."));
        if (!issuerCertificate.isCa()) {
            throw new IllegalArgumentException("Selected certificate cannot act as issuer.");
        }

        if (request.getValidTo().isBefore(request.getValidFrom())) {
            throw new IllegalArgumentException("validTo must be after validFrom.");
        }

        String csrPem;

        try {
            csrPem = new String(csrFile.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSR file.", e);
        }

        csrParserServiceImpl.parse(csrPem);
        CertificateRequest certificateRequest = new CertificateRequest();
        certificateRequest.setRequester(requester);
        certificateRequest.setIssuerCertificate(issuerCertificate);
        certificateRequest.setOrganization(requester.getOrganization());
        certificateRequest.setCsrPem(csrPem);
        certificateRequest.setStatus(CertificateRequestStatus.PENDING);
        certificateRequest.setValidFrom(request.getValidFrom());
        certificateRequest.setValidTo(request.getValidTo());
        certificateRequest = certificateRequestRepository.save(certificateRequest);
        return mapToResponse(certificateRequest);
    }
    @Transactional
    @Override
    public List<CertificateRequestResponse> getMyRequests() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User requester = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found."));

        return certificateRequestRepository
                .findByRequesterId(requester.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Transactional
    @Override
    public List<CertificateRequestResponse> getPendingRequests() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found."));

        return certificateRequestRepository
                .findByOrganizationAndStatus(
                        currentUser.getOrganization(),
                        CertificateRequestStatus.PENDING
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Transactional
    @Override
    public CertificateResponse approveRequest(Long requestId) {

        CertificateRequest request =
                certificateRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Certificate request not found."
                                ));



        if (request.getStatus() != CertificateRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending requests can be approved."
            );
        }

        ParsedCsrData csrData =
                csrParserServiceImpl.parse(
                        request.getCsrPem()
                );

        CertificateResponse response =
                certificateService.issueCertificateFromCsr(
                        csrData,
                        request.getIssuerCertificate(),
                        request.getValidFrom(),
                        request.getValidTo()
                );

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        User caUser =
                userRepository.findByEmail(
                        authentication.getName()
                ).orElseThrow(() ->
                        new NotFoundException(
                                "CA user not found."
                        ));

        request.setStatus(
                CertificateRequestStatus.APPROVED
        );

        if (!request.getOrganization().getId().equals(
                caUser.getOrganization().getId()
        )) {
            throw new IllegalStateException(
                    "You cannot process requests from another organization."
            );
        }

        request.setProcessedBy(caUser);
        request.setProcessedAt(LocalDateTime.now());

        certificateRequestRepository.save(request);

        return response;
    }

    @Override
    public void rejectRequest(Long requestId, String rejectionReason) {
        CertificateRequest request = certificateRequestRepository.findById(requestId).orElseThrow(() -> new NotFoundException("Certificate request not found."));

        if (request.getStatus() != CertificateRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be rejected.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User caUser = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("CA user not found."));
        request.setStatus(CertificateRequestStatus.REJECTED);
        request.setProcessedBy(caUser);
        request.setProcessedAt(LocalDateTime.now());
        request.setRejectionReason(rejectionReason);

        certificateRequestRepository.save(request);
    }

    private CertificateRequestResponse mapToResponse(CertificateRequest request) {
        CertificateRequestResponse response = new CertificateRequestResponse();
        response.setId(request.getId());
        response.setRequesterEmail(request.getRequester().getEmail());
        response.setIssuerCommonName(request.getIssuerCertificate().getSubjectData().getCommonName());
        response.setStatus(request.getStatus());
        response.setValidFrom(request.getValidFrom());
        response.setValidTo(request.getValidTo());
        response.setCreatedAt(request.getCreatedAt());
        response.setProcessedAt(request.getProcessedAt());

        return response;
    }

}