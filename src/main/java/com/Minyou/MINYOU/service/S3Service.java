package com.Minyou.MINYOU.service;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private static final Dotenv dotenv = Dotenv.load();
    
    private String getBucketName() {
        String bucketName = dotenv.get("AWS_S3_BUCKET_NAME");
        if (bucketName == null || bucketName.isEmpty()) {
            log.error("AWS_S3_BUCKET_NAME이 설정되지 않았습니다. .env 파일을 확인하세요.");
            throw new IllegalStateException("AWS_S3_BUCKET_NAME이 설정되지 않았습니다.");
        }
        return bucketName;
    }
    
    private String getRegion() {
        return dotenv.get("AWS_REGION", "ap-northeast-2");
    }

    /**
     * 이미지 업로드
     * @param file 업로드할 파일
     * @param folder S3 폴더 경로 (예: "listings", "buildings", "users")
     * @return 업로드된 이미지의 S3 URL
     */
    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일 이름이 없습니다.");
        }

        String extension = getFileExtension(originalFilename);
        if (!isValidImageExtension(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, gif, webp만 허용)");
        }

        // 파일 크기 검증 (10MB 제한)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }

        try {
            String bucketName = getBucketName();
            String region = getRegion();
            
            // 고유한 파일명 생성
            String fileName = folder + "/" + UUID.randomUUID().toString() + "." + extension;
            
            log.debug("S3 업로드 시작: 버킷={}, 파일명={}, 크기={}", bucketName, fileName, file.getSize());
            
            // S3에 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // S3 URL 생성
            String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
            
            log.info("이미지 업로드 성공: {}", imageUrl);
            return imageUrl;

        } catch (S3Exception e) {
            log.error("S3 업로드 실패: 에러코드={}, 메시지={}", e.awsErrorDetails().errorCode(), e.getMessage(), e);
            throw new RuntimeException("S3 업로드 실패: " + e.awsErrorDetails().errorCode() + " - " + e.getMessage());
        } catch (IOException e) {
            log.error("이미지 업로드 실패 (IO 오류): {}", e.getMessage(), e);
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("이미지 업로드 실패 (예상치 못한 오류): {}", e.getMessage(), e);
            throw new RuntimeException("이미지 업로드 중 예상치 못한 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 이미지 삭제
     * @param imageUrl 삭제할 이미지의 S3 URL
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            String bucketName = getBucketName();
            
            // URL에서 키 추출
            String key = extractKeyFromUrl(imageUrl);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("이미지 삭제 성공: {}", imageUrl);

        } catch (Exception e) {
            log.error("이미지 삭제 실패: {}", e.getMessage(), e);
            // 삭제 실패해도 예외를 던지지 않음 (이미 삭제되었을 수 있음)
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 유효한 이미지 확장자 확인
     */
    private boolean isValidImageExtension(String extension) {
        return extension.equals("jpg") || extension.equals("jpeg") || 
               extension.equals("png") || extension.equals("gif") || 
               extension.equals("webp");
    }

    /**
     * S3 URL에서 키 추출
     */
    private String extractKeyFromUrl(String url) {
        // https://bucket-name.s3.region.amazonaws.com/folder/filename 형식에서 키 추출
        try {
            URL urlObj = new URL(url);
            String path = urlObj.getPath();
            // 첫 번째 '/' 제거
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.warn("URL 파싱 실패: {}", url);
            // URL 형식이 아닌 경우 그대로 반환 (이미 키일 수 있음)
            return url;
        }
    }
}
