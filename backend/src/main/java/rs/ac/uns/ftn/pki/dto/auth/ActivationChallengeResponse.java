package rs.ac.uns.ftn.pki.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ActivationChallengeResponse {
    private String encryptedChallenge;
}