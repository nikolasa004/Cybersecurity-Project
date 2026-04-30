package rs.ac.uns.ftn.pki.util;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.pki.model.entity.Certificate;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.model.entity.PrivateKeyEntity;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.model.entity.X500SubjectData;
import rs.ac.uns.ftn.pki.model.enums.CertificateStatus;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;
import rs.ac.uns.ftn.pki.model.enums.UserRole;
import rs.ac.uns.ftn.pki.repository.CertificateRepository;
import rs.ac.uns.ftn.pki.repository.OrganizationRepository;
import rs.ac.uns.ftn.pki.repository.PrivateKeyRepository;
import rs.ac.uns.ftn.pki.repository.UserRepository;
import rs.ac.uns.ftn.pki.service.crypto.CertificateGeneratorService;
import rs.ac.uns.ftn.pki.service.crypto.KeyEncryptionService;
import rs.ac.uns.ftn.pki.service.crypto.KeyPairGeneratorService;
import rs.ac.uns.ftn.pki.service.crypto.X500NameBuilderService;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final CertificateRepository certificateRepository;
    private final PrivateKeyRepository privateKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeyPairGeneratorService keyPairGeneratorService;
    private final X500NameBuilderService x500NameBuilderService;
    private final CertificateGeneratorService certificateGeneratorService;
    private final KeyEncryptionService keyEncryptionService;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        Organization systemOrganization = createOrganization(
                "PKI System",
                "00000000",
                "Bulevar oslobodjenja 1",
                "Novi Sad",
                "Srbija",
                "pki.com"
        );

        Organization ftn = createOrganization(
                "FTN Novi Sad",
                "12345678",
                "Trg Dositeja Obradovića 6",
                "Novi Sad",
                "Srbija",
                "ftn.uns.ac.rs"
        );

        Organization nextesy = createOrganization(
                "Nextesy Software",
                "87654321",
                "Industrijska zona",
                "Šabac",
                "Srbija",
                "nextesy.com"
        );

        createUsers(systemOrganization, ftn, nextesy);
        generateSystemRoot(systemOrganization);

        System.out.println(">> PKI test data and System Root CA successfully initialized.");
    }

    private Organization createOrganization(
            String name,
            String registrationNumber,
            String address,
            String city,
            String country,
            String emailDomain
    ) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setRegistrationNumber(registrationNumber);
        organization.setAddress(address);
        organization.setCity(city);
        organization.setCountry(country);
        organization.setEmailDomain(emailDomain);

        SecretKey orgKey = keyEncryptionService.generateOrgKey();
        byte[] encryptedOrgKey = keyEncryptionService.encrypt(
                orgKey.getEncoded(),
                keyEncryptionService.getMasterKey()
        );

        organization.setEncryptedOrgKey(encryptedOrgKey);

        return organizationRepository.save(organization);
    }

    private void createUsers(Organization systemOrganization, Organization ftn, Organization nextesy) {
        User admin = new User();
        admin.setFirstName("Sistem");
        admin.setLastName("Administrator");
        admin.setEmail("admin@pki.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        admin.setOrganization(systemOrganization);
        userRepository.save(admin);

        User caFtn = new User();
        caFtn.setFirstName("Nikola");
        caFtn.setLastName("Savic");
        caFtn.setEmail("causer1@pki.com");
        caFtn.setPasswordHash(passwordEncoder.encode("causer123"));
        caFtn.setRole(UserRole.CA_USER);
        caFtn.setActive(true);
        caFtn.setOrganization(ftn);
        userRepository.save(caFtn);

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

    private void generateSystemRoot(Organization systemOrganization) {
        X500SubjectData subjectData = new X500SubjectData();
        subjectData.setCommonName("PKI System Root CA");
        subjectData.setOrganization(systemOrganization.getName());
        subjectData.setCountry("RS");
        subjectData.setEmail("admin@pki.com");

        X500Name subjectX500Name = x500NameBuilderService.build(subjectData);
        KeyPair keyPair = keyPairGeneratorService.generateRsaKeyPair();
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis()).abs();

        Date validFrom = new Date();
        Date validTo = new Date(System.currentTimeMillis() + (10L * 365 * 24 * 60 * 60 * 1000));

        X509Certificate x509Certificate = certificateGeneratorService.generateCertificate(
                subjectX500Name,
                keyPair.getPrivate(),
                subjectX500Name,
                keyPair.getPublic(),
                serialNumber,
                validFrom,
                validTo,
                true,
                true,
                true,
                false,
                "Alternative Names",
                "SHA256withRSA"
        );

        Certificate rootCertificate = new Certificate();
        rootCertificate.setSerialNumber(serialNumber.toString());
        rootCertificate.setSelfSigned(true);
        rootCertificate.setSubjectData(subjectData);
        rootCertificate.setType(CertificateType.ROOT);
        rootCertificate.setStatus(CertificateStatus.ACTIVE);
        rootCertificate.setValidFrom(LocalDateTime.now());
        rootCertificate.setValidTo(LocalDateTime.now().plusYears(10));
        rootCertificate.setCa(true);
        rootCertificate.setKeyCertSign(true);
        rootCertificate.setDigitalSignature(true);
        rootCertificate.setKeyEncipherment(false);
        rootCertificate.setEncodedCertificate(toPem(x509Certificate));
        rootCertificate.setOrganization(systemOrganization);
        rootCertificate.setIssuerCertificate(null);

        Certificate savedRoot = certificateRepository.save(rootCertificate);

        byte[] orgKeyBytes = keyEncryptionService.decrypt(
                systemOrganization.getEncryptedOrgKey(),
                keyEncryptionService.getMasterKey()
        );

        SecretKey orgKey = new SecretKeySpec(orgKeyBytes, "AES");

        byte[] encryptedPrivateKey = keyEncryptionService.encrypt(
                keyPair.getPrivate().getEncoded(),
                orgKey
        );

        PrivateKeyEntity privateKeyEntity = new PrivateKeyEntity();
        privateKeyEntity.setCertificate(savedRoot);
        privateKeyEntity.setEncryptedPrivateKey(encryptedPrivateKey);
        privateKeyEntity.setOrganization(systemOrganization);

        privateKeyRepository.save(privateKeyEntity);
    }

    private String toPem(X509Certificate certificate) {
        try {
            String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                    .encodeToString(certificate.getEncoded());

            return "-----BEGIN CERTIFICATE-----\n"
                    + base64
                    + "\n-----END CERTIFICATE-----";
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode certificate to PEM.", e);
        }
    }
}