/* ============================================
   travel-planner Prototype - Common JS
   Web Components + 샘플 데이터 + 네비게이션 + 공통 유틸리티
   file:// 프로토콜 동작 지원 / 외부 라이브러리 없음
   ============================================ */

'use strict';

// ============================================================
// SVG Icons (Lucide-style inline SVGs)
// ============================================================
const ICONS = {
  // Navigation
  chevronLeft: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>',
  chevronRight: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>',
  chevronDown: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>',
  x: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>',

  // Tab bar
  calendarDays: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/><path d="M8 14h.01"/><path d="M12 14h.01"/><path d="M16 14h.01"/><path d="M8 18h.01"/><path d="M12 18h.01"/><path d="M16 18h.01"/></svg>',
  bell: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>',
  user: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',

  // Status badges
  checkCircle: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="m9 11 3 3L22 4"/></svg>',
  alertTriangle: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg>',
  xCircle: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg>',
  helpCircle: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><path d="M12 17h.01"/></svg>',

  // Actions
  plus: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/><path d="M12 5v14"/></svg>',
  search: '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>',
  star: '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
  mapPin: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>',
  clock: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
  walking: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="13" cy="4" r="1.5"/><path d="m9.5 21 2-7"/><path d="m15 21-3.5-5.5L9 12l1-4 4 1 2.5 3"/><path d="M6 12h.01"/></svg>',
  arrowDown: '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14"/><path d="m19 12-7 7-7-7"/></svg>',
  settings: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>',
  logOut: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>',
  shield: '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"/></svg>',
  compass: '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/></svg>',
  zap: '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z"/></svg>',
  globe: '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/><path d="M2 12h20"/></svg>',
  info: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>',
  alertCircle: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>',
};

// ============================================================
// Sample Data: Tokyo 3-night 4-day trip
// ============================================================
const SAMPLE_DATA = {
  trip: {
    id: 'trip-001',
    name: '도쿄 3박4일',
    city: '도쿄',
    startDate: '2026-03-15',
    endDate: '2026-03-18',
    days: 4,
  },

  user: {
    nickname: '여행자',
    email: 'traveler@gmail.com',
    plan: 'Free',
    profileImage: null,
  },

  places: [
    {
      id: 'place-001',
      name: '메이지진구',
      address: '도쿄 시부야구 요요기카미조노초 1-1',
      category: '신사/사원',
      rating: 4.5,
      time: '09:00',
      date: '2026-03-16',
      day: 2,
      status: 'green',
      statusText: '정상',
      statusReason: '모든 항목 정상',
      details: {
        business: { label: '영업상태', value: '영업중', status: 'green' },
        crowd: { label: '혼잡도', value: '보통 (42%)', status: 'green' },
        weather: { label: '날씨', value: '맑음 22°C', status: 'green' },
        transit: { label: '이동시간', value: '도보 15분', status: 'green' },
      },
      businessHours: '06:00 ~ 16:30',
      travelTime: '도보 20분',
    },
    {
      id: 'place-002',
      name: '이치란 라멘 시부야',
      address: '도쿄 시부야구 진난 1-22-7',
      category: '라멘',
      rating: 4.2,
      time: '12:00',
      date: '2026-03-16',
      day: 2,
      status: 'yellow',
      statusText: '주의',
      statusReason: '혼잡도가 높습니다',
      details: {
        business: { label: '영업상태', value: '영업중', status: 'green' },
        crowd: { label: '혼잡도', value: '혼잡 (78%)', status: 'yellow' },
        weather: { label: '날씨', value: '맑음 22°C', status: 'green' },
        transit: { label: '이동시간', value: '도보 15분', subValue: '대중교통 8분', status: 'green' },
      },
      businessHours: '11:00 ~ 22:00',
      travelTime: '도보 10분',
    },
    {
      id: 'place-003',
      name: '시부야 스카이',
      address: '도쿄 시부야구 시부야 2-24-12',
      category: '전망대',
      rating: 4.6,
      time: '14:30',
      date: '2026-03-16',
      day: 2,
      status: 'red',
      statusText: '위험',
      statusReason: '임시 휴업',
      details: {
        business: { label: '영업상태', value: '임시 휴업', status: 'red' },
        crowd: { label: '혼잡도', value: '데이터 없음', status: 'gray' },
        weather: { label: '날씨', value: '맑음 22°C', status: 'green' },
        transit: { label: '이동시간', value: '도보 25분', subValue: '대중교통 12분', status: 'green' },
      },
      businessHours: '10:00 ~ 22:30',
      travelTime: '도보 25분',
    },
    {
      id: 'place-004',
      name: '하라주쿠 쇼핑',
      address: '도쿄 시부야구 진구마에 1-8',
      category: '쇼핑',
      rating: 4.0,
      time: '17:00',
      date: '2026-03-16',
      day: 2,
      status: 'gray',
      statusText: '미확인',
      statusReason: '데이터 미확인',
      details: {
        business: { label: '영업상태', value: '미확인', status: 'gray' },
        crowd: { label: '혼잡도', value: '미확인', status: 'gray' },
        weather: { label: '날씨', value: '맑음 22°C', status: 'green' },
        transit: { label: '이동시간', value: '미확인', status: 'gray' },
      },
      businessHours: '10:00 ~ 21:00',
      travelTime: null,
    },
    {
      id: 'place-005',
      name: '츠타야 서점 다이칸야마',
      address: '도쿄 시부야구 사루가쿠초 17-5',
      category: '서점/카페',
      rating: 4.4,
      time: '10:00',
      date: '2026-03-17',
      day: 3,
      status: 'green',
      statusText: '정상',
      statusReason: '모든 항목 정상',
      details: {
        business: { label: '영업상태', value: '영업중', status: 'green' },
        crowd: { label: '혼잡도', value: '한적 (22%)', status: 'green' },
        weather: { label: '날씨', value: '구름 많음 19°C', status: 'green' },
        transit: { label: '이동시간', value: '도보 20분', status: 'green' },
      },
      businessHours: '07:00 ~ 22:00',
      travelTime: null,
    },
  ],

  briefings: [
    {
      id: 'briefing-001',
      placeId: 'place-002',
      placeName: '이치란 라멘 시부야',
      type: 'warning',
      status: 'yellow',
      summary: '혼잡도가 높게 감지되었습니다. 대안을 확인해보세요.',
      createdAt: '11:32',
      departureTime: '12:00',
      date: '2026-03-16',
      details: {
        business: { label: '영업상태', value: '영업중', status: 'green' },
        crowd: { label: '혼잡도', value: '혼잡 (78%)', status: 'yellow' },
        weather: { label: '날씨', value: '맑음 22°C', status: 'green' },
        transit: { label: '이동시간', value: '도보 15분', status: 'green' },
      },
      isExpired: false,
    },
    {
      id: 'briefing-002',
      placeId: 'place-001',
      placeName: '메이지진구',
      type: 'safe',
      status: 'green',
      summary: '현재까지 모든 항목 정상입니다. 예정대로 출발하세요.',
      createdAt: '09:02',
      departureTime: '09:00',
      date: '2026-03-16',
      details: {
        business: { label: '영업상태', value: '영업중', status: 'green' },
        crowd: { label: '혼잡도', value: '보통 (42%)', status: 'green' },
        weather: { label: '날씨', value: '맑음 22°C', status: 'green' },
        transit: { label: '이동시간', value: '도보 15분', status: 'green' },
      },
      isExpired: false,
    },
  ],

  alternatives: [
    {
      id: 'alt-001',
      name: '아후리 라멘 시부야',
      address: '도쿄 시부야구 에비스 1-1-7',
      category: '라멘',
      rating: 4.3,
      status: 'green',
      distance: '350m',
      walkTime: '도보 5분',
      crowdLevel: '혼잡도 낮음',
      reason: '같은 라멘 카테고리, 현재 혼잡도 낮고 도보 5분 거리',
      imageColor: '#2D5A3D',
    },
    {
      id: 'alt-002',
      name: '멘야 하나다 시부야',
      address: '도쿄 시부야구 우다가와초 31-1',
      category: '라멘',
      rating: 3.9,
      status: 'yellow',
      statusLabel: '주의 필요',
      distance: '820m',
      walkTime: '도보 12분',
      transitTime: '대중교통 6분',
      crowdLevel: '혼잡도 보통',
      reason: '높은 평점, 넓은 좌석',
      imageColor: '#3D2D5A',
    },
    {
      id: 'alt-003',
      name: '후쿠로 라멘',
      address: '도쿄 시부야구 사쿠라가오카 15-8',
      category: '라멘',
      rating: 4.1,
      status: 'green',
      distance: '1.2km',
      walkTime: '도보 18분',
      transitTime: '대중교통 8분',
      crowdLevel: '혼잡도 낮음',
      reason: '가장 한적한 선택지',
      imageColor: '#5A3D2D',
    },
  ],

  searchResults: [
    { name: '이치란 라멘 시부야', address: '도쿄 시부야구 진난 1-22-7', rating: 4.2, category: '라멘' },
    { name: '아후리 라멘 에비스', address: '도쿄 시부야구 에비스 1-1-7', rating: 4.3, category: '라멘' },
    { name: '멘야 하나다', address: '도쿄 시부야구 우다가와초 31-1', rating: 3.9, category: '라멘' },
    { name: '후쿠로 라멘', address: '도쿄 시부야구 사쿠라가오카 15-8', rating: 4.1, category: '라멘' },
  ],

  cities: ['도쿄', '오사카', '교토', '방콕', '싱가포르'],
};

// ============================================================
// 네비게이션 유틸리티
// ============================================================

/**
 * 지정 페이지로 이동. data를 함께 전달할 경우 sessionStorage에 저장.
 * @param {string} page - 이동할 HTML 파일 경로
 * @param {*} [data] - 페이지 전달 데이터 (직렬화 가능한 값)
 */
function navigateTo(page, data) {
  if (data !== undefined) {
    sessionStorage.setItem('pageData', JSON.stringify(data));
  }
  window.location.href = page;
}

/**
 * navigateTo로 전달된 페이지 데이터를 반환.
 * @returns {*|null}
 */
function getPageData() {
  var raw = sessionStorage.getItem('pageData');
  return raw ? JSON.parse(raw) : null;
}

/** pageData를 sessionStorage에서 삭제. */
function clearPageData() {
  sessionStorage.removeItem('pageData');
}

/** 브라우저 히스토리 뒤로 이동. */
function goBack() {
  window.history.back();
}

// ============================================================
// 폼 데이터 저장/복원 (sessionStorage 기반)
// ============================================================

/**
 * 폼 데이터를 sessionStorage에 저장.
 * @param {string} key - 폼 식별자
 * @param {*} data - 저장할 데이터
 */
function saveFormData(key, data) {
  sessionStorage.setItem('form_' + key, JSON.stringify(data));
}

/**
 * 저장된 폼 데이터를 복원.
 * @param {string} key - 폼 식별자
 * @returns {*|null}
 */
function restoreFormData(key) {
  var raw = sessionStorage.getItem('form_' + key);
  return raw ? JSON.parse(raw) : null;
}

/**
 * 저장된 폼 데이터를 삭제.
 * @param {string} key - 폼 식별자
 */
function clearFormData(key) {
  sessionStorage.removeItem('form_' + key);
}

// ============================================================
// 상태 관련 헬퍼 함수 (전역 - Web Component 미사용 페이지 호환)
// ============================================================

/**
 * 상태값에 대응하는 aria-label 텍스트를 반환.
 * @param {string} status - green | yellow | red | gray
 * @returns {string}
 */
function getStatusAriaLabel(status) {
  var labels = {
    green: '상태: 정상. 모든 항목 양호',
    yellow: '상태: 주의',
    red: '상태: 위험',
    gray: '상태: 데이터 미확인',
  };
  return labels[status] || '';
}

/**
 * 상태값에 대응하는 한국어 레이블을 반환.
 * @param {string} status - green | yellow | red | gray
 * @returns {string}
 */
function getStatusLabel(status) {
  var labels = {
    green: '정상',
    yellow: '주의',
    red: '위험',
    gray: '미확인',
  };
  return labels[status] || '';
}

/**
 * 상태 배지 HTML 문자열을 반환 (badge + icon).
 * @param {string} status - green | yellow | red | gray
 * @param {string} [size='sm'] - sm | lg
 * @returns {string} HTML 문자열
 */
function getStatusBadge(status, size) {
  size = size || 'sm';
  var iconMap = {
    green: ICONS.checkCircle,
    yellow: ICONS.alertTriangle,
    red: ICONS.xCircle,
    gray: ICONS.helpCircle,
  };
  var icon = iconMap[status] || iconMap.gray;
  return (
    '<span class="badge badge--' + size + ' badge--' + status + '" ' +
    'role="img" aria-label="' + getStatusAriaLabel(status) + '">' +
    icon +
    '</span>'
  );
}

/**
 * 배지 레이블 HTML 문자열을 반환 (icon + text).
 * @param {string} status - green | yellow | red | gray
 * @param {string} [text] - 표시할 텍스트 (미입력 시 getStatusLabel 사용)
 * @returns {string} HTML 문자열
 */
function getBadgeLabelHTML(status, text) {
  var iconMap = {
    green: ICONS.checkCircle,
    yellow: ICONS.alertTriangle,
    red: ICONS.xCircle,
    gray: ICONS.helpCircle,
  };
  var icon = iconMap[status] || iconMap.gray;
  var label = text || getStatusLabel(status);
  return (
    '<span class="badge-label badge-label--' + status + '">' +
    '<span class="icon icon--16">' + icon + '</span>' +
    label +
    '</span>'
  );
}

/**
 * 정보 행(info-row) HTML 문자열을 반환.
 * @param {{ label: string, value: string, subValue?: string, status: string }} item
 * @returns {string} HTML 문자열
 */
function renderInfoRow(item) {
  var valueHtml = item.value;
  if (item.subValue) {
    valueHtml += '<br><span class="text-caption text-secondary">' + item.subValue + '</span>';
  }
  return (
    '<div class="info-row">' +
    '<span class="info-row__label">' + item.label + '</span>' +
    '<span class="info-row__value">' + valueHtml + '</span>' +
    '<span class="info-row__badge">' + getStatusBadge(item.status, 'sm') + '</span>' +
    '</div>'
  );
}

// ============================================================
// Bottom Sheet 관리
// ============================================================

/**
 * 지정 ID의 바텀시트를 열기.
 * @param {string} sheetId - 바텀시트 루트 요소 ID (overlay: sheetId-overlay)
 */
function openBottomSheet(sheetId) {
  var overlay = document.getElementById(sheetId + '-overlay');
  var sheet = document.getElementById(sheetId);
  if (overlay) overlay.classList.add('active');
  if (sheet) sheet.classList.add('active');
  document.body.style.overflow = 'hidden';
}

/**
 * 지정 ID의 바텀시트를 닫기.
 * @param {string} sheetId - 바텀시트 루트 요소 ID
 */
function closeBottomSheet(sheetId) {
  var overlay = document.getElementById(sheetId + '-overlay');
  var sheet = document.getElementById(sheetId);
  if (overlay) overlay.classList.remove('active');
  if (sheet) sheet.classList.remove('active');
  document.body.style.overflow = '';
}

/**
 * 바텀시트 오버레이 클릭 및 내부 [data-close-sheet] 버튼 이벤트를 설정.
 * @param {string} sheetId - 바텀시트 루트 요소 ID
 */
function setupBottomSheet(sheetId) {
  var overlay = document.getElementById(sheetId + '-overlay');
  if (overlay) {
    overlay.addEventListener('click', function () {
      closeBottomSheet(sheetId);
    });
  }
  var sheet = document.getElementById(sheetId);
  if (sheet) {
    var closeBtns = sheet.querySelectorAll('[data-close-sheet]');
    closeBtns.forEach(function (btn) {
      btn.addEventListener('click', function () {
        closeBottomSheet(sheetId);
      });
    });
  }
}

// ============================================================
// Toast 알림
// ============================================================

/**
 * 하단 토스트 메시지를 표시.
 * @param {string} message - 표시할 메시지 텍스트
 * @param {number} [duration=3000] - 표시 지속 시간 (ms)
 */
function showToast(message, duration) {
  duration = duration || 3000;

  // 기존 토스트 제거
  var existing = document.querySelectorAll('.toast');
  existing.forEach(function (el) { el.remove(); });

  var toast = document.createElement('div');
  toast.className = 'toast';
  toast.setAttribute('role', 'status');
  toast.setAttribute('aria-live', 'polite');
  toast.textContent = message;
  document.body.appendChild(toast);

  // 애니메이션 트리거
  requestAnimationFrame(function () {
    requestAnimationFrame(function () {
      toast.classList.add('show');
    });
  });

  // 자동 숨김
  setTimeout(function () {
    toast.classList.remove('show');
    setTimeout(function () { toast.remove(); }, 250);
  }, duration);
}

// ============================================================
// 날짜 포맷 헬퍼
// ============================================================

/**
 * 날짜 문자열을 "M월 D일 (요일)" 형식으로 변환.
 * @param {string} dateStr - ISO 날짜 문자열 (예: '2026-03-16')
 * @returns {string}
 */
function formatDate(dateStr) {
  var d = new Date(dateStr);
  var days = ['일', '월', '화', '수', '목', '금', '토'];
  return (d.getMonth() + 1) + '월 ' + d.getDate() + '일 (' + days[d.getDay()] + ')';
}

/**
 * 날짜 문자열을 "M/D(요일)" 짧은 형식으로 변환.
 * @param {string} dateStr - ISO 날짜 문자열
 * @returns {string}
 */
function formatDateShort(dateStr) {
  var d = new Date(dateStr);
  var days = ['일', '월', '화', '수', '목', '금', '토'];
  return (d.getMonth() + 1) + '/' + d.getDate() + '(' + days[d.getDay()] + ')';
}

// ============================================================
// Web Component: <app-tab-bar active="schedule">
// 하단 탭 바 - 일정 / 브리핑 / 마이 (3탭)
// 브리핑 탭 미읽음 뱃지(빨강 도트) 포함
// ============================================================
class AppTabBar extends HTMLElement {
  static get observedAttributes() {
    return ['active'];
  }

  connectedCallback() {
    this._render();
  }

  attributeChangedCallback() {
    this._render();
  }

  _render() {
    var activeTab = this.getAttribute('active') || 'schedule';
    var tabs = [
      { id: 'schedule', label: '일정',   icon: ICONS.calendarDays, page: '01-일정표.html',    hasBadge: false },
      { id: 'briefing', label: '브리핑', icon: ICONS.bell,         page: '11-브리핑목록.html', hasBadge: true  },
      { id: 'my',       label: '마이',   icon: ICONS.user,         page: '10-마이페이지.html', hasBadge: false },
    ];

    var html = '<nav class="tab-bar" role="navigation" aria-label="하단 탭 메뉴">';
    tabs.forEach(function (tab) {
      var isActive = tab.id === activeTab;
      var activeClass = isActive ? ' tab-bar__item--active' : '';
      var badgeHtml = (tab.hasBadge && !isActive)
        ? '<span class="tab-bar__badge" aria-label="새 알림" role="status"></span>'
        : '';

      html +=
        '<a href="' + tab.page + '" ' +
        'class="tab-bar__item' + activeClass + '" ' +
        'role="tab" ' +
        'aria-selected="' + isActive + '" ' +
        'aria-label="' + tab.label + ' 탭">' +
        '<span class="tab-bar__icon">' + tab.icon + badgeHtml + '</span>' +
        '<span class="tab-bar__label">' + tab.label + '</span>' +
        '</a>';
    });
    html += '</nav>';

    this.innerHTML = html;
  }
}

// ============================================================
// Web Component: <app-top-nav title="제목" back close>
// 상단 네비게이션 바
// 속성: title, back(boolean), close(boolean), left-content
// ============================================================
class AppTopNav extends HTMLElement {
  static get observedAttributes() {
    return ['title', 'back', 'close', 'left-content', 'back-action', 'close-action'];
  }

  connectedCallback() {
    this._render();
  }

  attributeChangedCallback() {
    this._render();
  }

  _render() {
    var title       = this.getAttribute('title') || '';
    var hasBack     = this.hasAttribute('back');
    var hasClose    = this.hasAttribute('close');
    var leftContent = this.getAttribute('left-content') || '';
    var backAction  = this.getAttribute('back-action') || 'goBack()';
    var closeAction = this.getAttribute('close-action') || 'goBack()';

    var leftHtml = '';
    if (hasBack) {
      leftHtml =
        '<button class="top-nav__back" onclick="' + backAction + '" aria-label="뒤로가기">' +
        ICONS.chevronLeft +
        '</button>';
    } else if (leftContent) {
      leftHtml = '<div class="top-nav__title top-nav__title--left">' + leftContent + '</div>';
    } else {
      leftHtml = '<div aria-hidden="true" style="width:44px;"></div>';
    }

    var titleHtml = title
      ? '<h1 class="top-nav__title">' + title + '</h1>'
      : '';

    var rightHtml = '';
    if (hasClose) {
      rightHtml =
        '<button class="top-nav__action" onclick="' + closeAction + '" aria-label="닫기">' +
        ICONS.x +
        '</button>';
    } else {
      // slot 방식 대신 right-content 속성으로 HTML 삽입 가능
      var rightContent = this.getAttribute('right-content') || '';
      rightHtml = rightContent
        ? '<div>' + rightContent + '</div>'
        : '<div aria-hidden="true" style="width:44px;"></div>';
    }

    this.innerHTML =
      '<header class="top-nav" role="banner">' +
      leftHtml +
      titleHtml +
      rightHtml +
      '</header>';
  }
}

// ============================================================
// Web Component: <app-status-badge status="green" size="sm">
// 상태 배지 (원형 아이콘)
// 속성: status(green|yellow|red|gray), size(sm|lg)
// ============================================================
class AppStatusBadge extends HTMLElement {
  static get observedAttributes() {
    return ['status', 'size'];
  }

  connectedCallback() {
    this._render();
  }

  attributeChangedCallback() {
    this._render();
  }

  _render() {
    var status = this.getAttribute('status') || 'gray';
    var size   = this.getAttribute('size') || 'sm';
    var iconMap = {
      green:  ICONS.checkCircle,
      yellow: ICONS.alertTriangle,
      red:    ICONS.xCircle,
      gray:   ICONS.helpCircle,
    };
    var icon = iconMap[status] || iconMap.gray;
    var ariaLabel = getStatusAriaLabel(status);

    this.innerHTML =
      '<span class="badge badge--' + size + ' badge--' + status + '" ' +
      'role="img" aria-label="' + ariaLabel + '">' +
      icon +
      '</span>';
  }
}

// ============================================================
// Web Component: <app-badge-label status="green">안심</app-badge-label>
// 배지 레이블 (아이콘 + 텍스트)
// 속성: status(green|yellow|red|gray)
// ============================================================
class AppBadgeLabel extends HTMLElement {
  static get observedAttributes() {
    return ['status'];
  }

  connectedCallback() {
    // 초기 텍스트 보존 후 렌더링
    this._slotText = this.textContent.trim();
    this._render();
  }

  attributeChangedCallback() {
    this._render();
  }

  _render() {
    var status = this.getAttribute('status') || 'gray';
    var iconMap = {
      green:  ICONS.checkCircle,
      yellow: ICONS.alertTriangle,
      red:    ICONS.xCircle,
      gray:   ICONS.helpCircle,
    };
    var icon  = iconMap[status] || iconMap.gray;
    // 슬롯 텍스트 우선, 없으면 getStatusLabel
    var label = (this._slotText && this._slotText.length > 0)
      ? this._slotText
      : getStatusLabel(status);

    this.innerHTML =
      '<span class="badge-label badge-label--' + status + '">' +
      '<span class="icon icon--16">' + icon + '</span>' +
      label +
      '</span>';
  }
}

// ============================================================
// Custom Elements 등록
// ============================================================
if (typeof customElements !== 'undefined') {
  if (!customElements.get('app-tab-bar')) {
    customElements.define('app-tab-bar', AppTabBar);
  }
  if (!customElements.get('app-top-nav')) {
    customElements.define('app-top-nav', AppTopNav);
  }
  if (!customElements.get('app-status-badge')) {
    customElements.define('app-status-badge', AppStatusBadge);
  }
  if (!customElements.get('app-badge-label')) {
    customElements.define('app-badge-label', AppBadgeLabel);
  }
}

// ============================================================
// 페이지 초기화
// DOMContentLoaded 시 footer[data-tab] 자동 렌더링 포함
// ============================================================
function initPage() {
  // footer[data-tab] 자동 렌더링 (Web Component 미사용 페이지 호환)
  var footer = document.querySelector('footer[role="contentinfo"]');
  if (footer && footer.dataset && footer.dataset.tab) {
    // app-tab-bar Web Component로 교체
    var tabBar = document.createElement('app-tab-bar');
    tabBar.setAttribute('active', footer.dataset.tab);
    footer.innerHTML = '';
    footer.appendChild(tabBar);
  }

  // 모든 setupBottomSheet 대상 자동 초기화
  var sheets = document.querySelectorAll('.bottom-sheet[id]');
  sheets.forEach(function (sheet) {
    setupBottomSheet(sheet.id);
  });
}

document.addEventListener('DOMContentLoaded', initPage);
