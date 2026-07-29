package com.fesi.deadlinemate.domain.gatheringDraft.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class WeeklyGuideListConverter implements AttributeConverter<List<WeeklyGuideItem>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<WeeklyGuideItem> weeklyGuides) {
        if (weeklyGuides == null || weeklyGuides.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(weeklyGuides);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.GATHERING_DRAFT_INVALID_DATA);
        }
    }

    @Override
    public List<WeeklyGuideItem> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WeeklyGuideItem.class)
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.GATHERING_DRAFT_INVALID_DATA);
        }
    }
}
