package com.fesi.deadlinemate.domain.review.entity;

import com.fesi.deadlinemate.global.error.BusinessException;
import com.fesi.deadlinemate.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewTag {

    DILIGENT("성실해요"),
    GOOD_COMMUNICATION("소통이 좋아요"),
    HELPFUL("잘 도와줘요"),
    PUNCTUAL("시간을 잘 지켜요"),
    WANT_AGAIN("다시 함께하고 싶어요");

    private final String displayName;

    public static ReviewTag fromDisplayName(String displayName) {
        for (ReviewTag tag : values()) {
            if (tag.displayName.equals(displayName)) {
                return tag;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_REVIEW_TAG);
    }
}
