package rs.ac.uns.ftn.pki.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.ac.uns.ftn.pki.model.enums.MasterKeyStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "master_keys")
@Getter
@Setter
@NoArgsConstructor
public class MasterKeyEntity {

    @Id
    @Column(nullable = false, unique = true)
    private String version;

    @Lob
    @Column(nullable = false)
    private byte[] encryptedKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MasterKeyStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}