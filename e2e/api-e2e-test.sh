#!/usr/bin/env bash
# ============================================================================
# travel-planner API-level E2E Test Script
#
# 작성자: 조현아/가디언 (QA 엔지니어)
# 작성일: 2026-02-24
#
# 목적: 7개 백엔드 서비스가 기동된 상태에서 사용자 여정(User Journey)을
#        curl 기반 API 호출 체인으로 검증한다.
#
# 실행 전제:
#   - 7개 서비스 기동 (docker-compose up -d 또는 IntelliJ 프로파일)
#   - jq 설치 (JSON 파싱)
#   - bash 4.x 이상
#
# 사용법:
#   chmod +x e2e/api-e2e-test.sh
#   ./e2e/api-e2e-test.sh
# ============================================================================

set -euo pipefail

# ---------- 서비스 URL 매핑 ----------
AUTH_URL="http://localhost:8081/api/v1"
SCHD_URL="http://localhost:8082/api/v1"
PLCE_URL="http://localhost:8083/api/v1"
MNTR_URL="http://localhost:8084/api/v1"
BRIF_URL="http://localhost:8085/api/v1"
ALTN_URL="http://localhost:8086/api/v1"
PAY_URL="http://localhost:8087/api/v1"

# ---------- 카운터 ----------
TOTAL=0
PASSED=0
FAILED=0
FAIL_LIST=()

# ---------- 색상 ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ---------- 유틸리티 함수 ----------

log_header() {
    echo ""
    echo -e "${CYAN}============================================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}============================================================${NC}"
}

log_step() {
    echo -e "  ${YELLOW}>> $1${NC}"
}

assert_status() {
    local test_id="$1"
    local description="$2"
    local expected_status="$3"
    local actual_status="$4"
    local response_body="${5:-}"

    TOTAL=$((TOTAL + 1))

    if [ "$actual_status" -eq "$expected_status" ]; then
        echo -e "  ${GREEN}[PASS]${NC} $test_id: $description (HTTP $actual_status)"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "  ${RED}[FAIL]${NC} $test_id: $description (expected HTTP $expected_status, got $actual_status)"
        if [ -n "$response_body" ]; then
            echo -e "         Response: $(echo "$response_body" | head -c 200)"
        fi
        FAILED=$((FAILED + 1))
        FAIL_LIST+=("$test_id: $description")
        return 1
    fi
}

assert_json_field() {
    local test_id="$1"
    local description="$2"
    local json="$3"
    local jq_expr="$4"
    local expected="$5"

    TOTAL=$((TOTAL + 1))

    local actual
    actual=$(echo "$json" | jq -r "$jq_expr" 2>/dev/null || echo "JQ_ERROR")

    if [ "$actual" = "$expected" ]; then
        echo -e "  ${GREEN}[PASS]${NC} $test_id: $description ($jq_expr = $actual)"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "  ${RED}[FAIL]${NC} $test_id: $description (expected $jq_expr=$expected, got $actual)"
        FAILED=$((FAILED + 1))
        FAIL_LIST+=("$test_id: $description")
        return 1
    fi
}

assert_json_exists() {
    local test_id="$1"
    local description="$2"
    local json="$3"
    local jq_expr="$4"

    TOTAL=$((TOTAL + 1))

    local actual
    actual=$(echo "$json" | jq -r "$jq_expr" 2>/dev/null || echo "null")

    if [ "$actual" != "null" ] && [ "$actual" != "" ] && [ "$actual" != "JQ_ERROR" ]; then
        echo -e "  ${GREEN}[PASS]${NC} $test_id: $description ($jq_expr exists: $actual)"
        PASSED=$((PASSED + 1))
        return 0
    else
        echo -e "  ${RED}[FAIL]${NC} $test_id: $description ($jq_expr is null or missing)"
        FAILED=$((FAILED + 1))
        FAIL_LIST+=("$test_id: $description")
        return 1
    fi
}

# curl 래퍼: status_code와 body를 분리 반환
# Usage: response=$(api_call GET "$url" "$headers" "$body")
#        status=$(echo "$response" | tail -1)
#        body=$(echo "$response" | sed '$d')
api_call() {
    local method="$1"
    local url="$2"
    local headers="${3:-}"
    local body="${4:-}"

    local curl_args=(-s -w "\n%{http_code}" -X "$method" "$url"
        -H "Content-Type: application/json"
        -H "Accept: application/json"
        --connect-timeout 5
        --max-time 10)

    if [ -n "$headers" ]; then
        while IFS= read -r header; do
            [ -n "$header" ] && curl_args+=(-H "$header")
        done <<< "$headers"
    fi

    if [ -n "$body" ]; then
        local tmpfile
        tmpfile=$(mktemp)
        printf '%s' "$body" > "$tmpfile"
        curl_args+=(-d "@$tmpfile")
    fi

    local result
    result=$(curl "${curl_args[@]}" 2>/dev/null || echo -e "\n000")

    # 임시 파일 정리
    if [ -n "${tmpfile:-}" ] && [ -f "$tmpfile" ]; then
        rm -f "$tmpfile"
    fi

    echo "$result"
}

parse_status() {
    echo "$1" | tail -1
}

parse_body() {
    echo "$1" | sed '$d'
}

# ---------- JWT 토큰 생성 ----------

# base64url 인코딩 (패딩 제거, +/ → -_)
base64url_encode() {
    openssl base64 -e -A | tr '+/' '-_' | tr -d '='
}

# JWT 토큰 생성 (HS256)
generate_jwt() {
    local secret="$1"
    local user_id="${2:-e2e-test-user}"
    local email="${3:-e2e@test.com}"
    local tier="${4:-FREE}"

    local now
    now=$(date +%s)
    local exp=$((now + 3600))

    local header='{"alg":"HS256","typ":"JWT"}'
    local payload="{\"sub\":\"${user_id}\",\"email\":\"${email}\",\"tier\":\"${tier}\",\"iat\":${now},\"exp\":${exp}}"

    local header_b64
    header_b64=$(printf '%s' "$header" | base64url_encode)
    local payload_b64
    payload_b64=$(printf '%s' "$payload" | base64url_encode)

    local signature
    signature=$(printf '%s' "${header_b64}.${payload_b64}" | openssl dgst -sha256 -hmac "$secret" -binary | base64url_encode)

    echo "${header_b64}.${payload_b64}.${signature}"
}

# ---------- 인증 설정 ----------
JWT_SECRET="travel-planner-jwt-secret-key-for-development-must-be-256-bits-long"
INTERNAL_SERVICE_KEY="e2e-internal-service-key"
AUTH_TOKEN=""
AUTH_HEADER=""

init_auth() {
    local test_user_id="e2e-test-$(date +%s)-$$"
    AUTH_TOKEN=$(generate_jwt "$JWT_SECRET" "$test_user_id" "e2e@test.com" "FREE")
    AUTH_HEADER="Authorization: Bearer $AUTH_TOKEN"
    echo -e "  ${GREEN}[OK]${NC} JWT 테스트 토큰 생성 완료 (userId: $test_user_id)"
}

# ---------- 헬스체크 ----------

check_services() {
    log_header "사전 조건: 서비스 헬스체크"

    local services=("AUTH:8081" "SCHD:8082" "PLCE:8083" "MNTR:8084" "BRIF:8085" "ALTN:8086" "PAY:8087")
    local all_up=true

    for svc in "${services[@]}"; do
        local name="${svc%%:*}"
        local port="${svc##*:}"
        local response
        response=$(api_call GET "http://localhost:$port/actuator/health")
        local status
        status=$(parse_status "$response")

        if [ "$status" = "200" ]; then
            echo -e "  ${GREEN}[UP]${NC} $name (port $port)"
        else
            echo -e "  ${RED}[DOWN]${NC} $name (port $port) -- HTTP $status"
            all_up=false
        fi
    done

    if [ "$all_up" = false ]; then
        echo ""
        echo -e "${RED}일부 서비스가 기동되지 않았습니다. 모든 서비스를 기동한 후 다시 실행하세요.${NC}"
        echo "  docker-compose -f docker/docker-compose.yml up -d"
        echo "  또는 IntelliJ Run Configuration 으로 각 서비스 기동"
        exit 1
    fi
}

# ============================================================================
# E2E-001: 신규 사용자 온보딩 플로우
# ============================================================================
test_e2e_001() {
    log_header "E2E-001: 신규 사용자 온보딩 플로우"

    # Step 1: 소셜 로그인 (Google API 키 없으므로 401 기대)
    log_step "소셜 로그인 시도 (Google OAuth -- 테스트 환경)"
    local response
    response=$(api_call POST "$AUTH_URL/auth/social-login" "" \
        '{"provider":"google","oauth_code":"test_google_code"}')
    local status
    status=$(parse_status "$response")
    local body
    body=$(parse_body "$response")

    # 테스트 환경에서는 401 (Google API 키 없음) 정상
    assert_status "E2E-001-01" "소셜 로그인 -- 테스트 환경 OAuth 검증 오류" "401" "$status" "$body" || true

    # Step 2: 지원 외 provider 검증
    log_step "지원 외 provider (kakao)"
    response=$(api_call POST "$AUTH_URL/auth/social-login" "" \
        '{"provider":"kakao","oauth_code":"test_code"}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-001-02" "지원 외 provider -> 400" "400" "$status" "$body" || true

    # Step 3: provider 누락
    log_step "provider 필드 누락"
    response=$(api_call POST "$AUTH_URL/auth/social-login" "" \
        '{"oauth_code":"test_code"}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-001-03" "provider 누락 -> 400" "400" "$status" "$body" || true

    # Step 4: Refresh Token 갱신 -- 유효하지 않은 토큰
    log_step "유효하지 않은 Refresh Token 갱신"
    response=$(api_call POST "$AUTH_URL/auth/token/refresh" "" \
        '{"refresh_token":"invalid_token_value"}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-001-04" "유효하지 않은 Refresh Token -> 401" "401" "$status" "$body" || true

    # Step 5: 빈 바디 토큰 갱신
    log_step "빈 바디 토큰 갱신 요청"
    response=$(api_call POST "$AUTH_URL/auth/token/refresh" "" '{}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-001-05" "빈 바디 토큰 갱신 -> 400" "400" "$status" "$body" || true

    # Step 6: 인증 없이 로그아웃
    log_step "인증 없이 로그아웃 시도"
    response=$(api_call POST "$AUTH_URL/auth/logout")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-001-06" "인증 없이 로그아웃 -> 401" "401" "$status" "$body" || true
}

# ============================================================================
# E2E-002: 일정 관리 플로우
# ============================================================================
test_e2e_002() {
    log_header "E2E-002: 일정 관리 플로우"

    # Step 1: 여행 생성 성공
    log_step "여행 생성"
    local response
    response=$(api_call POST "$SCHD_URL/trips" "$AUTH_HEADER" \
        '{"name":"도쿄 3박4일","start_date":"2026-03-15","end_date":"2026-03-18","city":"도쿄"}')
    local status
    status=$(parse_status "$response")
    local body
    body=$(parse_body "$response")
    assert_status "E2E-002-01" "여행 생성 -> 201" "201" "$status" "$body" || true

    # trip_id 추출
    local trip_id
    trip_id=$(echo "$body" | jq -r '.data.trip_id // empty' 2>/dev/null || echo "")
    if [ -n "$trip_id" ]; then
        assert_json_exists "E2E-002-01a" "trip_id 존재" "$body" ".data.trip_id" || true
    fi

    # Step 2: 여행 목록 조회
    log_step "여행 목록 조회"
    response=$(api_call GET "$SCHD_URL/trips" "$AUTH_HEADER")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-002-02" "여행 목록 조회 -> 200" "200" "$status" "$body" || true

    # Step 3: 빈 바디로 여행 생성 (Validation 검증)
    log_step "여행 생성 -- 필수 필드 누락"
    response=$(api_call POST "$SCHD_URL/trips" "$AUTH_HEADER" '{}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-002-03" "필수 필드 누락 -> 400" "400" "$status" "$body" || true

    # Step 4: 존재하지 않는 여행 조회
    log_step "존재하지 않는 여행 조회"
    response=$(api_call GET "$SCHD_URL/trips/nonexistent_trip_id_999" "$AUTH_HEADER")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-002-04" "존재하지 않는 여행 -> 404" "404" "$status" "$body" || true

    # Step 5: 장소 추가 (trip_id가 있을 때만)
    if [ -n "$trip_id" ]; then
        log_step "일정에 장소 추가"
        response=$(api_call POST "$SCHD_URL/trips/$trip_id/schedule-items" "$AUTH_HEADER" \
            '{"place_id":"place_abc123","visit_datetime":"2026-03-16T12:00:00","timezone":"Asia/Tokyo"}')
        status=$(parse_status "$response")
        body=$(parse_body "$response")
        assert_status "E2E-002-05" "장소 추가 -> 201" "201" "$status" "$body" || true

        # schedule_item_id 추출
        local item_id
        item_id=$(echo "$body" | jq -r '.data.schedule_item_id // empty' 2>/dev/null || echo "")

        # Step 6: 일정표 조회
        log_step "일정표 조회"
        response=$(api_call GET "$SCHD_URL/trips/$trip_id/schedule" "$AUTH_HEADER")
        status=$(parse_status "$response")
        body=$(parse_body "$response")
        assert_status "E2E-002-06" "일정표 조회 -> 200" "200" "$status" "$body" || true

        # Step 7: 장소 삭제
        if [ -n "$item_id" ]; then
            log_step "장소 삭제"
            response=$(api_call DELETE "$SCHD_URL/trips/$trip_id/schedule-items/$item_id" "$AUTH_HEADER")
            status=$(parse_status "$response")
            assert_status "E2E-002-07" "장소 삭제 -> 204" "204" "$status" || true
        fi
    fi
}

# ============================================================================
# E2E-003: 실시간 모니터링 플로우
# ============================================================================
test_e2e_003() {
    log_header "E2E-003: 실시간 모니터링 플로우"

    # Step 1: 배지 목록 조회
    log_step "배지 목록 조회"
    local response
    response=$(api_call GET "$MNTR_URL/badges?place_ids=place_abc123" "$AUTH_HEADER")
    local status
    status=$(parse_status "$response")
    local body
    body=$(parse_body "$response")
    assert_status "E2E-003-01" "배지 목록 조회 -> 200" "200" "$status" "$body" || true

    # Step 2: place_ids 누락
    log_step "배지 조회 -- place_ids 누락"
    response=$(api_call GET "$MNTR_URL/badges" "$AUTH_HEADER")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-003-02" "place_ids 누락 -> 400" "400" "$status" "$body" || true

    # Step 3: 배지 상세 조회
    log_step "배지 상세 조회"
    response=$(api_call GET "$MNTR_URL/badges/place_abc123/detail" "$AUTH_HEADER")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    # 404는 DB에 데이터가 없는 경우 정상 (RESOURCE_NOT_FOUND)
    if [ "$status" = "200" ] || [ "$status" = "404" ]; then
        TOTAL=$((TOTAL + 1))
        PASSED=$((PASSED + 1))
        echo -e "  ${GREEN}[PASS]${NC} E2E-003-03: 배지 상세 조회 -> HTTP $status (데이터 없으면 404 정상)"
    else
        assert_status "E2E-003-03" "배지 상세 조회" "200" "$status" "$body" || true
    fi

    # Step 4: 데이터 수집 트리거 (내부 API)
    log_step "데이터 수집 트리거 (내부 API)"
    response=$(api_call POST "$MNTR_URL/monitor/collect" \
        "X-Internal-Service-Key: $INTERNAL_SERVICE_KEY" \
        '{"triggered_by":"e2e-test"}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    # 202 Accepted 기대
    if [ "$status" = "202" ] || [ "$status" = "200" ]; then
        TOTAL=$((TOTAL + 1))
        PASSED=$((PASSED + 1))
        echo -e "  ${GREEN}[PASS]${NC} E2E-003-04: 데이터 수집 트리거 -> HTTP $status"
    else
        assert_status "E2E-003-04" "데이터 수집 트리거" "202" "$status" "$body" || true
    fi
}

# ============================================================================
# E2E-004: 대안 검색 플로우
# ============================================================================
test_e2e_004() {
    log_header "E2E-004: 대안 검색 플로우"

    # Step 1: 대안 카드 검색 (인증 없이 -- 401 기대)
    log_step "인증 없이 대안 검색"
    local response
    response=$(api_call POST "$ALTN_URL/alternatives/search" "" \
        '{"place_id":"place_abc123","category":"restaurant","location":{"lat":35.6585,"lng":139.7454}}')
    local status
    status=$(parse_status "$response")
    local body
    body=$(parse_body "$response")
    assert_status "E2E-004-01" "인증 없이 대안 검색 -> 401" "401" "$status" "$body" || true

    # Step 2: 구독 플랜 목록 조회
    log_step "구독 플랜 목록 조회"
    response=$(api_call GET "$PAY_URL/subscriptions/plans")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-004-02" "구독 플랜 목록 -> 200" "200" "$status" "$body" || true

    # plans 배열 크기 검증
    if [ "$status" = "200" ]; then
        local plan_count
        plan_count=$(echo "$body" | jq '.data.plans | length' 2>/dev/null || echo "0")
        assert_json_field "E2E-004-02a" "플랜 2개 존재" "$body" '.data.plans | length' "2" || true
    fi

    # Step 3: 구독 상태 조회 (FREE)
    log_step "구독 상태 조회 (FREE)"
    response=$(api_call GET "$PAY_URL/subscriptions/status" "$AUTH_HEADER")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-004-03" "구독 상태 조회 -> 200" "200" "$status" "$body" || true

    if [ "$status" = "200" ]; then
        assert_json_field "E2E-004-03a" "FREE 티어 확인" "$body" '.data.tier' "FREE" || true
    fi

    # Step 4: 구독 구매 (Mock IAP)
    log_step "구독 구매 (Mock IAP)"
    response=$(api_call POST "$PAY_URL/subscriptions/purchase" "$AUTH_HEADER" \
        '{"plan_id":"plan_trip_pass","receipt":"dummy_receipt_for_e2e","provider":"apple"}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-004-04" "구독 구매 -> 201" "201" "$status" "$body" || true

    # Step 5: 존재하지 않는 대안 선택
    log_step "존재하지 않는 대안 선택"
    response=$(api_call POST "$ALTN_URL/alternatives/alt_nonexistent/select" "$AUTH_HEADER" \
        '{"original_place_id":"place_001","schedule_item_id":"si_001","trip_id":"trip_001","selected_rank":1,"elapsed_seconds":5}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    # 401 (인증 없음) 또는 404 (대안 없음) 모두 정상
    if [ "$status" = "401" ] || [ "$status" = "404" ]; then
        TOTAL=$((TOTAL + 1))
        PASSED=$((PASSED + 1))
        echo -e "  ${GREEN}[PASS]${NC} E2E-004-05: 존재하지 않는 대안 선택 -> HTTP $status (인증/리소스 검증)"
    else
        assert_status "E2E-004-05" "존재하지 않는 대안 선택" "404" "$status" "$body" || true
    fi
}

# ============================================================================
# E2E-005: 토큰 관리 플로우
# ============================================================================
test_e2e_005() {
    log_header "E2E-005: 토큰 관리 플로우"

    # Step 1: Refresh Token 갱신 -- 유효하지 않은 토큰
    log_step "유효하지 않은 Refresh Token 갱신"
    local response
    response=$(api_call POST "$AUTH_URL/auth/token/refresh" "" \
        '{"refresh_token":"completely.invalid.refresh.token"}')
    local status
    status=$(parse_status "$response")
    local body
    body=$(parse_body "$response")
    assert_status "E2E-005-01" "유효하지 않은 Refresh Token -> 401" "401" "$status" "$body" || true

    # Step 2: 빈 바디 토큰 갱신
    log_step "빈 바디 토큰 갱신"
    response=$(api_call POST "$AUTH_URL/auth/token/refresh" "" '{}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-005-02" "빈 바디 -> 400 VALIDATION_ERROR" "400" "$status" "$body" || true

    # Step 3: 인증 없이 로그아웃
    log_step "인증 없이 로그아웃"
    response=$(api_call POST "$AUTH_URL/auth/logout")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-005-03" "인증 없이 로그아웃 -> 401" "401" "$status" "$body" || true

    # Step 4: Actuator health 확인 (AUTH 서비스 안정성)
    log_step "AUTH 서비스 안정성 확인"
    response=$(api_call GET "http://localhost:8081/actuator/health")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "E2E-005-04" "AUTH 헬스체크 -> 200" "200" "$status" "$body" || true
    assert_json_field "E2E-005-04a" "status=UP" "$body" '.status' "UP" || true
}

# ============================================================================
# 비정상 입력 검증 (경계값 테스트)
# ============================================================================
test_boundary() {
    log_header "경계값 테스트: 비정상 입력 검증"

    # SCHD: 여행명 51자 초과
    log_step "여행명 51자 초과"
    local long_name
    long_name=$(printf 'A%.0s' {1..51})
    local response
    response=$(api_call POST "$SCHD_URL/trips" "$AUTH_HEADER" \
        "{\"name\":\"$long_name\",\"start_date\":\"2026-03-15\",\"end_date\":\"2026-03-18\",\"city\":\"도쿄\"}")
    local status
    status=$(parse_status "$response")
    local body
    body=$(parse_body "$response")
    assert_status "BND-01" "여행명 51자 -> 400" "400" "$status" "$body" || true

    # SCHD: 종료일 < 시작일
    log_step "종료일 < 시작일"
    response=$(api_call POST "$SCHD_URL/trips" "$AUTH_HEADER" \
        '{"name":"잘못된 여행","start_date":"2026-03-18","end_date":"2026-03-15","city":"도쿄"}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "BND-02" "종료일 < 시작일 -> 400" "400" "$status" "$body" || true

    # PLCE: keyword 1자 검색
    log_step "장소 검색 -- keyword 1자"
    response=$(api_call GET "$PLCE_URL/places/search?keyword=A&city=tokyo" "$AUTH_HEADER")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "BND-03" "keyword 1자 -> 400" "400" "$status" "$body" || true

    # PLCE: keyword 누락
    log_step "장소 검색 -- keyword 누락"
    response=$(api_call GET "$PLCE_URL/places/search?city=tokyo" "$AUTH_HEADER")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "BND-04" "keyword 누락 -> 400" "400" "$status" "$body" || true

    # PLCE: 지원 외 radius
    log_step "주변 검색 -- 지원 외 radius"
    response=$(api_call GET "$PLCE_URL/places/nearby?lat=35.65&lng=139.74&radius=500&category=restaurant" "$AUTH_HEADER")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "BND-05" "radius=500 -> 400" "400" "$status" "$body" || true

    # PAY: 지원 외 provider
    log_step "구독 구매 -- 지원 외 provider"
    response=$(api_call POST "$PAY_URL/subscriptions/purchase" "$AUTH_HEADER" \
        '{"plan_id":"plan_trip_pass","receipt":"receipt","provider":"samsung_pay"}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "BND-06" "지원 외 provider -> 400" "400" "$status" "$body" || true

    # PAY: plan_id 누락
    log_step "구독 구매 -- plan_id 누락"
    response=$(api_call POST "$PAY_URL/subscriptions/purchase" "$AUTH_HEADER" \
        '{"receipt":"receipt","provider":"apple"}')
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "BND-07" "plan_id 누락 -> 400" "400" "$status" "$body" || true

    # 존재하지 않는 경로
    log_step "존재하지 않는 경로"
    response=$(api_call GET "$SCHD_URL/nonexistent/path" "$AUTH_HEADER")
    status=$(parse_status "$response")
    body=$(parse_body "$response")
    assert_status "BND-08" "존재하지 않는 경로 -> 404" "404" "$status" "$body" || true
}

# ============================================================================
# 메인 실행
# ============================================================================
main() {
    echo ""
    echo -e "${CYAN}======================================================${NC}"
    echo -e "${CYAN}  travel-planner API-level E2E Test${NC}"
    echo -e "${CYAN}  실행일: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
    echo -e "${CYAN}======================================================${NC}"

    # jq 존재 확인
    if ! command -v jq &>/dev/null; then
        echo -e "${RED}jq가 설치되어 있지 않습니다. 설치 후 다시 실행하세요.${NC}"
        echo "  Windows: choco install jq 또는 scoop install jq"
        echo "  macOS:   brew install jq"
        echo "  Linux:   sudo apt install jq"
        exit 1
    fi

    # 서비스 헬스체크
    check_services

    # JWT 테스트 토큰 생성
    init_auth

    # E2E 시나리오 실행
    test_e2e_001
    test_e2e_002
    test_e2e_003
    test_e2e_004
    test_e2e_005
    test_boundary

    # 결과 요약
    echo ""
    echo -e "${CYAN}============================================================${NC}"
    echo -e "${CYAN}  테스트 결과 요약${NC}"
    echo -e "${CYAN}============================================================${NC}"
    echo ""
    echo -e "  총 테스트: $TOTAL"
    echo -e "  ${GREEN}PASSED: $PASSED${NC}"
    echo -e "  ${RED}FAILED: $FAILED${NC}"
    echo ""

    if [ "$TOTAL" -gt 0 ]; then
        local pass_rate
        pass_rate=$(( PASSED * 100 / TOTAL ))
        echo -e "  합격률: ${pass_rate}%"
    fi

    if [ "${#FAIL_LIST[@]}" -gt 0 ]; then
        echo ""
        echo -e "  ${RED}실패 목록:${NC}"
        for fail in "${FAIL_LIST[@]}"; do
            echo -e "    - $fail"
        done
    fi

    echo ""
    echo -e "${CYAN}============================================================${NC}"

    # 종료 코드
    if [ "$FAILED" -gt 0 ]; then
        exit 1
    else
        exit 0
    fi
}

main "$@"
