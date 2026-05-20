package rs.ac.uns.ftn.pki.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.pki.dto.Mfa.MfaSetupResponse;
import rs.ac.uns.ftn.pki.dto.user.UserProfileResponse;
import rs.ac.uns.ftn.pki.exception.NotFoundException;
import rs.ac.uns.ftn.pki.mapper.UserMapper;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.repository.UserRepository;
import rs.ac.uns.ftn.pki.service.MfaService;
import rs.ac.uns.ftn.pki.service.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final MfaService mfaService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new NotFoundException("Authenticated user not found.");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmailWithOrganization(email)
                .orElseThrow(() -> new NotFoundException("User not found."));

        return userMapper.toProfileResponse(user);
    }


    @Override
    public MfaSetupResponse generateMfaSetup(User user) {
        String secretKey = mfaService.generateSecretKey();

        user.setTwoFactorSecret(secretKey);
        userRepository.save(user);

        return mfaService.generateMfaSetup(secretKey, user.getEmail());
    }

    @Override
    public boolean enableMfa(User user, String code) {
        boolean isCodeValid = mfaService.verifyCode(user.getTwoFactorSecret(), code);

        if (isCodeValid) {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);
            return true;
        }

        return false;
    }}