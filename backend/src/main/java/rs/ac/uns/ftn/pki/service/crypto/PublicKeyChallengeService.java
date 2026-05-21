package rs.ac.uns.ftn.pki.service.crypto;

public interface PublicKeyChallengeService {
    String generateRandomChallenge();
    String encryptWithPublicKey(String plainText, String publicKeyPem);
}