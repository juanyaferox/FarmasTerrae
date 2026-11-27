package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.utils.ByteArrayMultipartFile;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ImageCompressionServiceTest {

    private final ImageCompressionService service = new ImageCompressionService();

    @Test
    void byteArrayMultipartFileExposesContentAndCanTransferIt() throws Exception {
        byte[] content = new byte[]{1, 2, 3};
        ByteArrayMultipartFile file = new ByteArrayMultipartFile("upload", "photo.jpg", "image/jpeg", content);

        assertEquals("upload", file.getName());
        assertEquals("photo.jpg", file.getOriginalFilename());
        assertEquals("image/jpeg", file.getContentType());
        assertFalse(file.isEmpty());
        assertEquals(3, file.getSize());
        assertArrayEquals(content, file.getBytes());
        assertArrayEquals(content, file.getInputStream().readAllBytes());

        File target = Files.createTempFile("multipart-file", ".jpg").toFile();
        try {
            file.transferTo(target);
            assertArrayEquals(content, Files.readAllBytes(target.toPath()));
        } finally {
            Files.deleteIfExists(target.toPath());
        }
    }

    @Test
    void compressImageReturnsValidJpegAsByteArrayMultipartFile() throws Exception {
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        image.setRGB(4, 5, 0xCC8844);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        MultipartFile original = new MockMultipartFile(
                "imageFile", "photo.png", "image/png", output.toByteArray());

        MultipartFile compressed = service.compressImage(original);

        assertInstanceOf(ByteArrayMultipartFile.class, compressed);
        assertFalse(compressed.isEmpty());
        assertEquals("image/jpeg", compressed.getContentType());
        assertEquals("photo_compressed.jpg", compressed.getOriginalFilename());
        assertNotNull(ImageIO.read(compressed.getInputStream()));
    }
}
