package rs.ac.uns.ftn.pki.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.pki.model.entity.CertificateRequest;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.model.enums.CertificateRequestStatus;

import java.util.List;

public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, Long> {

    List<CertificateRequest> findByStatus(CertificateRequestStatus status);
    List<CertificateRequest> findByRequesterId(Long requesterId);
    List<CertificateRequest> findByIssuerCertificateId(Long issuerCertificateId);
    List<CertificateRequest> findByOrganizationAndStatus(Organization organization, CertificateRequestStatus status);
}