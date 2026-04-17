package rs.ac.uns.ftn.pki.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.ac.uns.ftn.pki.model.enums.CertificateStatus;
import rs.ac.uns.ftn.pki.model.enums.CertificateType;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificates", indexes = {
        @Index(name = "idx_certificate_serial_number", columnList = "serialNumber", unique = true),
        @Index(name = "idx_certificate_status", columnList = "status"),
        @Index(name = "idx_certificate_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status;

    @Embedded
    private X500SubjectData subjectData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_certificate_id")
    private Certificate issuerCertificate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false)
    private boolean ca;

    @Column(nullable = false)
    private boolean selfSigned;

    @Column(nullable = false)
    private boolean keyCertSign;

    @Column(nullable = false)
    private boolean digitalSignature;

    @Column(nullable = false)
    private boolean keyEncipherment;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validTo;

    @Column(length = 100)
    private String signatureAlgorithm;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encodedCertificate;

    @Column(length = 255)
    private String keystoreAlias;

    @Column(length = 255)
    private String privateKeyAlias;

    @Column(length = 255)
    private String issuerCommonNameSnapshot;

    @Column(length = 255)
    private String revocationReason;

    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}