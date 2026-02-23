# 개발환경 물리 아키텍처 설계서

> 작성자: 아키 (소프트웨어 아키텍트)
> 작성일: 2026-02-23
> 프로젝트: travel-planner — 여행 중 실시간 일정 최적화 가이드 앱
> 환경: 개발(Development)
> Cloud: Azure

---

## 1. 개요

### 1.1 설계 목적

본 문서는 travel-planner 개발환경의 물리 아키텍처를 정의한다. 개발팀(5명)이 7개 마이크로서비스(AUTH, SCHD, PLCE, MNTR, BRIF, ALTN, PAY)를 Azure Kubernetes Service(AKS) 위에서 효율적으로 개발·검증할 수 있도록 인프라 구성, 컴퓨팅 자원 배분, 네트워크 설계, 데이터 관리, 보안, 모니터링 전략을 기술한다.

### 1.2 설계 원칙

| 원칙 | 설명 | 적용 방향 |
|------|------|---------|
| **MVP 우선** | 핵심 기능 검증에 집중. 과도한 인프라 선투자 금지 | 단일 노드풀, replicas=1, 최소 사양 |
| **비용 최적화** | 개발환경 월 비용 $100 이하 목표 | Spot Instance, Standard tier, 야간 축소 |
| **개발 편의성** | 팀원 누구나 빠르게 환경 구성 및 디버깅 가능 | ALLOW_ALL 네트워크 정책, kubectl 직접 접근 |
| **단순성** | 운영 복잡도 최소화. 프로덕션과 구조는 동일하되 규모를 축소 | 단일 Zone, 기본 보안, 수동 운영 허용 |

### 1.3 참조 아키텍처

| 문서 | 경로 |
|------|------|
| 아키텍처 패턴 선정 | `docs/design/architecture.md` |
| 논리 아키텍처 설계 | `docs/design/logical-architecture.md` |
| HighLevel 아키텍처 | `docs/design/high-level-architecture.md` |
| AI 서비스 아키텍처 | `docs/design/ai-service-architecture.md` |
| 전체 아키텍처 다이어그램 | `docs/design/physical/physical-architecture-dev.mmd` |
| 네트워크 다이어그램 | `docs/design/physical/network-dev.mmd` |

---

## 2. 개발환경 아키텍처 개요

### 2.1 환경 특성

| 항목 | 내용 |
|------|------|
| **목적** | 기능 개발, 통합 테스트, QA 검증 |
| **사용자** | 개발팀 5명 (백엔드 1, 프론트엔드 1, DevOps 1, QA 1, AI 1) |
| **가용성 목표** | 95% (업무 시간 기준, 야간·주말 다운타임 허용) |
| **확장성** | 제한적 수평 확장 (replicas 수동 조정) |
| **보안 수준** | 기본 보안 (K8s RBAC, JWT 검증, TLS) |
| **배포 방식** | GitHub Actions → ACR → kubectl apply |
| **데이터 정책** | 테스트 데이터만 허용, 실 사용자 데이터 금지 |

### 2.2 전체 아키텍처

전체 아키텍처 다이어그램은 아래 파일을 참조한다.

```
./physical-architecture-dev.mmd
```

구조 요약:
- **진입점**: NGINX Ingress Controller (LoadBalancer 타입, External IP)
- **서비스 레이어**: 7개 Spring Boot 3.4.x 서비스 Pod (ClusterIP)
- **데이터 레이어**: PostgreSQL Pod + Redis Pod (AKS 내부, Azure Managed Disk PVC)
- **메시징**: Azure Service Bus Basic (외부 관리형 서비스)
- **이미지 저장소**: Azure Container Registry Basic
- **CI/CD**: GitHub Actions

---

## 3. 컴퓨팅 아키텍처

### 3.1 AKS 클러스터 구성

#### 3.1.1 클러스터 설정

| 항목 | 값 | 비고 |
|------|-----|------|
| **Kubernetes 버전** | 1.29 | LTS 채널, 자동 패치 업데이트 |
| **클러스터 티어** | Basic (Free) | 개발환경, Control Plane 비용 없음 |
| **네트워크 플러그인** | Azure CNI | Pod 직접 VNet IP 할당 |
| **DNS 서비스** | CoreDNS | 내부 서비스 디스커버리 |
| **로드 밸런서** | Azure Load Balancer (Standard) | NGINX Ingress Controller 전면 배치 |
| **리전** | Korea Central | 개발팀 위치 기준 |
| **가용성 영역** | 단일 Zone (Zone 1) | 비용 절감, HA 불필요 |
| **RBAC** | 활성화 | K8s 내부 권한 제어 |
| **Azure AD 통합** | 비활성화 | 개발환경 단순화 |
| **자동 업그레이드** | 패치 채널 | 마이너 버전 수동 업그레이드 |
| **네임스페이스** | `travel-dev` | 환경 격리 단위 |

#### 3.1.2 노드 풀

| 항목 | 값 | 비고 |
|------|-----|------|
| **노드 수** | 2 | 최소 운영 단위 |
| **인스턴스 타입** | Standard_B2s | 2 vCPU / 4GB RAM |
| **구매 옵션** | Spot Instance | 최대 70% 비용 절감 |
| **OS 디스크** | 30GB Premium SSD | 기본 설정 |
| **가용성 영역** | 단일 Zone 1 | HA 불필요 |
| **자동 스케일링** | 비활성화 | 수동 조정 |
| **노드 OS** | Ubuntu 22.04 LTS | AKS 기본 |
| **최대 Pod 수/노드** | 30 | Azure CNI 기본값 |
| **선점 중단 처리** | 재시작 허용 | 개발환경 다운타임 허용 |

---

### 3.2 서비스별 리소스 할당

#### 3.2.1 애플리케이션 서비스

7개 마이크로서비스 Pod 리소스 할당표. 모든 서비스는 replicas=1로 운영한다.

| 서비스 | 포트 | CPU Request | CPU Limit | Memory Request | Memory Limit | replicas |
|--------|------|:-----------:|:---------:|:--------------:|:------------:|:--------:|
| **AUTH** | 8081 | 100m | 300m | 256Mi | 512Mi | 1 |
| **SCHD** | 8082 | 100m | 300m | 256Mi | 512Mi | 1 |
| **PLCE** | 8083 | 100m | 300m | 256Mi | 512Mi | 1 |
| **MNTR** | 8084 | 200m | 500m | 512Mi | 1Gi | 1 |
| **BRIF** | 8085 | 100m | 300m | 256Mi | 512Mi | 1 |
| **ALTN** | 8086 | 100m | 300m | 256Mi | 512Mi | 1 |
| **PAY** | 8087 | 100m | 200m | 256Mi | 512Mi | 1 |

> MNTR(Monitor) 서비스는 15분 주기 외부 API 병렬 수집으로 인해 타 서비스 대비 CPU/Memory를 높게 설정한다.

**Java 21 JVM 튜닝 (공통)**:
```
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:InitialRAMPercentage=50.0
-XX:+UseG1GC
```

#### 3.2.2 백킹 서비스

| 서비스 | 이미지 | CPU Request | CPU Limit | Memory Request | Memory Limit | Storage | PVC |
|--------|--------|:-----------:|:---------:|:--------------:|:------------:|:-------:|:---:|
| **PostgreSQL** | postgres:16-alpine | 200m | 500m | 512Mi | 1Gi | 20GB | `postgres-pvc` |
| **Redis** | redis:7-alpine | 100m | 200m | 256Mi | 256Mi | 1GB | `redis-pvc` |

**PVC 설정**:

| PVC 이름 | 스토리지 클래스 | 크기 | Access Mode |
|----------|----------------|:----:|:-----------:|
| `postgres-pvc` | `managed-standard` | 20Gi | ReadWriteOnce |
| `redis-pvc` | `managed-standard` | 1Gi | ReadWriteOnce |

#### 3.2.3 스토리지 클래스

| 스토리지 클래스 | 프로비저너 | 디스크 타입 | 용도 | Reclaim Policy |
|----------------|-----------|:----------:|------|:--------------:|
| `managed-standard` | `disk.csi.azure.com` | Standard HDD | 개발환경 DB 데이터 (비용 우선) | Retain |
| `managed-premium-ssd` | `disk.csi.azure.com` | Premium SSD | 성능 테스트 필요 시 선택적 사용 | Retain |

> 개발환경은 Standard HDD를 기본으로 사용하여 비용을 절감한다. I/O 성능 테스트 시에만 Premium SSD로 전환한다.

#### 3.2.4 AI 서비스

| 단계 | 배포 상태 | 설명 |
|------|:---------:|------|
| **MVP (현재)** | 미배포 | 규칙 기반 로직으로 대체, AI Pod 불필요 |
| **Phase 2 예비** | 예약 리소스 없음 | 노드 여유 용량 내 추가 가능 |

**Phase 2 AI Pipeline Pod 예비 설정** (참조용, 현재 미적용):

| 항목 | 값 |
|------|----|
| 이미지 | `{ACR}/ai-pipeline:latest` |
| 런타임 | Python 3.11 + FastAPI |
| CPU Request / Limit | 200m / 1000m |
| Memory Request / Limit | 512Mi / 2Gi |
| replicas | 1 |

---

## 4. 네트워크 아키텍처

### 4.1.1 네트워크 토폴로지

네트워크 토폴로지 다이어그램은 아래 파일을 참조한다.

```
./network-dev.mmd
```

**VNet 구성**:

| 구성 요소 | CIDR | 설명 |
|---------|------|------|
| VNet | `10.0.0.0/16` | 전체 개발 네트워크 |
| AKS Subnet | `10.0.1.0/24` | AKS 노드 및 Pod IP |
| Service Bus Subnet | `10.0.2.0/24` | Azure Service Bus 연결 |

**트래픽 흐름**:

```
개발자/QA (VPN or Direct)
    ↓ HTTPS:443
NGINX Ingress Controller (External IP, LoadBalancer)
    ↓ /api/{service}/**
ClusterIP Service (내부 DNS)
    ↓ TCP
Application Pod (Spring Boot 3.4.x)
    ↓ TCP:5432 / TCP:6379
PostgreSQL Pod / Redis Pod
```

**Ingress 라우팅 규칙**:

| 경로 | 대상 서비스 | 포트 |
|------|-----------|:----:|
| `/api/auth/**` | `auth-svc` | 8081 |
| `/api/schedules/**` | `schedule-svc` | 8082 |
| `/api/places/**` | `place-svc` | 8083 |
| `/api/monitor/**` | `monitor-svc` | 8084 |
| `/api/briefings/**` | `briefing-svc` | 8085 |
| `/api/alternatives/**` | `alternative-svc` | 8086 |
| `/api/payments/**` | `payment-svc` | 8087 |

### 4.1.2 네트워크 보안

개발환경은 개발 편의성을 위해 기본 Network Policy를 적용한다. DB 접근 제한만 예외적으로 강제한다.

| 정책 | 적용 범위 | 방향 | 설명 |
|------|---------|:----:|------|
| `ALLOW_ALL` | 애플리케이션 Pod 간 | Ingress/Egress | 개발 편의를 위해 서비스 간 자유 통신 허용 |
| `DENY DB from outside` | PostgreSQL Pod | Ingress | 애플리케이션 Pod 외부에서 직접 DB 접근 차단 |
| `ALLOW App → DB` | PostgreSQL Pod | Ingress | 7개 애플리케이션 Pod → PostgreSQL 허용 |
| `ALLOW App → Redis` | Redis Pod | Ingress | PLCE, MNTR, ALTN Pod → Redis 허용 |
| `TLS Termination` | Ingress Controller | Ingress | HTTPS 강제, HTTP → HTTPS 리다이렉트 |

### 4.2 서비스 디스커버리

K8s CoreDNS 기반 내부 서비스 디스커버리. 모든 서비스는 `{서비스명}.travel-dev.svc.cluster.local` 형식으로 접근한다.

| 서비스 | K8s 서비스 이름 | 내부 DNS | 포트 | 타입 |
|--------|--------------|---------|:----:|:----:|
| AUTH | `auth-svc` | `auth-svc.travel-dev.svc.cluster.local` | 8081 | ClusterIP |
| SCHD | `schedule-svc` | `schedule-svc.travel-dev.svc.cluster.local` | 8082 | ClusterIP |
| PLCE | `place-svc` | `place-svc.travel-dev.svc.cluster.local` | 8083 | ClusterIP |
| MNTR | `monitor-svc` | `monitor-svc.travel-dev.svc.cluster.local` | 8084 | ClusterIP |
| BRIF | `briefing-svc` | `briefing-svc.travel-dev.svc.cluster.local` | 8085 | ClusterIP |
| ALTN | `alternative-svc` | `alternative-svc.travel-dev.svc.cluster.local` | 8086 | ClusterIP |
| PAY | `payment-svc` | `payment-svc.travel-dev.svc.cluster.local` | 8087 | ClusterIP |
| PostgreSQL | `postgres-svc` | `postgres-svc.travel-dev.svc.cluster.local` | 5432 | ClusterIP |
| Redis | `redis-svc` | `redis-svc.travel-dev.svc.cluster.local` | 6379 | ClusterIP |

---

## 5. 데이터 아키텍처

### 5.1.1 PostgreSQL Pod

| 항목 | 값 |
|------|----|
| **이미지** | `postgres:16-alpine` |
| **CPU Request / Limit** | 200m / 500m |
| **Memory Request / Limit** | 512Mi / 1Gi |
| **스토리지** | Azure Managed Disk Standard 20GB |
| **PVC 이름** | `postgres-pvc` |
| **마운트 경로** | `/var/lib/postgresql/data` |
| **데이터베이스** | `travel_planner_dev` |
| **접속 포트** | 5432 |
| **인증** | 환경변수 `POSTGRES_PASSWORD` (K8s Secret) |
| **연결 풀** | HikariCP (Spring Boot 기본, 최대 10개/서비스) |

**PostgreSQL 설정 (`postgresql.conf` 주요 항목)**:

| 파라미터 | 값 | 설명 |
|---------|-----|------|
| `max_connections` | 100 | 7개 서비스 × 10개 풀 = 70개 예상 |
| `shared_buffers` | 128MB | Memory Limit의 25% |
| `work_mem` | 4MB | 정렬/해시 작업 메모리 |
| `maintenance_work_mem` | 64MB | VACUUM, CREATE INDEX 등 |
| `log_slow_queries` | 1000ms | 슬로우 쿼리 로깅 |

### 5.1.2 Redis Pod

| 항목 | 값 |
|------|----|
| **이미지** | `redis:7-alpine` |
| **CPU Request / Limit** | 100m / 200m |
| **Memory Request / Limit** | 256Mi / 256Mi |
| **스토리지** | Azure Managed Disk Standard 1GB |
| **PVC 이름** | `redis-pvc` |
| **마운트 경로** | `/data` |
| **접속 포트** | 6379 |
| **maxmemory** | `256mb` |
| **eviction policy** | `allkeys-lru` |
| **persistence** | AOF 비활성화 (개발환경 재시작 시 초기화 허용) |
| **인증** | `requirepass` (K8s Secret) |

**Redis 캐시 키 전략**:

| 서비스 | 캐시 키 패턴 | TTL |
|--------|-----------|:---:|
| PLCE | `place:{placeId}` | 1시간 |
| MNTR | `monitor:status:{placeId}` | 10분 |
| ALTN | `alternative:{placeId}:{category}` | 30분 |

### 5.2.1 데이터 초기화

개발환경 최초 구성 및 스키마 변경 시 아래 절차를 따른다.

| 단계 | 방법 | 설명 |
|------|------|------|
| 1. DB Pod 기동 | `kubectl apply -f postgres-deployment.yaml` | PostgreSQL Pod 생성 및 PVC 마운트 |
| 2. 스키마 마이그레이션 | Flyway (Spring Boot 시작 시 자동 실행) | `classpath:db/migration/V*.sql` 순차 적용 |
| 3. 기초 데이터 삽입 | K8s Job `flyway-seed-job` | 테스트용 시드 데이터 삽입 |
| 4. 검증 | `kubectl exec` → `psql` 접속 확인 | 테이블 생성 및 데이터 확인 |

**Flyway 마이그레이션 명명 규칙**:
```
V{버전}__{설명}.sql
예) V1__create_auth_schema.sql
    V2__create_schedule_schema.sql
    V3__seed_test_places.sql
```

### 5.2.2 백업 전략

개발환경은 테스트 데이터만 보유하므로 경량 백업 전략을 적용한다.

| 항목 | 값 |
|------|----|
| **백업 도구** | `pg_dump` |
| **백업 주기** | 매일 02:00 (K8s CronJob) |
| **백업 위치** | Azure Blob Storage (개발 버킷) |
| **보존 기간** | 3일 |
| **백업 포맷** | custom (`-Fc`) |
| **알림** | 실패 시 팀 Slack 채널 알림 |

**백업 CronJob 예시**:
```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
  namespace: travel-dev
spec:
  schedule: "0 2 * * *"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: pg-backup
            image: postgres:16-alpine
            command:
            - /bin/sh
            - -c
            - |
              pg_dump -Fc $DATABASE_URL > /backup/travel_dev_$(date +%Y%m%d).dump
              # Azure Blob 업로드 (azcopy)
          restartPolicy: OnFailure
```

---

## 6. 메시징 아키텍처

### 6.1.1 Azure Service Bus Basic 설정

| 항목 | 값 |
|------|----|
| **티어** | Basic |
| **네임스페이스** | `travel-planner-dev` |
| **리전** | Korea Central |
| **메시지 최대 크기** | 256KB |
| **메시지 보존 기간** | 1일 |
| **Dead Letter Queue** | 활성화 |

**큐 구성**:

| 큐 이름 | 발행 서비스 | 구독 서비스 | 목적 | Lock Duration |
|--------|-----------|-----------|------|:-------------:|
| `monitor-status-change` | MNTR | BRIF, ALTN | 장소 상태 변경 알림 | 30초 |
| `briefing-generation` | SCHD | BRIF | 브리핑 생성 요청 | 60초 |
| `alternative-search` | ALTN | SCHD | 대안 선택 후 일정 반영 | 30초 |

### 6.1.2 연결 설정

| 항목 | 값 |
|------|----|
| **프로토콜** | AMQP 1.0 |
| **연결 문자열** | K8s Secret `servicebus-connection-string` |
| **재시도 횟수** | 3회 |
| **재시도 간격** | 지수 백오프 (1s → 2s → 4s) |
| **연결 타임아웃** | 30초 |
| **메시지 수신 모드** | PeekLock (처리 완료 후 삭제) |
| **최대 동시 처리** | 1개/서비스 (개발환경 단순화) |

**Spring Boot 설정 예시** (`application-dev.yml`):
```yaml
spring:
  cloud:
    azure:
      servicebus:
        connection-string: ${AZURE_SERVICEBUS_CONNECTION_STRING}
        entity-type: queue
  jms:
    servicebus:
      idle-timeout: 1800000
      retry:
        max-attempts: 3
        backoff:
          initial-interval: 1s
          multiplier: 2.0
          max-interval: 30s
```

---

## 7. 보안 아키텍처

### 7.1.1 기본 보안

| 보안 항목 | 구현 방법 | 적용 범위 |
|---------|---------|---------|
| **K8s RBAC** | ServiceAccount + ClusterRole 분리 | 네임스페이스 `travel-dev` |
| **JWT 검증** | NGINX Ingress `auth-url` 어노테이션 | 모든 API 엔드포인트 |
| **TLS** | cert-manager (Let's Encrypt) | Ingress → 외부 구간 |
| **컨테이너 보안** | `runAsNonRoot: true`, `readOnlyRootFilesystem: true` | 모든 애플리케이션 Pod |
| **이미지 스캔** | ACR 내장 취약점 스캔 (기본) | 배포 전 |

### 7.1.2 시크릿 관리

| 시크릿 이름 | 내용 | 관리 방법 | 순환 주기 |
|------------|------|---------|:--------:|
| `postgres-secret` | DB 비밀번호 | K8s Secret (base64) | 수동 (필요 시) |
| `redis-secret` | Redis 비밀번호 | K8s Secret (base64) | 수동 (필요 시) |
| `jwt-secret` | JWT 서명 키 | K8s Secret (base64) | 수동 (필요 시) |
| `servicebus-connection-string` | Service Bus 연결 문자열 | K8s Secret (base64) | 수동 (필요 시) |
| `google-api-key` | Google Places/Directions API 키 | K8s Secret (base64) | 수동 (필요 시) |
| `openweather-api-key` | OpenWeatherMap API 키 | K8s Secret (base64) | 수동 (필요 시) |
| `fcm-service-account` | FCM 서비스 계정 JSON | K8s Secret (base64) | 수동 (필요 시) |
| `oauth-credentials` | Google/Apple OAuth Client ID/Secret | K8s Secret (base64) | 수동 (필요 시) |

> 개발환경은 K8s Secret을 직접 사용한다. 프로덕션에서는 Azure Key Vault + CSI Driver로 전환한다.

### 7.2.1 Network Policies

개발환경은 개발 편의를 위해 ALLOW_ALL을 기본으로 하되, DB 직접 접근만 제한한다.

| 정책 이름 | 대상 | 허용 소스 | 포트 | 목적 |
|---------|------|---------|:----:|------|
| `allow-app-to-postgres` | PostgreSQL Pod | 7개 애플리케이션 Pod | 5432 | 애플리케이션 DB 접근 허용 |
| `deny-external-to-postgres` | PostgreSQL Pod | 외부 (Ingress 제외) | 5432 | 직접 DB 접근 차단 |
| `allow-app-to-redis` | Redis Pod | PLCE, MNTR, ALTN Pod | 6379 | 캐시 접근 허용 |
| `allow-all-ingress` | 애플리케이션 Pod | 모든 소스 | ALL | 개발 편의 (서비스 간 자유 통신) |

**PostgreSQL Network Policy 예시**:
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-app-to-postgres
  namespace: travel-dev
spec:
  podSelector:
    matchLabels:
      app: postgres
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          tier: application
    ports:
    - protocol: TCP
      port: 5432
```

---

## 8. 모니터링 및 로깅

### 8.1.1 K8s 기본 모니터링

| 도구 | 용도 | 접근 방법 |
|------|------|---------|
| `kubectl top nodes` | 노드 CPU/Memory 사용률 | CLI |
| `kubectl top pods` | Pod별 리소스 사용률 | CLI |
| `kubectl describe pod` | Pod 이벤트 및 상태 | CLI |
| `kubectl get events` | 클러스터 이벤트 | CLI |
| Azure Portal (AKS Insights) | 노드/Pod 메트릭 대시보드 | 웹 |

**기본 알림 (Azure Monitor Alert)**:

| 알림 조건 | 임계값 | 알림 채널 |
|---------|:------:|---------|
| Pod CrashLoopBackOff | 재시작 횟수 ≥ 3 | 팀 Slack |
| 노드 CPU 사용률 | ≥ 80% (5분 지속) | 팀 Slack |
| 노드 Memory 사용률 | ≥ 85% (5분 지속) | 팀 Slack |
| PVC 사용률 | ≥ 80% | 팀 Slack |

### 8.1.2 애플리케이션 모니터링

Spring Boot Actuator를 통한 헬스체크 및 메트릭 노출.

| 엔드포인트 | 경로 | 용도 | 접근 제어 |
|---------|------|------|---------|
| Health | `/actuator/health` | Pod Liveness/Readiness Probe | 내부 허용 |
| Info | `/actuator/info` | 빌드 정보, 버전 | 내부 허용 |
| Metrics | `/actuator/metrics` | JVM, HTTP 메트릭 | 내부 허용 |
| Prometheus | `/actuator/prometheus` | Prometheus 메트릭 스크래핑 | 내부 허용 |

**K8s Probe 설정 (공통)**:

| Probe 타입 | 경로 | 초기 대기 | 주기 | 실패 임계값 |
|-----------|------|:--------:|:---:|:-----------:|
| Liveness | `/actuator/health/liveness` | 60초 | 10초 | 3회 |
| Readiness | `/actuator/health/readiness` | 30초 | 5초 | 3회 |

### 8.2.1 로그 수집

개발환경은 ELK/Loki 스택 없이 K8s 기본 로그를 활용한다.

| 항목 | 값 |
|------|----|
| **로그 출력** | stdout / stderr (K8s 표준) |
| **로그 형식** | JSON (Logback + logstash-logback-encoder) |
| **로그 조회** | `kubectl logs` CLI |
| **로그 보존** | 7일 (K8s 기본 로테이션, 노드 디스크 용량 기준) |
| **로그 레벨** | DEBUG (개발환경, `application-dev.yml`) |
| **보존 스토리지** | Azure Monitor Log Analytics (기본 수집, 30일) |

**로그 조회 예시**:
```bash
# 특정 서비스 실시간 로그
kubectl logs -f deploy/monitor-service -n travel-dev

# 이전 Pod 로그 (재시작 후)
kubectl logs deploy/monitor-service -n travel-dev --previous

# 특정 시간 이후 로그
kubectl logs deploy/monitor-service -n travel-dev --since=1h
```

---

## 9. 배포 관련 컴포넌트

| 컴포넌트 | 도구/서비스 | 티어/설정 | 용도 |
|---------|-----------|---------|------|
| **CI/CD** | GitHub Actions | 무료 (Public 기준) | 빌드, 테스트, 이미지 빌드/푸시 |
| **컨테이너 레지스트리** | Azure Container Registry | Basic | 이미지 저장 (10GB 포함) |
| **배포 방식** | `kubectl apply -f` | 수동 트리거 | Manifest 직접 적용 |
| **이미지 태그 전략** | `{서비스}:{git-sha}` | — | 롤백 추적 가능 |
| **네임스페이스** | `travel-dev` | — | 환경 격리 |
| **배포 검증** | `kubectl rollout status` | — | 배포 성공 여부 확인 |
| **롤백** | `kubectl rollout undo` | — | 배포 실패 시 이전 버전 복구 |

**GitHub Actions 워크플로 (간략)**:
```yaml
name: Build and Deploy to Dev
on:
  push:
    branches: [develop]
jobs:
  build-and-push:
    steps:
    - uses: actions/checkout@v4
    - name: Login to ACR
      uses: azure/docker-login@v1
    - name: Build and Push
      run: |
        docker build -t $ACR_LOGIN_SERVER/$SERVICE:$GITHUB_SHA .
        docker push $ACR_LOGIN_SERVER/$SERVICE:$GITHUB_SHA
  deploy:
    needs: build-and-push
    steps:
    - name: Set K8s context
      uses: azure/aks-set-context@v3
    - name: Deploy
      run: |
        kubectl set image deployment/$SERVICE \
          $SERVICE=$ACR_LOGIN_SERVER/$SERVICE:$GITHUB_SHA \
          -n travel-dev
        kubectl rollout status deployment/$SERVICE -n travel-dev
```

---

## 10. 비용 최적화

### 10.1.1 주요 비용 요소

| 항목 | 서비스 | 월 예상 비용 | 비고 |
|------|--------|:----------:|------|
| **AKS 노드** | AKS (Standard_B2s × 2, Spot) | ~$30 | Spot 할인 70% 적용 |
| **DB 스토리지** | Azure Managed Disk (Standard 20GB) | ~$2 | HDD 기준 |
| **Azure Service Bus** | Service Bus Basic | ~$10 | 메시지 수 기준 (100만 건/월 이내 예상) |
| **Azure Container Registry** | ACR Basic | ~$5 | 10GB 스토리지 포함 |
| **Azure Load Balancer** | Standard (Ingress용) | ~$20 | 고정 IP + 데이터 처리 |
| **Azure Monitor** | Log Analytics (기본) | ~$5 | 5GB/월 무료 이후 과금 |
| **Azure Blob Storage** | 백업 저장소 | ~$1 | 10GB 이내 |
| **네트워크 Egress** | 외부 API 호출 | ~$5 | 개발 트래픽 기준 |
| **합계** | — | **~$78-98** | 월 $100 이하 목표 달성 |

### 10.1.2 비용 절약 전략

| 전략 | 절약 효과 | 적용 방법 |
|------|:--------:|---------|
| **Spot Instance 사용** | ~70% 노드 비용 절감 | AKS 노드풀 Spot Instance 설정 |
| **야간/주말 클러스터 축소** | ~40% 추가 절감 | 평일 09:00~21:00 이외 노드 0개 (Spot 중단 시 자동) |
| **단일 가용성 영역** | Zone 중복 비용 없음 | 단일 Zone 1 설정 |
| **Standard HDD 스토리지** | Premium SSD 대비 80% 절감 | 개발환경 Standard 적용 |
| **Service Bus Basic** | Standard 대비 ~$10 절감 | 토픽 대신 큐 사용 (Basic 제약 수용) |
| **ACR Basic** | Standard 대비 ~$15 절감 | 단일 레지스트리, 지역 복제 불필요 |
| **이미지 최적화** | 스토리지 비용 절감 | Alpine 기반 이미지, 멀티스테이지 빌드 |

---

## 11. 개발환경 운영 가이드

### 11.1.1 환경 시작/종료

**환경 시작 (클러스터 기동)**:
```bash
# AKS 클러스터 시작 (야간 절전 후 재시작)
az aks start --resource-group travel-planner-dev-rg \
             --name travel-planner-dev-aks

# kubectl 컨텍스트 설정
az aks get-credentials --resource-group travel-planner-dev-rg \
                       --name travel-planner-dev-aks

# 네임스페이스 확인
kubectl get namespaces

# 전체 서비스 기동 확인
kubectl get pods -n travel-dev -w
```

**환경 종료 (클러스터 중지)**:
```bash
# 클러스터 중지 (노드 VM 중지, 비용 절감)
az aks stop --resource-group travel-planner-dev-rg \
            --name travel-planner-dev-aks
```

**서비스 재시작**:
```bash
# 특정 서비스 재시작
kubectl rollout restart deployment/monitor-service -n travel-dev

# 전체 서비스 재시작
kubectl rollout restart deployment -n travel-dev

# 배포 상태 확인
kubectl rollout status deployment/monitor-service -n travel-dev
```

**스케일링 (필요 시 수동 조정)**:
```bash
# replicas 증가 (부하 테스트 시)
kubectl scale deployment/monitor-service --replicas=2 -n travel-dev

# replicas 원복
kubectl scale deployment/monitor-service --replicas=1 -n travel-dev
```

### 11.1.2 데이터 관리

**DB 초기화**:
```bash
# PostgreSQL Pod 접속
kubectl exec -it deploy/postgres -n travel-dev -- psql -U postgres travel_planner_dev

# 스키마 전체 삭제 후 재생성 (주의: 데이터 삭제)
kubectl exec -it deploy/postgres -n travel-dev -- psql -U postgres -c \
  "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# Flyway 마이그레이션 재실행 (서비스 재시작으로 자동 실행)
kubectl rollout restart deployment -n travel-dev
```

**수동 백업**:
```bash
# 즉시 백업 실행
kubectl exec -it deploy/postgres -n travel-dev -- \
  pg_dump -Fc travel_planner_dev > ./backup_$(date +%Y%m%d_%H%M%S).dump

# 백업 파일 로컬로 복사
kubectl cp travel-dev/postgres-pod:/tmp/backup.dump ./backup.dump
```

**백업에서 복원**:
```bash
# 로컬 dump 파일을 Pod로 복사
kubectl cp ./backup.dump travel-dev/postgres-pod:/tmp/backup.dump

# 복원 실행
kubectl exec -it deploy/postgres -n travel-dev -- \
  pg_restore -Fc -d travel_planner_dev /tmp/backup.dump
```

**Redis 캐시 초기화**:
```bash
# Redis Pod 접속 후 전체 캐시 삭제
kubectl exec -it deploy/redis -n travel-dev -- redis-cli FLUSHALL

# 특정 패턴 키 삭제 (예: 장소 캐시)
kubectl exec -it deploy/redis -n travel-dev -- redis-cli --scan --pattern "place:*" | \
  xargs kubectl exec -it deploy/redis -n travel-dev -- redis-cli DEL
```

---

### 11.2.1 트러블슈팅

| 증상 | 원인 | 확인 명령어 | 해결 방법 |
|------|------|-----------|---------|
| **Pod CrashLoopBackOff** | 애플리케이션 시작 오류 (DB 연결 실패, 설정 오류 등) | `kubectl logs deploy/{service} -n travel-dev --previous` | 로그 확인 후 Secret/ConfigMap 점검. DB Pod 기동 순서 확인 |
| **OOMKilled** | Memory Limit 초과 | `kubectl describe pod {pod-name} -n travel-dev` | Memory Limit 상향 (예: 512Mi → 1Gi) 또는 JVM 힙 설정 확인 (`-XX:MaxRAMPercentage`) |
| **ImagePullBackOff** | ACR 인증 실패 또는 이미지 미존재 | `kubectl describe pod {pod-name} -n travel-dev` | ACR Pull Secret 확인. 이미지 태그 존재 여부 확인: `az acr repository show-tags` |
| **Pending 상태 지속** | 노드 리소스 부족 (CPU/Memory) | `kubectl describe pod {pod-name} -n travel-dev` (Events 섹션) | `kubectl top nodes`로 노드 사용률 확인. 불필요한 Pod 종료 또는 노드 증설 |
| **DB 연결 타임아웃** | PostgreSQL Pod 미기동 또는 Network Policy 오류 | `kubectl get pods -n travel-dev \| grep postgres` | PostgreSQL Pod 상태 확인. `kubectl exec`으로 직접 접속 테스트 |
| **Spot Instance 선점** | Azure가 Spot VM 회수 | `kubectl get events -n travel-dev` | 클러스터 재시작: `az aks start`. Pod 자동 재스케줄링 대기 |
| **Service Bus 연결 오류** | 연결 문자열 오류 또는 네트워크 차단 | `kubectl logs deploy/{service} -n travel-dev \| grep servicebus` | Secret `servicebus-connection-string` 값 확인. AMQP 포트(5671) 아웃바운드 허용 확인 |
| **느린 응답 (> 5초)** | DB 슬로우 쿼리 또는 외부 API 타임아웃 | `kubectl logs deploy/monitor-service -n travel-dev \| grep "slow"` | PostgreSQL slow query 로그 확인. 외부 API 타임아웃 설정 점검 |

---

## 12. 개발환경 특성 요약

### 핵심 원칙

| 원칙 | 내용 |
|------|------|
| **단순 우선** | 모든 서비스 replicas=1, 단일 Zone, 수동 운영 허용 |
| **비용 최적화** | Spot Instance + 야간 중지로 월 $100 이내 유지 |
| **개발 속도** | ALLOW_ALL 네트워크, 자동 Flyway 마이그레이션, kubectl 직접 접근 |
| **프로덕션 동형** | 동일 K8s 구조, 동일 서비스 구성. 규모만 축소 |

### 제약사항

| 제약 | 내용 |
|------|------|
| **가용성** | 95% (Spot Instance 선점, 야간 중지로 다운타임 발생 가능) |
| **데이터** | 테스트 데이터만 허용. 실 사용자 PII 데이터 적재 금지 |
| **확장성** | 수평 확장 수동. 자동 스케일링 미적용 |
| **HA** | 단일 Pod, 단일 Zone. 장애 시 수동 복구 |
| **보안** | 기본 보안 수준. WAF, DDoS Protection 미적용 |

### 최적화 목표

| 목표 | 지표 | 달성 전략 |
|------|:----:|---------|
| 월 인프라 비용 | < $100 | Spot + 야간 축소 + 경량 티어 |
| 환경 구성 시간 | < 30분 | IaC(Terraform/Bicep) + 자동화 스크립트 |
| 배포 소요 시간 | < 5분 | GitHub Actions 병렬 빌드, 경량 이미지 |
| 로컬-개발환경 일치성 | 100% | Docker Compose 로컬 환경 동일 구성 |
| 디버깅 편의성 | 높음 | kubectl 직접 접근, DEBUG 로그 레벨, Port Forward 허용 |
