package com.fesi.deadlinemate.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "중복된 데이터입니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청 횟수를 초과했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    // Auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    LOGIN_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도 횟수를 초과했습니다. 30초 후 다시 시도해주세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다."),
    OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OAuth 인증에 실패했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PASSWORD_NOT_MATCHED(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    SOCIAL_USER_PASSWORD_CHANGE(HttpStatus.FORBIDDEN, "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다."),

    // Gathering
    INVALID_GATHERING_DATE(HttpStatus.BAD_REQUEST, "모임 종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_GATHERING_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 모임 타입입니다."),
    INVALID_GATHERING_LEADER(HttpStatus.FORBIDDEN, "해당 기능은 모임장만 가능합니다."),
    INVALID_RECRUIT_DEADLINE(HttpStatus.BAD_REQUEST, "모집 마감일은 시작일 이전이어야 합니다."),
    INVALID_IN_PROGRESS_UPDATE_ITEMS(HttpStatus.BAD_REQUEST, "진행 중인 모임은 description, weeklyGuides, endDate만 수정할 수 있습니다."),
    GATHERING_UPDATE_FORBIDDEN_IN_PROGRESS(HttpStatus.BAD_REQUEST,"현재 상태의 모임은 수정할 수 없습니다."),
    GATHERING_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "진행 중인 모임은 삭제할 수 없습니다."),
    INVALID_MAX_MEMBERS(HttpStatus.BAD_REQUEST, "최대 인원은 2명 이상 10명 이하여야 합니다."),
    GATHERING_NOT_FOUND(HttpStatus.NOT_FOUND, "모임을 찾을 수 없습니다."),
    GATHERING_COMPLETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "진행 중인 모임만 종료할 수 있습니다."),

    // GatheringApplication
    DUPLICATE_GATHERING_APPLICATION(HttpStatus.CONFLICT, "이미 신청한 모임입니다."),
    GATHERING_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "현재 모집 중인 모임만 신청할 수 있습니다."),
    GATHERING_FULL(HttpStatus.BAD_REQUEST, "모집 인원이 마감되었습니다."),
    ALREADY_GATHERING_MEMBER(HttpStatus.CONFLICT, "이미 해당 모임의 멤버입니다."),
    GATHERING_LEADER_CANNOT_APPLY(HttpStatus.BAD_REQUEST, "모임장은 본인 모임에 신청할 수 없습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 신청입니다."),
    INVALID_APPLICATION_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "변경할 수 없는 신청 상태입니다."),
    APPLICATION_CANCEL_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 신청만 취소할 수 있습니다."),
    APPLICATION_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "대기 중인 신청만 취소할 수 있습니다."),

    // Todo
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "할 일을 찾을 수 없습니다."),
    TODO_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 할 일만 수정할 수 있습니다."),
    TODO_NOT_CHANGED(HttpStatus.BAD_REQUEST, "변경된 내용이 없습니다."),
    TODO_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 할 일만 삭제할 수 있습니다."),
    INVALID_TODO_WEEK(HttpStatus.BAD_REQUEST, "유효하지 않은 주차입니다."),
    INVALID_TODO_CONTENT(HttpStatus.BAD_REQUEST, "할 일 내용이 올바르지 않습니다."),
    INVALID_TODO_WEEK_ACCESS(HttpStatus.BAD_REQUEST, "현재 진행 중인 주차의 할 일만 작성하거나 수정할 수 있습니다."),
    INVALID_TODO_PERIOD(HttpStatus.BAD_REQUEST, "현재 모임 진행 기간이 아닙니다."),

    //Like
    ALREADY_GATHERING_LIKED(HttpStatus.CONFLICT, "이미 찜한 모임입니다."),
    GATHERING_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "찜한 이력이 없습니다."),

    //ACHIEVEMENT & REPORT
    INVALID_ACHIEVEMENT_RATE(HttpStatus.BAD_REQUEST, "유효하지 않은 달성률입니다."),
    GATHERING_REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 결과 리포트가 생성된 모임입니다."),
    GATHERING_REPORT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "모임이 완료 상태가 아니므로 결과 리포트를 조회할 수 없습니다."),
    GATHERING_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "모임 결과 리포트를 찾을 수 없습니다."),
    INVALID_GATHERING_REPORT_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "모임 결과 리포트 데이터가 올바르지 않습니다."),

    //CATEGORY
    GATHERING_CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "카테고리는 최소 1개 이상 필요합니다."),
    GATHERING_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리가 포함되어 있습니다."),
    GATHERING_CATEGORY_COUNT(HttpStatus.BAD_REQUEST, "카테고리는 최대 3개까지 선택할 수 있습니다."),

    //WeeklyPlan
    INVALID_WEEKLY_GUIDE_SEQUENCE(HttpStatus.BAD_REQUEST,"주차 가이드는 1주차부터 순차적으로 입력되어야 합니다."),
    INVALID_WEEKLY_PLAN_DETAILS_COUNT(HttpStatus.BAD_REQUEST, "세부 계획은 최대 2개까지 입력할 수 있습니다."),

    // GatheringDraft
    GATHERING_DRAFT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "임시저장은 최대 5개까지 가능합니다."),
    GATHERING_DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "임시저장을 찾을 수 없습니다."),
    GATHERING_DRAFT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 임시저장만 접근할 수 있습니다."),
    GATHERING_DRAFT_INVALID_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "임시저장 데이터가 올바르지 않습니다."),

    // Membership
    NOT_A_MEMBER(HttpStatus.FORBIDDEN, "모임 멤버만 접근할 수 있습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 멤버를 찾을 수 없습니다."),
    CANNOT_KICK_LEADER(HttpStatus.BAD_REQUEST, "모임장은 퇴출할 수 없습니다."),
    LEADER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "모임장은 모임을 탈퇴할 수 없습니다."),
    CANNOT_POKE_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 찌를 수 없습니다."),

    // Review
    REVIEW_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 리뷰를 작성했습니다."),
    GATHERING_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "모임이 완료된 후에만 리뷰를 작성할 수 있습니다."),
    SELF_REVIEW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인에게 리뷰를 작성할 수 없습니다."),
    REVIEWER_NOT_A_MEMBER(HttpStatus.FORBIDDEN, "해당 모임의 멤버만 리뷰를 작성할 수 있습니다."),
    REVIEW_TARGET_NOT_A_MEMBER(HttpStatus.BAD_REQUEST, "리뷰 대상이 해당 모임의 멤버가 아닙니다."),
    INVALID_REVIEW_TAG(HttpStatus.BAD_REQUEST, "유효하지 않은 리뷰 태그입니다."),
    INVALID_MATES_TAG(HttpStatus.BAD_REQUEST, "유효하지 않은 MATES 태그입니다."),

    // Image
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다."),
    IMAGE_EMPTY(HttpStatus.BAD_REQUEST, "이미지 파일이 비어있습니다."),
    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "이미지 크기는 5MB 이하여야 합니다."),
    IMAGE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 형식입니다. (JPEG, PNG, GIF, WebP만 가능)"),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
