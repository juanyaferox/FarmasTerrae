package com.iyg16260.farmasterrae.service;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
import java.time.Duration;
import java.util.UUID;

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

    @Value ("${app.env:dev}")
    private String appEnv;

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
            if ("dev".equalsIgnoreCase(appEnv) && s3Endpoint != null && !s3Endpoint.isBlank()) {
                return s3Endpoint + "/" + bucketName + "/" + key;
            } else {
                // URL pública de AWS S3 (si el bucket es público o tiene políticas adecuadas)
                return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
            }

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar el archivo", e);
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al subir a S3: " + e.getMessage(), e);
        }
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al eliminar de S3: " + e.getMessage(), e);
        } catch (Exception e) {
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar URL firmada: " + e.getMessage(), e);
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

        // Si es URL completa, extraer la clave
        if (fileUrl.contains(bucketName)) {
            return fileUrl.substring(fileUrl.indexOf(bucketName) + bucketName.length() + 1);
        }

        return fileUrl;
    }
}