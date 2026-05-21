package rs.ac.uns.ftn.pki.service;

import rs.ac.uns.ftn.pki.dto.Mfa.MfaSetupResponse;;

public interface MfaService {

    String generateSecretKey();
    MfaSetupResponse generateMfaSetup(String secretKey, String email);
    boolean verifyCode(String secretKey, String code);
}