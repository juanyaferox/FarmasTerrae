package com.iyg16260.farmasterrae.utils;

import com.iyg16260.farmasterrae.service.ImageCompressionService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class ImageCompressionMain {

    private static final ImageCompressionService compressionService = new ImageCompressionService();
    private static final DecimalFormat df = new DecimalFormat("#.##");

    // Formatos de imagen soportados
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".bmp", ".gif", ".tiff", ".webp"
    );

    // Añadir carpeta de origen y de destino en run configuration (carpeta_origen carpeta_destino)
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ImageCompressionMain <carpeta_entrada> <carpeta_salida>");
            System.out.println("Ejemplo: java ImageCompressionMain ./input ./output");
            return;
        }

        String inputFolder = args[0];
        String outputFolder = args[1];

        try {
            processImagesInFolder(inputFolder, outputFolder);
        } catch (Exception e) {
            System.err.println("Error procesando imágenes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa todas las imágenes de una carpeta
     */
    private static void processImagesInFolder(String inputFolder, String outputFolder) throws IOException {
        File inputDir = new File(inputFolder);
        File outputDir = new File(outputFolder);

        // Validar carpeta de entrada
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new IllegalArgumentException("La carpeta de entrada no existe: " + inputFolder);
        }

        // Crear carpeta de salida si no existe
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (!created) {
                throw new IOException("No se pudo crear la carpeta de salida: " + outputFolder);
            }
        }

        // Obtener archivos de imagen
        File[] imageFiles = inputDir.listFiles(file ->
                file.isFile() && isImageFile(file.getName())
        );

        if (imageFiles == null || imageFiles.length == 0) {
            System.out.println("No se encontraron imágenes en la carpeta: " + inputFolder);
            return;
        }

        System.out.println("=".repeat(80));
        System.out.println("COMPRESIÓN DE IMÁGENES");
        System.out.println("=".repeat(80));
        System.out.println("Carpeta entrada: " + inputDir.getAbsolutePath());
        System.out.println("Carpeta salida:  " + outputDir.getAbsolutePath());
        System.out.println("Imágenes encontradas: " + imageFiles.length);
        System.out.println("=".repeat(80));

        long totalOriginalSize = 0;
        long totalCompressedSize = 0;
        int processedCount = 0;
        int errorCount = 0;

        // Procesar cada imagen
        for (File imageFile : imageFiles) {
            try {
                System.out.printf("📸 Procesando: %s%n", imageFile.getName());

                // Convertir File a MultipartFile
                MultipartFile multipartFile = fileToMultipartFile(imageFile);

                // Comprimir imagen
                MultipartFile compressedImage = compressionService.compressImage(multipartFile);

                // Guardar imagen comprimida
                String outputFileName = getOutputFileName(imageFile.getName());
                File outputFile = new File(outputDir, outputFileName);
                saveMultipartFile(compressedImage, outputFile);

                // Estadísticas
                long originalSize = imageFile.length();
                long compressedSize = compressedImage.getSize();
                double compressionRatio = (1.0 - (double) compressedSize / originalSize) * 100;

                totalOriginalSize += originalSize;
                totalCompressedSize += compressedSize;
                processedCount++;

                System.out.printf("   ✅ %s → %s (-%s%%)%n",
                        formatFileSize(originalSize),
                        formatFileSize(compressedSize),
                        df.format(compressionRatio)
                );

            } catch (Exception e) {
                errorCount++;
                System.out.printf("   ❌ Error: %s%n", e.getMessage());
            }
        }

        // Resumen final
        printSummary(processedCount, errorCount, totalOriginalSize, totalCompressedSize);
    }

    /**
     * Convierte un File en MultipartFile
     */
    private static MultipartFile fileToMultipartFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileContent = fis.readAllBytes();
            String contentType = getContentType(file.getName());

            return new MockMultipartFile(
                    file.getName(),
                    file.getName(),
                    contentType,
                    fileContent
            );
        }
    }

    /**
     * Guarda un MultipartFile en disco
     */
    private static void saveMultipartFile(MultipartFile multipartFile, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(multipartFile.getBytes());
        }
    }

    /**
     * Verifica si un archivo es una imagen soportada
     */
    private static boolean isImageFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return SUPPORTED_FORMATS.stream().anyMatch(lowerName::endsWith);
    }

    /**
     * Obtiene el content type basado en la extensión
     */
    private static String getContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".gif")) return "image/gif";
        if (lowerName.endsWith(".bmp")) return "image/bmp";
        if (lowerName.endsWith(".tiff")) return "image/tiff";
        if (lowerName.endsWith(".webp")) return "image/webp";
        return "image/jpeg"; // Por defecto
    }

    /**
     * Genera el nombre del archivo de salida
     */
    private static String getOutputFileName(String originalName) {
        int lastDotIndex = originalName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return originalName.substring(0, lastDotIndex) + "_compressed.jpg";
        }
        return originalName + "_compressed.jpg";
    }

    /**
     * Formatea el tamaño de archivo
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return df.format(bytes / 1024.0) + " KB";
        return df.format(bytes / (1024.0 * 1024.0)) + " MB";
    }

    /**
     * Imprime el resumen final
     */
    private static void printSummary(int processed, int errors, long originalTotal, long compressedTotal) {
        System.out.println("=".repeat(80));
        System.out.println("RESUMEN FINAL");
        System.out.println("=".repeat(80));
        System.out.printf("✅ Imágenes procesadas: %d%n", processed);
        System.out.printf("❌ Errores: %d%n", errors);
        System.out.printf("📊 Tamaño original total: %s%n", formatFileSize(originalTotal));
        System.out.printf("📊 Tamaño comprimido total: %s%n", formatFileSize(compressedTotal));

        if (originalTotal > 0) {
            double totalSavings = (1.0 - (double) compressedTotal / originalTotal) * 100;
            long savedBytes = originalTotal - compressedTotal;
            System.out.printf("💾 Espacio ahorrado: %s (%.1f%%)%n",
                    formatFileSize(savedBytes), totalSavings);
        }

        System.out.println("=".repeat(80));
        System.out.println("🎉 ¡Proceso completado!");
    }
}
