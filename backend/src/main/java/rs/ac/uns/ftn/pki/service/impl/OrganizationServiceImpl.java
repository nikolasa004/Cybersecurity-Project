package rs.ac.uns.ftn.pki.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.dto.organization.CreateOrganizationRequest;
import rs.ac.uns.ftn.pki.dto.organization.OrganizationResponse;
import rs.ac.uns.ftn.pki.exception.BadRequestException;
import rs.ac.uns.ftn.pki.mapper.OrganizationMapper;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.repository.OrganizationRepository;
import rs.ac.uns.ftn.pki.service.OrganizationService;
import rs.ac.uns.ftn.pki.service.crypto.KeyEncryptionService;

import javax.crypto.SecretKey;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final KeyEncryptionService keyEncryptionService;

    @Override
    public OrganizationResponse create(CreateOrganizationRequest request) {
        if (organizationRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("Organization name already exists.");
        }

        if (organizationRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new BadRequestException("Registration number already exists.");
        }

        Organization organization = organizationMapper.toEntity(request);

        SecretKey orgKey = keyEncryptionService.generateOrgKey();

        byte[] encryptedOrgKey = keyEncryptionService.encrypt(
                orgKey.getEncoded(),
                keyEncryptionService.getMasterKey()
        );

        organization.setEncryptedOrgKey(encryptedOrgKey);
        Organization saved = organizationRepository.save(organization);

        return organizationMapper.toResponse(saved);
    }

    @Override
    public List<OrganizationResponse> getAll() {
        return organizationRepository.findAll()
                .stream()
                .map(organizationMapper::toResponse)
                .toList();
    }
}