package rs.ac.uns.ftn.pki.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.pki.dto.certificate.CertificateListResponse;
import rs.ac.uns.ftn.pki.dto.certificate.IssuerOptionResponse;
import rs.ac.uns.ftn.pki.mapper.CertificateMapper;
import rs.ac.uns.ftn.pki.model.entity.Certificate;
import rs.ac.uns.ftn.pki.model.enums.CertificateStatus;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;
import rs.ac.uns.ftn.pki.repository.CertificateRepository;
import rs.ac.uns.ftn.pki.service.CertificateService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CertificateListResponse> getAllForAdmin() {
        return certificateRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(certificateMapper::toListResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssuerOptionResponse> getAvailableIssuers(CertificateType targetType) {
        if (targetType == CertificateType.ROOT) {
            return List.of();
        }

        List<CertificateType> allowedIssuerTypes = resolveAllowedIssuerTypes(targetType);

        return certificateRepository
                .findAllByTypeInAndStatusAndValidToAfterAndCaTrueAndKeyCertSignTrueOrderByCreatedAtDesc(
                        allowedIssuerTypes,
                        CertificateStatus.ACTIVE,
                        LocalDateTime.now()
                )
                .stream()
                .map(certificateMapper::toIssuerOptionResponse)
                .toList();
    }

    private List<CertificateType> resolveAllowedIssuerTypes(CertificateType targetType) {
        return switch (targetType) {
            case ROOT -> List.of();
            case INTERMEDIATE -> List.of(CertificateType.ROOT, CertificateType.INTERMEDIATE);
            case END_ENTITY -> List.of(CertificateType.ROOT, CertificateType.INTERMEDIATE);
        };
    }
}