package io.github.milyor.doc_storage_api.controller;

import io.github.milyor.doc_storage_api.model.UserPrincipal;
import io.github.milyor.doc_storage_api.model.Users;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class SecurityController {

    @GetMapping("/account/me")
    public AccountResponse getCurrentAccount(@AuthenticationPrincipal UserPrincipal principal) {
        Users user = principal.getUser();
        return new AccountResponse(user.getId(), user.getUsername(), user.getRole());
    }

    public record AccountResponse(UUID id, String username, String role) {
    }
}
