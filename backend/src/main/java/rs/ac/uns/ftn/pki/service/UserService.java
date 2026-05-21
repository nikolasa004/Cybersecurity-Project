package rs.ac.uns.ftn.pki.service;

import rs.ac.uns.ftn.pki.dto.Mfa.MfaSetupResponse;
import rs.ac.uns.ftn.pki.dto.user.UserProfileResponse;
import rs.ac.uns.ftn.pki.model.entity.User;

public interface UserService {
    UserProfileResponse getCurrentUserProfile();
    MfaSetupResponse generateMfaSetup(User user);
    boolean enableMfa(User user, String code);
}