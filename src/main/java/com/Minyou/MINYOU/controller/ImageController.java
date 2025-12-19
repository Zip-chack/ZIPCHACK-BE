package com.Minyou.MINYOU.controller;

import com.Minyou.MINYOU.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final S3Service s3Service;

    /**
     * 이미지 업로드
     * @param file 업로드할 이미지 파일
     * @param folder 폴더 경로 (선택사항, 기본값: "uploads")
     * @return 업로드된 이미지 URL
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "uploads") String folder) {
        try {
            if (file == null || file.isEmpty()) {
                log.warn("이미지 업로드 실패: 파일이 비어있습니다.");
                return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
            }

            log.info("이미지 업로드 요청: 파일명={}, 크기={} bytes, 타입={}, 폴더={}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType(), folder);

            String imageUrl = s3Service.uploadImage(file, folder);

            Map<String, String> response = new HashMap<>();
            response.put("url", imageUrl);
            response.put("message", "이미지 업로드 성공");

            log.info("이미지 업로드 성공: URL={}", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("이미지 업로드 실패 (잘못된 요청): {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("이미지 업로드 실패 (설정 오류): {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "서버 설정 오류: " + e.getMessage()));
        } catch (Exception e) {
            log.error("이미지 업로드 실패: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "이미지 업로드 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 이미지 삭제
     * @param imageUrl 삭제할 이미지의 URL
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteImage(@RequestParam("url") String imageUrl) {
        try {
            log.info("이미지 삭제 요청: {}", imageUrl);
            s3Service.deleteImage(imageUrl);
            return ResponseEntity.ok(Map.of("message", "이미지 삭제 성공"));
        } catch (Exception e) {
            log.error("이미지 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "이미지 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
