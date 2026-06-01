package com.fesi.deadlinemate.global.common;

import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @Value("${image.upload-dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 디렉토리를 생성할 수 없습니다: " + uploadDir, e);
        }
    }

    public String upload(MultipartFile file, String directory) {
        validateFile(file);

        String filename = UUID.randomUUID() + getExtension(file.getOriginalFilename());
        Path dirPath = Paths.get(uploadDir, directory);
        Path filePath = dirPath.resolve(filename);

        try {
            Files.createDirectories(dirPath);
            file.transferTo(filePath.toAbsolutePath());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        return baseUrl + "/images/" + directory + "/" + filename;
    }

    public List<String> uploadAll(List<MultipartFile> files, String directory) {
        return files.stream()
                .map(file -> upload(file, directory))
                .toList();
    }

    public void delete(String imagePath) {
        if (imagePath == null) {
            return;
        }

        String urlPath = imagePath;
        if (imagePath.startsWith("http")) {
            try {
                urlPath = URI.create(imagePath).getPath();
            } catch (IllegalArgumentException e) {
                return;
            }
        }

        if (!urlPath.startsWith("/images/")) {
            return;
        }

        String relativePath = urlPath.substring("/images/".length());
        Path filePath = Paths.get(uploadDir, relativePath);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 파일 삭제 실패는 무시 (로그로 처리 가능)
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_TYPE);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
