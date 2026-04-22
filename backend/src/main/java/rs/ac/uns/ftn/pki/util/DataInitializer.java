package rs.ac.uns.ftn.pki.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.model.enums.UserRole;
import rs.ac.uns.ftn.pki.repository.OrganizationRepository;
import rs.ac.uns.ftn.pki.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
            Organization ftn = new Organization();
            ftn.setName("FTN");
            ftn.setRegistrationNumber("12345678");
            ftn.setAddress("Trg Dositeja Obradovića 6");
            ftn.setCity("Novi Sad");
            ftn.setCountry("Srbija");
            ftn.setEmailDomain("ftn.uns.ac.rs");
            organizationRepository.save(ftn);

            User admin = new User();
            admin.setFirstName("Branislav");
            admin.setLastName("Markovic");
            admin.setEmail("admin@pki.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(UserRole.CA_USER);
            admin.setActive(true);
            admin.setOrganization(ftn);

            userRepository.save(admin);

            System.out.println(">> Test data initialized successfully!");
    }
}