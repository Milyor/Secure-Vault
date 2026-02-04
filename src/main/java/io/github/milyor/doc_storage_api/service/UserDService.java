package io.github.milyor.doc_storage_api.service;

import io.github.milyor.doc_storage_api.model.UserPrincipal;
import io.github.milyor.doc_storage_api.model.Users;
import io.github.milyor.doc_storage_api.repository.UserRep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDService implements UserDetailsService {

    @Autowired
    private UserRep repo;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = repo.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        return new UserPrincipal(user);
    }
}
