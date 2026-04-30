package rs.ac.uns.ftn.pki.mapper;

import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.pki.dto.user.UserProfileResponse;
import rs.ac.uns.ftn.pki.model.entity.User;

@Component
public class UserMapper {

    public UserProfileResponse toProfileResponse(User user) {
        String fullName = user.getFirstName() + " " + user.getLastName();

        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                fullName,
                user.getEmail(),
                user.getRole(),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.getOrganization() != null ? user.getOrganization().getName() : null
        );
    }
}