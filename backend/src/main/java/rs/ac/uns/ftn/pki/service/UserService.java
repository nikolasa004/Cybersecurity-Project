package rs.ac.uns.ftn.pki.service;

import rs.ac.uns.ftn.pki.dto.user.UserProfileResponse;

public interface UserService {
    UserProfileResponse getCurrentUserProfile();
}