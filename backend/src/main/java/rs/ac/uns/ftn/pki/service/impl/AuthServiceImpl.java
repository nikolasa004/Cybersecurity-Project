package rs.ac.uns.ftn.pki.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.exception.UnauthorizedException;
import rs.ac.uns.ftn.pki.model.entity.User;
import rs.ac.uns.ftn.pki.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return (User) authentication.getPrincipal();
    }
}

