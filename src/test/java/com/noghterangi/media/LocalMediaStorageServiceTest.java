package com.noghterangi.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class LocalMediaStorageServiceTest {
    @TempDir Path directory;
    LocalMediaStorageService storage;
    @BeforeEach void setUp() { storage = new LocalMediaStorageService(directory.resolve("media").toString()); }

    @Test void storesDecodableJpegAndPngWithUniqueSafeNamesAndRemovesOnlySelectedFile() throws Exception {
        var jpeg = ImageFixtures.image("jpeg");
        var first = storage.store(new MockMultipartFile("files", "../../photo.JPEG", "image/jpeg", jpeg.getBytes()));
        var second = storage.store(ImageFixtures.image("png"));
        assertThat(first.storedFileName()).matches("[0-9a-f-]{36}\\.jpg");
        assertThat(first.originalFileName()).isEqualTo("photo.JPEG");
        assertThat(first.contentType()).isEqualTo("image/jpeg");
        assertThat(first.fileSize()).isEqualTo(jpeg.getSize());
        assertThat(storage.read(first.storedFileName())).isEqualTo(jpeg.getBytes());
        assertThat(second.storedFileName()).matches("[0-9a-f-]{36}\\.png");
        storage.delete(first.storedFileName());
        storage.delete(first.storedFileName());
        assertThat(storage.read(second.storedFileName())).isNotEmpty();
        assertThatThrownBy(() -> storage.read(first.storedFileName())).isInstanceOf(ResponseStatusException.class);
    }

    @Test void rejectsEmptyAndOversizedFiles() {
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("files", "a.png", "image/png", new byte[0])))
                .isInstanceOf(MediaStorageException.class).hasMessageContaining("انتخاب");
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("files", "a.png", "image/png", new byte[LocalMediaStorageService.MAX_BYTES + 1])))
                .isInstanceOf(MediaStorageException.class).hasMessageContaining("حجم");
    }

    @Test void rejectsWrongMimeExtensionAndDisguisedContents() throws Exception {
        byte[] png = ImageFixtures.image("png").getBytes();
        for (var file : new MockMultipartFile[]{
                new MockMultipartFile("files", "a.png", "text/html", png),
                new MockMultipartFile("files", "a.svg", "image/png", png),
                new MockMultipartFile("files", "a.jpg", "image/jpeg", png),
                new MockMultipartFile("files", "a.png", "image/png", "<script>alert(1)</script>".getBytes()),
                new MockMultipartFile("files", "a.webp", "image/webp", png)}) {
            assertThatThrownBy(() -> storage.store(file)).isInstanceOf(MediaStorageException.class);
        }
        try (var files = Files.list(directory.resolve("media"))) { assertThat(files).isEmpty(); }
    }

    @Test void rejectsTraversalAndDoesNotReadOrDeleteSymlinkTarget() throws Exception {
        Path outside = Files.writeString(directory.resolve("outside.png"), "private");
        assertThatThrownBy(() -> storage.read("../outside.png")).isInstanceOf(MediaStorageException.class);
        assertThatThrownBy(() -> storage.delete(outside.toString())).isInstanceOf(MediaStorageException.class);
        String linkName = UUID.randomUUID() + ".png";
        Files.createSymbolicLink(directory.resolve("media").resolve(linkName), outside);
        assertThatThrownBy(() -> storage.read(linkName)).isInstanceOf(ResponseStatusException.class);
        storage.delete(linkName);
        assertThat(Files.readString(outside)).isEqualTo("private");
    }

    @Test void rejectsExcessiveDimensionsBeforeDecoding() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8193, 1, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("files", "wide.png", "image/png", bytes.toByteArray())))
                .isInstanceOf(MediaStorageException.class).hasMessageContaining("ابعاد");
    }
}
