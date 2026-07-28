# 마감 메이트 — API 명세서

> **Base URL:** `/api/v1`
> **인증:** `Authorization: Bearer <access_token>`
> **공통 응답:** `{ success: boolean, data: object | null, message: string }`

---

## 목차

1. [인증 (Authentication)](#1-인증-authentication)
2. [사용자 (Users)](#2-사용자-users)
3. [모임 (Gatherings)](#3-모임-gatherings)
4. [모임 신청 (Applications)](#4-모임-신청-applications)
5. [모임 멤버십 (Memberships)](#5-모임-멤버십-memberships)
6. [Todo](#6-todo)
7. [달성률 (Achievement)](#7-달성률-achievement)
8. [알림 (Notifications)](#8-알림-notifications)
9. [리뷰 (Reviews)](#9-리뷰-reviews)
10. [결과 리포트 (Reports)](#10-결과-리포트-reports)
11. [찜하기 (Likes)](#11-찜하기-likes)
12. [FCM 푸시 알림 (Push Notifications)](#12-fcm-푸시-알림-push-notifications)
13. [공통 에러 코드](#13-공통-에러-코드)

---

## 1. 인증 (Authentication)

### POST `/auth/register`
> 이메일 회원가입

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd1!",
  "nickname": "마감왕"
}
```

**Response `201`**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "마감왕",
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

**에러**
- `409` — 이메일 또는 닉네임 중복

---

### POST `/auth/login`
> 이메일 로그인

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd1!"
}
```

**Response `200`**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "마감왕",
    "profileImage": "https://...",
    "reputationScore": 36.5
  }
}
```

**에러**
- `401` — 이메일/비밀번호 불일치
- `429` — 5회 연속 실패 시 30초 잠금

---

### POST `/auth/refresh`
> Access Token 재발급

**Request Body**
```json
{
  "refreshToken": "eyJ..."
}
```

**Response `200`**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ..."
}
```

**에러**
- `401` — Refresh Token 만료 또는 유효하지 않음

---

### POST `/auth/logout` 🔒
> 로그아웃 (Refresh Token 무효화)

**Request Body**
```json
{
  "refreshToken": "eyJ..."
}
```

**Response `200`**
```json
{ "success": true }
```

---

### GET `/auth/kakao/callback`
> 카카오 OAuth 콜백

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|---|---|---|
| `code` | string | 카카오 인증 코드 |

**Response `200`**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "isNewUser": true
}
```

---

### GET `/auth/google/callback`
> Google OAuth 콜백

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `code` | string | Google 인증 코드 |

**Response `200`**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "isNewUser": false
}
```

---

### GET `/auth/check/email`
> 이메일 중복 확인

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `email` | string | 확인할 이메일 |

**Response `200`**
```json
{ "available": true }
```

---

### GET `/auth/check/nickname`
> 닉네임 중복 확인

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `nickname` | string | 확인할 닉네임 |

**Response `200`**
```json
{ "available": true }
```

---

## 2. 사용자 (Users)

### GET `/users/me` 🔒
> 내 프로필 조회

**Response `200`**
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "마감왕",
  "profileImage": "https://...",
  "provider": "EMAIL",
  "reputationScore": 36.5,
  "reputationLabel": "연기 메이트",
  "completedGatherings": 3,
  "avgAchievementRate": 78.5,
  "reviewCount": 5
}
```

---

### PATCH `/users/me` 🔒
> 내 프로필 수정 (multipart/form-data)

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `nickname` | string | 선택 | 2~10자 |
| `profileImage` | file | 선택 | jpg/png/webp, 5MB 이하 |

**Response `200`**
```json
{
  "id": 1,
  "nickname": "새닉네임",
  "profileImage": "https://..."
}
```

**에러**
- `409` — 닉네임 중복

---

### PATCH `/users/me/password` 🔒
> 비밀번호 변경 (이메일 가입자 전용)

**Request Body**
```json
{
  "currentPassword": "OldP@ss1!",
  "newPassword": "NewP@ss1!"
}
```

**Response `200`**
```json
{ "success": true }
```

**에러**
- `400` — 현재 비밀번호 불일치
- `403` — 소셜 로그인 사용자

---

### DELETE `/users/me` 🔒
> 회원 탈퇴

**Response `200`**
```json
{ "success": true }
```

**에러**
- `400` — 진행 중인 모임이 있는 경우

---

### GET `/users/:userId`
> 특정 유저 공개 프로필 조회

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `userId` | number | 대상 유저 ID |

**Response `200`**
```json
{
  "id": 2,
  "nickname": "열정맨",
  "profileImage": "https://...",
  "reputationScore": 38.0,
  "reputationLabel": "불씨 메이트",
  "reviews": [...]
}
```

---

## 3. 모임 (Gatherings)

### GET `/gatherings`
> 모임 목록 조회 (모임 찾기)

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|------|----|---|----|
| `type` | string | - | `스터디` \| `프로젝트` |
| `categoryIds` | number[] | - | 개발/어학/독서/자격증 등 반복 파라미터 |
| `sort` | string | `latest` | `latest` \| `popular` \| `deadline` |
| `status` | string | `recruiting` | `recruiting` \| `all` |
| `query` | string | - | 제목/소개/태그 검색어 |
| `page` | number | `1` | 페이지 번호 |
| `limit` | number | `12` | 페이지당 개수 |

**Response `200`**
```json
{
  "gatherings": [
    {
      "id": 1,
      "type": "스터디",
      "categories": ["개발", "자격증"],
      "title": "React 완전 정복 스터디",
      "shortDescription": "리액트 공식문서를 같이 읽어요",
      "tags": ["React", "프론트엔드"],
      "maxMembers": 6,
      "currentMembers": 3,
      "recruitDeadline": "2025-03-20",
      "startDate": "2025-03-22",
      "endDate": "2025-04-19",
      "status": "RECRUITING",
      "leader": { "id": 1, "nickname": "마감왕", "profileImage": "https://..." }
    }
  ],
  "totalCount": 42,
  "totalPages": 4,
  "currentPage": 1
}
```

---

### GET `/gatherings/categories`
> 카테고리 조회용

**Response `200`**
```json
{
  "categories": [
    { "id": 1, "name": "개발" },
    { "id": 2, "name": "어학" }
  ]
}
```

---

### POST `/gatherings` 🔒
> 모임 생성

**Content-Type**
> multipart/form-data

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `type` | string | 필수 | `STUDY` \| `PROJECT` |
| `categoryIds` | number[] | 필수 | 최소 1개 ~ 최대 3개 |
| `title` | string | 필수 | 2~30자 |
| `shortDescription` | string | 필수 | 2~50자 |
| `description` | string | 선택 | 작성 시 10~1000자 |
| `tags` | string[] | 필수 | 최소 1개 ~ 최대 10개, 태그당 15자 이하 |
| `goal` | string | 필수 | |
| `maxMembers` | number | 필수 | 2~10명 |
| `recruitDeadline` | string(date) | 필수 | |
| `startDate` | string(date) | 필수 | |
| `endDate` | string(date) | 필수 | |
| `weeklyGuides` | array | 선택 | 주차별 계획, 항목당 `title`/`details`도 선택 |
| `images` | file[] | 선택 | 별도 multipart part (`images`) |

```json
{
  "type": "스터디",
  "categoryIds": [1, 2],
  "title": "React 완전 정복 스터디",
  "shortDescription": "리액트 공식문서를 같이 읽어요",
  "description": "매주 공식문서 1챕터씩 읽고 블로그를 작성합니다...",
  "tags": ["React", "프론트엔드"],
  "goal": "React 공식문서 완독 + 블로그 5편 작성",
  "maxMembers": 6,
  "recruitDeadline": "2025-03-20",
  "startDate": "2025-03-22",
  "endDate": "2025-04-19",
  "weeklyGuides": [
    {
      "week": 1,
      "title": "피그마 이론",
      "details": [
        "01. 피그마 프레임 이론",
        "02. 피그마 컴포넌트 마스터"
      ]
    }
  ]
}
```

**Response `201`**
```json
{
  "gathering": { "id": 1, "...": "모임 전체 정보" }
}
```

**비고**
- 생성자는 자동으로 `LEADER` + 첫 번째 멤버로 등록
- 이미지는 여러 장 등록 가능

---

### GET `/gatherings/main`
> 메인 페이지용 모임 데이터 (ISR, revalidate 60s)

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `limit` | number | `5` | 섹션별 최대 개수 |

**Response `200`**
```json
{
  "popular": [...],
  "deadline": [...],
  "latest": [...]
}
```

---

### GET `/gatherings/:gatheringId`
> 모임 상세 조회

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `gatheringId` | number | 모임 ID |

**Response `200`**
```json
{
  "id": 1,
  "type": "스터디",
  "categories": ["개발", "자격증"],
  "title": "React 완전 정복 스터디",
  "shortDescription": "리액트 공식문서를 같이 읽어요",
  "description": "...",
  "tags": ["React", "프론트엔드"],
  "goal": "React 공식문서 완독 + 블로그 5편 작성",
  "maxMembers": 6,
  "currentMembers": 3,
  "recruitDeadline": "2025-03-20",
  "startDate": "2025-03-22",
  "endDate": "2025-04-19",
  "totalWeeks": 4,
  "images" : [
    {"url": "https://example.com/meeting1.jpg", "displayOrder": 0}
  ],
  "status": "RECRUITING",
  "leader": { "id": 1, "nickname": "마감왕", "profileImage": "https://..." },
  "weeklyPlans": [
    {
      "week": 1,
      "title": "피그마 이론",
      "startDate": "2025-03-15",
      "endDate": "2025-03-21",
      "details": [
        "01. 피그마 프레임 이론",
        "02. 피그마 컴포넌트 마스터"
      ]
    }
  ],
  "members": [
    { "userId": 1, "nickname": "마감왕", "profileImage": "https://...", "role": "LEADER" }
  ]
}
```

**비고**
- 비로그인도 조회 가능

---

### GET `/gatherings/{gatheringId}/application-status` 🔒
> 모임 신청 상태 조회

**Response `200`**
```json
{
  "myApplicationStatus": "PENDING"
}
```

**비고**
- `myApplicationStatus`: `null` \| `PENDING` \| `ACCEPTED` \| `REJECTED`

---

### PUT `/gatherings/:gatheringId` 🔒
> 모임 수정 (모임장 전용)

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `gatheringId` | number | 모임 ID |

**Request Body**
- 필드별 필수/선택 여부는 POST `/gatherings`와 동일 (`tags` 필수, `description`/`weeklyGuides` 선택)
- 모집 중 상태: 생성 시 필드 전부 수정 가능
- 진행 중 상태: `description`, `weeklyGuides`, `endDate` 만 수정 가능 (다른 필드를 변경하면 `INVALID_IN_PROGRESS_UPDATE_ITEMS` 에러)

**Response `200`**
```json
{
  "gathering": { "...": "수정된 모임 정보" }
}
```

**비고**
- categoryIds: number[] (카테고리 수정 가능)

---

### DELETE `/gatherings/:gatheringId` 🔒
> 모임 삭제 (모임장 전용)

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `gatheringId` | number | 모임 ID |

**Response `200`**
```json
{ "success": true }
```

**에러**
- `400` — 진행 중인 모임은 삭제 불가
- 대기 중 신청자에게 알림 자동 발송

---

## 4. 모임 신청 (Applications)

### POST `/gatherings/:gatheringId/applications` 🔒
> 모임 참여 신청

**Request Body**
```json
{
  "personalGoal": "React 기초 완벽 이해 + 블로그 5편 작성",
  "selfIntroduction": "프론트엔드 3개월차입니다. 열심히 하겠습니다!"
}
```

**Response `201`**
```json
{
  "application": {
    "id": 10,
    "status": "PENDING",
    "createdAt": "2025-03-15T10:00:00Z"
  }
}
```

**에러**
- `409` — 이미 신청 중 (중복 신청 방지)
- `400` — 모집 마감 또는 인원 마감

---

### GET `/gatherings/:gatheringId/applications` 🔒
> 신청 목록 조회 (모임장 전용)

**Response `200`**
```json
{
  "applications": [
    {
      "id": 10,
      "applicant": {
        "id": 5,
        "nickname": "열정맨",
        "profileImage": "https://...",
        "reputationScore": 37.2,
        "reviewSummary": {
          "reviewCount": 12,
          "topTags": ["성실해요", "소통이 좋아요"]
        },

        "recentReviews": [
          {
            "id": 30,
            "comment": "항상 열심히 참여해주셨어요!",
            "tags": ["성실해요", "소통이 좋아요"]
          },
          {
            "id": 31,
            "comment": "시간 약속을 잘 지켜요",
            "tags": ["시간을 잘 지켜요"]
          }
        ]
      },
      
      "personalGoal": "React 기초 완벽 이해...",
      "selfIntroduction": "프론트엔드 3개월차...",
      "status": "PENDING",
      "createdAt": "2025-03-15T10:00:00Z"
    }
  ]
}
```

---

### PATCH `/gatherings/:gatheringId/applications/:applicationId` 🔒
> 신청 수락 / 거절 (모임장 전용)

**Request Body**
```json
{
  "status": "ACCEPTED"
}
```
> `status`: `"ACCEPTED"` | `"REJECTED"`

**Response `200`**
```json
{
  "application": { "id": 10, "status": "ACCEPTED" }
}
```

**비고**
- 수락/거절 시 신청자에게 알림 자동 발송
- 수락 시 `gathering_members` 에 자동 등록

---

### DELETE `/gatherings/:gatheringId/applications/:applicationId` 🔒
> 신청 취소 (신청자 본인)

**Response `200`**
```json
{ "success": true }
```

**에러**
- `400` — `PENDING` 상태가 아닌 경우 취소 불가

---

### GET `/users/me/applications` 🔒
> 내 신청 목록 조회

**Response `200`**
```json
{
  "applications": [
    {
      "id": 10,
      "gathering": { "id": 1, "title": "React 완전 정복 스터디", "type": "스터디", "status": "RECRUITING" },
      "personalGoal": "React 기초 완벽 이해...",
      "status": "PENDING",
      "createdAt": "2025-03-15T10:00:00Z"
    }
  ]
}
```

---

## 5. 모임 멤버십 (Memberships)

### GET `/users/me/gatherings` 🔒
> 내가 참여 중인 모임 목록 조회

**Query Parameters**

| 파라미터 | 타입 | 기본값  | 설명 |
|---|---|------|----|
| `status` | string | `all`  | `recruiting` \| `in_progress` \| `completed` \| `all` |
| `page` | number | `1`  | 페이지 번호 |
| `limit` | number | `12` | 페이지당 개수 |

**Response `200`**
```json
{
  "gatherings": [
    {
      "id": 1,
      "type": "스터디",
      "categories": ["개발", "자격증"],
      "title": "React 완전 정복 스터디",
      "shortDescription": "리액트 공식문서를 같이 읽어요",
      "tags": ["React", "프론트엔드"],
      "maxMembers": 6,
      "currentMembers": 3,
      "startDate": "2025-03-22",
      "endDate": "2025-04-19",
      "status": "IN_PROGRESS",
      "myRole": "LEADER"
    },
    {
      "id": 2,
      "type": "프로젝트",
      "categories": ["개발", "자격증"],
      "title": "사이드 프로젝트 팀원 모집",
      "shortDescription": "Spring Boot 기반 협업 프로젝트",
      "tags": ["Spring", "백엔드"],
      "maxMembers": 5,
      "currentMembers": 4,
      "startDate": "2025-04-01",
      "endDate": "2025-05-30",
      "status": "RECRUITING",
      "myRole": "MEMBER"
    }
  ],
  "totalCount": 2,
  "totalPages": 1,
  "currentPage": 1
}
```


### GET `/gatherings/:gatheringId/members` 🔒
> 모임 멤버 목록 조회 (참여 멤버만 접근)

**Response `200`**
```json
{
  "members": [
    {
      "userId": 1,
      "nickname": "마감왕",
      "profileImage": "https://...",
      "role": "LEADER",
      "overallAchievementRate": 85.0,
      "isActive": true
    }
  ]
}
```

---

### DELETE `/gatherings/:gatheringId/members/:userId` 🔒
> 멤버 퇴출 (모임장 전용)

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `gatheringId` | number | 모임 ID |
| `userId` | number | 퇴출할 멤버 ID |

**Response `200`**
```json
{ "success": true }
```

**에러**
- `400` — 모임장 본인은 퇴출 불가

---

### DELETE `/gatherings/:gatheringId/members/me` 🔒
> 모임 탈퇴 (본인)

**Response `200`**
```json
{ "success": true }
```

**비고**
- 패널티 여부는 미정 (팀 논의 필요)

---

## 6. Todo

### GET `/gatherings/:gatheringId/todos` 🔒
> 모임 전체 Todo 조회 (참여 멤버만)

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `week` | number | (선택) 주차 번호. 없으면 전체 반환 |

**Response `200`**
```json
{
  "todos": [
    {
      "id": 100,
      "userId": 1,
      "nickname": "마감왕",
      "week": 3,
      "content": "공식문서 7챕터 읽기",
      "isCompleted": false,
      "createdAt": "2025-03-22T09:00:00Z"
    }
  ]
}
```

---

### GET `/gatherings/:gatheringId/todos/me` 🔒
> 내 Todo 조회 + 달성률

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `week` | number | (선택) 주차 번호 |

**Response `200`**
```json
{
  "todos": [...],
  "weeklyAchievementRate": 40.0,
  "overallAchievementRate": 65.0
}
```

---

### POST `/gatherings/:gatheringId/todos` 🔒
> Todo 생성

**Request Body**
```json
{
  "week": 3,
  "content": "공식문서 7챕터 읽기"
}
```

**Response `201`**
```json
{
  "todo": {
    "id": 100,
    "week": 3,
    "content": "공식문서 7챕터 읽기",
    "isCompleted": false,
    "createdAt": "2025-03-22T09:00:00Z"
  }
}
```

**에러**
- `400` — 현재 진행 중인 주차가 아닌 경우

---

### PATCH `/gatherings/:gatheringId/todos/:todoId` 🔒
> Todo 수정 / 체크 (본인 Todo만)

**Request Body**
```json
{
  "content": "수정된 내용",
  "isCompleted": true
}
```

**Response `200`**
```json
{
  "todo": { "id": 100, "content": "수정된 내용", "isCompleted": true }
}
```

---

### DELETE `/gatherings/:gatheringId/todos/:todoId` 🔒
> Todo 삭제 (본인 Todo만)

**Response `200`**
```json
{ "success": true }
```

---

## 7. 달성률 (Achievement)

### GET `/gatherings/:gatheringId/achievements` 🔒
> 모임 전체 달성률 현황

**Response `200`**
```json
{
  "members": [
    {
      "userId": 1,
      "nickname": "마감왕",
      "weeklyRates": [
        { "week": 1, "rate": 100.0 },
        { "week": 2, "rate": 80.0 },
        { "week": 3, "rate": 40.0 }
      ],
      "overallRate": 73.3,
      "streakDays": 7
    }
  ],
  "teamWeeklyRates": [
    { "week": 1, "rate": 90.0 },
    { "week": 2, "rate": 75.0 }
  ],
  "teamOverallRate": 68.5
}
```

---

### GET `/gatherings/:gatheringId/achievements/ranking` 🔒
> 달성률 순위

**Response `200`**
```json
{
  "ranking": [
    {
      "rank": 1,
      "userId": 1,
      "nickname": "마감왕",
      "profileImage": "https://...",
      "overallRate": 85.0
    }
  ]
}
```

---

## 8. 알림 (Notifications)

### GET `/notifications` 🔒
> 내 알림 목록

**Query Parameters**
| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `page` | number | `1` | 페이지 번호 |
| `limit` | number | `20` | 페이지당 개수 |

**Response `200`**
```json
{
  "notifications": [
    {
      "id": 50,
      "type": "APPLICATION_ACCEPTED",
      "content": "'React 완전 정복 스터디' 참여가 수락되었어요!",
      "isRead": false,
      "targetUrl": "/gatherings/1/dashboard",
      "createdAt": "2025-03-15T11:00:00Z"
    }
  ],
  "unreadCount": 3
}
```

**알림 타입**

| type | 설명 |
|---|---|
| `APPLICATION_RECEIVED` | 모임에 신청이 들어옴 (모임장 수신) |
| `APPLICATION_ACCEPTED` | 내 신청이 수락됨 |
| `APPLICATION_REJECTED` | 내 신청이 거절됨 |
| `PENALTY_WARNING` | 이번 주 달성률 50% 미만 경고 |
| `GATHERING_STARTED` | 모임 시작 |
| `GATHERING_ENDED` | 모임 종료 |
| `REVIEW_REQUEST` | 팀원 리뷰 작성 요청 |
| `POKE` | 콕 찌르기 |

---

### PATCH `/notifications/:notificationId/read` 🔒
> 단일 알림 읽음 처리

**Response `200`**
```json
{ "success": true }
```

---

### PATCH `/notifications/read-all` 🔒
> 전체 알림 읽음 처리

**Response `200`**
```json
{ "success": true }
```

---

## 9. 리뷰 (Reviews)

### POST `/gatherings/:gatheringId/reviews` 🔒
> 팀원 리뷰 작성 (모임 종료 후)

**Request Body**
```json
{
  "reviews": [
    {
      "targetUserId": 2,
      "tags": ["성실해요", "소통이 좋아요"],
      "comment": "항상 열심히 참여해주셨어요!"
    },
    {
      "targetUserId": 3,
      "tags": ["시간을 잘 지켜요"]
    }
  ]
}
```

**Response `201`**
```json
{ "success": true }
```

**에러**
- `400` — 모임이 완료 상태가 아닌 경우
- `409` — 이미 리뷰 작성 완료

**리뷰 태그 목록**
- `성실해요`
- `소통이 좋아요`
- `잘 도와줘요`
- `시간을 잘 지켜요`
- `다시 함께하고 싶어요`

---

### GET `/users/:userId/reviews`
> 유저가 받은 리뷰 목록

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `page` | number | `1` | 페이지 번호 |

**Response `200`**
```json
{
  "reviews": [
    {
      "id": 30,
      "reviewer": { "id": 1, "nickname": "마감왕" },
      "gatheringTitle": "React 완전 정복 스터디",
      "tags": ["성실해요", "소통이 좋아요"],
      "comment": "항상 열심히 참여해주셨어요!",
      "createdAt": "2025-04-20T10:00:00Z"
    }
  ],
  "totalCount": 12
}
```

---

## 10. 결과 리포트 (Reports)

### GET `/gatherings/:gatheringId/report` 🔒
> 모임 결과 리포트 조회 (모임 완료 후, 멤버만)

**Response `200`**
```json
{
  "gathering": {
    "title": "React 완전 정복 스터디",
    "startDate": "2025-03-22",
    "endDate": "2025-04-19"
  },
  "teamOverallRate": 78.5,
  "weeklyRates": [
    { "week": 1, "rate": 90.0 },
    { "week": 2, "rate": 80.0 },
    { "week": 3, "rate": 75.0 },
    { "week": 4, "rate": 68.5 }
  ],
  "memberResults": [
    {
      "userId": 1,
      "nickname": "마감왕",
      "overallRate": 92.0,
      "longestStreak": 4,
      "completedTodos": 23,
      "totalTodos": 25,
      "weeklyRates": [100.0, 100.0, 80.0, 92.0]
    }
  ],
  "awards": {
    "mvp": { "userId": 1, "nickname": "마감왕" },
    "longestStreak": { "userId": 1, "nickname": "마감왕", "streak": 4 },
    "mostImproved": { "userId": 3, "nickname": "성장맨" },
    "attendance": { "userId": 2, "nickname": "개근왕" }
  }
}
```

---

## 11. 찜하기 (Likes)

### POST `/gatherings/:gatheringId/likes` 🔒
> 모임 찜하기

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `gatheringId` | number | 모임 ID |

**Response `201`**
```json
{ "success": true }
```
**에러**
- `409` — 이미 찜한 모임인 경우

---

### DELETE `/gatherings/:gatheringId/likes` 🔒
> 모임 찜 취소

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `gatheringId` | number | 모임 ID |

**Response `200`**
```json
{ "success": true }
```
**에러**
- `404` — 찜한 이력이 없는 경우

---

### GET `/users/me/likes/ids` 🔒
> 내가 찜한 모임 ID 목록 조회 (찜 상태 확인용)

**Response `200`**
```json
{ "success": true,
  "data": [1, 5, 12, 34]
}
```

**비고**
- 하트 표시, 찜 여부 매핑, 낙관적 업데이트를 위한 경량 API
- 카드 정보가 아닌 ID 목록만 반환

---

### GET `/users/me/likes` 🔒
> 내가 찜한 모임 목록 조회 (마이페이지용)

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `page` | number | `1` | 페이지 번호 |
| `limit` | number | `20` | 페이지당 개수 |

**Response `200`**
```json
{ "success": true,
  "data": {
    "gatherings": [
      {
        "id": 1,
        "type": "스터디",
        "categories": ["개발", "자격증"],
        "title": "React 완전 정복",
        "shortDescription": "리액트 끝내기",
        "tags": ["React", "프론트엔드"],
        "maxMembers": 6,
        "currentMembers": 3,
        "recruitDeadline": "2025-03-20",
        "startDate": "2025-03-22",
        "endDate": "2025-04-19",
        "status": "RECRUITING",
        "leader": {
          "id": 10,
          "nickname": "마감왕",
          "profileImage": "https://..."
        }
      }
    ],
    "totalCount": 10,
    "totalPages": 1,
    "currentPage": 1
  } }
```

---

## 12. FCM 푸시 알림 (Push Notifications)

> PWA 웹 푸시를 위한 FCM 토큰 관리 API. 기존 인앱 알림(8번)과 병행 운영된다.
> 같은 이벤트 발생 시 인앱 알림 row INSERT + FCM 푸시 발송, 두 경로로 나간다.

### POST `/push/tokens` 🔒
> FCM 토큰 등록 (알림 허용 또는 토큰 갱신 시)

**Request Body**
```json
{ "token": "<fcm-registration-token>" }
```

**Response `200`**
```json
{ "success": true }
```

**비고**
- 같은 토큰이 이미 존재하면 무시 (중복 등록 방지)
- 한 사용자가 여러 기기 토큰 보유 가능 (멀티 디바이스 지원)

---

### DELETE `/push/tokens` 🔒
> FCM 토큰 삭제 (알림 끄기·로그아웃 시)

**Request Body**
```json
{ "token": "<fcm-registration-token>" }
```

**Response `200`**
```json
{ "success": true }
```

---

## 13. 공통 에러 코드

| HTTP 코드 | Error Code | 설명 |
|---|---|---|
| `400` | `BAD_REQUEST` | 잘못된 요청 (입력값 검증 실패) |
| `401` | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| `403` | `FORBIDDEN` | 권한 없음 |
| `404` | `NOT_FOUND` | 리소스 없음 |
| `409` | `CONFLICT` | 중복 데이터 (이메일/닉네임 중복, 재신청 등) |
| `429` | `TOO_MANY_REQUESTS` | 요청 횟수 초과 |
| `500` | `INTERNAL_ERROR` | 서버 내부 오류 |

**에러 응답 형식**
```json
{
  "success": false,
  "data": null,
  "message": "이미 사용 중인 이메일입니다.",
  "errorCode": "CONFLICT"
}
```
