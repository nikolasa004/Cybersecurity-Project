package rs.ac.uns.ftn.pki.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.pki.model.entity.MasterKeyEntity;
import rs.ac.uns.ftn.pki.model.enums.MasterKeyStatus;

import java.util.List;
import java.util.Optional;

public interface MasterKeyRepository extends JpaRepository<MasterKeyEntity, String> {


    Optional<MasterKeyEntity> findFirstByStatusOrderByCreatedAtDesc(MasterKeyStatus status);


    List<MasterKeyEntity> findByStatusIn(List<MasterKeyStatus> statuses);
}