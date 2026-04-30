package rs.ac.uns.ftn.pki.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rs.ac.uns.ftn.pki.model.enums.UserRole;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private UserRole role;
    private Long organizationId;
    private String organizationName;
}