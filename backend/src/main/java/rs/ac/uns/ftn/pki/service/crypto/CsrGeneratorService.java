package rs.ac.uns.ftn.pki.service.crypto;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.model.entity.X500SubjectData;

import java.io.StringWriter;
import java.security.KeyPair;

@Service
@RequiredArgsConstructor
public class CsrGeneratorService {

    private final X500NameBuilderService x500NameBuilderService;

    public String generatePemCsr(
            X500SubjectData subjectData,
            KeyPair keyPair
    ) {

        try {

            X500Name subject =
                    x500NameBuilderService.build(subjectData);

            PKCS10CertificationRequestBuilder builder =
                    new JcaPKCS10CertificationRequestBuilder(
                            subject,
                            keyPair.getPublic()
                    );

            JcaContentSignerBuilder signerBuilder =
                    new JcaContentSignerBuilder(
                            "SHA256withRSA"
                    );

            ContentSigner signer =
                    signerBuilder.build(
                            keyPair.getPrivate()
                    );

            PKCS10CertificationRequest csr =
                    builder.build(signer);

            StringWriter writer =
                    new StringWriter();

            try (JcaPEMWriter pemWriter =
                         new JcaPEMWriter(writer)) {

                pemWriter.writeObject(csr);
            }

            return writer.toString();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to generate CSR.",
                    e
            );
        }
    }
}
