package rs.ac.uns.ftn.pki.service;

import java.util.List;

import rs.ac.uns.ftn.pki.dto.certificate.CertificateListResponse;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateResponse;
import rs.ac.uns.ftn.pki.dto.certificate.IssueCertificateRequest;
import rs.ac.uns.ftn.pki.dto.certificate.IssuerOptionResponse;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;

public interface CertificateService {
    List<CertificateListResponse> getAllForAdmin();
    List<IssuerOptionResponse> getAvailableIssuers(CertificateType targetType);
    CertificateResponse issueCertificate(IssueCertificateRequest request);
    List<IssuerOptionResponse> getAvailableIssuersForCa();
    CertificateResponse issueCertificateAsCa(IssueCertificateRequest request);
    public List<CertificateListResponse> getAllForCa();
    List<CertificateListResponse> getAllForUser();
    public byte[] downloadPkcs12WithKey(Long certificateId);
}