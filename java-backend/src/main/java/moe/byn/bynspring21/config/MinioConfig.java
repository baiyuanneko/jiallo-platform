package moe.byn.bynspring21.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置类
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /**
     * MinIO 服务地址
     */
    private String endpoint;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 秘密密钥
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 是否自动创建存储桶
     */
    private boolean autoCreateBucket = true;

    /**
     * 创建 MinioClient Bean
     */
    @Bean
    public MinioClient minioClient() {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 如果配置了自动创建存储桶，检查并创建
            if (autoCreateBucket) {
                boolean exists = client.bucketExists(BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());
                if (!exists) {
                    client.makeBucket(MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build());
                    log.info("MinIO bucket '{}' created successfully", bucketName);
                } else {
                    log.info("MinIO bucket '{}' already exists", bucketName);
                }
            }

            log.info("MinIO client initialized successfully");
            return client;
        } catch (Exception e) {
            log.error("Failed to initialize MinIO client", e);
            throw new RuntimeException("Failed to initialize MinIO client", e);
        }
    }
}
