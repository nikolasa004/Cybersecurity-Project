package rs.ac.uns.ftn.pki.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CaUserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String organizationName;
    private boolean active;
}