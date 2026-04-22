package rs.ac.uns.ftn.pki.exception;

public class SecurityAlertException extends  RuntimeException {
    private static final long serialVersionUID = 1L;

    public SecurityAlertException(String message) {
        super(message);
    }
}
