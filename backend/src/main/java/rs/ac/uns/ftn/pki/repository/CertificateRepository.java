package rs.ac.uns.ftn.pki.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.ac.uns.ftn.pki.model.entity.Certificate;
import rs.ac.uns.ftn.pki.model.enums.CertificateStatus;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    @EntityGraph(attributePaths = {"issuerCertificate", "organization"})
    List<Certificate> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"issuerCertificate", "organization"})
    List<Certificate> findAllByTypeInAndStatusAndValidToAfterAndCaTrueAndKeyCertSignTrueOrderByCreatedAtDesc(List<CertificateType> types, CertificateStatus status, LocalDateTime now);
}