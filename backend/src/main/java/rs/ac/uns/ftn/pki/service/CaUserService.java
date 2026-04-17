package rs.ac.uns.ftn.pki.service;

import rs.ac.uns.ftn.pki.dto.user.CaUserResponse;
import rs.ac.uns.ftn.pki.dto.user.CreateCaUserRequest;
import rs.ac.uns.ftn.pki.dto.user.SetPasswordRequest;

import java.util.List;

public interface CaUserService {
    CaUserResponse create(CreateCaUserRequest request);
    List<CaUserResponse> getAllCaUsers();
    void activateAndSetPassword(SetPasswordRequest request);
}