package rs.ac.uns.ftn.pki.dto.organization;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrganizationResponse {
    private Long id;
    private String name;
    private String registrationNumber;
    private String address;
    private String city;
    private String country;
    private String emailDomain;
}