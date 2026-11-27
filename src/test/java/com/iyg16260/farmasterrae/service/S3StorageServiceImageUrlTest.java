package com.iyg16260.farmasterrae.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceImageUrlTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3StorageService service;

    @BeforeEach
    void setUp() {
        service = new S3StorageService();
        ReflectionTestUtils.setField(service, "s3Client", s3Client);
        ReflectionTestUtils.setField(service, "s3Presigner", s3Presigner);
        ReflectionTestUtils.setField(service, "bucketName", "farmas-bucket");
        ReflectionTestUtils.setField(service, "s3Endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(service, "s3PublicEndpoint", "http://localhost:9000");
    }

    @Test
    void extractsOnlyProductImageKeysFromStoredDevelopmentAndProductionUrls() {
        assertThat(service.extractKeyFromUrl("product-images/item.jpg"))
                .isEqualTo("product-images/item.jpg");
        assertThat(service.extractKeyFromUrl("http://localhost:9000/farmas-bucket/product-images/item.jpg"))
                .isEqualTo("product-images/item.jpg");
        assertThat(service.extractKeyFromUrl("https://farmas-bucket.s3.eu-west-1.amazonaws.com/product-images/item.jpg"))
                .isEqualTo("product-images/item.jpg");
        assertThat(service.extractKeyFromUrl("https://s3.eu-west-1.amazonaws.com/farmas-bucket/product-images/item.jpg"))
                .isEqualTo("product-images/item.jpg");
    }

    @Test
    void rejectsUntrustedUrlsSpoofedHostsTraversalAndForeignPrefixes() {
        assertThat(service.extractKeyFromUrl("https://example.com/product-images/item.jpg")).isNull();
        assertThat(service.extractKeyFromUrl("https://example.com/farmas-bucket/product-images/item.jpg")).isNull();
        assertThat(service.extractKeyFromUrl("https://farmas-bucket.s3.attacker.com/product-images/item.jpg")).isNull();
        assertThat(service.extractKeyFromUrl("other-folder/item.jpg")).isNull();
        assertThat(service.extractKeyFromUrl("product-images/../secret.txt")).isNull();
        assertThat(service.extractKeyFromUrl("https://farmas-bucket.s3.eu-west-1.amazonaws.com/product-images/%2e%2e/secret.txt")).isNull();
        assertThat(service.resolveImageUrl("https://example.com/product-images/item.jpg"))
                .isEqualTo("/assets/img/placeholder.svg");
    }

    @Test
    void cachesPresignedUrlsAndEvictsEntryWhenDeleted() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://farmas-bucket.s3.eu-west-1.amazonaws.com/product-images/item.jpg?sig=1").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String first = service.resolveImageUrl("product-images/item.jpg");
        String second = service.resolveImageUrl("product-images/item.jpg");

        assertThat(first).isEqualTo(second);
        verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));

        // Deleting by full prod URL extracts the exact key and evicts the cache entry
        service.deleteFile("https://farmas-bucket.s3.eu-west-1.amazonaws.com/product-images/item.jpg");
        ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().bucket()).isEqualTo("farmas-bucket");
        assertThat(deleteCaptor.getValue().key()).isEqualTo("product-images/item.jpg");

        service.resolveImageUrl("product-images/item.jpg");
        verify(s3Presigner, times(2)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void s3SigningExceptionFallsBackToPlaceholder() {
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(S3Exception.builder().message("S3 unavailable").statusCode(503).build());

        assertThat(service.resolveImageUrl("product-images/item.jpg"))
                .isEqualTo("/assets/img/placeholder.svg");
    }

    @Test
    void uploadFileStoresJpegKeyUnderFolderWithExplicitImageJpegContentType() {
        MockMultipartFile file = new MockMultipartFile("imageFile", "photo.jpg", "text/html", new byte[]{1, 2, 3});

        String key = service.uploadFile(file, "product-images");

        assertThat(key).startsWith("product-images/").endsWith(".jpg");
        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));
        assertThat(putCaptor.getValue().bucket()).isEqualTo("farmas-bucket");
        assertThat(putCaptor.getValue().key()).isEqualTo(key);
        assertThat(putCaptor.getValue().contentType()).isEqualTo("image/jpeg");
    }
}
