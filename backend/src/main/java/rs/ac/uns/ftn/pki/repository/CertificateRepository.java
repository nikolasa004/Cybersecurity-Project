package rs.ac.uns.ftn.pki.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rs.ac.uns.ftn.pki.model.entity.Certificate;
import rs.ac.uns.ftn.pki.model.entity.Organization;
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

    @Query("SELECT c FROM Certificate c WHERE (c.organization = :organization OR c.type = 'ROOT') " +
            "AND c.status = 'ACTIVE' " +
            "AND c.validTo > :now " +
            "AND c.ca = true " +
            "AND c.keyCertSign = true " +
            "ORDER BY c.type DESC, c.createdAt DESC")
    List<Certificate> findAvailableIssuersForCa(
            @Param("organization") Organization organization,
            @Param("now") LocalDateTime now
    );
}