package rs.ac.uns.ftn.pki.util;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.public-base-url:https://localhost:8443}")
    private String publicBaseUrl;

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
        generateEndEntityCertificates();

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
                keyEncryptionService.getCurrentActiveMasterKey()
        );

        organization.setEncryptedOrgKey(encryptedOrgKey);
        organization.setMasterKeyVersion(keyEncryptionService.getCurrentActiveVersion());

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
        admin.setTwoFactorEnabled(false);
        userRepository.save(admin);

        User caFtn = new User();
        caFtn.setFirstName("Nikola");
        caFtn.setLastName("Savic");
        caFtn.setEmail("causer1@pki.com");
        caFtn.setPasswordHash(passwordEncoder.encode("causer123"));
        caFtn.setRole(UserRole.CA_USER);
        caFtn.setActive(true);
        caFtn.setOrganization(ftn);
        caFtn.setTwoFactorEnabled(false);
        userRepository.save(caFtn);

        User caNextesy = new User();
        caNextesy.setFirstName("Uros");
        caNextesy.setLastName("Milinovic");
        caNextesy.setEmail("causer2@pki.com");
        caNextesy.setPasswordHash(passwordEncoder.encode("causer123"));
        caNextesy.setRole(UserRole.CA_USER);
        caNextesy.setActive(true);
        caNextesy.setOrganization(nextesy);
        caNextesy.setTwoFactorEnabled(false);
        userRepository.save(caNextesy);

        User newUser = new User();
        newUser.setFirstName("Nikola");
        newUser.setLastName("Bjelica");
        newUser.setEmail("user1@pki.com");
        newUser.setPasswordHash(passwordEncoder.encode("user123"));
        newUser.setRole(UserRole.USER);
        newUser.setActive(true);
        newUser.setOrganization(systemOrganization);
        newUser.setTwoFactorEnabled(false);
        userRepository.save(newUser);

        User newUser2 = new User();
        newUser2.setFirstName("Nikola");
        newUser2.setLastName("Milinovic");
        newUser2.setEmail("user2@pki.com");
        newUser2.setPasswordHash(passwordEncoder.encode("user123"));
        newUser2.setRole(UserRole.USER);
        newUser2.setActive(true);
        newUser2.setOrganization(systemOrganization);
        newUser2.setTwoFactorEnabled(false);
        userRepository.save(newUser2);
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

        String crlUrl = buildCrlUrl(serialNumber.toString());
        String ocspUrl = buildOcspUrl(serialNumber.toString());

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
                "SHA256withRSA",
                crlUrl,
                ocspUrl
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
        rootCertificate.setSignatureAlgorithm("SHA256withRSA");
        rootCertificate.setIssuerCommonNameSnapshot(subjectData.getCommonName());
        rootCertificate.setSubjectAlternativeName("Alternative Names");

        Certificate savedRoot = certificateRepository.save(rootCertificate);

        byte[] orgKeyBytes = keyEncryptionService.decrypt(
                systemOrganization.getEncryptedOrgKey(),
                keyEncryptionService.getMasterKeyByVersion(systemOrganization.getMasterKeyVersion())
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

    private void generateEndEntityCertificates() {
        User user1 = userRepository.findByEmail("user1@pki.com").orElseThrow();
        User user2 = userRepository.findByEmail("user2@pki.com").orElseThrow();

        Certificate root = certificateRepository.findAll().stream()
                .filter(c -> c.getType() == CertificateType.ROOT)
                .findFirst()
                .orElseThrow();

        createEndEntityCert(user1, root);
        createEndEntityCert(user1, root);
        createEndEntityCert(user2, root);
    }

    private void createEndEntityCert(User user, Certificate issuerCert) {
        X500SubjectData subjectData = new X500SubjectData();
        subjectData.setCommonName(user.getFirstName() + " " + user.getLastName());
        subjectData.setOrganization(user.getOrganization().getName());
        subjectData.setCountry("RS");
        subjectData.setEmail(user.getEmail());

        X500Name subjectX500Name = x500NameBuilderService.build(subjectData);
        X500Name issuerX500Name = x500NameBuilderService.build(issuerCert.getSubjectData());

        KeyPair keyPair = keyPairGeneratorService.generateRsaKeyPair();
        BigInteger serialNumber = BigInteger.valueOf(System.nanoTime()).abs();

        Date validFrom = new Date();
        Date validTo = new Date(System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000));

        String crlUrl = buildCrlUrl(issuerCert.getSerialNumber());
        String ocspUrl = buildOcspUrl(serialNumber.toString());

        X509Certificate x509Certificate = certificateGeneratorService.generateCertificate(
                issuerX500Name,
                keyPair.getPrivate(),
                subjectX500Name,
                keyPair.getPublic(),
                serialNumber,
                validFrom,
                validTo,
                false,
                false,
                true,
                true,
                null,
                "SHA256withRSA",
                crlUrl,
                ocspUrl
        );

        Certificate cert = new Certificate();
        cert.setSerialNumber(serialNumber.toString());
        cert.setSelfSigned(false);
        cert.setSubjectData(subjectData);
        cert.setType(CertificateType.END_ENTITY);
        cert.setStatus(CertificateStatus.ACTIVE);
        cert.setValidFrom(LocalDateTime.now());
        cert.setValidTo(LocalDateTime.now().plusYears(1));
        cert.setCa(false);
        cert.setKeyCertSign(false);
        cert.setDigitalSignature(true);
        cert.setKeyEncipherment(true);
        cert.setEncodedCertificate(toPem(x509Certificate));
        cert.setOrganization(user.getOrganization());
        cert.setIssuerCertificate(issuerCert);
        cert.setSignatureAlgorithm("SHA256withRSA");
        cert.setIssuerCommonNameSnapshot(issuerCert.getSubjectData().getCommonName());

        Certificate saved = certificateRepository.save(cert);

        byte[] orgKeyBytes = keyEncryptionService.decrypt(
                user.getOrganization().getEncryptedOrgKey(),
                keyEncryptionService.getMasterKeyByVersion(user.getOrganization().getMasterKeyVersion())
        );

        SecretKey orgKey = new SecretKeySpec(orgKeyBytes, "AES");

        byte[] encryptedPrivateKey = keyEncryptionService.encrypt(
                keyPair.getPrivate().getEncoded(),
                orgKey
        );

        PrivateKeyEntity privateKeyEntity = new PrivateKeyEntity();
        privateKeyEntity.setCertificate(saved);
        privateKeyEntity.setEncryptedPrivateKey(encryptedPrivateKey);
        privateKeyEntity.setOrganization(user.getOrganization());

        privateKeyRepository.save(privateKeyEntity);
        System.out.println("CREATED CERT FOR: " + user.getEmail());
    }

    private String buildCrlUrl(String issuerSerialNumber) {
        return publicBaseUrl + "/api/revocation/crl/" + issuerSerialNumber;
    }

    private String buildOcspUrl(String serialNumber) {
        return publicBaseUrl + "/api/revocation/ocsp/" + serialNumber;
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