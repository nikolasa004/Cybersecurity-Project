package rs.ac.uns.ftn.pki.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.model.enums.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByRoleOrderByIdDesc(UserRole role);

    @Query("""
        select u
        from User u
        join fetch u.organization
        where u.role = :role
        order by u.id desc
    """)
    List<User> findAllByRoleWithOrganization(@Param("role") UserRole role);

    Optional<User> findByActivationToken(String activationToken);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.organization WHERE u.email = :email")
    Optional<User> findByEmailWithOrganization(@Param("email") String email);
}