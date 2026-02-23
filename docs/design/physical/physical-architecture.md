# 물리 아키텍처 마스터 설계서

> 작성자: 홍길동/아키 (소프트웨어 아키텍트)
> 작성일: 2026-02-23
> 프로젝트: travel-planner — 여행 중 실시간 일정 최적화 가이드 앱
> Cloud: Microsoft Azure (Korea Central)
> 버전: v1.0

---

## 목차

1. [개요](#1-개요)
2. [환경별 아키텍처 개요](#2-환경별-아키텍처-개요)
3. [네트워크 아키텍처 비교](#3-네트워크-아키텍처-비교)
4. [데이터 아키텍처 비교](#4-데이터-아키텍처-비교)
5. [보안 아키텍처 비교](#5-보안-아키텍처-비교)
6. [모니터링 및 운영](#6-모니터링-및-운영)
7. [비용 분석](#7-비용-분석)
8. [전환 및 확장 계획](#8-전환-및-확장-계획)
9. [핵심 SLA 지표](#9-핵심-sla-지표)

---

## 1. 개요

### 1.1 설계 목적

본 문서는 travel-planner 서비스의 물리 아키텍처를 **개발환경(Dev)**과 **운영환경(Prod)** 두 가지 관점에서 체계적으로 관리하기 위한 **마스터 인덱스**이다.

개별 환경 설계서는 각각 독립 문서로 관리되며, 본 마스터 문서는 다음 역할을 수행한다.

- **통합 관리 체계**: 두 환경의 아키텍처 결정 사항을 단일 지점에서 비교·관리
- **환경별 비교 기준 제공**: 목적, 가용성, 비용, 보안 수준의 명확한 기준 정의
- **전환 가이드**: 개발환경에서 운영환경으로의 단계적 전환 체크리스트 제공
- **의사결정 근거 보존**: 환경별 아키텍처 선택의 트레이드오프 기록

### 1.2 아키텍처 분리 원칙

| 원칙 | 설명 |
|------|------|
| **환경별 특화** | 개발환경은 개발 생산성 최적화, 운영환경은 가용성·보안 최적화 |
| **단계적 발전** | MVP 모놀리스 → Phase 2 MNTR 분리 → Phase 3 완전 마이크로서비스 |
| **비용 효율** | 개발환경 ~$100/월, 운영환경 ~$2,800/월 — 목적에 맞는 비용 투입 |
| **운영 단순성** | 서비스 메시 미사용, Azure 관리형 서비스 우선, 8인 팀 운영 한계 반영 |

### 1.3 문서 구조

```
physical-architecture.md          ← 현재 문서 (마스터 인덱스)
├── physical-architecture-dev.md  ← 개발환경 상세 설계서
│   ├── network-dev.mmd           ← 개발환경 네트워크 다이어그램
│   └── physical-architecture-dev.mmd  ← 개발환경 물리 아키텍처 다이어그램
└── physical-architecture-prod.md ← 운영환경 상세 설계서
    ├── network-prod.mmd          ← 운영환경 네트워크 다이어그램
    └── physical-architecture-prod.mmd ← 운영환경 물리 아키텍처 다이어그램
```

### 1.4 참조 아키텍처

| 문서 | 경로 | 설명 |
|------|------|------|
| HighLevel 아키텍처 | `docs/design/high-level-architecture.md` | 전체 아키텍처 정의서 (ADR 포함) |
| 논리 아키텍처 | `docs/design/logical-architecture.md` | 바운디드 컨텍스트, 서비스 구성 |
| 논리 아키텍처 다이어그램 | `docs/design/logical-architecture.mmd` | Mermaid 논리 구조도 |
| 아키텍처 패턴 | `docs/design/architecture.md` | 클라우드 패턴 선정 근거 |
| API 설계서 | `docs/design/api/` | 7개 서비스 OpenAPI 스펙 |
| 외부 시퀀스 | `docs/design/sequence/outer/` | 주요 시나리오 시퀀스 다이어그램 |
| 데이터 설계서 | `docs/design/database/` | 서비스별 ERD 및 스키마 |
| 캐시 DB 설계 | `docs/design/database/cache-db-design.md` | Redis DB 0~8 설계 |

---

## 2. 환경별 아키텍처 개요

### 2.1 환경별 특성 비교

| 항목 | 개발환경 (Dev) | 운영환경 (Prod) |
|------|--------------|----------------|
| **목적** | 기능 개발, 통합 테스트, QA 검증 | 실사용자 서비스 제공 |
| **가용성 목표** | 단일 노드 허용, 다운타임 허용 | 99.9% 이상, Zone-Redundant |
| **사용자** | 개발자, QA팀 (10명 이하) | 실사용자 (MVP 수백 명 → Phase 3 수만 명) |
| **확장성** | 고정 2노드, 수동 스케일링 | HPA 자동 스케일링 (CPU 기준) |
| **보안** | VPN/직접 접근 허용, 기본 TLS | WAF + DDoS 보호 + Private Endpoint 전계층 |
| **데이터베이스** | PostgreSQL Pod (클러스터 내부) | Azure Database for PostgreSQL Flexible Server |
| **캐시** | Redis Pod (클러스터 내부) | Azure Cache for Redis Premium |
| **이벤트 버스** | 인메모리 이벤트 버스 (Spring Application Event) | Azure Service Bus Premium |
| **인그레스** | NGINX Ingress Controller | Azure Application Gateway + WAF v2 |
| **모니터링** | 기본 kubectl 로그 + Spring Actuator | Azure Monitor + Application Insights + Alert |
| **배포 방식** | 수동 kubectl 또는 GitHub Actions | ArgoCD GitOps + Rolling Update |
| **월간 비용** | ~$80~100 | ~$2,500~3,000 |

### 2.2.1 개발환경 아키텍처

상세 설계서: [`./physical-architecture-dev.md`](./physical-architecture-dev.md)
네트워크 다이어그램: [`./network-dev.mmd`](./network-dev.mmd)
물리 아키텍처 다이어그램: [`./physical-architecture-dev.mmd`](./physical-architecture-dev.mmd)

**주요 특징**

- Azure Kubernetes Service (AKS) 단일 클러스터, **2x Standard_B2s 노드** (2 vCPU / 4GB RAM)
- PostgreSQL과 Redis를 **클러스터 내부 Pod**로 운영하여 Azure 관리형 서비스 비용 절감
- NGINX Ingress Controller를 통한 경로 기반 라우팅 (`/api/{service}/**`)
- Spring Application Event 기반 **인메모리 이벤트 버스** (네트워크 없음, 설정 없음)
- 개발자·QA팀이 VPN 또는 직접 접근 (IP 허용 리스트)

**핵심 구성**

| 구성요소 | 사양 | 비고 |
|----------|------|------|
| AKS 노드 | Standard_B2s × 2 | 2 vCPU / 4GB RAM |
| PostgreSQL | Pod (32GB Premium SSD) | Azure Managed Disk PV |
| Redis | Pod (메모리 캐시) | TTL 기반 Cache-Aside |
| Ingress | NGINX Ingress Controller | ClusterIP 서비스 라우팅 |
| 이벤트 버스 | Spring Application Event | 인메모리, Phase 2 교체 예정 |
| 네임스페이스 | `travel-planner-dev` | 환경 격리 |

### 2.2.2 운영환경 아키텍처

상세 설계서: [`./physical-architecture-prod.md`](./physical-architecture-prod.md)
네트워크 다이어그램: [`./network-prod.mmd`](./network-prod.mmd)
물리 아키텍처 다이어그램: [`./physical-architecture-prod.mmd`](./physical-architecture-prod.mmd)

**주요 특징**

- AKS Premium 멀티존 클러스터 (**Zone 1/2/3 분산**)
- 시스템 노드풀(D2s_v3 × 3)과 앱 노드풀(D4s_v3 × 3) 분리
- Azure Database for PostgreSQL Flexible Server (Primary + Read Replica)
- Azure Cache for Redis Premium P2 (Primary + Secondary, Geo-Replication)
- Azure Service Bus Premium (Partitioned Queue, Private Endpoint)
- Azure Front Door + CDN → Application Gateway + WAF → AKS 다층 인그레스
- ArgoCD GitOps 기반 자동 배포, HPA 오토스케일링

**핵심 구성**

| 구성요소 | 사양 | 비고 |
|----------|------|------|
| AKS System Pool | D2s_v3 × 3 (Zone 1/2/3) | CoreDNS, metrics-server, ArgoCD |
| AKS App Pool | D4s_v3 × 3 (Zone 1/2/3) | 7개 서비스 Pod, HPA |
| PostgreSQL | GP_Standard_D4s_v3 (Primary + Replica) | Zone-Redundant, 자동 백업 |
| Redis | Premium P2 (Primary + Secondary) | Geo-Replication, 클러스터 샤딩 |
| Service Bus | Premium 1MU (Partitioned Queue) | Private Endpoint 연결 |
| Application Gateway | WAF v2, Zone-Redundant | OWASP CRS 3.2 |
| Azure Front Door | 글로벌 CDN + DDoS 보호 | 글로벌 PoP 캐시 |
| Key Vault | 비밀/인증서/키 통합 관리 | Managed Identity 연동 |
| Container Registry | ACR Premium (Geo-Replication) | CI/CD 이미지 저장소 |
| 네임스페이스 | `travel-planner-prod` | 환경 격리 |

### 2.3.1 공통 아키텍처 원칙

두 환경에 동일하게 적용되는 설계 원칙이다.

| 원칙 | 내용 | 적용 패턴 |
|------|------|----------|
| **서비스 메시 미사용** | 7개 서비스 규모에서 Istio/Linkerd 오버헤드 불필요. 쿠버네티스 네이티브 서비스 디스커버리와 APIM 조합으로 대체 | Azure APIM + K8s ClusterIP |
| **비동기 통신** | 서비스 간 이벤트는 인터페이스 추상화(`EventPublisher`)로 구현체 교체 가능 | Publisher-Subscriber 패턴 |
| **Managed Identity** | API 키, DB 자격증명을 코드/환경변수에 직접 노출 금지. Azure Key Vault + Managed Identity 연동 | Federated Identity |
| **다층 보안** | 인터넷 진입점부터 데이터 계층까지 독립적 보안 레이어 적용 | Defense in Depth |
| **외부 의존성 3중 방어** | Circuit Breaker + Retry + Cache-Aside Fallback 조합으로 외부 API 장애 격리 | Resilience4j |
| **단일 진입점** | JWT 검증, Rate Limiting, 로깅을 API Gateway에 집중 | Gateway Offloading |

### 2.3.2 환경별 차별화 전략

| 전략 | 개발환경 | 운영환경 |
|------|---------|---------|
| **우선 가치** | 개발 속도 + 비용 최소화 | 가용성 + 보안 |
| **데이터 영속성** | Pod 내부 Volume (재시작 시 데이터 유지, 재배포 시 초기화 가능) | Azure 관리형 서비스 (자동 백업, HA) |
| **장애 대응** | 수동 Pod 재시작, kubectl 디버깅 | HPA 자동 스케일링, AlertRule 자동 알림 |
| **접근 제어** | VPN/IP 허용 리스트 단순 제어 | WAF + NSG + Private Endpoint 다층 제어 |
| **이벤트 버스** | Spring Application Event (인메모리, Zero 설정) | Azure Service Bus Premium (내구성, 순서 보장) |
| **CI/CD** | GitHub Actions 수동 트리거 또는 푸시 자동화 | ArgoCD GitOps, 자동 Sync, 롤백 |
| **비용 최적화** | Spot 인스턴스 미사용, 노드 수 최소화 | Reserved Instance 할인, HPA로 유휴 자원 회수 |

---

## 3. 네트워크 아키텍처 비교

### 3.1.1 환경별 네트워크 전략 비교

| 항목 | 개발환경 | 운영환경 |
|------|---------|---------|
| **인그레스 방식** | NGINX Ingress Controller (AKS 내부) | Azure Front Door → Application Gateway + WAF v2 |
| **VNet 구성** | 단일 서브넷 (AKS Subnet 10.0.1.0/24) | 4개 서브넷 분리 (Gateway/App/DB/Cache) |
| **DB 접근** | Pod 직접 TCP 연결 (클러스터 내부) | Private Endpoint (10.0.2.0/24, 외부 노출 없음) |
| **Redis 접근** | Pod 직접 TCP 연결 (클러스터 내부) | Private Endpoint (10.0.3.0/24, 외부 노출 없음) |
| **Service Bus 접근** | HTTPS/AMQP (공개 엔드포인트) | Private Endpoint (10.0.1.50, VNet 내부 전용) |
| **외부 접근** | 개발자/QA팀 VPN 또는 직접 접근 | Azure Front Door 글로벌 PoP → Static Public IP 경유 |
| **DDoS 보호** | 미적용 (개발 트래픽만) | Azure Front Door 내장 DDoS Protection |
| **DNS** | CoreDNS (클러스터 내부 서비스 디스커버리) | CoreDNS + Azure Private DNS Zone |

**개발환경 네트워크 흐름**

```
개발자/QA팀
    │ HTTPS:443
    ▼
LoadBalancer (NGINX Ingress, External IP)
    │ ClusterIP 라우팅
    ▼
ClusterIP Services (auth-svc:8081 ~ pay-svc:8087)
    │ Pod Load Balancing
    ▼
Service Pods (AUTH/SCHD/PLCE/MNTR/BRIF/ALTN/PAY)
    ├──TCP:5432──▶ PostgreSQL Pod (PersistentVolume 32GB)
    ├──TCP:6379──▶ Redis Pod
    └──HTTPS/AMQP──▶ Azure Service Bus Basic
```

**운영환경 네트워크 흐름**

```
실사용자 (1만~10만 명)
    │ HTTPS
    ▼
Azure Front Door + CDN (글로벌 PoP Cache)
    │ Route to origin
    ▼
Public IP (Static)
    │ HTTP/S
    ▼
Application Gateway + WAF v2 (Gateway Subnet 10.0.4.0/24)
    │ OWASP CRS 3.2 필터링
    ▼
Internal Load Balancer (AKS 내부)
    │ :8001~:8007 NodePort
    ▼
Service Pods (HA replicas + HPA, Zone 1/2/3)
    ├──Private Link──▶ PostgreSQL Private Endpoint (10.0.2.10)
    ├──Private Link──▶ Redis Private Endpoint (10.0.3.10)
    └──Private Link──▶ Service Bus Private Endpoint (10.0.1.50)
```

### 3.2.1 공통 보안 원칙

| 원칙 | 구현 방법 |
|------|----------|
| **Network Policies** | 서비스 간 허용된 포트만 통신 (K8s NetworkPolicy) |
| **Managed Identity** | 서비스 → Azure 리소스 접근 시 자격증명 없이 AAD Managed Identity 사용 |
| **Private Endpoints** | 운영환경 DB, Redis, Service Bus, Key Vault 모두 공개 인터넷 차단 |
| **TLS 전 구간** | 사용자 → Front Door → WAF → AKS → 서비스 전 구간 TLS 1.3 |
| **JWT 검증 집중** | API Gateway에서 모든 요청의 JWT 서명·만료·클레임 검증 |
| **Rate Limiting** | 구독 티어별 요청 수 제한 (API Gateway Inbound Policy) |

### 3.2.2 환경별 보안 수준 비교

| 보안 항목 | 개발환경 | 운영환경 |
|-----------|---------|---------|
| **WAF** | 미적용 | Azure Application Gateway WAF v2 (OWASP CRS 3.2) |
| **DDoS 보호** | 미적용 | Azure Front Door 내장 DDoS Protection |
| **Private Endpoint** | 미적용 | PostgreSQL, Redis, Service Bus, Key Vault 전체 적용 |
| **NSG** | 기본 NSG (AKS 자동 생성) | 서브넷별 NSG 세밀 규칙 (인바운드/아웃바운드 화이트리스트) |
| **Key Vault** | 환경변수 직접 주입 허용 | Key Vault 전용, Managed Identity 연동 필수 |
| **접근 방법** | VPN 또는 IP 허용 리스트 | Azure AD 기반 RBAC + MFA 필수 |
| **감사 로그** | 기본 K8s 이벤트 로그 | Azure Monitor + Azure Security Center 통합 감사 |
| **취약점 스캔** | 개발자 로컬 SAST | Microsoft Defender for Cloud + ACR 이미지 스캔 |

---

## 4. 데이터 아키텍처 비교

### 4.1.1 환경별 데이터 구성 비교

| 항목 | 개발환경 | 운영환경 |
|------|---------|---------|
| **PostgreSQL 호스팅** | AKS Pod (클러스터 내부) | Azure Database for PostgreSQL Flexible Server |
| **PostgreSQL 스펙** | 공유 컨테이너 (메모리 제한 1GB) | GP_Standard_D4s_v3 (4 vCPU / 16GB RAM) |
| **스토리지** | Azure Managed Disk 32GB Premium SSD (PV) | Flexible Server 내장 스토리지 (자동 확장) |
| **HA 구성** | 단일 Pod (재시작 시 자동 복구) | Primary + Read Replica (Zone-Redundant) |
| **백업** | 없음 (개발 데이터, 손실 허용) | 자동 백업 7일 보관, Point-in-Time Restore |
| **Redis 호스팅** | AKS Pod (클러스터 내부) | Azure Cache for Redis Premium P2 |
| **Redis HA** | 단일 Pod | Primary + Secondary, Geo-Replication |
| **Redis 클러스터** | 미적용 | 샤딩 클러스터 (Phase 2 대용량 시) |
| **이벤트 버스** | 인메모리 (Spring Application Event) | Azure Service Bus Premium (Partitioned Queue) |
| **App Configuration** | application.yml 파일 직접 관리 | Azure App Configuration (동적 임계값 변경) |

**Redis DB 논리 할당 (공통)**

| DB 번호 | 서비스 | 용도 | TTL |
|---------|--------|------|-----|
| DB 0 | AUTH | JWT 블랙리스트 (`auth:blacklist:{jti}`) | 30분 |
| DB 1 | AUTH | Refresh Token (`auth:refresh:{userId}`) | 30일 |
| DB 2 | SCHD | 일정 캐시 | 10분 |
| DB 3 | PLCE | 장소 데이터 Cache-Aside | 5분 |
| DB 4 | MNTR | 배지 상태 + 날씨 + 이동시간 | 10분 |
| DB 5 | BRIF | 브리핑 생성 멱등성 키 | 30분 |
| DB 6 | ALTN | 대안 카드 캐시 | 5분 |
| DB 7 | PAY | 구독 상태 캐시 | 60분 |
| DB 8 | AI | LLM 응답 캐시 (Phase 3) | 30분 |

### 4.2.1 다층 캐시 구조

| 계층 | 캐시 유형 | 개발환경 | 운영환경 |
|------|----------|---------|---------|
| **L1 App Cache** | Spring 인메모리 캐시 (`@Cacheable`) | 동일 적용 | 동일 적용 |
| **L2 Distributed Cache** | Redis Cache-Aside | Redis Pod (내부) | Azure Cache for Redis Premium |
| **L3 CDN Cache** | 정적 에셋 캐시 | 미적용 | Azure Front Door CDN (24시간) |
| **L4 DB Read Replica** | 읽기 전용 쿼리 분산 | 미적용 | PostgreSQL Read Replica |

### 4.2.2 환경별 캐시 특성 비교

| 항목 | 개발환경 | 운영환경 |
|------|---------|---------|
| **Redis 가용성** | Pod 재시작 시 캐시 초기화 (Cold Start) | HA Primary-Secondary, 장애 시 자동 Failover |
| **Redis 용량** | Pod 메모리 제한 내 (약 512MB) | Premium P2 (13GB 메모리) |
| **캐시 무효화** | PlaceStatusChanged 이벤트 인메모리 처리 | Service Bus 메시지 수신 후 Redis 즉시 무효화 |
| **TTL 정책** | 개발환경 동일 (동작 검증 목적) | 배지 TTL 10분 < 수집 주기 15분 (설계 원칙 준수) |
| **모니터링** | 없음 (Redis Pod 메트릭 미수집) | Azure Monitor Redis 메트릭, 히트율 알림 |

---

## 5. 보안 아키텍처 비교

### 5.1.1 공통 보안 계층

| 계층 | 구성요소 | 역할 |
|------|----------|------|
| **L1 네트워크 경계** | Azure Front Door WAF (운영), NGINX Ingress (개발) | DDoS 방어, OWASP Top 10 차단, IP 필터링 |
| **L2 게이트웨이** | Azure API Management (Inbound Policy) | JWT 서명 검증, 만료 확인, Rate Limiting, 구독 티어 클레임 추출 |
| **L3 애플리케이션** | Spring Security OAuth2 Resource Server | 서비스별 권한 확인, Paywall 검증 (ALTN Free 티어), 동의 이력 확인 |
| **L4 데이터** | Azure Key Vault + Managed Identity, TDE (PostgreSQL), Private Endpoint | 자격증명 보호, 저장 데이터 암호화, 네트워크 격리 |

**인증 흐름**

```
모바일 앱
    │ 소셜 로그인 (Google OAuth 2.0 / Apple Sign In)
    ▼
Google/Apple ID 서버 → Authorization Code
    │
    ▼
AUTH 서비스 (Federated Identity 패턴)
    ├── Access Token (JWT, HS256, 30분, 구독 티어 클레임 포함)
    └── Refresh Token (UUID, Redis DB 1, 30일)
    │
    ▼
API Gateway (Inbound Policy)
    ├── JWT 서명 검증 (Gateway Offloading)
    ├── 만료 시간 확인
    ├── 구독 티어 클레임 추출 (FREE / TRIP_PASS / PRO)
    └── Rate Limiting 적용 (티어별 차등)
```

### 5.2.1 환경별 보안 수준 비교

| 보안 영역 | 개발환경 수준 | 운영환경 수준 |
|-----------|-------------|-------------|
| **인증 방식** | JWT 검증 동일 적용 (기능 동일) | JWT 검증 + WAF 레이어 추가 |
| **키 관리** | application.yml 또는 환경변수 직접 주입 허용 | Azure Key Vault 필수, 환경변수 직접 노출 금지 |
| **네트워크 격리** | AKS 클러스터 내부 격리 (기본 NSG) | 서브넷 분리 + NSG 화이트리스트 + Private Endpoint |
| **TLS** | Let's Encrypt 또는 자체 서명 인증서 | Azure Key Vault 관리 인증서 (자동 갱신) |
| **위협 감지** | 미적용 | Microsoft Defender for Cloud (실시간 위협 감지) |
| **이미지 보안** | GitHub Actions 기본 스캔 | ACR Premium + Microsoft Defender 취약점 스캔 |
| **컴플라이언스** | 개발 데이터 (개인정보 최소화) | 개인정보보호법, 위치정보법 동의 이력 저장 필수 |
| **접근 로그** | kubectl logs (단기 보관) | Azure Monitor Logs (30일 보관, KQL 분석) |

---

## 6. 모니터링 및 운영

### 6.1.1 환경별 모니터링 도구 비교

| 항목 | 개발환경 | 운영환경 |
|------|---------|---------|
| **로그 수집** | `kubectl logs` + Spring Boot Actuator stdout | Azure Monitor Agent → Log Analytics Workspace |
| **메트릭 수집** | Spring Boot Micrometer (로컬 확인) | Azure Monitor Metrics + Prometheus 형식 수집 |
| **분산 추적** | Spring Boot DevTools + 로컬 로그 | Azure Application Insights (OpenTelemetry 연동) |
| **대시보드** | 없음 (개발자 로컬 확인) | Azure Dashboard + Application Insights 자동 생성 |
| **알림** | 없음 | Azure Monitor Alert Rules + Action Groups (이메일/Slack) |
| **헬스체크** | `/actuator/health` 수동 확인 | Liveness/Readiness Probe 자동, K8s 자동 재시작 |
| **AKS 모니터링** | 기본 kubectl 명령어 | Azure Monitor for Containers (노드/Pod 메트릭) |
| **외부 API 추적** | Circuit Breaker 상태 로컬 로그 | Application Insights Dependency 추적 (레이턴시 측정) |

**운영환경 알림 정책**

| 알림 조건 | 임계값 | 대응 방안 |
|-----------|--------|----------|
| CPU 사용률 초과 | 70% 이상 5분 지속 | HPA 자동 스케일 아웃 + 담당자 알림 |
| 에러율 초과 | 5xx 오류 5% 이상 | 긴급 알림 + 롤백 검토 |
| 수집 파이프라인 실패 | MNTR 스케줄러 연속 3회 실패 | Circuit Breaker OPEN → 회색 배지 Fallback |
| Circuit Breaker OPEN | 외부 API 연속 실패 감지 | 담당자 알림 + Fallback 자동 적용 |
| Redis 히트율 저하 | Cache Hit Rate 60% 미만 | TTL 조정 검토 |
| Pod CrashLoop | 재시작 5회 이상 | 긴급 알림 + 로그 분석 |

### 6.2.1 환경별 배포 방식 비교

| 항목 | 개발환경 | 운영환경 |
|------|---------|---------|
| **CI 도구** | GitHub Actions (Push 트리거 또는 수동) | GitHub Actions (PR Merge 트리거) |
| **CD 도구** | kubectl apply 직접 또는 GitHub Actions | ArgoCD GitOps (자동 Sync) |
| **이미지 저장소** | Azure Container Registry Basic | Azure Container Registry Premium (Geo-Replication) |
| **배포 전략** | Recreate (빠른 배포, 짧은 다운타임 허용) | Rolling Update (무중단, 헬스체크 기반) |
| **롤백 방법** | kubectl rollout undo | ArgoCD 이전 Revision 즉시 롤백 |
| **배포 소요 시간** | ~2분 (단순 Recreate) | ~5~10분 (Rolling Update + 헬스체크) |
| **헬름 차트** | `values-dev.yaml` | `values-prod.yaml` |
| **승인 프로세스** | 없음 (개발자 자유 배포) | PR Review 필수 + ArgoCD Sync 확인 |

**GitHub Actions → ArgoCD 운영 배포 흐름**

```
개발자 PR → Code Review → main 브랜치 Merge
    │
    ▼
GitHub Actions CI
    ├── Gradle Build + Test (JUnit 5, Testcontainers)
    ├── Docker Multi-stage Build (JDK21 빌드 → JRE21 런타임)
    ├── ACR 이미지 푸시 (sha 태그)
    └── Helm Chart values-prod.yaml 이미지 태그 업데이트
    │
    ▼
ArgoCD GitOps
    ├── Git 변경 감지 → 자동 Sync
    ├── Rolling Update 실행
    ├── Liveness/Readiness Probe 확인
    └── 실패 시 이전 Revision 자동 롤백
```

---

## 7. 비용 분석

### 7.1.1 월간 비용 비교

| 구성요소 | 개발환경 (월) | 운영환경 (월) | 비고 |
|----------|-------------|-------------|------|
| **AKS 노드 (컴퓨팅)** | ~$35 (B2s × 2) | ~$800 (D2s_v3 × 3 + D4s_v3 × 3) | 개발: 최소 사양 |
| **Azure API Management** | ~$0 (Consumption Tier) | ~$250 (Standard Tier) | 개발: 호출 당 과금 |
| **PostgreSQL** | ~$0 (Pod 내부) | ~$400 (D4s_v3 Flexible Server + Replica) | 개발: 디스크 비용만 |
| **Azure Cache for Redis** | ~$0 (Pod 내부) | ~$300 (Premium P2) | 개발: 클러스터 자원 사용 |
| **Azure Service Bus** | ~$0 (인메모리) | ~$100 (Premium 1MU) | 개발: 인메모리 이벤트 버스 |
| **Azure Front Door + CDN** | ~$0 (미적용) | ~$150 | 운영 전용 |
| **Application Gateway + WAF** | ~$0 (NGINX 대체) | ~$250 | 운영 전용 |
| **Key Vault** | ~$5 | ~$20 | 비밀 관리 |
| **Azure Monitor + App Insights** | ~$5 | ~$150 | 운영: 대용량 로그 |
| **Container Registry** | ~$5 (Basic) | ~$50 (Premium) | 개발: Basic |
| **Azure Managed Disk** | ~$5 (32GB SSD) | ~$0 (Flexible Server 내장) | 개발: Pod PV |
| **네트워킹 (Egress 등)** | ~$10 | ~$100 | 외부 API 호출 포함 |
| **DR 백업 (Azure Backup)** | ~$0 (미적용) | ~$80 | 운영 전용 |
| **기타 (DNS, 로드밸런서 등)** | ~$10 | ~$150 | |
| **합계** | **~$75~100/월** | **~$2,500~3,000/월** | |

### 7.1.2 환경별 비용 최적화 전략 비교

| 전략 | 개발환경 | 운영환경 |
|------|---------|---------|
| **컴퓨팅 최적화** | 최소 사양 노드 (B2s), 야간 AKS 중지 자동화 | Reserved Instance 1년 약정 (~30% 절감), HPA로 유휴 Pod 제거 |
| **데이터베이스** | Pod 내부 운영 (관리형 서비스 비용 제로) | Flexible Server Burstable 티어 검토 (트래픽 적은 초기) |
| **API 게이트웨이** | Consumption Tier (호출 당 과금, 저트래픽 유리) | Standard Tier → 트래픽 증가 시 Premium 업그레이드 |
| **모니터링** | 무료 티어 / 최소 설정 | Log Analytics 데이터 보관 기간 조정 (30일 → 필요 시 90일) |
| **이벤트 버스** | 인메모리 (비용 없음) | Service Bus Premium → Standard 다운그레이드 검토 (MVP 초기) |
| **DR** | 미적용 (개발 데이터 손실 허용) | Zone-Redundant 우선, Korea South DR은 Phase 2 이후 검토 |

---

## 8. 전환 및 확장 계획

### 8.1 개발→운영 전환 체크리스트

| 영역 | 항목 | 확인 방법 | 담당 |
|------|------|----------|------|
| **데이터 마이그레이션** | 개발 DB 스키마 운영 환경 적용 확인 | `flyway migrate` 성공 여부 | 데브-백 |
| **데이터 마이그레이션** | 테스트 데이터 제거 후 운영 초기 데이터 삽입 | 데이터 검증 쿼리 실행 | 데브-백 |
| **설정 변경** | application.yml → Azure App Configuration 전환 | 서비스 기동 후 설정값 로드 확인 | 데브-백 |
| **설정 변경** | 환경변수 Key Vault Secret 참조로 전환 | Managed Identity 접근 테스트 | 파이프 |
| **이벤트 버스** | 인메모리 EventPublisher → ServiceBusEventPublisher 교체 | 이벤트 발행/구독 E2E 테스트 | 데브-백 |
| **네트워크** | Private Endpoint 연결 확인 (PostgreSQL, Redis, Service Bus) | `nslookup` + 연결 테스트 | 파이프 |
| **보안** | WAF 규칙 활성화 후 정상 트래픽 오탐 여부 확인 | 시나리오 기반 API 테스트 | 가디언 |
| **보안** | Key Vault 비밀 최신화 (운영 API 키 등록) | 서비스 기동 후 외부 API 호출 확인 | 파이프 |
| **모니터링** | Alert Rules + Action Groups 설정 완료 | 테스트 알림 발송 확인 | 파이프 |
| **모니터링** | Application Insights 연결 + 분산 추적 동작 확인 | 샘플 요청 후 Trace 확인 | 파이프 |
| **배포** | ArgoCD Sync + Rollback 동작 확인 | 배포 후 강제 실패 → 롤백 테스트 | 파이프 |
| **성능** | 배지 조회 P95 응답시간 2초 이내 확인 | k6 또는 Artillery 부하 테스트 | 가디언 |
| **성능** | MNTR 15분 수집 파이프라인 2초 이내 완료 확인 | 로그 + Application Insights Trace | 데브-백 |
| **DR** | PostgreSQL 자동 백업 + Point-in-Time Restore 테스트 | 복원 후 데이터 정합성 확인 | 파이프 |
| **컴플라이언스** | 위치정보법 동의 이력 저장 확인 (`consent_records`) | DB 조회 + 시나리오 테스트 | 가디언 |

### 8.2 Phase 1~3 확장 로드맵

| Phase | 기간 | 아키텍처 변화 | 트리거 조건 | 예상 비용 |
|-------|------|-------------|-----------|---------|
| **Phase 1 (MVP)** | 5~8주 | 모놀리스 단일 배포, 인메모리 이벤트 버스, S04+S05+S06 핵심 기능 | 출시 기준 | ~$100/월 (개발) |
| **Phase 2 (확장)** | MVP 후 3~4개월 | MNTR 서비스 독립 분리, Azure Service Bus Standard 전환, PostgreSQL Read Replica 추가 | 동시 모니터링 장소 1,000개 초과 또는 인메모리 버스 안정성 이슈 | ~$3,500/월 (운영) |
| **Phase 3 (고도화)** | Phase 2 후 6개월+ | Azure OpenAI GPT-4o-mini 연동, Python/FastAPI AI 서비스 분리, Korea South DR 사이트, Azure Service Bus Premium 전환 | MAU 10만 초과 또는 AI 브리핑 총평 품질 요구 | ~$5,000~8,000/월 (운영) |

**Phase별 주요 아키텍처 변경 상세**

| 변경 항목 | Phase 1 → Phase 2 | Phase 2 → Phase 3 |
|-----------|-------------------|-------------------|
| 이벤트 버스 | 인메모리 → Azure Service Bus Standard | Service Bus Standard → Premium |
| MNTR 서비스 | 모놀리스 내부 | 독립 배포 (별도 Docker 이미지, HPA 독립) |
| PostgreSQL | 단일 Primary | Primary + Read Replica 추가 |
| AI 서비스 | 규칙 기반 (BriefingTextGenerator RuleBasedImpl) | Azure OpenAI + Python/FastAPI AI 서비스 |
| 글로벌 확장 | 단일 리전 (Korea Central) | Korea South DR 사이트 추가 검토 |
| Saga 패턴 | 단일 DB 트랜잭션 (모놀리스) | MNTR 분리 후 Saga 패턴 도입 |

---

## 9. 핵심 SLA 지표

### 9.1 환경별 SLA 비교

| 지표 | 개발환경 목표 | 운영환경 목표 | 측정 방법 |
|------|-------------|-------------|----------|
| **서비스 가용성** | 95% (업무 시간 기준, 야간 중지 허용) | 99.5% 이상 | Azure Monitor Uptime 메트릭 |
| **배지 조회 응답시간** | 3초 이내 (P95, 개발 환경 느슨한 기준) | **2초 이내 (P95)** | Application Insights Latency |
| **대안 카드 응답시간** | 5초 이내 (P95) | **3초 이내 (P95)** | Application Insights Latency |
| **브리핑 Push SLA** | 테스트 발송 성공 확인 | **99.5% 이상** (출발 15~30분 전 발송) | BRIF 서비스 Push 발송 이력 |
| **배포 소요 시간** | ~2분 (Recreate) | ~10분 이내 (Rolling Update) | GitHub Actions + ArgoCD 로그 |
| **복구 목표 (RTO)** | 30분 (수동 재시작) | **30분 이내** (자동 복구 + 수동 대응) | 장애 발생 ~ 서비스 정상화 시간 |
| **데이터 복구 (RPO)** | 없음 (개발 데이터 손실 허용) | **1시간 이내** (PostgreSQL Point-in-Time Restore) | 마지막 백업 ~ 장애 시점 간격 |
| **동시 사용자** | 10명 이하 (개발/QA팀) | MVP: 수백 명 → Phase 2: 수천 명 | Azure Monitor 동시 연결 수 |
| **수집 파이프라인** | 2초 이내 완료 (기능 검증) | **2초 이내 (타임아웃 하드 리밋)** | Application Insights Dependency |
| **월간 비용** | **~$80~100/월** | **~$2,500~3,000/월** | Azure Cost Management |

**SLA 달성 전략 요약**

| SLA 항목 | 핵심 달성 전략 |
|----------|--------------|
| 가용성 99.5% | Zone-Redundant AKS + PostgreSQL + Redis, HPA 자동 스케일링 |
| 배지 조회 2초 (P95) | Redis Cache-Aside (DB 4, TTL 10분), L1 App Cache 병행 |
| 브리핑 Push 99.5% | FCM Circuit Breaker (10회/분 실패 → OPEN), 인앱 알림 Fallback |
| RTO 30분 | ArgoCD 즉시 롤백, K8s 자동 Pod 재시작 |
| RPO 1시간 | PostgreSQL 자동 백업 (Point-in-Time Restore, 7일 보관) |

---

## 문서 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| v1.0 | 2026-02-23 | 홍길동/아키 | 초기 작성 — 환경별 비교 중심 마스터 설계서 |
