package com.noghterangi.media;

import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

final class ImageFixtures {
    private ImageFixtures() {}
    static MockMultipartFile image(String format) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB), format, bytes);
        return new MockMultipartFile("files", "sample." + format, "image/" + format, bytes.toByteArray());
    }
}
