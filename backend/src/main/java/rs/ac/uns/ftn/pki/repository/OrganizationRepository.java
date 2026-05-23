package rs.ac.uns.ftn.pki.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.pki.model.entity.Organization;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByRegistrationNumber(String registrationNumber);
    Optional<Organization> findByNameIgnoreCase(String name);
    List<Organization> findByMasterKeyVersion(String masterKeyVersion);
}