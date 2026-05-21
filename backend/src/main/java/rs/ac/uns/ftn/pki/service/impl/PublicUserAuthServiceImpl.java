package rs.ac.uns.ftn.pki.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.dto.auth.*;
import rs.ac.uns.ftn.pki.exception.BadRequestException;
import rs.ac.uns.ftn.pki.exception.NotFoundException;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.model.enums.UserRole;
import rs.ac.uns.ftn.pki.repository.OrganizationRepository;
import rs.ac.uns.ftn.pki.repository.UserRepository;
import rs.ac.uns.ftn.pki.service.MailService;
import rs.ac.uns.ftn.pki.service.PublicUserAuthService;
import rs.ac.uns.ftn.pki.service.crypto.PublicKeyChallengeService;
import rs.ac.uns.ftn.pki.util.TokenGenerator;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PublicUserAuthServiceImpl implements PublicUserAuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MailService mailService;
    private final TokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final PublicKeyChallengeService publicKeyChallengeService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void register(RegisterUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email already exists.");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match.");
        }

        if (request.getPassword().length() < 15) {
            throw new BadRequestException("Password must have at least 15 characters.");
        }

        if (request.getPassword().length() > 64) {
            throw new BadRequestException("Password must not exceed 64 characters.");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found."));

        String token = tokenGenerator.generate();

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setActive(false);
        user.setOrganization(organization);
        user.setActivationToken(token);
        user.setActivationTokenExpiresAt(LocalDateTime.now().plusDays(1));

        userRepository.save(user);

        String activationLink = frontendUrl + "/activate-user?token=" + token;
        mailService.sendActivationEmail(user.getEmail(), activationLink);
    }

    @Override
    public ActivationChallengeResponse createActivationChallenge(ActivationChallengeRequest request) {
        User user = userRepository.findByActivationToken(request.getToken())
                .orElseThrow(() -> new NotFoundException("Invalid activation token."));

        if (user.getActivationTokenExpiresAt() == null ||
                user.getActivationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Activation token has expired.");
        }

        user.setPublicKey(request.getPublicKey());

        String challenge = publicKeyChallengeService.generateRandomChallenge();
        String encryptedChallenge = publicKeyChallengeService.encryptWithPublicKey(challenge, request.getPublicKey());

        user.setActivationChallenge(challenge);
        user.setActivationChallengeExpiresAt(LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);

        return new ActivationChallengeResponse(encryptedChallenge);
    }

    @Override
    public void completeActivation(ActivationCompleteRequest request) {
        User user = userRepository.findByActivationToken(request.getToken())
                .orElseThrow(() -> new NotFoundException("Invalid activation token."));

        if (user.getActivationChallengeExpiresAt() == null ||
                user.getActivationChallengeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Activation challenge has expired.");
        }

        if (user.getActivationChallenge() == null ||
                !user.getActivationChallenge().equals(request.getDecryptedChallenge())) {
            throw new BadRequestException("Invalid activation challenge response.");
        }

        user.setActive(true);
        user.setActivationToken(null);
        user.setActivationTokenExpiresAt(null);
        user.setActivationChallenge(null);
        user.setActivationChallengeExpiresAt(null);

        userRepository.save(user);
    }

    @Override
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = tokenGenerator.generate();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);

            String resetLink = frontendUrl + "/forgot-password?token=" + token;
            mailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });
    }

    @Override
    public ActivationChallengeResponse createPasswordResetChallenge(ForgotPasswordChallengeRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new NotFoundException("Invalid password reset token."));

        if (user.getPasswordResetTokenExpiresAt() == null ||
                user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired.");
        }

        if (user.getPublicKey() == null || user.getPublicKey().isBlank()) {
            throw new BadRequestException("No public key is associated with this account.");
        }

        String challenge = publicKeyChallengeService.generateRandomChallenge();
        String encryptedChallenge = publicKeyChallengeService.encryptWithPublicKey(challenge, user.getPublicKey());

        user.setPasswordResetChallenge(challenge);
        user.setPasswordResetChallengeExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        return new ActivationChallengeResponse(encryptedChallenge);
    }

    @Override
    public void completePasswordReset(ForgotPasswordCompleteRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new NotFoundException("Invalid password reset token."));

        if (user.getPasswordResetChallengeExpiresAt() == null ||
                user.getPasswordResetChallengeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset challenge has expired.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match.");
        }

        if (request.getNewPassword().length() < 15) {
            throw new BadRequestException("Password must have at least 15 characters.");
        }

        if (request.getNewPassword().length() > 64) {
            throw new BadRequestException("Password must not exceed 64 characters.");
        }

        if (user.getPasswordResetChallenge() == null ||
                !user.getPasswordResetChallenge().equals(request.getDecryptedChallenge())) {
            throw new BadRequestException("Invalid password reset challenge response.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        user.setPasswordResetChallenge(null);
        user.setPasswordResetChallengeExpiresAt(null);

        userRepository.save(user);
    }
}