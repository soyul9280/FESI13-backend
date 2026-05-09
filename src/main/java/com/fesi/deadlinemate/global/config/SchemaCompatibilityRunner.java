package com.fesi.deadlinemate.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 엔티티 필드 제거로 인해 DB에 남은 레거시 컬럼을 정리하는 마이그레이션 러너.
 * ddl-auto: update 환경(prod/MySQL)에서 Hibernate가 컬럼을 자동으로 삭제하지 않기 때문에
 * 서버 시작 시 한 번 실행하여 불필요한 NOT NULL 컬럼을 제거한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaCompatibilityRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
    }

    private void dropColumnIfExists(String tableName, String columnName) {
        try {
            boolean exists = !jdbcTemplate.queryForList(
                    "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE LOWER(TABLE_NAME) = ? AND LOWER(COLUMN_NAME) = ?",
                    tableName.toLowerCase(), columnName.toLowerCase()
            ).isEmpty();

            if (exists) {
                jdbcTemplate.execute(
                        "ALTER TABLE " + tableName + " DROP COLUMN " + columnName
                );
                log.info("Schema migration: dropped column {}.{}", tableName, columnName);
            }
        } catch (Exception e) {
            log.warn("Schema migration skipped for {}.{}: {}", tableName, columnName, e.getMessage());
        }
    }
}
