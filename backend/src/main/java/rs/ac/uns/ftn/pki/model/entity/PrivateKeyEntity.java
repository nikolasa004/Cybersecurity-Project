package rs.ac.uns.ftn.pki.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "private_keys")
@Getter
@Setter
@NoArgsConstructor
public class PrivateKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;

    @Lob
    @Column(nullable = false)
    private byte[] encryptedPrivateKey;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

}