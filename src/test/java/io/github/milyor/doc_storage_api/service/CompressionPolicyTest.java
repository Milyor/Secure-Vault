package io.github.milyor.doc_storage_api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompressionPolicyTest {

    @Test
    void compressesTextLikeTypes() {
        assertThat(CompressionPolicy.shouldCompress("text/plain")).isTrue();
        assertThat(CompressionPolicy.shouldCompress("application/json")).isTrue();
        assertThat(CompressionPolicy.shouldCompress("image/svg+xml")).isTrue();
        assertThat(CompressionPolicy.shouldCompress("text/markdown")).isTrue();
    }

    @Test
    void skipsAlreadyCompressedTypes() {
        assertThat(CompressionPolicy.shouldCompress("application/pdf")).isFalse();
        assertThat(CompressionPolicy.shouldCompress("image/png")).isFalse();
        assertThat(CompressionPolicy.shouldCompress("image/jpeg")).isFalse();
        assertThat(CompressionPolicy.shouldCompress("application/zip")).isFalse();
        assertThat(CompressionPolicy.shouldCompress("video/mp4")).isFalse();
    }

    @Test
    void handlesCharsetParameterAndCase() {
        assertThat(CompressionPolicy.shouldCompress("text/plain; charset=UTF-8")).isTrue();
        assertThat(CompressionPolicy.shouldCompress("APPLICATION/JSON")).isTrue();
    }

    @Test
    void nullTypeIsNotCompressed() {
        assertThat(CompressionPolicy.shouldCompress(null)).isFalse();
    }
}
