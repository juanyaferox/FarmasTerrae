package com.iyg16260.farmasterrae.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value ("${aws.accessKey}")
    private String accessKey; // únicamente para desarrollo con MINIO

    @Value ("${aws.secretKey}")
    private String secretKey;  // únicamente para desarrollo con MINIO

    @Value ("${aws.region}")
    private String region;

    @Value ("${aws.s3.endpoint:}") // opcional
    private String endpoint;

    @Value ("${app.env:dev}") // por defecto desarrollo
    private String appEnv;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .overrideConfiguration(ClientOverrideConfiguration.builder().build());
        // En modo desarrollo usamos credenciales estáticas
        if ("dev".equalsIgnoreCase(appEnv)) {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(awsCreds));

            if (!endpoint.isBlank()) {
                builder.endpointOverride(URI.create(endpoint));
            }
        } else {
            // En producción confiamos en el rol de la instancia
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        if ("dev".equalsIgnoreCase(appEnv)) {
            AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
            S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(creds))
                    .serviceConfiguration(
                            S3Configuration.builder()
                                    .pathStyleAccessEnabled(true)
                                    .build()
                    );

            if (!endpoint.isBlank()) {
                // En caso de usar docker compose
                String fixedEndpoint = endpoint.replace("http://minio:9000",
                        "http://localhost:9000");
                presignerBuilder.endpointOverride(URI.create(fixedEndpoint));
            }
            return presignerBuilder.build();
        } else {
            // En prod: también usamos DefaultCredentialsProvider para el presigner
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .serviceConfiguration(
                            S3Configuration.builder()
                                    .pathStyleAccessEnabled(true)
                                    .build()
                    )
                    .build();
        }
    }
}