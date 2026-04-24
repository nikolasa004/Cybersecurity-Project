package rs.ac.uns.ftn.pki.service;

import rs.ac.uns.ftn.pki.dto.certificate.CertificateListResponse;
import rs.ac.uns.ftn.pki.dto.certificate.IssueCertificateRequest;
import rs.ac.uns.ftn.pki.dto.certificate.IssuerOptionResponse;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;

import java.util.List;

public interface CertificateService {
    List<CertificateListResponse> getAllForAdmin();
    List<IssuerOptionResponse> getAvailableIssuers(CertificateType targetType);
    CertificateListResponse issueCertificate(IssueCertificateRequest request);
    List<IssuerOptionResponse> getAvailableIssuersForCa();
    CertificateListResponse issueCertificateAsCa(IssueCertificateRequest request);
}