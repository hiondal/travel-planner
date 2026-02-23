# travel-planner — Flutter 프론트엔드

여행 중 실시간 일정 최적화 가이드 앱 프론트엔드

## 기술 스택

| 항목 | 결정 |
|------|------|
| 플랫폼 | iOS + Android (Flutter 크로스플랫폼) |
| 언어 | Dart 3.x |
| 프레임워크 | Flutter 3.x |
| 상태관리 | flutter_riverpod 2.x |
| 라우팅 | go_router |
| HTTP 클라이언트 | Dio (JWT 인터셉터) |
| 보안 저장소 | flutter_secure_storage |
| UI 디자인 | Material Design 3 (다크 테마) |

## 시작하기

```bash
# 의존성 설치
flutter pub get

# 코드 생성 (Riverpod, Freezed, json_serializable)
dart run build_runner build --delete-conflicting-outputs

# 개발 실행 (Prism Mock 서버: http://localhost:4010)
flutter run
```

## 폴더 구조

```
lib/
├── main.dart          # 앱 진입점 (Firebase 초기화, ProviderScope)
├── app.dart           # MaterialApp.router
├── core/              # 앱 전역 인프라
│   ├── config/        # 환경 설정
│   ├── constants/     # 색상, 간격, 타이포그래피 토큰
│   ├── network/       # Dio 클라이언트, 인터셉터
│   ├── routing/       # go_router 라우트 정의
│   ├── theme/         # Material Design 3 다크 테마
│   └── utils/         # secure_storage 등 유틸
├── features/          # 기능별 수직 분리
│   ├── auth/          # 인증/온보딩
│   ├── schedule/      # 일정 관리
│   ├── briefing/      # 브리핑 + 대안 카드 + Paywall
│   ├── monitoring/    # 상태 배지 바텀시트
│   ├── payment/       # 결제/구독
│   └── profile/       # 마이페이지
└── shared/            # 복수 feature 공유
    ├── models/        # StatusLevel, SubscriptionTier 등
    ├── providers/     # AppUserProvider (전역 사용자 상태)
    └── widgets/       # StatusBadge 등 공통 위젯
```

## 디자인 토큰

style-guide.md 기반. 모든 색상/간격/타이포그래피는 `core/constants/`에서 참조.

| 파일 | 내용 |
|------|------|
| `app_colors.dart` | HEX 색상 상수 (배경/텍스트/상태배지/액센트) |
| `app_spacing.dart` | 4px Grid 간격 토큰 |
| `app_typography.dart` | Pretendard 폰트 TextStyle |

## 상태 배지

4단계 상태를 색상 + 아이콘으로 표시 (WCAG 2.1 AA 준수):

| 상태 | 색상 | 아이콘 |
|------|------|--------|
| 정상 (SAFE) | #34C759 (Green) | check_circle |
| 주의 (CAUTION) | #FFD60A (Yellow) | warning_amber |
| 위험 (DANGER) | #FF3B30 (Red) | cancel |
| 미확인 (UNKNOWN) | #8E8E93 (Gray) | help |
