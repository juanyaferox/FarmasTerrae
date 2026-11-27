package com.iyg16260.farmasterrae.service;

import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Instant;

@Slf4j
@Service
public class S3StorageService {
    private static final String IMAGE_PREFIX = "product-images/";
    private static final String PLACEHOLDER = "/assets/img/placeholder.svg";
    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(30);
    private final Map<String, CachedUrl> signedUrls = new LinkedHashMap<>(128, 0.75f, true);
    private record CachedUrl(String url, Instant expiresAt) { }

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Value ("${aws.s3.bucket-name}")
    private String bucketName;

    @Value ("${aws.s3.endpoint:}")
    private String s3Endpoint;

    /** Browser-facing address of a custom endpoint (e.g. http://localhost:9000 for Docker MinIO). */
    @Value ("${aws.s3.public-endpoint:}")
    private String s3PublicEndpoint;

    /**
     * Sube un archivo al bucket de S3 y devuelve la URL
     *
     * @param file   El archivo a subir
     * @param folder La carpeta donde se guardará (opcional)
     * @return La URL del archivo subido
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // Validar archivo
            if (file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
            }

            // Generar nombre único para evitar colisiones
            String fileName = UUID.randomUUID() + ".jpg";

            // Añadir la carpeta si se especificó
            String key = folder != null && !folder.isEmpty()
                    ? folder + "/" + fileName
                    : fileName;

            // Configurar la petición de subida
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("image/jpeg")
                    .build();

            // Subir el archivo
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Construir y devolver la URL
            return key;

        } catch (IOException e) {
            log.error("Error al procesar archivo para S3", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar el archivo", e);
        } catch (S3Exception e) {
            log.error("Error al subir archivo a S3", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al subir el archivo", e);
        }
    }

    /**
     * Elimina un archivo de S3 basado en su URL
     *
     * @param fileUrl La URL completa del archivo a eliminar
     */
    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key == null) return;

            // Configurar la petición de eliminación
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Eliminar el archivo
            s3Client.deleteObject(deleteRequest);
            synchronized (signedUrls) { signedUrls.remove(key); }

        } catch (S3Exception e) {
            log.error("Error al eliminar archivo de S3", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al eliminar el archivo", e);
        } catch (Exception e) {
            log.error("Error al procesar URL de archivo", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar la URL del archivo", e);
        }
    }

    /**
     * Genera una URL firmada para descargar un archivo de S3
     *
     * @param key      La clave del objeto en S3
     * @param duration La duración de validez de la URL firmada
     * @return La URL firmada
     */
    public String generatePresignedUrl(String key, Duration duration) {
        try {
            if (key == null || !key.startsWith(IMAGE_PREFIX) || key.contains("..") || key.contains("\\")) {
                throw new IllegalArgumentException("Invalid product image key");
            }
            // Construye la petición de descarga
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Construye la petición de prefirmado
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            // Genera y devuelve la URL firmada
            return s3Presigner.presignGetObject(presignRequest).url().toString();

        } catch (S3Exception e) {
            log.error("Error al generar URL firmada de S3", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar URL firmada", e);
        }
    }

    public String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        if (!fileUrl.startsWith("http")) {
            return validImageKey(fileUrl) ? fileUrl : null;
        }

        try {
            URI uri = new URI(fileUrl);
            String host = uri.getHost();
            String path = uri.getPath();
            if (path == null) return null;
            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

            if (host != null && (host.equals(bucketName + ".s3.amazonaws.com")
                    || (host.startsWith(bucketName + ".s3.") && host.endsWith(".amazonaws.com")))) {
                return validImageKey(normalizedPath) ? normalizedPath : null;
            }

            if (host != null && (host.equals("s3.amazonaws.com")
                    || (host.startsWith("s3.") && host.endsWith(".amazonaws.com")))) {
                String[] parts = normalizedPath.split("/", 2);
                if (parts.length == 2 && parts[0].equals(bucketName)) {
                    return validImageKey(parts[1]) ? parts[1] : null;
                }
            }

            String[] parts = normalizedPath.split("/", 2);
            if (isConfiguredEndpoint(uri) && parts.length == 2 && parts[0].equals(bucketName)) {
                return validImageKey(parts[1]) ? parts[1] : null;
            }

        } catch (URISyntaxException e) {
            log.warn("Could not interpret image URL", e);
            return null;
        }

        return null;
    }

    public String resolveImageUrl(String imagePath) {
        if (!StringUtils.hasText(imagePath)) return PLACEHOLDER;
        String key = extractKeyFromUrl(imagePath);
        if (key == null) return PLACEHOLDER;
        Instant now = Instant.now();
        synchronized (signedUrls) {
            CachedUrl cached = signedUrls.get(key);
            if (cached != null && cached.expiresAt().isAfter(now)) return cached.url();
        }
        try {
            String url = generatePresignedUrl(key, PRESIGN_DURATION);
            synchronized (signedUrls) {
                signedUrls.put(key, new CachedUrl(url, now.plus(PRESIGN_TTL)));
                while (signedUrls.size() > 1000) signedUrls.remove(signedUrls.keySet().iterator().next());
            }
            return url;
        } catch (Exception e) {
            log.warn("Could not sign product image {}; using placeholder", key, e);
            return PLACEHOLDER;
        }
    }

    private boolean validImageKey(String key) {
        return key != null && key.startsWith(IMAGE_PREFIX) && key.length() > IMAGE_PREFIX.length()
                && !key.contains("..") && !key.contains("\\") && !key.startsWith("/");
    }

    private boolean isConfiguredEndpoint(URI uri) {
        return matchesEndpoint(uri, s3Endpoint) || matchesEndpoint(uri, s3PublicEndpoint);
    }

    private boolean matchesEndpoint(URI candidate, String endpoint) {
        if (!StringUtils.hasText(endpoint)) return false;
        try {
            URI configured = URI.create(endpoint);
            return configured.getHost() != null && configured.getHost().equalsIgnoreCase(candidate.getHost())
                    && configured.getPort() == candidate.getPort();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
