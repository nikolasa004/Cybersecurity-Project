package rs.ac.uns.ftn.pki.service;

import rs.ac.uns.ftn.pki.model.entity.User;

public interface AuthService {
    User getLoggedInUser();
}
