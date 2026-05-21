# 마감 메이트 — ERD

---

## 테이블 관계 요약

```
users ──────────────────────────────────────────────────────────────────────┐
  │  1:N  gatherings (leader_id)                                            │
  │  1:N  gathering_members (user_id)                                       │
  │  1:N  applications (applicant_id)                                       │
  │  1:N  todos (user_id)                                                   │
  │  1:N  notifications (user_id)                                           │
  │  1:N  reviews (reviewer_id / target_user_id)                            │
  │  1:N  gathering_likes (user_id)                                         │
  │  1:N  reputation_logs (user_id)                                         │
  └───────────────────────────────────────────────────────────────────────┘

gatherings ─────────────────────────────────────────────────────────────────┐
  │  1:N  gathering_tags (gathering_id)                                     │
  │  1:N  weekly_plans (gathering_id)                                       │
  │  1:N  applications (gathering_id)                                       │
  │  1:N  gathering_members (gathering_id)                                  │
  │  1:N  gathering_images (gathering_id)                                   │
  │  1:N  gathering_likes (gathering_id)                                    │
  │  1:N  gathering_categories (gathering_id)                               │
  │  1:N  todos (gathering_id)                                              │
  │  1:N  notifications (gathering_id)                                      │
  │  1:N  reviews (gathering_id)                                            │
  │  1:1  gathering_reports (gathering_id)                                  │
  └───────────────────────────────────────────────────────────────────────┘
  
  weekly_plans ─────────────────────────────────────────────────────────────┐
  │  1:N  weekly_plan_details (weekly_plan_id)
  └───────────────────────────────────────────────────────────────────────┘
```

---

## 테이블 명세

### `users` — 사용자

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `email` | VARCHAR(255) | NOT NULL | UNIQUE | 로그인 이메일 |
| `password_hash` | VARCHAR(255) | NULL | | 소셜 로그인은 NULL |
| `nickname` | VARCHAR(20) | NOT NULL | UNIQUE | 2~10자 |
| `profile_image` | TEXT | NULL | | 프로필 이미지 URL |
| `provider` | ENUM | NOT NULL | | `EMAIL` \| `KAKAO` \| `GOOGLE` |
| `provider_id` | VARCHAR(255) | NULL | | 소셜 provider 고유 ID |
| `reputation_score` | DECIMAL(4,1) | NOT NULL | | 기본값 36.5 |
| `is_active` | BOOLEAN | NOT NULL | | 탈퇴 여부, 기본값 true |
| `created_at` | TIMESTAMP | NOT NULL | | 가입일시 |
| `updated_at` | TIMESTAMP | NOT NULL | | 수정일시 |

---

### `gatherings` — 모임

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `leader_id` | BIGINT | NOT NULL | FK → users.id | 모임장 |
| `type` | ENUM | NOT NULL | | `STUDY` \| `PROJECT` |
| `title` | VARCHAR(60) | NOT NULL | | 모임 제목 (2~30자) |
| `short_description` | VARCHAR(100) | NOT NULL | | 한 줄 소개 (2~50자) |
| `description` | TEXT | NOT NULL | | 상세 설명 (10~1000자) |
| `goal` | TEXT | NOT NULL | | 모임 최종 목표 |
| `max_members` | TINYINT | NOT NULL | | 2~10명 |
| `current_members` | TINYINT | NOT NULL | | 현재 멤버 수, 기본값 1 |
| `recruit_deadline` | DATE | NOT NULL | | 모집 마감일 |
| `start_date` | DATE | NOT NULL | | 모임 시작일 |
| `end_date` | DATE | NOT NULL | | 모임 종료일 |
| `total_weeks` | TINYINT | NOT NULL | | 총 주차 수 (자동 계산) |
| `status` | ENUM | NOT NULL | | `RECRUITING` \| `IN_PROGRESS` \| `COMPLETED` \| `CANCELLED` |
| `view_count` | INT | NOT NULL | | 조회수, 기본값 0 |
| `created_at` | TIMESTAMP | NOT NULL | | 생성일시 |
| `updated_at` | TIMESTAMP | NOT NULL | | 수정일시 |

---

### `gathering_images` — 모임 이미지

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|------|-----|---|---|------|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `gathering_id` | BIGINT | NOT NULL | FK → gatherings.id |  |
| `image_url` | VARCHAR(500) | NOT NULL | 모임 이미지 URL |
| `display_order` | TINYINT | NOT NULL | | 이미지 정렬 순서 |
| `created_at` | TIMESTAMP | NOT NULL | | 생성일시 |
| `updated_at` | TIMESTAMP | NOT NULL | | 수정일시 |
> **UNIQUE:** `(gathering_id, display_order)` — 중복 순서 방지
> 
---

### `gathering_tags` — 모임 태그

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `gathering_id` | BIGINT | NOT NULL | FK → gatherings.id | |
| `tag` | VARCHAR(30) | NOT NULL | | 태그명 (최대 15자) |

---

### `categories` — 모임 카테고리

| 컬럼명          | 타입          | NULL | KEY    | 설명              |
|--------------|-------------|---|--------|-----------------|
| `id`         | BIGINT      | NOT NULL | PK     | Auto Increment  |
| `name`       | VARCHAR(50) | NOT NULL | UNIQUE | 카테고리명(개발, 어학 등) |
| `created_at` | TIMESTAMP | NOT NULL | | 생성일시 |
| `updated_at` | TIMESTAMP | NOT NULL | | 수정일시 |

---

### `gathering_categories` — 모임 카테고리 매핑

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|------|----|---|-----|----|
| `id` | BIGINT | NOT NULL | PK  | Auto Increment |
| `gathering_id` | BIGINT | NOT NULL | FK → gatherings.id | |
| `category_id` | BIGINT | NOT NULL | FK → categories.id | |
| `created_at` | TIMESTAMP | NOT NULL | | 생성일시 |
> **UNIQUE:** `(gathering_id, category_id) — 동일 모임에 동일 카테고리 중복 방지

---

### `weekly_plans` — 주차별 계획

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `gathering_id` | BIGINT | NOT NULL | FK → gatherings.id | |
| `week_number` | TINYINT | NOT NULL | | 주차 번호 (1~N) |
| `title` | VARCHAR(100) | NULL | | 주차 제목 (선택) |
| `start_date` | DATE | NOT NULL | | 해당 주차 시작일 |
| `end_date` | DATE | NOT NULL | | 해당 주차 종료일 |

---

### `weekly_plan_details` — 주차별 세부 계획

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `weekly_plan_id` | BIGINT | NOT NULL | FK → weekly_plans.id | 소속 주차 계획 ID |
| `display_order` | TINYINT | NOT NULL | | 세부 계획 순서 |
| `content` | VARCHAR(200) | NOT NULL | | 세부 계획 내용 |
| `created_at` | TIMESTAMP | NOT NULL | | 생성일시 |
| `updated_at` | TIMESTAMP | NOT NULL | | 수정일시 |

> **UNIQUE:** `(weekly_plan_id, display_order)` — 같은 주차 내 순서 중복 방지
---

### `applications` — 모임 신청

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `gathering_id` | BIGINT | NOT NULL | FK → gatherings.id | |
| `applicant_id` | BIGINT | NOT NULL | FK → users.id | 신청자 |
| `personal_goal` | TEXT | NOT NULL | | 신청 시 작성한 개인 목표 |
| `self_introduction` | TEXT | NULL | | 한 줄 자기소개 (선택) |
| `status` | ENUM | NOT NULL | | `PENDING` \| `ACCEPTED` \| `REJECTED` |
| `created_at` | TIMESTAMP | NOT NULL | | 신청일시 |
| `updated_at` | TIMESTAMP | NOT NULL | | 수정일시 |

> **UNIQUE:** `(gathering_id, applicant_id)` — 중복 신청 방지

---

### `gathering_members` — 모임 멤버십

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `gathering_id` | BIGINT | NOT NULL | FK → gatherings.id | |
| `user_id` | BIGINT | NOT NULL | FK → users.id | |
| `role` | ENUM | NOT NULL | | `LEADER` \| `MEMBER` |
| `personal_goal` | TEXT | NULL | | 이 모임에서의 개인 목표 |
| `overall_achievement_rate` | DECIMAL(5,2) | NOT NULL | | 전체 달성률, 기본값 0.00 |
| `is_active` | BOOLEAN | NOT NULL | | 탈퇴 여부, 기본값 true |
| `joined_at` | TIMESTAMP | NOT NULL | | 참여일시 |

> **UNIQUE:** `(gathering_id, user_id)`

---

### `todos` — Todo 항목

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `gathering_id` | BIGINT | NOT NULL | FK → gatherings.id | |
| `user_id` | BIGINT | NOT NULL | FK → users.id | Todo 작성자 |
| `week_number` | TINYINT | NOT NULL | | 주차 번호 |
| `content` | VARCHAR(400) | NOT NULL | | Todo 내용 |
| `is_completed` | BOOLEAN | NOT NULL | | 완료 여부, 기본값 false |
| `completed_at` | TIMESTAMP | NULL | | 완료일시 |
| `created_at` | TIMESTAMP | NOT NULL | | 생성일시 |
| `updated_at` | TIMESTAMP | NOT NULL | | 수정일시 |

---

### `notifications` — 알림

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `user_id` | BIGINT | NOT NULL | FK → users.id | 수신자 |
| `gathering_id` | BIGINT | NULL | FK → gatherings.id | 관련 모임 (nullable) |
| `type` | ENUM | NOT NULL | | `APPLICATION_RECEIVED` \| `APPLICATION_ACCEPTED` \| `APPLICATION_REJECTED` \| `PENALTY_WARNING` \| `GATHERING_STARTED` \| `GATHERING_ENDED` \| `REVIEW_REQUEST` \| `POKE` |
| `content` | TEXT | NOT NULL | | 알림 내용 텍스트 |
| `target_url` | VARCHAR(255) | NULL | | 클릭 시 이동 URL |
| `is_read` | BOOLEAN | NOT NULL | | 읽음 여부, 기본값 false |
| `created_at` | TIMESTAMP | NOT NULL | | 생성일시 |

---

### `reviews` — 리뷰

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `gathering_id` | BIGINT | NOT NULL | FK → gatherings.id | 리뷰가 작성된 모임 |
| `reviewer_id` | BIGINT | NOT NULL | FK → users.id | 작성자 |
| `target_user_id` | BIGINT | NOT NULL | FK → users.id | 대상자 |
| `comment` | VARCHAR(200) | NULL | | 한 줄 코멘트 (선택) |
| `created_at` | TIMESTAMP | NOT NULL | | 작성일시 |

> **UNIQUE:** `(gathering_id, reviewer_id, target_user_id)` — 동일 모임 내 중복 리뷰 방지

---

### `review_tags` — 리뷰 태그

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `review_id` | BIGINT | NOT NULL | FK → reviews.id | |
| `tag` | VARCHAR(50) | NOT NULL | | `성실해요` \| `소통이 좋아요` \| `잘 도와줘요` \| `시간을 잘 지켜요` \| `다시 함께하고 싶어요` |

---

### `reputation_logs` — 평판 점수 변동 로그

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `user_id` | BIGINT | NOT NULL | FK → users.id | |
| `gathering_id` | BIGINT | NULL | FK → gatherings.id | 관련 모임 |
| `delta` | DECIMAL(4,1) | NOT NULL | | 변동값 (+/-), 예: +0.5, -0.3 |
| `reason` | ENUM | NOT NULL | | `COMPLETION_HIGH` \| `COMPLETION_MID` \| `WEEKLY_PENALTY` \| `STREAK_PENALTY` \| `REVIEW_POSITIVE` |
| `created_at` | TIMESTAMP | NOT NULL | | 변동일시 |

**점수 변동 규칙**
| reason | delta | 조건 |
|---|---|---|
| `COMPLETION_HIGH` | +0.5 | 모임 완료 + 달성률 80% 이상 |
| `COMPLETION_MID` | +0.1 | 모임 완료 + 달성률 50~79% |
| `WEEKLY_PENALTY` | -0.3 | 주간 달성률 50% 미만 |
| `STREAK_PENALTY` | -0.5 | 연속 미달성 |
| `REVIEW_POSITIVE` | +0.1 | 긍정 리뷰 1개당 |

---

### `gathering_reports` — 모임 결과 리포트

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|-----|---|---|---|----|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `gathering_id` | BIGINT | NOT NULL | FK → gatherings.id | UNIQUE (1:1) |
| `team_overall_rate` | DECIMAL(5,2) | NOT NULL | | 팀 전체 달성률 |
| `mvp_user_ids` | TEXT | NULL | | MVP 수상자 목록 (공동수상 지원, JSON 배열) |
| `longest_streak_user_ids` | TEXT | NULL | | 최장 스트릭 수상자 목록 (JSON 배열) |
| `longest_streak_value` | INT | NOT NULL | | 최장 연속 100% 주차 수 |
| `most_improved_user_ids` | TEXT | NULL | | 가장 성장한 멤버 목록 (JSON 배열) |
| `attendance_user_ids` | TEXT | NULL | | 매주 Todo 1개 이상 완료한 멤버 목록 (JSON 배열) |
| `weekly_rates` | JSON | NOT NULL | | `[{ "week": 1, "rate": 80.0 }, ...]` |
| `created_at` | TIMESTAMP | NOT NULL | | 생성일시 |

---

### `gathering_likes` — 모임 찜

| 컬럼명 | 타입 | NULL | KEY | 설명 |
|---|---|---|---|----|
| `id` | BIGINT | NOT NULL | PK | Auto Increment |
| `gathering_id` | BIGINT | NOT NULL | FK → gatherings.id | 찜한 모임 |
| `user_id` | BIGINT | NOT NULL | FK → users.id | 찜한 사용자 |
| `created_at` | TIMESTAMP | NOT NULL | | 생성일시 |

> **UNIQUE:** `(gathering_id, user_id)` — 동일 사용자의 중복 찜 방지
---

## 인덱스 전략

| 테이블 | 인덱스 컬럼  | 목적 |
|-----|---------|----|
| `users` | `email` | 로그인 조회 |
| `users` | `nickname` | 닉네임 중복 확인 |
| `users` | `(provider, provider_id)` | 소셜 로그인 조회 |
| `gatherings` | `(status, created_at)` | 목록 조회 / 최신순 정렬 |
| `gatherings` | `recruit_deadline` | 마감임박 정렬 |
| `gatherings` | `view_count` | 인기순 정렬 |
| `applications` | `(gathering_id, applicant_id)` UNIQUE | 중복 신청 방지 |
| `gathering_members` | `(gathering_id, user_id)` UNIQUE | 멤버 중복 방지 |
| `gathering_members` | `(user_id, is_active)` | 내가 참여 중인 모임 조회 |
| `gathering_categories` | `(category_id, gathering_id)` | 필터링 |
| `gathering_images` | `(gathering_id, display_order)` | 모임별 이미지 조회 및 정렬 |
| `gathering_likes` | `(user_id, created_at)` | 찜한 모임 목록 조회 |
| `gathering_likes` | `(gathering_id, user_id)` UNIQUE | 중복 찜 방지/찜 여부 조회 |
| `weekly_plan_details` | `(weekly_plan_id, display_order)` UNIQUE | 세부 계획 순서 보장 / 정렬 조회 |
| `todos` | `(gathering_id, user_id, week_number)` | Todo 조회 |
| `notifications` | `(user_id, is_read)` | 읽지 않은 알림 조회 |
| `reviews` | `(gathering_id, reviewer_id, target_user_id)` UNIQUE | 중복 리뷰 방지 |
