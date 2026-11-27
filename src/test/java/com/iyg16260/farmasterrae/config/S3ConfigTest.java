package com.iyg16260.farmasterrae.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3ConfigTest {

    private static final String BUCKET = "farmasterrae";
    private static final String KEY = "product-images/photo.jpg";

    private static String objectUrl(S3Client client) {
        return client.utilities().getUrl(request -> request.bucket(BUCKET).key(KEY)).toString();
    }

    private static String presignedUrl(S3Presigner presigner) {
        return presigner.presignGetObject(request -> request
                        .signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(get -> get.bucket(BUCKET).key(KEY)))
                .url()
                .toString();
    }

    @Test
    void awsS3UsesVirtualHostedStyleAndTheDefaultCredentialsChain() {
        S3Config config = new S3Config("eu-west-1", "", "", false, "", "");

        try (S3Client client = config.s3Client()) {
            S3ServiceClientConfiguration configuration = client.serviceClientConfiguration();

            assertThat(objectUrl(client)).isEqualTo("https://farmasterrae.s3.eu-west-1.amazonaws.com/" + KEY);
            assertThat(configuration.endpointOverride()).isEmpty();
            assertThat(configuration.credentialsProvider()).isInstanceOf(DefaultCredentialsProvider.class);
            assertThat(configuration.requestChecksumCalculation()).isEqualTo(RequestChecksumCalculation.WHEN_SUPPORTED);
            assertThat(configuration.responseChecksumValidation()).isEqualTo(ResponseChecksumValidation.WHEN_SUPPORTED);
        }
    }

    @Test
    void staticKeysAgainstAwsKeepVirtualHostedPresignedUrls() {
        S3Config config = new S3Config("eu-west-1", "", "", true, "AKIATESTKEY", "test-secret");

        try (S3Client client = config.s3Client(); S3Presigner presigner = config.s3Presigner()) {
            assertThat(objectUrl(client)).isEqualTo("https://farmasterrae.s3.eu-west-1.amazonaws.com/" + KEY);
            assertThat(presignedUrl(presigner))
                    .startsWith("https://farmasterrae.s3.eu-west-1.amazonaws.com/" + KEY + "?")
                    .contains("X-Amz-Credential=AKIATESTKEY%2F");
        }
    }

    @Test
    void customEndpointUsesPathStyleStaticKeysAndOnlyRequiredChecksums() {
        S3Config config = new S3Config("us-east-1", "http://minio:9000", "http://localhost:9000",
                true, "minio-user", "minio-secret");

        try (S3Client client = config.s3Client(); S3Presigner presigner = config.s3Presigner()) {
            S3ServiceClientConfiguration configuration = client.serviceClientConfiguration();

            assertThat(objectUrl(client)).isEqualTo("http://minio:9000/farmasterrae/" + KEY);
            assertThat(configuration.endpointOverride()).contains(URI.create("http://minio:9000"));
            assertThat(configuration.requestChecksumCalculation()).isEqualTo(RequestChecksumCalculation.WHEN_REQUIRED);
            assertThat(configuration.responseChecksumValidation()).isEqualTo(ResponseChecksumValidation.WHEN_REQUIRED);
            assertThat(configuration.credentialsProvider()).isInstanceOf(StaticCredentialsProvider.class);
            assertThat(configuration.credentialsProvider().resolveIdentity().join().accessKeyId()).isEqualTo("minio-user");

            // Browsers cannot resolve the Docker-internal host, so URLs are signed for the public one
            assertThat(presignedUrl(presigner))
                    .startsWith("http://localhost:9000/farmasterrae/" + KEY + "?")
                    .contains("X-Amz-Credential=minio-user%2F");
        }
    }

    @Test
    void presignedUrlsFallBackToTheEndpointWithoutAPublicOne() {
        S3Config config = new S3Config("us-east-1", "http://localhost:9000", " ", true, "minio-user", "minio-secret");

        try (S3Presigner presigner = config.s3Presigner()) {
            assertThat(presignedUrl(presigner)).startsWith("http://localhost:9000/farmasterrae/" + KEY + "?");
        }
    }

    @Test
    void usesTheDefaultCredentialsChainUnlessStaticKeysAreEnabled() {
        S3Config config = new S3Config("us-east-1", "http://minio:9000", "", false, "ignored", "ignored");

        AwsCredentialsProvider first = config.credentialsProvider();

        assertThat(first).isInstanceOf(DefaultCredentialsProvider.class);
        assertThat(config.credentialsProvider()).isNotSameAs(first);
    }

    @Test
    void staticCredentialsRequireBothKeys() {
        S3Config config = new S3Config("us-east-1", "http://minio:9000", "", true, "minio-user", "");

        assertThatThrownBy(config::credentialsProvider)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aws.secretKey");
    }
}
