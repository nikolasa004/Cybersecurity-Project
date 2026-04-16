package rs.ac.uns.ftn.pki.dto.organization;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrganizationRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String registrationNumber;

    @NotBlank
    private String address;

    @NotBlank
    private String city;

    @NotBlank
    private String country;

    @NotBlank
    private String emailDomain;
}