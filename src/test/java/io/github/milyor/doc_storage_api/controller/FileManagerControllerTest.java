package io.github.milyor.doc_storage_api.controller;

import io.github.milyor.doc_storage_api.config.SecurityConfig;
import io.github.milyor.doc_storage_api.model.FileDocument;
import io.github.milyor.doc_storage_api.model.UserPrincipal;
import io.github.milyor.doc_storage_api.model.Users;
import io.github.milyor.doc_storage_api.service.FileStorageService;
import io.github.milyor.doc_storage_api.service.UserDService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.stream.Stream;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer slice tests for FileManagerController.
 * Loads the real SecurityConfig so auth behaviour is exercised; services are mocked.
 */
@WebMvcTest(FileManagerController.class)
@Import(SecurityConfig.class)
class FileManagerControllerTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    // SecurityConfig's AuthenticationProvider depends on UserDService; mock it so context loads.
    @MockitoBean
    private UserDService userDService;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/files"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadReturnsTrueOnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.txt", "text/plain", "hello vault".getBytes());

        mockMvc.perform(multipart("/upload-file").file(file).with(csrf()).with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // file is stamped with the authenticated user's id
        verify(fileStorageService).saveFile(any(), eq(OWNER_ID));
    }

    @Test
    void uploadReturnsFalseWhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("boom")).when(fileStorageService).saveFile(any(), any());
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.txt", "text/plain", "data".getBytes());

        mockMvc.perform(multipart("/upload-file").file(file).with(csrf()).with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void listReturnsOnlyOwnersFilesWithDownloadUrl() throws Exception {
        byte[] bytes = "hello vault".getBytes();
        FileDocument doc = new FileDocument(
                "report.txt", "text/plain", bytes.length, "some-s3-key", false, OWNER_ID);
        doc.setId(UUID.randomUUID());
        when(fileStorageService.getAllFiles(OWNER_ID)).thenReturn(Stream.of(doc));

        mockMvc.perform(get("/files").with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("report.txt"))
                .andExpect(jsonPath("$[0].type").value("text/plain"))
                .andExpect(jsonPath("$[0].size").value(bytes.length))
                .andExpect(jsonPath("$[0].url").value(org.hamcrest.Matchers.containsString("/download/")));

        // list is scoped to the authenticated owner
        verify(fileStorageService).getAllFiles(OWNER_ID);
    }

    @Test
    void downloadStreamsFileBytesWithContentDisposition() throws Exception {
        byte[] bytes = "hello vault".getBytes();
        FileDocument doc = new FileDocument(
                "report.txt", "text/plain", bytes.length, "some-s3-key", false, OWNER_ID);
        UUID id = UUID.randomUUID();
        doc.setId(id);
        when(fileStorageService.getFile(id.toString(), OWNER_ID)).thenReturn(doc);
        // controller streams whatever openDownloadStream returns straight to the response
        when(fileStorageService.openDownloadStream(doc)).thenReturn(new ByteArrayInputStream(bytes));

        mockMvc.perform(get("/download/" + id).with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("report.txt")))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void downloadOfOtherUsersFileReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        // service treats a non-owned file as not found
        when(fileStorageService.getFile(id.toString(), OWNER_ID))
                .thenThrow(new FileNotFoundException("File not found with id " + id));

        mockMvc.perform(get("/download/" + id).with(authentication(authenticatedUser())))
                .andExpect(status().isNotFound());
    }

    private static UsernamePasswordAuthenticationToken authenticatedUser() {
        Users user = new Users();
        user.setId(OWNER_ID);
        user.setUsername("alice");
        user.setPassword("irrelevant-hash");
        user.setRole("ROLE_ADMIN");

        UserPrincipal principal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(
                principal, principal.getPassword(), principal.getAuthorities());
    }
}
