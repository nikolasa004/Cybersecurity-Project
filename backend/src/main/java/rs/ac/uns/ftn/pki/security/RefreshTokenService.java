package rs.ac.uns.ftn.pki.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.pki.exception.NotFoundException;
import rs.ac.uns.ftn.pki.exception.SecurityAlertException;
import rs.ac.uns.ftn.pki.exception.TokenExpiredException;
import rs.ac.uns.ftn.pki.model.entity.RefreshToken;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.repository.RefreshTokenRepository;
import rs.ac.uns.ftn.pki.repository.UserRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${pki.jwt.refresh-expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Refresh token is not in database!"));
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .used(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(noRollbackFor = SecurityAlertException.class)
    public String rotateToken(String oldToken) {
        RefreshToken token = refreshTokenRepository.findByToken(oldToken)
                .orElseThrow(() -> new NotFoundException("Token not found!"));

        if (token.isUsed()) {
            refreshTokenRepository.deleteByUser(token.getUser());
            throw new SecurityAlertException("SECURITY ALERT: Compromised token used. All sessions revoked.");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenExpiredException("Refresh token expired.");
        }

        token.setUsed(true);
        refreshTokenRepository.save(token);

        return createRefreshToken(token.getUser().getId()).getToken();
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUser(userRepository.findById(userId).get());
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }
}