package io.github.milyor.doc_storage_api.service;

import java.util.Set;

public final class CompressionPolicy {

    private static final Set<String> COMPRESSIBLE_TYPES = Set.of(
            "text/plain",
            "text/csv",
            "text/html",
            "text/xml",
            "text/markdown",
            "application/json",
            "application/xml",
            "application/javascript",
            "image/svg+xml",
            "application/x-ndjson"
    );

    private CompressionPolicy() {
    }

    public static boolean shouldCompress(String contentType) {
        if (contentType == null) {
            return false;
        }
        String type = stripParameters(contentType);
        if (COMPRESSIBLE_TYPES.contains(type)) {
            return true;
        }
        return type.startsWith("text/");
    }

    private static String stripParameters(String contentType) {
        int semicolon = contentType.indexOf(';');
        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return base.trim().toLowerCase();
    }
}
