package rs.ac.uns.ftn.pki.service.crypto;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.model.entity.X500SubjectData;

@Service
public class X500NameBuilderService {

    public X500Name build(X500SubjectData data) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

        addIfPresent(builder, BCStyle.CN, data.getCommonName());
        addIfPresent(builder, BCStyle.C, data.getCountry());
        addIfPresent(builder, BCStyle.ST, data.getState());
        addIfPresent(builder, BCStyle.L, data.getLocality());
        addIfPresent(builder, BCStyle.O, data.getOrganization());
        addIfPresent(builder, BCStyle.OU, data.getOrganizationalUnit());
        addIfPresent(builder, BCStyle.EmailAddress, data.getEmail());
        addIfPresent(builder, BCStyle.SERIALNUMBER, data.getSerialNumber());

        return builder.build();
    }

    private void addIfPresent(X500NameBuilder builder, org.bouncycastle.asn1.ASN1ObjectIdentifier oid, String value) {
        if (value != null && !value.isBlank()) {
            builder.addRDN(oid, value);
        }
    }
}