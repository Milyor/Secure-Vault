package io.github.milyor.doc_storage_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity // 1. Tells Hibernate: "Make a table out of this class"
@Table(name = "files") // 2. Name the table "files" in Postgres
@Getter @Setter @NoArgsConstructor // Lombok magic (less boilerplate)

public class FileDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // 3. Security best practice
    private UUID id;

    private String fileName;

    private String contentType; // e.g., "application/pdf" or "image/png"

    @Column(nullable = false)
    private UUID ownerId;

    private long size;

    @Column(nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private boolean compressed = false;

    public FileDocument(String fileName, String contentType, long size,
                        String s3Key, boolean compressed, UUID ownerId) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.s3Key = s3Key;
        this.compressed = compressed;
        this.ownerId = ownerId;
    }
}