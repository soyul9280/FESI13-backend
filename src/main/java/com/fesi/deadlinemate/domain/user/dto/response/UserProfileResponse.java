package com.fesi.deadlinemate.domain.user.dto.response;

import com.fesi.deadlinemate.domain.user.entity.Provider;
import com.fesi.deadlinemate.domain.user.entity.User;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImage;
    private Provider provider;
    private BigDecimal reputationScore;
    private String reputationLabel;
    private long completedGatherings;
    private BigDecimal avgAchievementRate;
    private long reviewCount;

    public static UserProfileResponse from(User user,
                                           long completedGatherings,
                                           BigDecimal avgAchievementRate,
                                           long reviewCount) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .provider(user.getProvider())
                .reputationScore(user.getReputationScore())
                .reputationLabel(getReputationLabel(user.getReputationScore()))
                .completedGatherings(completedGatherings)
                .avgAchievementRate(avgAchievementRate)
                .reviewCount(reviewCount)
                .build();
    }

    private static String getReputationLabel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(45.0)) >= 0) return "태양 메이트";
        if (score.compareTo(BigDecimal.valueOf(40.0)) >= 0) return "불꽃 메이트";
        if (score.compareTo(BigDecimal.valueOf(37.5)) >= 0) return "불씨 메이트";
        return "연기 메이트";
    }
}
