package rs.ac.uns.ftn.pki.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.pki.model.entity.Certificate;
import rs.ac.uns.ftn.pki.model.entity.PrivateKeyEntity;

import java.util.Optional;

public interface PrivateKeyRepository extends JpaRepository<PrivateKeyEntity, Long> {
    Optional<PrivateKeyEntity> findByCertificate(Certificate certificate);
}