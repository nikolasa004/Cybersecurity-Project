package rs.ac.uns.ftn.pki.service.crypto;

import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
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
            String signatureAlgorithm,
            String crlDistributionPointUrl,
            String ocspUrl
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

            if (subjectAlternativeName != null && !subjectAlternativeName.isBlank()) {
                GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, subjectAlternativeName));
                
                certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
            }

            if (crlDistributionPointUrl != null && !crlDistributionPointUrl.isBlank()) {
                DistributionPointName distributionPointName = new DistributionPointName(new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, crlDistributionPointUrl)));
                DistributionPoint distributionPoint = new DistributionPoint(distributionPointName, null, null);
                CRLDistPoint crlDistPoint = new CRLDistPoint(new DistributionPoint[]{distributionPoint});
                
                certBuilder.addExtension(Extension.cRLDistributionPoints, false, crlDistPoint);
            }

            if (ocspUrl != null && !ocspUrl.isBlank()) {
                AccessDescription ocspAccess = new AccessDescription(AccessDescription.id_ad_ocsp, new GeneralName(GeneralName.uniformResourceIdentifier, ocspUrl));
                AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(new DERSequence(ocspAccess));
                
                certBuilder.addExtension(Extension.authorityInfoAccess, false, aia);
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