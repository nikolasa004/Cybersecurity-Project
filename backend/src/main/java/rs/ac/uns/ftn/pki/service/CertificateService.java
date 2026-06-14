package rs.ac.uns.ftn.pki.service;

import java.time.LocalDateTime;
import java.util.List;

import rs.ac.uns.ftn.pki.dto.certificate.*;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CertificateRequestResponse;
import rs.ac.uns.ftn.pki.dto.certificateRequest.CreateAutoCertificateRequest;
import rs.ac.uns.ftn.pki.dto.certificateRequest.ParsedCsrData;
import rs.ac.uns.ftn.pki.model.entity.Certificate;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;

public interface CertificateService {
    List<CertificateListResponse> getAllForAdmin();
    List<IssuerOptionResponse> getAvailableIssuers(CertificateType targetType);
    CertificateResponse issueCertificate(IssueCertificateRequest request);
    
    List<IssuerOptionResponse> getAvailableIssuersForCa();
    CertificateResponse issueCertificateAsCa(IssueCertificateRequest request);
    public List<CertificateListResponse> getAllForCa();
    
    List<CertificateListResponse> getAllForUser();
    public byte[] downloadPkcs12WithKey(Long certificateId, DownloadCertificateRequest request);
    String getCertificateAsPem(Long certificateId);
    
    void revokeCertificateAsAdmin(Long certificateId, RevokeCertificateRequest request);
    void revokeCertificateAsCa(Long certificateId, RevokeCertificateRequest request);
    void revokeCertificateAsUser(Long certificateId, RevokeCertificateRequest request);
    CrlResponse getCrl(String issuerSerialNumber);
    OcspStatusResponse getOcspStatus(String serialNumber);
    CertificateResponse issueCertificateFromCsr(ParsedCsrData csrData, Certificate issuerCertificate, LocalDateTime validFrom, LocalDateTime validTo);
    CertificateResponse autoGenerateCertificate(CreateAutoCertificateRequest request);
}