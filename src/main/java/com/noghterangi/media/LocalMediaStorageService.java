package com.noghterangi.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class LocalMediaStorageService implements MediaStorageService {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Pattern STORED_NAME = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)");
    private final Path root;

    public LocalMediaStorageService(@Value("${app.media.storage-path}") String directory) {
        if (directory == null || directory.isBlank()) {
            throw new IllegalStateException("Media storage directory must be configured");
        }
        try {
            root = Files.createDirectories(Path.of(directory).toAbsolutePath().normalize()).toRealPath();
        } catch (IOException | InvalidPathException exception) {
            throw new IllegalStateException("Media storage directory is unavailable");
        }
    }

    @Override
    public StoredImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) { throw invalid("لطفاً یک تصویر انتخاب کنید."); }
        if (file.getSize() > MAX_BYTES) { throw tooLarge(); }
        String original = originalName(file.getOriginalFilename());
        String lower = original.toLowerCase(Locale.ROOT);
        boolean jpeg = lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        boolean png = lower.endsWith(".png");
        String type = jpeg ? "image/jpeg" : "image/png";
        if ((!jpeg && !png) || !type.equals(file.getContentType())) {
            throw invalid("فقط تصویر JPEG یا PNG با پسوند و نوع معتبر پذیرفته می‌شود.");
        }
        byte[] bytes;
        try (var input = file.getInputStream()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException exception) {
            throw invalid("خواندن تصویر ناموفق بود.");
        }
        if (bytes.length > MAX_BYTES) { throw tooLarge(); }
        validateImage(bytes, jpeg ? "JPEG" : "PNG");
        String name = UUID.randomUUID() + (jpeg ? ".jpg" : ".png");
        Path target = resolve(name);
        boolean created = false;
        try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            created = true;
            output.write(bytes);
        } catch (IOException exception) {
            if (created) {
                try { Files.deleteIfExists(target); } catch (IOException ignored) { /* incomplete file, never referenced */ }
            }
            throw invalid("ذخیره تصویر ناموفق بود. دوباره تلاش کنید.");
        }
        return new StoredImage(name, original, type, bytes.length);
    }

    private void validateImage(byte[] bytes, String expectedFormat) {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) { throw invalid("فایل تصویر معتبر نیست."); }
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (!expectedFormat.equalsIgnoreCase(reader.getFormatName())) { throw invalid("فایل تصویر معتبر نیست."); }
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 8192 || height > 8192 || (long) width * height > 10_000_000) {
                    throw invalid("ابعاد تصویر بیش از حد مجاز است (حداکثر ۱۰ میلیون پیکسل و ۸۱۹۲ پیکسل در هر ضلع).");
                }
                if (reader.read(0) == null) { throw invalid("فایل تصویر معتبر نیست."); }
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException exception) {
            throw invalid("فایل تصویر معتبر نیست.");
        }
    }

    private String originalName(String value) {
        if (value == null) { throw invalid("نام فایل تصویر معتبر نیست."); }
        String name = value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        if (name.isBlank() || name.length() > 255) { throw invalid("نام فایل تصویر معتبر نیست."); }
        return name;
    }

    private Path resolve(String name) {
        if (name == null || !STORED_NAME.matcher(name).matches()) {
            throw invalid("شناسه فایل تصویر معتبر نیست.");
        }
        return root.resolve(name);
    }

    @Override
    public byte[] read(String name) {
        Path path = resolve(name);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) { throw missing(); }
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) { throw invalid("خواندن تصویر ناموفق بود."); }
            return bytes;
        } catch (NoSuchFileException exception) {
            throw missing();
        } catch (IOException exception) {
            throw invalid("خواندن تصویر ناموفق بود.");
        }
    }

    @Override
    public void delete(String name) {
        try { Files.deleteIfExists(resolve(name)); }
        catch (IOException exception) { throw invalid("حذف فایل تصویر ناموفق بود."); }
    }

    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "تصویر پیدا نشد."); }
    private MediaStorageException tooLarge() { return invalid("حجم تصویر بیش از حد مجاز است (۵ مگابایت)."); }
    private MediaStorageException invalid(String message) { return new MediaStorageException(message); }
}
