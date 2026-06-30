package io.github.milyor.doc_storage_api.controller;

import io.github.milyor.doc_storage_api.config.SecurityConfig;
import io.github.milyor.doc_storage_api.model.UserPrincipal;
import io.github.milyor.doc_storage_api.model.Users;
import io.github.milyor.doc_storage_api.service.UserDService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer slice test for SecurityController#/account/me.
 * Sends a real UserPrincipal through MockMvc so @AuthenticationPrincipal resolves it.
 */
@WebMvcTest(SecurityController.class)
@Import(SecurityConfig.class)
class SecurityControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDService userDService;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/account/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountMeReturnsCurrentUser() throws Exception {
        mockMvc.perform(get("/account/me").with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    private static UsernamePasswordAuthenticationToken authenticatedUser() {
        Users user = new Users();
        user.setId(USER_ID);
        user.setUsername("alice");
        user.setPassword("irrelevant-hash");
        user.setRole("ROLE_ADMIN");

        UserPrincipal principal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(
                principal, principal.getPassword(), principal.getAuthorities());
    }
}
