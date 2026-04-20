package rs.ac.uns.ftn.pki.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class X500SubjectData {

    @Column(name = "subject_common_name", nullable = false)
    private String commonName;

    @Column(name = "subject_country")
    private String country;

    @Column(name = "subject_state")
    private String state;

    @Column(name = "subject_locality")
    private String locality;

    @Column(name = "subject_organization")
    private String organization;

    @Column(name = "subject_organizational_unit")
    private String organizationalUnit;

    @Column(name = "subject_email")
    private String email;

    @Column(name = "subject_serial_number")
    private String serialNumber;
}