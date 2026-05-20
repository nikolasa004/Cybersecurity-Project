package rs.ac.uns.ftn.pki.service.impl;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.dto.Mfa.MfaSetupResponse;
import rs.ac.uns.ftn.pki.service.MfaService;

@Service
public class MfaServiceImpl implements MfaService {

    @Override
    public String generateSecretKey() {

        SecretGenerator secretGenerator =
                new DefaultSecretGenerator();

        return secretGenerator.generate();
    }

    @Override
    public MfaSetupResponse generateMfaSetup(
            String secretKey,
            String email
    ) {

        String issuer = "PKI-App";

        QrData data = new QrData.Builder()
                .label(email)
                .secret(secretKey)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrCodeUrl = data.getUri();
        return new MfaSetupResponse(secretKey, qrCodeUrl);
    }

    @Override
    public boolean verifyCode(
            String secretKey,
            String code
    ) {

        if (secretKey == null || code == null) {
            return false;
        }

        DefaultCodeGenerator codeGenerator =
                new DefaultCodeGenerator(
                        HashingAlgorithm.SHA1
                );

        DefaultCodeVerifier verifier =
                new DefaultCodeVerifier(
                        codeGenerator,
                        new SystemTimeProvider()
                );

        return verifier.isValidCode(
                secretKey,
                code.trim()
        );
    }
}