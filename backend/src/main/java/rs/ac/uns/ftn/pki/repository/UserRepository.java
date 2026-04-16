package rs.ac.uns.ftn.pki.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.model.enums.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);
    List<User> findAllByRoleOrderByIdDesc(UserRole role);
    Optional<User> findByActivationToken(String activationToken);
}