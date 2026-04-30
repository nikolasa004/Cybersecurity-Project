package rs.ac.uns.ftn.pki.security;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.pki.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceCustom implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailWithOrganization(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}