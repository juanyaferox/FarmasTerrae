package com.iyg16260.farmasterrae.service;

import com.iyg16260.farmasterrae.utils.ByteArrayMultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

@Service
public class ImageCompressionService {

    // Configuración recomendada
    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 800;
    private static final int MAX_INPUT_DIMENSION = 8000;
    private static final long MAX_INPUT_PIXELS = 25_000_000L;
    private static final float COMPRESSION_QUALITY = 0.85f;

    /**
     * Valida y comprime una imagen JPEG o PNG y la devuelve como MultipartFile en formato JPEG.
     *
     * @param originalFile La imagen original a comprimir
     * @return MultipartFile con la imagen comprimida en formato JPEG
     * @throws IOException             Si hay errores al codificar el resultado
     * @throws ResponseStatusException BAD_REQUEST si el archivo no es una imagen JPEG/PNG válida o supera los límites
     */
    public MultipartFile compressImage(MultipartFile originalFile) throws IOException {
        validateFile(originalFile);
        byte[] bytes = originalFile.getBytes();
        String format = detectFormat(bytes);
        if (format == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de imagen no soportado. Usa JPEG o PNG.");
        }
        BufferedImage originalImage;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw badImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String readerFormat = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!(readerFormat.equals(format) || (format.equals("jpeg") && readerFormat.equals("jpg")))) {
                    throw badImage();
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0
                        || width > MAX_INPUT_DIMENSION || height > MAX_INPUT_DIMENSION
                        || (long) width * height > MAX_INPUT_PIXELS) {
                    throw badImage();
                }
                ImageReadParam readParam = reader.getDefaultReadParam();
                int subsampling = Math.max(1, Math.min(width / MAX_WIDTH, height / MAX_HEIGHT));
                if (subsampling > 1) {
                    readParam.setSourceSubsampling(subsampling, subsampling, 0, 0);
                }
                originalImage = reader.read(0, readParam);
            } finally {
                reader.dispose();
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no es válida", e);
        }
        if (originalImage == null) {
            throw badImage();
        }

        // Procesar la imagen
        BufferedImage processedImage = processImage(originalImage);
        byte[] compressedData = compressToJPEG(processedImage);
        String compressedName = generateCompressedFileName(originalFile.getOriginalFilename());

        return new ByteArrayMultipartFile(
                "imageFile",
                compressedName,
                "image/jpeg",
                compressedData
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badImage();
        }

        // Validar tamaño máximo (ej: 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen es demasiado grande (máximo 10MB)");
        }
    }

    private String detectFormat(byte[] b) {
        if (b.length >= 3
                && (b[0] & 0xff) == 0xff
                && (b[1] & 0xff) == 0xd8
                && (b[2] & 0xff) == 0xff) {
            return "jpeg";
        }
        if (b.length >= 8
                && (b[0] & 0xff) == 0x89
                && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 13 && b[5] == 10 && b[6] == 26 && b[7] == 10) {
            return "png";
        }
        return null;
    }

    private ResponseStatusException badImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no es válida");
    }

    private BufferedImage processImage(BufferedImage originalImage) {
        BufferedImage resizedImage = resizeImageIfNeeded(originalImage);
        return convertToRGB(resizedImage);
    }

    private BufferedImage resizeImageIfNeeded(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Si ya cumple con los límites, no redimensionar
        if (originalWidth <= MAX_WIDTH && originalHeight <= MAX_HEIGHT) {
            return originalImage;
        }

        // Calcular nuevas dimensiones manteniendo la proporción
        double scale = Math.min(
                (double) MAX_WIDTH / originalWidth,
                (double) MAX_HEIGHT / originalHeight
        );

        int newWidth = (int) Math.round(originalWidth * scale);
        int newHeight = (int) Math.round(originalHeight * scale);

        return resizeWithHighQuality(originalImage, newWidth, newHeight);
    }

    private BufferedImage resizeWithHighQuality(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();

        try {
            // Configuración para máxima calidad
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

            g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        return resized;
    }

    private BufferedImage convertToRGB(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }

        BufferedImage rgbImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = rgbImage.createGraphics();
        try {
            // Fondo blanco para imágenes con transparencia
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2d.drawImage(image, 0, 0, null);
        } finally {
            g2d.dispose();
        }

        return rgbImage;
    }

    private byte[] compressToJPEG(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No hay escritor JPEG disponible en el sistema");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        // Configurar compresión si está disponible
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(COMPRESSION_QUALITY);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return outputStream.toByteArray();
    }

    private String generateCompressedFileName(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            return "compressed_image.jpg";
        }

        String cleanName = originalName.trim();
        int lastDotIndex = cleanName.lastIndexOf('.');

        if (lastDotIndex > 0 && lastDotIndex < cleanName.length() - 1) {
            return cleanName.substring(0, lastDotIndex) + "_compressed.jpg";
        }

        return cleanName + "_compressed.jpg";
    }
}
