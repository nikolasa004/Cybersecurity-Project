package rs.ac.uns.ftn.pki.mapper;

import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.pki.dto.organization.CreateOrganizationRequest;
import rs.ac.uns.ftn.pki.dto.organization.OrganizationResponse;
import rs.ac.uns.ftn.pki.model.entity.Organization;

@Component
public class OrganizationMapper {

    public Organization toEntity(CreateOrganizationRequest request) {
        Organization organization = new Organization();
        organization.setName(request.getName());
        organization.setRegistrationNumber(request.getRegistrationNumber());
        organization.setAddress(request.getAddress());
        organization.setCity(request.getCity());
        organization.setCountry(request.getCountry());
        organization.setEmailDomain(request.getEmailDomain());
        return organization;
    }

    public OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getRegistrationNumber(),
                organization.getAddress(),
                organization.getCity(),
                organization.getCountry(),
                organization.getEmailDomain()
        );
    }
}