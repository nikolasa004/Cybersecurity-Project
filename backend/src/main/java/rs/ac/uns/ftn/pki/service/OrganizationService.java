package rs.ac.uns.ftn.pki.service;

import rs.ac.uns.ftn.pki.dto.organization.CreateOrganizationRequest;
import rs.ac.uns.ftn.pki.dto.organization.OrganizationResponse;

import java.util.List;

public interface OrganizationService {
    OrganizationResponse create(CreateOrganizationRequest request);
    List<OrganizationResponse> getAll();
}