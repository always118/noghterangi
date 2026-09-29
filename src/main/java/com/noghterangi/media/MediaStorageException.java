package com.noghterangi.media;

/** Only safe, user-facing messages belong in this exception. */
public class MediaStorageException extends RuntimeException {
    public MediaStorageException(String message) { super(message); }
}
