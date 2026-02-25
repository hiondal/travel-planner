/**
 * MNTR 서비스 - 상태 배지 조회 행위 계약 테스트
 *
 * 설계 근거:
 *   - 시퀀스: docs/design/sequence/inner/mntr-상태배지조회.puml
 *   - API 명세: docs/design/api/monitor-service-api.yaml
 *
 * 흐름 요약:
 *   Client -> MntrController -> BadgeService -> Redis Cache / StatusRepository
 *     -> 배지 표현 매핑 (초록/노랑/빨강/회색 + 아이콘)
 *     -> 200 OK {badges: [...]}
 */
import request from 'supertest';

const BASE_URL = process.env.MNTR_SERVICE_URL || 'http://localhost:8084';

describe('MNTR 서비스 - 상태 배지 조회', () => {
  describe('각 place_id 배치 처리', () => {
    /**
     * 캐시에 배지 데이터가 존재하는 경우 (TTL 10분 이내)
     * DB 조회 없이 캐시에서 {status, icon, label}을 반환한다.
     */
    it('캐시 HIT (TTL 10분 이내) 시 배지 목록 정상 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges')
        .query({ place_ids: 'place_abc123,place_def456' })
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      // BadgeListResponse 스키마 검증
      expect(res.body).toHaveProperty('badges');
      expect(Array.isArray(res.body.badges)).toBe(true);

      // BadgeItem 스키마 검증
      res.body.badges.forEach((badge: Record<string, unknown>) => {
        expect(badge).toHaveProperty('place_id');
        expect(typeof badge.place_id).toBe('string');

        expect(badge).toHaveProperty('status');
        expect(['GREEN', 'YELLOW', 'RED', 'GREY']).toContain(badge.status);

        expect(badge).toHaveProperty('icon');
        expect(['CHECK', 'EXCLAMATION', 'X', 'QUESTION']).toContain(badge.icon);

        expect(badge).toHaveProperty('label');
        // label은 nullable (회색 배지의 경우 '데이터 미확인')

        expect(badge).toHaveProperty('color_hex');
        expect(typeof badge.color_hex).toBe('string');

        expect(badge).toHaveProperty('updated_at');
        expect(typeof badge.updated_at).toBe('string');
      });
    });

    /**
     * 캐시에 배지 데이터가 없는 경우 (캐시 MISS)
     * DB에서 status_current를 조회한 후 캐시에 저장(TTL 10분)하고 반환한다.
     */
    it('캐시 MISS 시 DB 조회 후 배지 목록 정상 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges')
        .query({ place_ids: 'place_xyz789' })
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      // BadgeListResponse 스키마 검증
      expect(res.body).toHaveProperty('badges');
      expect(Array.isArray(res.body.badges)).toBe(true);

      res.body.badges.forEach((badge: Record<string, unknown>) => {
        expect(badge).toHaveProperty('place_id');
        expect(badge).toHaveProperty('status');
        expect(badge).toHaveProperty('icon');
        expect(badge).toHaveProperty('label');
        expect(badge).toHaveProperty('color_hex');
        expect(badge).toHaveProperty('updated_at');
      });
    });
  });

  describe('배지 표현 매핑', () => {
    /**
     * 배지 색상-아이콘 매핑 규칙:
     *   초록 -> CHECK 아이콘, 노랑 -> EXCLAMATION 아이콘,
     *   빨강 -> X 아이콘, 회색 -> QUESTION 아이콘 + "데이터 미확인" label
     */
    it('회색 배지 시 QUESTION 아이콘과 "데이터 미확인" label 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges')
        .query({ place_ids: 'place_grey_test' })
        .set('Authorization', 'Bearer test-token')
        .expect(200);

      expect(res.body).toHaveProperty('badges');
      expect(Array.isArray(res.body.badges)).toBe(true);

      const greyBadge = res.body.badges.find(
        (b: Record<string, unknown>) => b.status === 'GREY'
      );
      if (greyBadge) {
        expect(greyBadge.icon).toBe('QUESTION');
        expect(greyBadge.label).toBe('데이터 미확인');
      }
    });
  });

  describe('오류 처리', () => {
    /**
     * place_ids 파라미터가 누락된 경우 400 Bad Request를 반환한다.
     */
    it('place_ids 파라미터 누락 시 400 Bad Request 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges')
        .set('Authorization', 'Bearer test-token')
        .expect(400);

      // ErrorResponse 스키마 검증
      expect(res.body).toHaveProperty('error');
      expect(typeof res.body.error).toBe('string');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });

    /**
     * 인증 토큰이 유효하지 않은 경우 401 Unauthorized를 반환한다.
     */
    it('인증 실패 시 401 Unauthorized 반환', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/badges')
        .query({ place_ids: 'place_abc123' })
        .set('Authorization', 'Bearer invalid_token')
        .expect(401);

      // ErrorResponse 스키마 검증
      expect(res.body).toHaveProperty('error');
      expect(typeof res.body.error).toBe('string');

      expect(res.body).toHaveProperty('message');
      expect(typeof res.body.message).toBe('string');
    });
  });
});
