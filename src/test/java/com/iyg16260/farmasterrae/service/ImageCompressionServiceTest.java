package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.utils.ByteArrayMultipartFile;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

class ImageCompressionServiceTest {

    private final ImageCompressionService service = new ImageCompressionService();

    private byte[] encodeImage(int width, int height, String format) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xCC8844);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    /** Builds a tiny valid PNG header (IHDR + IEND) declaring huge pixel dimensions without pixel data. */
    private byte[] pngWithDeclaredDimensions(int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // PNG signature
        out.writeBytes(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        // IHDR chunk (13 bytes)
        byte[] ihdrData = ByteBuffer.allocate(13)
                .putInt(width)
                .putInt(height)
                .put((byte) 8) // bit depth
                .put((byte) 2) // RGB
                .put((byte) 0) // compression
                .put((byte) 0) // filter
                .put((byte) 0) // interlace
                .array();
        writePngChunk(out, "IHDR", ihdrData);
        writePngChunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private void writePngChunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        out.writeBytes(typeBytes);
        out.writeBytes(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

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
        MultipartFile original = new MockMultipartFile(
                "imageFile", "photo.png", "image/png", encodeImage(20, 10, "png"));

        MultipartFile compressed = service.compressImage(original);

        assertInstanceOf(ByteArrayMultipartFile.class, compressed);
        assertFalse(compressed.isEmpty());
        assertEquals("image/jpeg", compressed.getContentType());
        assertEquals("photo_compressed.jpg", compressed.getOriginalFilename());
        assertNotNull(ImageIO.read(compressed.getInputStream()));
    }

    @Test
    void acceptsValidJpegAndPngWithTrailingBytes() throws Exception {
        byte[] jpeg = encodeImage(30, 20, "jpg");
        byte[] jpegWithPadding = new byte[jpeg.length + 16];
        System.arraycopy(jpeg, 0, jpegWithPadding, 0, jpeg.length);
        MultipartFile paddedJpeg = new MockMultipartFile("imageFile", "motion.jpg", "image/jpeg", jpegWithPadding);

        MultipartFile compressedJpeg = service.compressImage(paddedJpeg);
        assertNotNull(ImageIO.read(compressedJpeg.getInputStream()));

        byte[] png = encodeImage(30, 20, "png");
        byte[] pngWithNewline = new byte[png.length + 1];
        System.arraycopy(png, 0, pngWithNewline, 0, png.length);
        pngWithNewline[png.length] = '\n';
        MultipartFile trailingPng = new MockMultipartFile("imageFile", "trailing.png", "image/png", pngWithNewline);

        MultipartFile compressedPng = service.compressImage(trailingPng);
        assertNotNull(ImageIO.read(compressedPng.getInputStream()));
    }

    @Test
    void downscalesLargeValidImageWithSubsampling() throws Exception {
        MultipartFile large = new MockMultipartFile(
                "imageFile", "large.jpg", "image/jpeg", encodeImage(2600, 1800, "jpg"));

        MultipartFile compressed = service.compressImage(large);
        BufferedImage decoded = ImageIO.read(compressed.getInputStream());

        assertNotNull(decoded);
        assertTrue(decoded.getWidth() <= 1200);
        assertTrue(decoded.getHeight() <= 800);
    }

    @Test
    void rejectsTextAndUnsupportedWebpWithBadRequest() {
        MultipartFile disguised = new MockMultipartFile("imageFile", "photo.jpg", "image/jpeg",
                "<html>not an image</html>".getBytes(StandardCharsets.UTF_8));
        ResponseStatusException htmlEx = assertThrows(ResponseStatusException.class, () -> service.compressImage(disguised));
        assertEquals(HttpStatus.BAD_REQUEST, htmlEx.getStatusCode());

        byte[] webpHeader = new byte[]{'R', 'I', 'F', 'F', 4, 0, 0, 0, 'W', 'E', 'B', 'P'};
        MultipartFile webp = new MockMultipartFile("imageFile", "photo.webp", "image/webp", webpHeader);
        ResponseStatusException webpEx = assertThrows(ResponseStatusException.class, () -> service.compressImage(webp));
        assertEquals(HttpStatus.BAD_REQUEST, webpEx.getStatusCode());
        assertTrue(webpEx.getReason() != null && webpEx.getReason().contains("JPEG o PNG"));
    }

    @Test
    void rejectsDecompressionBombDimensionsBeforeDecoding() {
        MultipartFile bomb = new MockMultipartFile(
                "imageFile", "bomb.png", "image/png", pngWithDeclaredDimensions(12000, 12000));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.compressImage(bomb));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
