package com.iyg16260.farmasterrae.service;

import org.apache.commons.io.FilenameUtils;
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

@Slf4j
@Service
public class S3StorageService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Value ("${aws.s3.bucket-name}")
    private String bucketName;

    @Value ("${aws.region}")
    private String region;

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
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + "." + extension;

            // Añadir la carpeta si se especificó
            String key = folder != null && !folder.isEmpty()
                    ? folder + "/" + fileName
                    : fileName;

            // Configurar la petición de subida
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            // Subir el archivo
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Construir y devolver la URL
            return buildObjectUrl(key);

        } catch (IOException e) {
            log.error("Error al procesar archivo para S3", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar el archivo", e);
        } catch (S3Exception e) {
            log.error("Error al subir archivo a S3", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al subir el archivo", e);
        }
    }

    /**
     * URL stored for an uploaded object: path-style on the public address of a custom endpoint
     * (MinIO), otherwise the virtual-hosted AWS S3 URL. Images are always served through presigned
     * URLs, so the bucket can stay private.
     */
    private String buildObjectUrl(String key) {
        if (StringUtils.hasText(s3Endpoint)) {
            String base = StringUtils.hasText(s3PublicEndpoint) ? s3PublicEndpoint : s3Endpoint;
            return StringUtils.trimTrailingCharacter(base.trim(), '/') + "/" + bucketName + "/" + key;
        }
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }

    /**
     * Elimina un archivo de S3 basado en su URL
     *
     * @param fileUrl La URL completa del archivo a eliminar
     */
    public void deleteFile(String fileUrl) {
        try {
            // Extraer la clave del archivo desde la URL
            String key = fileUrl.substring(fileUrl.indexOf(bucketName) + bucketName.length() + 1);

            // Configurar la petición de eliminación
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Eliminar el archivo
            s3Client.deleteObject(deleteRequest);

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

        // Si ya es solo la clave
        if (!fileUrl.startsWith("http")) {
            return fileUrl;
        }

        try {
            URI uri = new URI(fileUrl);
            String host = uri.getHost();
            String path = uri.getPath();
            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

            if (host != null && host.startsWith(bucketName + ".s3.")) {
                return normalizedPath;
            }

            if (host != null && (host.equals("s3.amazonaws.com")
                    || (host.startsWith("s3.") && host.endsWith(".amazonaws.com")))) {
                String[] parts = normalizedPath.split("/", 2);
                if (parts.length == 2 && parts[0].equals(bucketName)) {
                    return parts[1];
                }
            }

            String[] parts = normalizedPath.split("/", 2);
            if (parts.length == 2 && parts[0].equals(bucketName)) {
                return parts[1];
            }

        } catch (URISyntaxException e) {
            log.error("Error al interpretar URL de imagen", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener la imagen");
        }

        return fileUrl;
    }
}