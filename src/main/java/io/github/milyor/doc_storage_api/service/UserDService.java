package io.github.milyor.doc_storage_api.service;

import io.github.milyor.doc_storage_api.model.UserPrincipal;
import io.github.milyor.doc_storage_api.model.Users;
import io.github.milyor.doc_storage_api.repository.UserRep;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserDService implements UserDetailsService {

    private final UserRep repo;

    public UserDService(UserRep repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = Optional.ofNullable(repo.findByUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new UserPrincipal(user);
    }
}