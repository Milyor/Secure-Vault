package io.github.milyor.doc_storage_api.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseFile {
    private String id;
    private String name;
    private Long size;
    String url;
    private String type;
}
