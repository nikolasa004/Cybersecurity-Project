package rs.ac.uns.ftn.pki.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.model.enums.UserRole;
import rs.ac.uns.ftn.pki.repository.CertificateRepository;
import rs.ac.uns.ftn.pki.repository.OrganizationRepository;
import rs.ac.uns.ftn.pki.repository.UserRepository;
import rs.ac.uns.ftn.pki.service.crypto.CertificateGeneratorService;
import rs.ac.uns.ftn.pki.service.crypto.CertificateKeyMaterialService;
import rs.ac.uns.ftn.pki.service.crypto.KeyPairGeneratorService;
import rs.ac.uns.ftn.pki.service.crypto.X500NameBuilderService;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    // Novi servisi potrebni za Root
    private final CertificateRepository certificateRepository;
    private final KeyPairGeneratorService keyPairGeneratorService;
    private final X500NameBuilderService x500NameBuilderService;
    private final CertificateGeneratorService certificateGeneratorService;
    private final CertificateKeyMaterialService certificateKeyMaterialService;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // Prosleđujemo sve potrebne podatke pomoćnoj metodi
        Organization ftn = createOrganization(
                "FTN Novi Sad", "12345678", "Trg Dositeja Obradovića 6",
                "Novi Sad", "Srbija", "ftn.uns.ac.rs"
        );

        Organization nextesy = createOrganization(
                "Nextesy Software", "87654321", "Industrijska zona",
                "Šabac", "Srbija", "nextesy.com"
        );

        createUsers(ftn, nextesy);
        generateSystemRoot();

        System.out.println(">> PKI Test data and System Root CA successfully initialized!");
    }

    private void generateSystemRoot() {
        // Kreiranje podataka za Subject
        rs.ac.uns.ftn.pki.model.entity.X500SubjectData subjectData = new rs.ac.uns.ftn.pki.model.entity.X500SubjectData();
        subjectData.setCommonName("PKI System Root CA");
        subjectData.setOrganization("PKI Root Authority");
        subjectData.setCountry("RS");
        subjectData.setEmail("admin@pki.com");

        org.bouncycastle.asn1.x500.X500Name x500Name = x500NameBuilderService.build(subjectData);
        java.security.KeyPair keyPair = keyPairGeneratorService.generateRsaKeyPair();
        java.math.BigInteger serialNumber = java.math.BigInteger.valueOf(System.currentTimeMillis());

        // Datumi (Root obično traje duže, npr. 10 godina)
        java.util.Date validFrom = new java.util.Date();
        java.util.Date validTo = new java.util.Date(System.currentTimeMillis() + (10L * 365 * 24 * 60 * 60 * 1000));

        // Generisanje X509 objekta (Self-signed: issuer name i ključ su isti kao subject)
        java.security.cert.X509Certificate x509 = certificateGeneratorService.generateCertificate(
                x500Name, keyPair.getPrivate(), x500Name, keyPair.getPublic(),
                serialNumber, validFrom, validTo,
                true,  // isCa
                true,  // keyCertSign
                true,  // digitalSignature
                false, // keyEncipherment
                "SHA256withRSA"
        );

        // Čuvanje privatnog ključa u privremenu mapu (KLJUČNI KORAK)
        certificateKeyMaterialService.rememberPrivateKey(serialNumber.toString(), keyPair.getPrivate());

        // Kreiranje i čuvanje entiteta
        rs.ac.uns.ftn.pki.model.entity.Certificate rootEntity = new rs.ac.uns.ftn.pki.model.entity.Certificate();
        rootEntity.setSerialNumber(serialNumber.toString());
        rootEntity.setSubjectData(subjectData);
        rootEntity.setType(rs.ac.uns.ftn.pki.model.enums.CertificateType.ROOT);
        rootEntity.setStatus(rs.ac.uns.ftn.pki.model.enums.CertificateStatus.ACTIVE);
        rootEntity.setValidFrom(java.time.LocalDateTime.now());
        rootEntity.setValidTo(java.time.LocalDateTime.now().plusYears(10));
        rootEntity.setCa(true);
        rootEntity.setKeyCertSign(true);
        rootEntity.setDigitalSignature(true);
        rootEntity.setEncodedCertificate(toPem(x509));
        rootEntity.setOrganization(null); // Root je sistemski

        certificateRepository.save(rootEntity);
    }

    private String toPem(java.security.cert.X509Certificate cert) {
        try {
            byte[] encoded = cert.getEncoded();
            String base64 = java.util.Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
            return "-----BEGIN CERTIFICATE-----\n" + base64 + "\n-----END CERTIFICATE-----";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Pomoćne metode za preglednost
    private Organization createOrganization(String name, String reg, String addr, String city, String country, String domain) {
        Organization org = new Organization();
        org.setName(name);
        org.setRegistrationNumber(reg);
        org.setAddress(addr);     // Rešava tvoj ERROR: null value in column "address"
        org.setCity(city);
        org.setCountry(country);
        org.setEmailDomain(domain);
        return organizationRepository.save(org);
    }

    private void createUsers(Organization ftn, Organization nextesy) {
        // Admin
        User admin = new User();
        admin.setFirstName("Sistem");
        admin.setLastName("Administrator");
        admin.setEmail("admin@pki.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);

        // CA FTN
        User caFtn = new User();
        caFtn.setFirstName("Nikola");
        caFtn.setLastName("Savic");
        caFtn.setEmail("causer1@pki.com");
        caFtn.setPasswordHash(passwordEncoder.encode("causer123"));
        caFtn.setRole(UserRole.CA_USER);
        caFtn.setActive(true);
        caFtn.setOrganization(ftn);
        userRepository.save(caFtn);

        // CA Nextesy
        User caNextesy = new User();
        caNextesy.setFirstName("Uros");
        caNextesy.setLastName("Milinovic");
        caNextesy.setEmail("causer2@pki.com");
        caNextesy.setPasswordHash(passwordEncoder.encode("causer123"));
        caNextesy.setRole(UserRole.CA_USER);
        caNextesy.setActive(true);
        caNextesy.setOrganization(nextesy);
        userRepository.save(caNextesy);
    }
}