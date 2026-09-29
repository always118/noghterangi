package com.noghterangi.media;

import org.springframework.web.multipart.MultipartFile;

/** Physical storage boundary; product services do not depend on local paths. */
public interface MediaStorageService {
    StoredImage store(MultipartFile file);
    byte[] read(String storedFileName);
    void delete(String storedFileName);

    record StoredImage(String storedFileName, String originalFileName, String contentType, long fileSize) {}
}
