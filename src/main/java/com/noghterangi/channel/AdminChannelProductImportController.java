package com.noghterangi.channel;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Protected admin API for an explicitly reviewed channel manifest. */
@RestController
@RequestMapping("/admin/import/products")
@Profile("!prod & !production")
@ConditionalOnProperty(name = "app.channel-import.enabled", havingValue = "true")
public class AdminChannelProductImportController {
    private final ChannelProductImportService importer;

    public AdminChannelProductImportController(ChannelProductImportService importer) { this.importer = importer; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChannelProductImportService.ImportReport> importProducts(
            @RequestPart("manifest") MultipartFile manifest,
            @RequestParam(defaultValue = "false") boolean deleteExisting,
            @RequestParam(defaultValue = "false") boolean allowPriceReviewRequired) throws IOException {
        if (manifest == null || manifest.isEmpty()) { return ResponseEntity.badRequest().build(); }
        return ResponseEntity.ok(importer.importManifest(manifest.getBytes(), deleteExisting, allowPriceReviewRequired));
    }
}
