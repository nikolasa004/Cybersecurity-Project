package rs.ac.uns.ftn.pki.mapper;

import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.pki.dto.certificate.IssueCertificateRequest;
import rs.ac.uns.ftn.pki.model.entity.X500SubjectData;

@Component
public class CertificateSubjectMapper {

    public X500SubjectData fromIssueRequest(IssueCertificateRequest request) {
        X500SubjectData data = new X500SubjectData();
        data.setCommonName(request.getCommonName());
        data.setCountry(request.getCountry());
        data.setState(request.getState());
        data.setLocality(request.getLocality());
        data.setOrganization(request.getOrganization());
        data.setOrganizationalUnit(request.getOrganizationalUnit());
        data.setEmail(request.getEmail());
        data.setSerialNumber(request.getSubjectSerialNumber());
        return data;
    }
}