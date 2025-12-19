package com.Minyou.MINYOU.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Slf4j
public class S3Config {

    @Bean
    public S3Client s3Client() {
        try {
            Dotenv dotenv = Dotenv.load();
            
            String accessKey = dotenv.get("AWS_ACCESS_KEY_ID");
            String secretKey = dotenv.get("AWS_SECRET_ACCESS_KEY");
            String region = dotenv.get("AWS_REGION", "ap-northeast-2"); // 기본값: 서울 리전
            String bucketName = dotenv.get("AWS_S3_BUCKET_NAME");

            if (accessKey == null || accessKey.isEmpty() || accessKey.equals("your_access_key_id_here")) {
                log.error("AWS_ACCESS_KEY_ID가 설정되지 않았거나 기본값입니다.");
                throw new IllegalStateException(
                    "AWS_ACCESS_KEY_ID가 설정되지 않았습니다. ZIPCHACK-BE/.env 파일에 AWS_ACCESS_KEY_ID를 추가하세요."
                );
            }
            
            if (secretKey == null || secretKey.isEmpty() || secretKey.equals("your_secret_access_key_here")) {
                log.error("AWS_SECRET_ACCESS_KEY가 설정되지 않았거나 기본값입니다.");
                throw new IllegalStateException(
                    "AWS_SECRET_ACCESS_KEY가 설정되지 않았습니다. ZIPCHACK-BE/.env 파일에 AWS_SECRET_ACCESS_KEY를 추가하세요."
                );
            }
            
            if (bucketName == null || bucketName.isEmpty() || bucketName.equals("your_bucket_name_here")) {
                log.error("AWS_S3_BUCKET_NAME이 설정되지 않았거나 기본값입니다.");
                throw new IllegalStateException(
                    "AWS_S3_BUCKET_NAME이 설정되지 않았습니다. ZIPCHACK-BE/.env 파일에 AWS_S3_BUCKET_NAME을 추가하세요."
                );
            }

            log.info("AWS S3 설정 완료: 리전={}, 버킷={}", region, bucketName);

            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

            S3Client client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();
            
            log.info("S3Client 빈 생성 완료");
            return client;
        } catch (Exception e) {
            log.error("S3Client 빈 생성 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}
