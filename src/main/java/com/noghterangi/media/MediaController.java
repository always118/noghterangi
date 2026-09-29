package com.noghterangi.media;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/media")
public class MediaController {
    private final MediaService service;
    public MediaController(MediaService service) { this.service = service; }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        var image = service.content(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.bytes().length).body(image.bytes());
    }

    @ExceptionHandler(MediaStorageException.class)
    public ResponseEntity<Void> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
