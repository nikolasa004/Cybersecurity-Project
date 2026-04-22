package rs.ac.uns.ftn.pki.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }


        Organization ftn = new Organization();
        ftn.setName("FTN Novi Sad");
        ftn.setRegistrationNumber("12345678");
        ftn.setAddress("Trg Dositeja Obradovića 6");
        ftn.setCity("Novi Sad");
        ftn.setCountry("Srbija");
        ftn.setEmailDomain("ftn.uns.ac.rs");
        organizationRepository.save(ftn);

        Organization nextesy = new Organization();
        nextesy.setName("Nextesy Software");
        nextesy.setRegistrationNumber("87654321");
        nextesy.setAddress("Industrijska zona");
        nextesy.setCity("Šabac");
        nextesy.setCountry("Srbija");
        nextesy.setEmailDomain("nextesy.com");
        organizationRepository.save(nextesy);


        User admin = new User();
        admin.setFirstName("Sistem");
        admin.setLastName("Administrator");
        admin.setEmail("admin@pki.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        // Admin obično ne mora imati organizaciju, ili je u "Sistemskoj"
        userRepository.save(admin);

        User caFtn = new User();
        caFtn.setFirstName("Nikola");
        caFtn.setLastName("Savic");
        caFtn.setEmail("causer1@ftn.com");
        caFtn.setPasswordHash(passwordEncoder.encode("causer123"));
        caFtn.setRole(UserRole.CA_USER);
        caFtn.setActive(true);
        caFtn.setOrganization(ftn);
        userRepository.save(caFtn);

        User userFtn = new User();
        userFtn.setFirstName("Lazar");
        userFtn.setLastName("Vilotoc");
        userFtn.setEmail("user1@ftn.com");
        userFtn.setPasswordHash(passwordEncoder.encode("user123"));
        userFtn.setRole(UserRole.USER);
        userFtn.setActive(true);
        userFtn.setOrganization(ftn);
        userRepository.save(userFtn);

        User caNextesy = new User();
        caNextesy.setFirstName("Uros");
        caNextesy.setLastName("Milinovic");
        caNextesy.setEmail("causer2@nextesy.com");
        caNextesy.setPasswordHash(passwordEncoder.encode("causer123"));
        caNextesy.setRole(UserRole.CA_USER);
        caNextesy.setActive(true);
        caNextesy.setOrganization(nextesy);
        userRepository.save(caNextesy);

        System.out.println(">> PKI Test podaci uspešno ubačeni!");
    }
}