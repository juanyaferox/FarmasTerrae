package com.iyg16260.farmasterrae.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 clients for AWS S3 (default) or an S3-compatible server such as MinIO ({@code aws.s3.endpoint}).
 * <ul>
 *     <li>{@code aws.s3.static-credentials=true} uses {@code aws.accessKey}/{@code aws.secretKey};
 *     otherwise the AWS default credentials chain (e.g. an EC2/ECS IAM role) is used.</li>
 *     <li>{@code aws.s3.endpoint} switches to path-style requests against that server.</li>
 *     <li>{@code aws.s3.public-endpoint} is the browser-facing address used for presigned URLs
 *     (defaults to {@code aws.s3.endpoint}).</li>
 * </ul>
 */
@Configuration
public class S3Config {

    private static final S3Configuration PATH_STYLE = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build();

    private final String region;
    private final String endpoint;
    private final String publicEndpoint;
    private final boolean staticCredentials;
    private final String accessKey;
    private final String secretKey;

    public S3Config(@Value ("${aws.region}") String region,
                    @Value ("${aws.s3.endpoint:}") String endpoint,
                    @Value ("${aws.s3.public-endpoint:}") String publicEndpoint,
                    @Value ("${aws.s3.static-credentials:false}") boolean staticCredentials,
                    @Value ("${aws.accessKey:}") String accessKey,
                    @Value ("${aws.secretKey:}") String secretKey) {
        this.region = region;
        this.endpoint = StringUtils.hasText(endpoint) ? endpoint.trim() : "";
        this.publicEndpoint = StringUtils.hasText(publicEndpoint) ? publicEndpoint.trim() : this.endpoint;
        this.staticCredentials = staticCredentials;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (!endpoint.isEmpty()) {
            // S3-compatible server (MinIO): path-style URLs, and only the checksums an operation
            // requires (the SDK's default CRC checksums are not supported by every server)
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(PATH_STYLE)
                    .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                    .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        }
        // Real AWS S3 keeps the default virtual-hosted-style addressing
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (!endpoint.isEmpty()) {
            // Presigned URLs are opened by the browser, so sign them for the public address
            // (e.g. http://localhost:9000 instead of the Docker-internal http://minio:9000)
            builder.endpointOverride(URI.create(publicEndpoint))
                    .serviceConfiguration(PATH_STYLE);
        }
        return builder.build();
    }

    /**
     * Returns an independent provider for each client: the deprecated
     * {@code DefaultCredentialsProvider.create()} singleton could be closed by one client while
     * the other one still uses it.
     */
    AwsCredentialsProvider credentialsProvider() {
        if (staticCredentials) {
            if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
                throw new IllegalStateException(
                        "aws.s3.static-credentials=true requires aws.accessKey and aws.secretKey");
            }
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.builder().build();
    }
}
