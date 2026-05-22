package rs.ac.uns.ftn.pki.service;

public interface MailService {
    void sendActivationEmail(String to, String activationLink, boolean isCa);
    void sendPasswordResetEmail(String to, String resetLink);
}