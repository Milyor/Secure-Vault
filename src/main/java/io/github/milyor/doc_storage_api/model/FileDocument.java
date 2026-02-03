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

    @Lob // 4. "Large Object" - tells Postgres this is a big chunk of data
    @Column(columnDefinition = "OID") // Postgres specific optimization for blobs
    private byte[] data; // The actual file content lives here

    // Custom constructor for easy creation
    public FileDocument(String fileName, String contentType, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
    }
}