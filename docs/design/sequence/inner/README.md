# 내부 시퀀스 다이어그램 (Inner Sequence)

> 서비스 내부 레이어별 처리 흐름 (Controller → Service → Repository → DB/Cache/External API)

| 파일 | 서비스 | 시나리오 | 관련 UFR |
|------|--------|----------|----------|
| auth-소셜로그인.puml | AUTH | 소셜 로그인 | UFR-AUTH-010 |
| schd-여행일정생성.puml | SCHD | 여행 일정 생성 | UFR-SCHD-010 |
| schd-장소추가.puml | SCHD | 장소 추가 | UFR-SCHD-030 |
| schd-일정장소교체.puml | SCHD | 일정 장소 교체 | UFR-SCHD-040 |
| schd-일정표조회.puml | SCHD | 일정표 조회 | UFR-SCHD-050 |
| plce-장소검색.puml | PLCE | 장소 검색 | UFR-PLCE-010 |
| plce-주변장소검색.puml | PLCE | 주변 장소 검색 | UFR-PLCE-030 |
| mntr-외부데이터수집.puml | MNTR | 외부 데이터 수집 | UFR-MNTR-010 |
| mntr-상태배지판정.puml | MNTR | 상태 배지 판정 | UFR-MNTR-020 |
| mntr-상태배지조회.puml | MNTR | 상태 배지 조회 | UFR-MNTR-030 |
| brif-브리핑생성.puml | BRIF | 브리핑 생성 | UFR-BRIF-010, UFR-BRIF-020, UFR-BRIF-030 |
| brif-브리핑조회.puml | BRIF | 브리핑 조회 | UFR-BRIF-050 |
| altn-대안장소검색.puml | ALTN | 대안 장소 검색 | UFR-ALTN-010, UFR-ALTN-050 |
| altn-대안카드선택.puml | ALTN | 대안 카드 선택 | UFR-ALTN-030 |
| pay-구독구매.puml | PAY | 구독 구매 | UFR-PAY-010 |
