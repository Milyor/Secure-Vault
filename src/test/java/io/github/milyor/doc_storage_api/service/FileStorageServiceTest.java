package io.github.milyor.doc_storage_api.service;

import io.github.milyor.doc_storage_api.model.FileDocument;
import io.github.milyor.doc_storage_api.repository.FileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private FileRepository fileRepository;

    @Mock
    private S3StorageService s3;

    @InjectMocks
    private FileStorageService service;

    @Test
    void nonCompressibleFileIsStoredRawWithOriginalSize() throws Exception {
        byte[] content = "binary-ish".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", content);

        service.saveFile(file, OWNER);

        verify(s3).put(anyString(), any(InputStream.class), eq((long) content.length), eq("application/pdf"));

        ArgumentCaptor<FileDocument> captor = ArgumentCaptor.forClass(FileDocument.class);
        verify(fileRepository).save(captor.capture());
        FileDocument saved = captor.getValue();
        assertThat(saved.getOwnerId()).isEqualTo(OWNER);
        assertThat(saved.getSize()).isEqualTo(content.length);
        assertThat(saved.getS3Key()).isNotBlank();
        assertThat(saved.isCompressed()).isFalse();
    }

    @Test
    void compressibleFileIsGzippedAndMarkedCompressed() throws Exception {
        byte[] content = "repeat repeat repeat repeat repeat repeat".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", content);

        ArgumentCaptor<Long> lengthCaptor = ArgumentCaptor.forClass(Long.class);
        service.saveFile(file, OWNER);

        verify(s3).put(anyString(), any(InputStream.class), lengthCaptor.capture(), eq("text/plain"));

        ArgumentCaptor<FileDocument> captor = ArgumentCaptor.forClass(FileDocument.class);
        verify(fileRepository).save(captor.capture());
        FileDocument saved = captor.getValue();
        assertThat(saved.isCompressed()).isTrue();
        // metadata records the ORIGINAL size, not the compressed length sent to S3
        assertThat(saved.getSize()).isEqualTo(content.length);
        assertThat(lengthCaptor.getValue()).isNotEqualTo((long) content.length);
    }

    @Test
    void downloadOfCompressedFileInflatesBackToOriginal() throws Exception {
        byte[] original = "the original text content".getBytes();
        byte[] gzipped = gzip(original);

        FileDocument doc = new FileDocument("notes.txt", "text/plain", original.length, "key", true, OWNER);
        when(s3.openStream("key")).thenReturn(new ByteArrayInputStream(gzipped));

        try (InputStream in = service.openDownloadStream(doc)) {
            assertThat(in.readAllBytes()).isEqualTo(original);
        }
    }

    @Test
    void downloadOfUncompressedFilePassesBytesThrough() throws Exception {
        byte[] bytes = "abc".getBytes();
        FileDocument doc = new FileDocument("doc.pdf", "application/pdf", bytes.length, "key", false, OWNER);
        when(s3.openStream("key")).thenReturn(new ByteArrayInputStream(bytes));

        try (InputStream in = service.openDownloadStream(doc)) {
            assertThat(in.readAllBytes()).isEqualTo(bytes);
        }
    }

    @Test
    void getFileReturnsOwnersFile() throws Exception {
        UUID id = UUID.randomUUID();
        FileDocument doc = new FileDocument("r.txt", "text/plain", 3, "key", false, OWNER);
        when(fileRepository.findByIdAndOwnerId(id, OWNER)).thenReturn(Optional.of(doc));

        assertThat(service.getFile(id.toString(), OWNER)).isSameAs(doc);
    }

    @Test
    void getFileThrowsWhenNotOwned() {
        UUID id = UUID.randomUUID();
        when(fileRepository.findByIdAndOwnerId(id, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFile(id.toString(), OWNER))
                .isInstanceOf(FileNotFoundException.class);
    }

    private static byte[] gzip(byte[] data) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(data);
        }
        return out.toByteArray();
    }
}
