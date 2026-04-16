package rs.ac.uns.ftn.pki.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.service.MailService;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendActivationEmail(String to, String activationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Activate your CA account");
        message.setText("""
                Your CA account has been created.

                Open the following link to set your password:
                %s
                """.formatted(activationLink));

        mailSender.send(message);
    }
}