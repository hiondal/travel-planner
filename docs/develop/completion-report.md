# 개발 완료

## 구현된 기능
- 백엔드 API: 26개 엔드포인트 (7개 서비스)
  - AUTH(8081): 소셜 로그인, 토큰 갱신, 동의 저장, 로그아웃, 프로필 조회 — 5개
  - SCHD(8082): 여행 CRUD, 장소 추가/삭제/교체, 일정표 조회 — 7개
  - PLCE(8083): 장소 검색, 상세 조회, 주변 검색 — 3개
  - MNTR(8084): 배지 목록, 배지 상세, 데이터 수집 트리거 — 3개
  - BRIF(8085): 브리핑 생성, 조회 + 스케줄러 트리거 — 3개
  - ALTN(8086): 대안 검색, 대안 선택 — 2개
  - PAY(8087): 플랜 목록, 구독 구매, 구독 상태 조회 — 3개
- 프론트엔드 컴포넌트: 20개 페이지 (Flutter)
  - 인증: 스플래시, 온보딩, 로그인 — 3개
  - 일정: 여행 목록, 여행 생성, 일정 상세, 장소 검색, 시간 선택, 변경 결과, 권한 요청 — 7개
  - 브리핑: 목록, 상세, 대안 카드, 페이월 — 4개
  - 결제: 체크아웃, 성공 — 2개
  - 프로필: 프로필, 구독, 위치 동의, 알림 설정 — 4개
- AI 기능: Phase 1 범위 외 (SKIP)
- 테스트 통과율: 242/242 (100%)
  - 단위 테스트 124건, 통합 테스트 80건, E2E 테스트 38건
- 프론트엔드 검증: Flutter 3.27.4
  - `flutter analyze`: error 0건 (info/warning 104건)
  - `flutter build web`: ✓ Built build/web
  - Architect 코드 리뷰 APPROVED (HIGH 버그 2건 수정 완료)
    - AuthInterceptor 동시 401 race condition → Completer 패턴 적용
    - AlternativeCardPage tripId 빈문자열 fallback → briefing 로딩 게이트 추가

## 백킹서비스 설정
- 데이터베이스: PostgreSQL 16 (7개 DB — auth, schedule, place, monitor, briefing, alternative, payment)
- 캐시: Redis 7 (DB 0~6, 서비스별 격리)
- MQ: 해당 없음 (Phase 1)
- Mock 서버: 해당 없음 (실제 API 직접 연동)

## 직접 실행 가이드

### 1. 백킹서비스 기동
```bash
docker compose up -d
docker compose ps    # 모든 서비스 running 확인
```

### 2. 백엔드 서비스 기동
```bash
python3 tools/run-intellij-service-profile.py --config-dir . --delay 5
# 헬스체크 확인 (7개 서비스)
for port in 8081 8082 8083 8084 8085 8086 8087; do
  echo "Port $port: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:$port/actuator/health)"
done
```

### 3. 프론트엔드 기동
```bash
cd frontend && flutter pub get && flutter run -d chrome --web-port 3000
# 브라우저에서 http://localhost:3000 접속
```

### 4. 테스트 실행
```bash
# 단위 + 통합 테스트
./gradlew clean test

# E2E 테스트 (7개 서비스 기동 상태에서)
bash e2e/api-e2e-test.sh
```

### 5. 서비스 중지
```bash
# 백엔드 중지
# Linux/Mac:
kill $(pgrep -f run-intellij-service-profile.py) && pkill -f 'java.*spring'
# Windows:
taskkill //F //IM java.exe

# 백킹서비스 중지
docker compose down
```

## 다음 단계
`/npd:deploy` 로 배포를 시작하세요.
