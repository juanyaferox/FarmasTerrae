package com.iyg16260.farmasterrae.service;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
public class ImageCompressionService {

    // Configuración recomendada
    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 800;
    private static final float COMPRESSION_QUALITY = 0.85f;

    /**
     * Comprime una imagen y la devuelve como MultipartFile en formato JPEG
     *
     * @param originalFile La imagen original a comprimir
     * @return MultipartFile con la imagen comprimida en formato JPEG
     * @throws IOException              Si hay errores en el procesamiento de la imagen
     * @throws IllegalArgumentException Si el archivo no es válido
     */
    public MultipartFile compressImage(MultipartFile originalFile) throws IOException {
        validateFile(originalFile);

        BufferedImage originalImage = ImageIO.read(originalFile.getInputStream());
        if (originalImage == null) {
            throw new IOException("No se pudo leer la imagen. Formato no soportado.");
        }

        // Procesar la imagen
        BufferedImage processedImage = processImage(originalImage);
        byte[] compressedData = compressToJPEG(processedImage);
        String compressedName = generateCompressedFileName(originalFile.getOriginalFilename());

        // Usar MockMultipartFile que es más simple y funcional
        return new MockMultipartFile(
                "imageFile",
                compressedName,
                "image/jpeg",
                compressedData
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o es nulo");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo no es una imagen válida");
        }

        // Validar tamaño máximo (ej: 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("La imagen es demasiado grande (máximo 10MB)");
        }
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