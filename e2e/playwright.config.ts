import { defineConfig } from '@playwright/test';

/**
 * travel-planner E2E 테스트 설정
 *
 * API 레벨 E2E 테스트 전용 설정.
 * 브라우저 UI가 아닌 HTTP request API로 백엔드 서비스 간 연동을 검증한다.
 *
 * 실행 전제:
 *   - 7개 백엔드 서비스가 docker-compose로 모두 기동되어야 함
 *   - docker-compose -f docker/docker-compose.yml up -d
 */
export default defineConfig({
  testDir: './tests',
  timeout: 30000,
  retries: 1,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['json', { outputFile: 'test-results/results.json' }],
  ],

  use: {
    // 기본 baseURL은 AUTH 서비스 (TC-01 인증 플로우 시작점)
    baseURL: 'http://localhost:8081',
    extraHTTPHeaders: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
    // API 레벨 테스트이므로 브라우저 설정 불필요
    ignoreHTTPSErrors: false,
  },

  // 단일 프로젝트 (API 요청 기반, 브라우저 없음)
  projects: [
    {
      name: 'api-e2e',
      use: {},
    },
  ],
});
