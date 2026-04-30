package rs.ac.uns.ftn.pki.service.crypto;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

@Service
public class CertificateGeneratorService {

    public CertificateGeneratorService() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public X509Certificate generateCertificate(
            X500Name issuerName,
            PrivateKey issuerPrivateKey,
            X500Name subjectName,
            PublicKey subjectPublicKey,
            BigInteger serialNumber,
            Date validFrom,
            Date validTo,
            boolean ca,
            boolean keyCertSign,
            boolean digitalSignature,
            boolean keyEncipherment,
            String subjectAlternativeName,
            String signatureAlgorithm
    ) {
        try {
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuerName,
                    serialNumber,
                    validFrom,
                    validTo,
                    subjectName,
                    subjectPublicKey
            );

            certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));

            int usage = 0;
            if (digitalSignature) {
                usage |= KeyUsage.digitalSignature;
            }
            if (keyEncipherment) {
                usage |= KeyUsage.keyEncipherment;
            }
            if (keyCertSign) {
                usage |= KeyUsage.keyCertSign;
            }

            certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(usage));
            
            if (subjectAlternativeName != null && !subjectAlternativeName.isEmpty()) {
				GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, subjectAlternativeName));
				
				certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
			}

            ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm)
                    .setProvider("BC")
                    .build(issuerPrivateKey);

            X509CertificateHolder certificateHolder = certBuilder.build(signer);

            return new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(certificateHolder);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate X.509 certificate.", e);
        }
    }
}