package rs.ac.uns.ftn.pki.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.dto.user.CaUserResponse;
import rs.ac.uns.ftn.pki.dto.user.CreateCaUserRequest;
import rs.ac.uns.ftn.pki.dto.user.SetPasswordRequest;
import rs.ac.uns.ftn.pki.exception.BadRequestException;
import rs.ac.uns.ftn.pki.exception.NotFoundException;
import rs.ac.uns.ftn.pki.mapper.CaUserMapper;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.model.enums.UserRole;
import rs.ac.uns.ftn.pki.repository.OrganizationRepository;
import rs.ac.uns.ftn.pki.repository.UserRepository;
import rs.ac.uns.ftn.pki.service.CaUserService;
import rs.ac.uns.ftn.pki.service.MailService;
import rs.ac.uns.ftn.pki.util.TokenGenerator;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaUserServiceImpl implements CaUserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MailService mailService;
    private final TokenGenerator tokenGenerator;
    private final CaUserMapper caUserMapper;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public CaUserResponse create(CreateCaUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already exists.");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found."));

        String token = tokenGenerator.generate();

        User user = caUserMapper.toEntity(request, organization, token);
        user.setActivationTokenExpiresAt(LocalDateTime.now().plusDays(1));

        User saved = userRepository.save(user);

        String activationLink = frontendUrl + "/activate?token=" + token;
        mailService.sendActivationEmail(saved.getEmail(), activationLink);

        return caUserMapper.toResponse(saved);
    }

    @Override
    public List<CaUserResponse> getAllCaUsers() {
        return userRepository.findAllByRoleOrderByIdDesc(UserRole.CA_USER)
                .stream()
                .map(caUserMapper::toResponse)
                .toList();
    }

    @Override
    public void activateAndSetPassword(SetPasswordRequest request) {
        User user = userRepository.findByActivationToken(request.getToken())
                .orElseThrow(() -> new NotFoundException("Invalid activation token."));

        if (user.getActivationTokenExpiresAt() == null ||
                user.getActivationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Activation token has expired.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setActivationToken(null);
        user.setActivationTokenExpiresAt(null);

        userRepository.save(user);
    }
}