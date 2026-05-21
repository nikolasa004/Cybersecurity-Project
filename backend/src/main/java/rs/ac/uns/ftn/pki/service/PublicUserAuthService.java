package rs.ac.uns.ftn.pki.service;

import rs.ac.uns.ftn.pki.dto.auth.*;

public interface PublicUserAuthService {
    void register(RegisterUserRequest request);
    ActivationChallengeResponse createActivationChallenge(ActivationChallengeRequest request);
    void completeActivation(ActivationCompleteRequest request);
    void requestPasswordReset(ForgotPasswordRequest request);
    ActivationChallengeResponse createPasswordResetChallenge(ForgotPasswordChallengeRequest request);
    void completePasswordReset(ForgotPasswordCompleteRequest request);
}