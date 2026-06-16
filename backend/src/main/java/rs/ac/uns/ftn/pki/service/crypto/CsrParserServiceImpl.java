package rs.ac.uns.ftn.pki.service.crypto;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.dto.certificateRequest.ParsedCsrData;
import rs.ac.uns.ftn.pki.service.CsrParserService;

import java.io.StringReader;

@Service
@RequiredArgsConstructor
public class CsrParserServiceImpl implements CsrParserService {

    @Override
    public ParsedCsrData parse(String csrPem) {

        try (
                PEMParser parser = new PEMParser(new StringReader(csrPem))
        ) {
            Object object = parser.readObject();

            if (!(object instanceof PKCS10CertificationRequest csr)) {
                throw new IllegalArgumentException("Invalid CSR file.");
            }

            ParsedCsrData result = new ParsedCsrData();

            result.setCommonName(getValue(csr, BCStyle.CN));
            result.setCountry(getValue(csr, BCStyle.C));
            result.setState(getValue(csr, BCStyle.ST));
            result.setLocality(getValue(csr, BCStyle.L));
            result.setOrganization(getValue(csr, BCStyle.O));
            result.setOrganizationalUnit(getValue(csr, BCStyle.OU));
            result.setEmail(getValue(csr, BCStyle.EmailAddress));
            result.setPublicKey(new JcaPEMKeyConverter().getPublicKey(csr.getSubjectPublicKeyInfo()));
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse CSR.", e);
        }
    }

    private String getValue(PKCS10CertificationRequest csr,org.bouncycastle.asn1.ASN1ObjectIdentifier identifier) {
        RDN[] rdns = csr.getSubject().getRDNs(identifier);

        if (rdns.length == 0) {
            return null;
        }

        return rdns[0]
                .getFirst()
                .getValue()
                .toString();
    }
}