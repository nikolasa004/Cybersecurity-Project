package rs.ac.uns.ftn.pki.exception;

public class CertificateFileReadException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CertificateFileReadException(String message, Throwable cause) {
        super(message);
    }
}
