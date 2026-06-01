package com.fesi.deadlinemate.global.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.multipart.MultipartFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() throws Exception {
        imageStorageService = new ImageStorageService();
        setField(imageStorageService, "uploadDir", tempDir.toString());
        setField(imageStorageService, "baseUrl", "http://localhost:8080");
        imageStorageService.init();
    }

    @Nested
    @DisplayName("upload()")
    class Upload {

        @Test
        @DisplayName("정상 이미지 업로드 시 http://localhost:8080/images/{directory}/{filename} 절대 URL을 반환한다")
        void upload_success() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "photo.jpg", "image/jpeg", new byte[100]);

            String result = imageStorageService.upload(file, "gatherings");

            assertThat(result).startsWith("http://localhost:8080/images/gatherings/");
            assertThat(result).endsWith(".jpg");
        }

        @Test
        @DisplayName("업로드된 파일이 실제로 디스크에 저장된다")
        void upload_fileExistsOnDisk() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "photo.png", "image/png", new byte[100]);

            String url = imageStorageService.upload(file, "users");

            String filename = URI.create(url).getPath().substring("/images/users/".length());
            Path savedPath = tempDir.resolve("users").resolve(filename);
            assertThat(Files.exists(savedPath)).isTrue();
        }

        @Test
        @DisplayName("확장자가 없는 원본 파일명이어도 업로드가 된다")
        void upload_noExtension() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "photo", "image/jpeg", new byte[100]);

            String result = imageStorageService.upload(file, "gatherings");

            assertThat(result).startsWith("http://localhost:8080/images/gatherings/");
        }

        @Test
        @DisplayName("빈 파일 업로드 시 IMAGE_EMPTY 예외가 발생한다")
        void upload_emptyFile() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "photo.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> imageStorageService.upload(file, "gatherings"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.IMAGE_EMPTY.getMessage());
        }

        @Test
        @DisplayName("5MB 초과 파일 업로드 시 IMAGE_TOO_LARGE 예외가 발생한다")
        void upload_tooLarge() {
            byte[] largeContent = new byte[5 * 1024 * 1024 + 1];
            MockMultipartFile file = new MockMultipartFile(
                    "image", "big.jpg", "image/jpeg", largeContent);

            assertThatThrownBy(() -> imageStorageService.upload(file, "gatherings"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.IMAGE_TOO_LARGE.getMessage());
        }

        @Test
        @DisplayName("허용되지 않는 Content-Type 업로드 시 IMAGE_INVALID_TYPE 예외가 발생한다")
        void upload_invalidType() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "document.pdf", "application/pdf", new byte[100]);

            assertThatThrownBy(() -> imageStorageService.upload(file, "gatherings"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.IMAGE_INVALID_TYPE.getMessage());
        }

        @Test
        @DisplayName("WebP 형식 파일은 정상적으로 업로드된다")
        void upload_webp() {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "photo.webp", "image/webp", new byte[100]);

            String result = imageStorageService.upload(file, "gatherings");

            assertThat(result).startsWith("http://localhost:8080/images/gatherings/");
        }
    }

    @Nested
    @DisplayName("uploadAll()")
    class UploadAll {

        @Test
        @DisplayName("여러 파일을 한 번에 업로드하면 각각의 경로 목록을 반환한다")
        void uploadAll_success() {
            List<MultipartFile> files = List.of(
                    new MockMultipartFile("f1", "a.jpg", "image/jpeg", new byte[100]),
                    new MockMultipartFile("f2", "b.png", "image/png", new byte[100])
            );

            List<String> results = imageStorageService.uploadAll(files, "gatherings");

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(p -> p.startsWith("http://localhost:8080/images/gatherings/"));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("업로드한 파일을 경로로 삭제하면 디스크에서 제거된다")
        void delete_success() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "photo.jpg", "image/jpeg", new byte[100]);
            String url = imageStorageService.upload(file, "gatherings");

            imageStorageService.delete(url);

            String filename = URI.create(url).getPath().substring("/images/gatherings/".length());
            Path savedPath = tempDir.resolve("gatherings").resolve(filename);
            assertThat(Files.exists(savedPath)).isFalse();
        }

        @Test
        @DisplayName("null 경로로 delete 호출 시 예외 없이 무시된다")
        void delete_nullPath() {
            assertThatCode(() -> imageStorageService.delete(null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("/images/ 로 시작하지 않는 경로는 무시된다")
        void delete_externalUrl() {
            assertThatCode(() ->
                    imageStorageService.delete("https://cdn.example.com/photo.jpg"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("존재하지 않는 파일 경로로 delete 호출 시 예외 없이 무시된다")
        void delete_notExists() {
            assertThatCode(() ->
                    imageStorageService.delete("/images/gatherings/nonexistent.jpg"))
                    .doesNotThrowAnyException();
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
