package rs.ac.uns.ftn.pki.service.crypto;

import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.pki.dto.certificate.IssueCertificateRequest;
import rs.ac.uns.ftn.pki.model.entity.Certificate;
import rs.ac.uns.ftn.pki.model.entity.Organization;
import rs.ac.uns.ftn.pki.model.entity.X500SubjectData;
import rs.ac.uns.ftn.pki.model.enums.CertificateStatus;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;

@Component
public class CertificateEntityFactory {

    public Certificate create(
            IssueCertificateRequest request,
            String serialNumber,
            X500SubjectData subjectData,
            Certificate issuerCertificate,
            Organization organization,
            String pem
    ) {
        Certificate certificate = new Certificate();
        certificate.setSerialNumber(serialNumber);
        certificate.setType(request.getType());
        certificate.setStatus(CertificateStatus.ACTIVE);
        certificate.setSubjectData(subjectData);
        certificate.setIssuerCertificate(issuerCertificate);
        certificate.setOrganization(organization);
        certificate.setCa(request.isCa());
        certificate.setSelfSigned(request.getType() == CertificateType.ROOT);
        certificate.setKeyCertSign(request.isKeyCertSign());
        certificate.setDigitalSignature(request.isDigitalSignature());
        certificate.setKeyEncipherment(request.isKeyEncipherment());
        certificate.setValidFrom(request.getValidFrom());
        certificate.setValidTo(request.getValidTo());
        certificate.setSignatureAlgorithm("SHA256withRSA");
        certificate.setEncodedCertificate(pem);
        certificate.setIssuerCommonNameSnapshot(
                request.getType() == CertificateType.ROOT
                        ? subjectData.getCommonName()
                        : issuerCertificate.getSubjectData().getCommonName()
        );

        return certificate;
    }
}