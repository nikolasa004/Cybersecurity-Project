package rs.ac.uns.ftn.pki.mapper;

import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.pki.dto.user.CaUserResponse;
import rs.ac.uns.ftn.pki.dto.user.CreateCaUserRequest;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.model.enums.UserRole;

@Component
public class CaUserMapper {

    public User toEntity(CreateCaUserRequest request, Organization organization, String activationToken) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setRole(UserRole.CA_USER);
        user.setActive(false);
        user.setOrganization(organization);
        user.setActivationToken(activationToken);
        return user;
    }

    public CaUserResponse toResponse(User user) {
        return new CaUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getOrganization().getName(),
                user.isActive()
        );
    }
}