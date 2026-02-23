# 운영환경 물리 아키텍처 설계서

> 작성자: 홍길동/아키 (소프트웨어 아키텍트)
> 작성일: 2026-02-23
> 프로젝트: travel-planner — 여행 중 실시간 일정 최적화 가이드 앱
> 환경: Production (운영)
> 참조: high-level-architecture.md, logical-architecture.md, physical-architecture-prod.mmd

---

## 1. 개요

### 1.1 설계 목적

본 문서는 travel-planner 서비스의 운영환경(Production) 물리 아키텍처를 정의한다. 1만~10만 명 규모의 실사용자를 대상으로 99.9% 가용성을 보장하며, 엔터프라이즈 수준의 보안·관측 가능성·재해복구 체계를 갖춘 Azure 기반 클라우드 인프라를 명세한다.

### 1.2 설계 원칙

| 원칙 | 설명 | 적용 수단 |
|------|------|----------|
| **고가용성** | 단일 장애점(SPOF) 제거. 모든 핵심 컴포넌트 다중화 | Multi-Zone AKS, Zone Redundant DB/Redis/Gateway |
| **확장성** | 트래픽 변화에 자동 대응. 서비스별 독립 스케일링 | HPA, KEDA, Azure Auto Scale |
| **보안 우선** | 네트워크 경계부터 데이터 계층까지 다층 방어 | WAF, Private Endpoint, RBAC, Key Vault HSM |
| **관측 가능성** | 모든 계층에서 메트릭·로그·트레이스 수집 | Azure Monitor, App Insights, Container Insights |
| **재해복구** | RTO 4시간, RPO 1시간 목표. 자동 장애조치 우선 | Geo-redundant Backup, Auto Failover, PDB |

### 1.3 참조 아키텍처

- Azure Well-Architected Framework (Reliability, Security, Cost Optimization)
- Azure AKS Landing Zone Accelerator
- Azure Database for PostgreSQL — Flexible Server HA 설계 가이드
- NIST Cybersecurity Framework 2.0

---

## 2. 운영환경 아키텍처 개요

### 2.1 환경 특성

| 항목 | 값 |
|------|-----|
| 환경 구분 | Production (실서비스) |
| 대상 사용자 규모 | 1만~10만 명 |
| 목표 가용성 | 99.9% (월 약 43분 허용 다운타임) |
| 배포 리전 | Korea Central (주), Korea South (DR 백업) |
| 스케일링 방식 | 자동 스케일링 (HPA + Cluster Autoscaler) |
| 보안 등급 | 엔터프라이즈 (Private Cluster, WAF, HSM) |
| 배포 전략 | GitOps (ArgoCD) + Blue/Green |
| 비용 목표 | 월 $2,500~3,000 |

### 2.2 전체 아키텍처

전체 물리 아키텍처 다이어그램은 아래 파일을 참조한다.

```
./physical-architecture-prod.mmd
```

주요 트래픽 흐름:

```
모바일 클라이언트 (Flutter 3.x)
  → Azure Front Door + CDN (글로벌 POP)
    → Application Gateway + WAF v2 (Zone Redundant)
      → AKS Ingress (AGIC)
        → 7개 마이크로서비스 Pod (Multi-Zone)
          → Azure PostgreSQL Flexible (HA Zone Redundant)
          → Azure Cache for Redis Premium P2
          → Azure Service Bus Premium
          → Azure Key Vault Premium
```

---

## 3. 컴퓨팅 아키텍처

### 3.1 AKS 구성

#### 3.1.1 클러스터 설정

| 항목 | 값 |
|------|-----|
| Kubernetes 버전 | 1.29 (LTS 채널, 자동 패치) |
| 클러스터 티어 | Standard (SLA 99.9% 보장) |
| 네트워크 플러그인 | Azure CNI (Pod 직접 VNet IP 할당) |
| 인그레스 컨트롤러 | AGIC (Application Gateway Ingress Controller) |
| 인증/인가 | Azure AD RBAC + Kubernetes RBAC 통합 |
| 클러스터 접근 | Private Cluster (Public API Server 비활성화) |
| DNS | CoreDNS (내부), Azure DNS (외부) |
| 컨테이너 런타임 | containerd 1.7+ |
| 이미지 레지스트리 | Azure Container Registry Premium (Geo-replicated) |
| GitOps | ArgoCD HA (System Pool 배포) |

#### 3.1.2 노드 풀

| 풀 이름 | VM SKU | 최소 노드 | 최대 노드 | 가용 영역 | 용도 |
|---------|--------|-----------|-----------|-----------|------|
| systempool | Standard_D2s_v3 | 3 | 3 | Zone 1/2/3 | CoreDNS, metrics-server, ArgoCD, kube-system |
| apppool | Standard_D4s_v3 | 3 | 10 | Zone 1/2/3 | 7개 마이크로서비스 Pod |
| aipool (Phase 2) | Standard_NC6s_v3 | 0 | 3 | Zone 1/2 | AI Pipeline (GPU, Python/FastAPI) |

> **노드 사양 상세**
> - D2s_v3: vCPU 2, RAM 8GB, 임시 SSD 16GB
> - D4s_v3: vCPU 4, RAM 16GB, 임시 SSD 32GB
> - NC6s_v3 (Phase 2): vCPU 6, RAM 112GB, NVIDIA Tesla V100 1개

### 3.2 고가용성

#### 3.2.1 Multi-Zone 배포

| 항목 | 구성 | 비고 |
|------|------|------|
| 배포 영역 | Korea Central Zone 1, 2, 3 | 3개 물리 데이터센터 분산 |
| Pod Anti-Affinity | topologyKey: topology.kubernetes.io/zone | 동일 Zone 집중 방지 |
| Pod Disruption Budget | minAvailable: 1 (모든 서비스) | 유지보수 시 최소 1개 Pod 보장 |
| Node 장애 허용 | Zone 1개 전체 장애 시 서비스 유지 | 2/3 Zone 정상 운영 |
| Cluster Autoscaler | 3~10 노드 자동 조정 | 10분 스케일 다운 대기 |

### 3.3 서비스별 리소스

#### 3.3.1 애플리케이션 서비스 리소스

| 서비스 | CPU Request | CPU Limit | Mem Request | Mem Limit | Min Replicas | Max Replicas | HPA Target CPU |
|--------|------------|-----------|-------------|-----------|:------------:|:------------:|:--------------:|
| AUTH | 200m | 500m | 256Mi | 512Mi | 2 | 6 | 70% |
| SCHD | 300m | 800m | 512Mi | 1Gi | 2 | 8 | 60% |
| PLCE | 300m | 800m | 512Mi | 1Gi | 2 | 8 | 60% |
| MNTR | 400m | 1000m | 512Mi | 1Gi | 2 | 6 | 70% |
| BRIF | 200m | 600m | 256Mi | 512Mi | 2 | 6 | 65% |
| ALTN | 200m | 600m | 256Mi | 512Mi | 2 | 6 | 65% |
| PAY | 100m | 300m | 128Mi | 256Mi | 2 | 4 | 50% |

#### 3.3.2 HPA 구성

```yaml
# 예시: SCHD 서비스 HPA (타 서비스도 동일 구조 적용)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: schd-hpa
  namespace: travel-planner
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: schd-service
  minReplicas: 2
  maxReplicas: 8
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 75
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 2
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Pods
          value: 1
          periodSeconds: 120
---
# AUTH HPA
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-hpa
  namespace: travel-planner
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 2
  maxReplicas: 6
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
---
# MNTR HPA (KEDA 기반 Service Bus 큐 길이 연동)
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: mntr-scaledobject
  namespace: travel-planner
spec:
  scaleTargetRef:
    name: mntr-service
  minReplicaCount: 2
  maxReplicaCount: 6
  triggers:
    - type: azure-servicebus
      metadata:
        queueName: schedule-events
        namespace: travel-planner-prod-sb
        messageCount: "20"
```

#### 3.3.3 AI 서비스 (Phase 2)

```yaml
# Phase 2: AI Pipeline Pod 설정
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-pipeline
  namespace: travel-planner
spec:
  replicas: 1
  template:
    spec:
      nodeSelector:
        agentpool: aipool
      tolerations:
        - key: "sku"
          operator: "Equal"
          value: "gpu"
          effect: "NoSchedule"
      containers:
        - name: ai-pipeline
          image: travelplanneracr.azurecr.io/ai-pipeline:latest
          resources:
            requests:
              cpu: "2"
              memory: "8Gi"
              nvidia.com/gpu: "1"
            limits:
              cpu: "4"
              memory: "16Gi"
              nvidia.com/gpu: "1"
          env:
            - name: AZURE_OPENAI_ENDPOINT
              valueFrom:
                secretKeyRef:
                  name: ai-secrets
                  key: openai-endpoint
```

---

## 4. 네트워크 아키텍처

### 4.1 네트워크 토폴로지

네트워크 토폴로지 다이어그램은 아래 파일을 참조한다.

```
./network-prod.mmd
```

#### 4.1.1 VNet 구성

| 서브넷 이름 | CIDR | 용도 | 주요 리소스 |
|------------|------|------|------------|
| gateway-subnet | 10.0.4.0/24 | 인터넷 경계 | Application Gateway v2 (WAF) |
| aks-subnet | 10.0.1.0/24 | AKS Pod/Node | AKS 노드, Pod IP (/22로 확장 가능) |
| data-subnet | 10.0.2.0/24 | 데이터 계층 | PostgreSQL, Redis Private Endpoint |
| mgmt-subnet | 10.0.3.0/24 | 관리 | Bastion Host, Jump Server |

> AKS Pod CIDR는 Azure CNI 사용으로 aks-subnet 범위 내 직접 할당. Pod 수 증가 대비 /22 (10.0.0.0/22) 확장 설계.

#### 4.1.2 NSG (Network Security Group)

**인바운드 규칙**

| 우선순위 | 이름 | 소스 | 포트 | 프로토콜 | 액션 |
|---------|------|------|------|----------|------|
| 100 | Allow-HTTPS-Internet | Internet | 443 | TCP | Allow |
| 110 | Allow-AGW-to-AKS | 10.0.4.0/24 | 80, 443 | TCP | Allow |
| 120 | Allow-AKS-to-Data | 10.0.1.0/24 | 5432, 6380 | TCP | Allow |
| 130 | Allow-Mgmt-SSH | 10.0.3.0/24 | 22 | TCP | Allow |
| 4096 | Deny-All | * | * | * | Deny |

**아웃바운드 규칙**

| 우선순위 | 이름 | 대상 | 포트 | 프로토콜 | 액션 |
|---------|------|------|------|----------|------|
| 100 | Allow-AKS-to-ACR | AzureContainerRegistry | 443 | TCP | Allow |
| 110 | Allow-AKS-to-KeyVault | AzureKeyVault | 443 | TCP | Allow |
| 120 | Allow-AKS-to-Monitor | AzureMonitor | 443 | TCP | Allow |
| 130 | Allow-AKS-Internet | Internet | 443 | TCP | Allow |
| 4096 | Deny-All | * | * | * | Deny |

### 4.2 Application Gateway 및 WAF

#### 4.2.1 Application Gateway

| 항목 | 값 |
|------|-----|
| SKU | WAF_v2 |
| 배포 방식 | Zone Redundant (Zone 1, 2, 3) |
| 최소 인스턴스 | 2 |
| 최대 인스턴스 | 10 |
| TLS 버전 | 1.2 이상 (1.3 권장) |
| 인증서 관리 | Azure Key Vault 연동 자동 갱신 |

**라우팅 규칙**

| 경로 | 백엔드 서비스 | 비고 |
|------|--------------|------|
| /api/v1/auth/* | auth-service:8080 | JWT 발급/갱신 |
| /api/v1/schedules/* | schd-service:8080 | 일정 관리 |
| /api/v1/places/* | plce-service:8080 | 장소 검색 |
| /api/v1/monitor/* | mntr-service:8080 | 상태 조회 |
| /api/v1/briefings/* | brif-service:8080 | 브리핑 |
| /api/v1/alternatives/* | altn-service:8080 | 대안 카드 |
| /api/v1/payments/* | pay-service:8080 | 결제 |
| /health | health-aggregator | 전체 헬스체크 |

#### 4.2.2 WAF 정책

```yaml
# WAF 정책 (OWASP CRS 3.2 + 커스텀 규칙)
apiVersion: network.azure.com/v1
kind: WebApplicationFirewallPolicy
metadata:
  name: travel-planner-waf-prod
spec:
  policySettings:
    state: Enabled
    mode: Prevention
    requestBodyCheck: true
    maxRequestBodySizeInKb: 128
    fileUploadLimitInMb: 10
  managedRules:
    managedRuleSets:
      - ruleSetType: OWASP
        ruleSetVersion: "3.2"
      - ruleSetType: Microsoft_BotManagerRuleSet
        ruleSetVersion: "1.0"
  customRules:
    - name: RateLimitAnonymous
      priority: 10
      ruleType: RateLimitRule
      rateLimitDuration: OneMin
      rateLimitThreshold: 100
      matchConditions:
        - matchVariables:
            - variableName: RequestHeaders
              selector: X-User-Tier
          operator: Equal
          matchValues: ["anonymous"]
      action: Block
    - name: RateLimitFreeUser
      priority: 20
      ruleType: RateLimitRule
      rateLimitDuration: OneMin
      rateLimitThreshold: 300
      matchConditions:
        - matchVariables:
            - variableName: RequestHeaders
              selector: X-User-Tier
          operator: Equal
          matchValues: ["free"]
      action: Block
    - name: BlockSuspiciousGeo
      priority: 30
      ruleType: MatchRule
      matchConditions:
        - matchVariables:
            - variableName: RemoteAddr
          operator: GeoMatch
          matchValues: ["CN", "RU", "KP"]
      action: Log
```

### 4.3 Network Policies

#### 4.3.1 서비스 간 통신 정책

| 소스 서비스 | 허용 대상 (Ingress) | 허용 포트 | 방향 |
|------------|---------------------|-----------|------|
| agic (ingress) | auth, schd, plce, mntr, brif, altn, pay | 8080 | → |
| schd | plce (장소 검색), altn (교체 수신) | 8080 | → |
| altn | plce (반경 검색) | 8080 | → |
| brif | 외부 FCM (egress only) | 443 | → |
| mntr | 외부 Google Places, OpenWeatherMap, Directions | 443 | → |
| all services | postgresql, redis, service-bus | 5432, 6380, 5671 | → |
| all services | key-vault, app-config | 443 | → |

```yaml
# 예시: SCHD 서비스 NetworkPolicy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: schd-network-policy
  namespace: travel-planner
spec:
  podSelector:
    matchLabels:
      app: schd-service
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: ingress-nginx
      ports:
        - protocol: TCP
          port: 8080
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: plce-service
      ports:
        - protocol: TCP
          port: 8080
    - to:
        - podSelector:
            matchLabels:
              app: altn-service
      ports:
        - protocol: TCP
          port: 8080
    - to:  # PostgreSQL Private Endpoint
        - ipBlock:
            cidr: 10.0.2.0/24
      ports:
        - protocol: TCP
          port: 5432
    - to:  # Redis Private Endpoint
        - ipBlock:
            cidr: 10.0.2.0/24
      ports:
        - protocol: TCP
          port: 6380
    - to:  # Service Bus
        - ipBlock:
            cidr: 10.0.2.0/24
      ports:
        - protocol: TCP
          port: 5671
```

### 4.4 서비스 디스커버리

| 서비스 | 내부 DNS | 포트 | 프로토콜 |
|--------|---------|------|---------|
| AUTH | auth-service.travel-planner.svc.cluster.local | 8080 | HTTP/2 |
| SCHD | schd-service.travel-planner.svc.cluster.local | 8080 | HTTP/2 |
| PLCE | plce-service.travel-planner.svc.cluster.local | 8080 | HTTP/2 |
| MNTR | mntr-service.travel-planner.svc.cluster.local | 8080 | HTTP/2 |
| BRIF | brif-service.travel-planner.svc.cluster.local | 8080 | HTTP/2 |
| ALTN | altn-service.travel-planner.svc.cluster.local | 8080 | HTTP/2 |
| PAY | pay-service.travel-planner.svc.cluster.local | 8080 | HTTP/2 |
| PostgreSQL | tp-prod-pg.postgres.database.azure.com | 5432 | TCP |
| Redis | tp-prod-redis.redis.cache.windows.net | 6380 | TLS |
| Service Bus | tp-prod-sb.servicebus.windows.net | 5671 | AMQP/TLS |
| Key Vault | tp-prod-kv.vault.azure.net | 443 | HTTPS |

---

## 5. 데이터 아키텍처

### 5.1 Azure PostgreSQL Flexible Server

#### 5.1.1 데이터베이스 구성

| 항목 | 값 |
|------|-----|
| SKU | General Purpose — Standard_D4s_v3 |
| vCPU | 4 |
| RAM | 16GB |
| 스토리지 | 128GB (자동 증가, 최대 32TB) |
| 가용성 모드 | Zone Redundant HA (주: Zone 1, 대기: Zone 2) |
| PostgreSQL 버전 | 16 |
| 자동 장애조치 | 120초 이내 |
| 백업 보존 기간 | 35일 |
| 백업 방식 | 자동 전체+증분, Point-in-Time Recovery |
| 암호화 | Azure 관리형 키 (CMK 옵션) |
| 접근 방식 | Private Endpoint 전용 (Public 액세스 비활성화) |
| 연결 풀링 | PgBouncer 통합 (트랜잭션 모드, 최대 연결 200) |

**스키마 분리 (논리적 멀티테넌트)**

| 스키마 | 소유 서비스 | 주요 테이블 |
|--------|------------|------------|
| auth | AUTH | users, auth_sessions, consent_records |
| schedule | SCHD | trips, schedule_items, monitoring_targets |
| place | PLCE | places, place_cache |
| monitor | MNTR | monitor_status, status_history |
| briefing | BRIF | briefings, push_logs |
| alternative | ALTN | alternative_cards, selection_history |
| payment | PAY | subscriptions, transactions, refunds |

#### 5.1.2 읽기 전용 복제본

```yaml
# Read Replica 설정 (Korea South 리전)
resource "azurerm_postgresql_flexible_server" "read_replica" {
  name                = "tp-prod-pg-replica"
  resource_group_name = "travel-planner-prod-rg"
  location            = "Korea South"  # 재해복구용 교차 리전

  create_mode         = "Replica"
  source_server_id    = azurerm_postgresql_flexible_server.primary.id

  sku_name            = "GP_Standard_D2s_v3"  # 읽기 전용 — 더 작은 SKU 허용
  storage_mb          = 131072

  # 복제 지연 모니터링
  tags = {
    role        = "read-replica"
    source      = "tp-prod-pg"
    replication = "async"
  }
}
```

> 읽기 복제본 활용 대상: PLCE 장소 검색 조회, MNTR 상태 이력 조회, 리포팅/분석 쿼리

### 5.2 Azure Cache for Redis Premium

#### 5.2.1 Redis 구성

| 항목 | 값 |
|------|-----|
| SKU | Premium P2 |
| 메모리 | 6GB |
| 가용성 | Zone Redundant (Zone 1, 2) |
| 클러스터링 | 비활성화 (P2 단일 샤드, Phase 3 클러스터 전환) |
| TLS | 6380 포트 강제 (비TLS 6379 비활성화) |
| 지속성 | RDB (15분 스냅샷) + AOF (everysec) |
| 최대 메모리 정책 | allkeys-lru |
| 접근 방식 | Private Endpoint 전용 |

#### 5.2.2 캐시 전략

```yaml
# 서비스별 캐시 TTL 정책
cache_policies:
  # PLCE: 장소 상세 정보 (Google Places API 응답 캐싱)
  place_detail:
    key_pattern: "place:{place_id}:detail"
    ttl: 3600          # 1시간 (영업시간 변경 반영 주기)
    strategy: cache_aside
    serialization: json

  # MNTR: 장소 상태 배지 (실시간 판정 결과 캐싱)
  place_status:
    key_pattern: "monitor:{place_id}:status"
    ttl: 900           # 15분 (수집 주기와 동기화)
    strategy: write_through
    serialization: json

  # MNTR: 외부 API 수집 Fallback (Circuit Open 시 사용)
  api_fallback:
    key_pattern: "fallback:{place_id}:{api_type}"
    ttl: 1800          # 30분
    strategy: cache_aside
    serialization: json

  # ALTN: 대안 카드 (동일 장소 반복 요청 최적화)
  alternative_cards:
    key_pattern: "altn:{place_id}:{category}:cards"
    ttl: 600           # 10분
    strategy: cache_aside
    serialization: json

  # AUTH: JWT Refresh Token 블랙리스트
  token_blacklist:
    key_pattern: "blacklist:token:{jti}"
    ttl: 1800          # Access Token 만료 시간(30분) x 2 여유
    strategy: write_through
    serialization: string

  # AUTH: 세션 캐시
  user_session:
    key_pattern: "session:{user_id}"
    ttl: 86400         # 24시간
    strategy: cache_aside
    serialization: json
```

### 5.3 백업 정책

#### 5.3.1 자동 백업 구성

```yaml
# PostgreSQL 백업 정책
postgresql_backup:
  automated_backup:
    enabled: true
    backup_retention_days: 35
    geo_redundant_backup: Enabled  # Korea South 복제
    point_in_time_recovery:
      enabled: true
      earliest_restore_point: "now - 35days"

  manual_backup:
    schedule: "0 2 * * 0"   # 매주 일요일 새벽 2시 KST
    retention: 12_weeks

  backup_encryption:
    method: AzureManagedKey   # 기본. CMK 옵션 별도 설정

# Redis 백업 정책
redis_backup:
  rdb_backup:
    enabled: true
    frequency: 15min    # RDB 스냅샷 주기
    storage_account: tpprodstorage
    geo_replication: Enabled

  aof_backup:
    enabled: true
    appendfsync: everysec

# AKS 구성 백업 (Velero)
aks_backup:
  tool: Velero
  schedule: "0 3 * * *"    # 매일 새벽 3시
  retention: 30days
  storage: Azure Blob Storage (GRS)
  scope:
    - namespace: travel-planner
    - persistent_volumes: true
```

---

## 6. 메시징 아키텍처

### 6.1 Azure Service Bus Premium

#### 6.1.1 네임스페이스 구성

```yaml
# Service Bus Premium 네임스페이스 설정
service_bus_namespace:
  name: tp-prod-sb
  tier: Premium
  messaging_units: 1    # 1MU = 1GB RAM, 2 vCPU. 필요 시 2MU 확장
  zone_redundant: true
  private_endpoint: true

  security:
    minimum_tls_version: "1.2"
    public_network_access: Disabled
    local_auth_disabled: true    # Azure AD 인증 강제
    managed_identity_auth: true

  network_rule_set:
    default_action: Deny
    virtual_network_rules:
      - subnet_id: aks-subnet    # AKS 서브넷만 허용

  tags:
    environment: production
    cost_center: platform
```

#### 6.1.2 큐/토픽 설계

```yaml
# 큐 1: 일정 이벤트 (SCHD → MNTR)
queue_schedule_events:
  name: schedule-events
  max_size_gb: 5
  message_ttl: "PT24H"        # 24시간
  lock_duration: "PT5M"       # 5분 처리 잠금
  max_delivery_count: 3       # 3회 실패 시 DLQ
  dead_letter_on_expiration: true
  enable_partitioning: false  # Premium은 기본 파티셔닝
  publishers:
    - SCHD 서비스 (ScheduleItemAdded, ScheduleItemReplaced, ScheduleItemRemoved)
  consumers:
    - MNTR 서비스 (모니터링 대상 등록/해제)

# 큐 2: 상태 변경 이벤트 (MNTR → BRIF)
queue_status_changed:
  name: status-changed
  max_size_gb: 5
  message_ttl: "PT1H"         # 1시간 (브리핑 타임 윈도우 이내)
  lock_duration: "PT3M"
  max_delivery_count: 3
  dead_letter_on_expiration: true
  publishers:
    - MNTR 서비스 (StatusChanged: GREEN→YELLOW, YELLOW→RED, RED→GRAY)
  consumers:
    - BRIF 서비스 (Push 알림 트리거)

# 큐 3: 결제 이벤트 (PAY → AUTH/SCHD)
queue_payment_events:
  name: payment-events
  max_size_gb: 1
  message_ttl: "PT72H"        # 72시간 (감사 목적 보존)
  lock_duration: "PT10M"
  max_delivery_count: 5       # 결제는 재시도 더 허용
  dead_letter_on_expiration: true
  requires_session: true      # 사용자별 순서 보장
  publishers:
    - PAY 서비스 (SubscriptionActivated, SubscriptionExpired, RefundProcessed)
  consumers:
    - AUTH 서비스 (구독 티어 갱신)
    - SCHD 서비스 (모니터링 한도 조정)

# DLQ 공통 처리
dead_letter_queue:
  monitoring: Azure Monitor Alert (DLQ 메시지 1개 이상 시 즉시 알림)
  retention: 7days
  reprocessing: 수동 (운영팀 검토 후 재발행)
```

---

## 7. 보안 아키텍처

### 7.1 다층 보안

#### 7.1.1 4계층 보안 모델

```yaml
security_layers:
  L1_Network:
    name: 네트워크 경계 보안
    components:
      - Azure DDoS Protection Standard
      - Application Gateway WAF v2 (OWASP CRS 3.2)
      - NSG (화이트리스트 기반 인바운드/아웃바운드)
      - Private Cluster (AKS API Server 인터넷 차단)
      - Private Endpoint (DB, Redis, Service Bus, Key Vault)
    controls:
      - DDoS 자동 감지 및 완화
      - SQL Injection, XSS, CSRF 자동 차단
      - 허가되지 않은 IP/포트 전면 차단

  L2_Gateway:
    name: 애플리케이션 경계 보안
    components:
      - Azure API Management (APIM)
      - JWT 서명 검증 (게이트웨이 집중)
      - Rate Limiting (구독 티어별 차등 적용)
      - TLS 1.2+ 강제
    controls:
      - 모든 API 호출 인증 필수
      - Free 티어 브리핑 일 1회 제한 게이트웨이 선제 차단
      - 비정상 요청 패턴 탐지 및 차단

  L3_Identity:
    name: 신원 및 접근 제어
    components:
      - Azure AD (Managed Identity, RBAC)
      - Kubernetes RBAC (ServiceAccount 최소권한)
      - Workload Identity (Pod → Azure 서비스 접근)
      - JWT Access Token (30분), Refresh Token 관리
    controls:
      - 서비스 간 인증 (mTLS 옵션 Phase 2)
      - 비밀 키 코드베이스 완전 배제
      - 최소권한 원칙 (PoLP)

  L4_Data:
    name: 데이터 보안
    components:
      - Azure Key Vault Premium (HSM 보호 키)
      - TDE (PostgreSQL 저장 암호화)
      - Redis TLS (전송 암호화)
      - Azure Storage Service Encryption
    controls:
      - 키 자동 순환 (90일)
      - 키 접근 감사 로그 전량 보존
      - PII 데이터 마스킹 (로그 내 개인정보)
```

### 7.2 Azure AD 통합

#### 7.2.1 Managed Identity

```yaml
# AKS Workload Identity 설정 (각 서비스별 독립 관리 ID)
apiVersion: v1
kind: ServiceAccount
metadata:
  name: auth-service-sa
  namespace: travel-planner
  annotations:
    azure.workload.identity/client-id: "$(AUTH_CLIENT_ID)"
    azure.workload.identity/tenant-id: "$(TENANT_ID)"
---
# Managed Identity → Key Vault 접근 권한
resource "azurerm_key_vault_access_policy" "auth_service" {
  key_vault_id = azurerm_key_vault.prod.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_user_assigned_identity.auth_service.principal_id

  secret_permissions = ["Get", "List"]   # 최소권한
  key_permissions    = []
  certificate_permissions = []
}
```

#### 7.2.2 RBAC 구성

```yaml
# ClusterRole: 서비스 읽기 전용
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: travel-planner-readonly
rules:
  - apiGroups: [""]
    resources: ["pods", "services", "endpoints"]
    verbs: ["get", "list", "watch"]
  - apiGroups: ["apps"]
    resources: ["deployments", "replicasets"]
    verbs: ["get", "list", "watch"]
---
# ClusterRole: 배포 관리자 (ArgoCD 전용)
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: argocd-deployer
rules:
  - apiGroups: ["apps"]
    resources: ["deployments"]
    verbs: ["get", "list", "watch", "create", "update", "patch"]
  - apiGroups: ["autoscaling"]
    resources: ["horizontalpodautoscalers"]
    verbs: ["get", "list", "watch", "update", "patch"]
---
# ServiceAccount: 각 마이크로서비스 (최소권한)
apiVersion: v1
kind: ServiceAccount
metadata:
  name: schd-service-sa
  namespace: travel-planner
  annotations:
    azure.workload.identity/client-id: "$(SCHD_CLIENT_ID)"
automountServiceAccountToken: false  # 불필요한 API 토큰 자동 마운트 방지
```

### 7.3 Private Endpoints

#### 7.3.1 Private Endpoint 구성

```yaml
# PostgreSQL Private Endpoint
resource "azurerm_private_endpoint" "postgresql" {
  name                = "tp-prod-pg-pe"
  resource_group_name = "travel-planner-prod-rg"
  location            = "Korea Central"
  subnet_id           = azurerm_subnet.data_subnet.id

  private_service_connection {
    name                           = "postgresql-connection"
    private_connection_resource_id = azurerm_postgresql_flexible_server.prod.id
    subresource_names              = ["postgresqlServer"]
    is_manual_connection           = false
  }
  private_dns_zone_group {
    name = "postgresql-dns-zone-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.postgresql.id]
  }
}

# Redis Private Endpoint
resource "azurerm_private_endpoint" "redis" {
  name                = "tp-prod-redis-pe"
  resource_group_name = "travel-planner-prod-rg"
  location            = "Korea Central"
  subnet_id           = azurerm_subnet.data_subnet.id

  private_service_connection {
    name                           = "redis-connection"
    private_connection_resource_id = azurerm_redis_cache.prod.id
    subresource_names              = ["redisCache"]
    is_manual_connection           = false
  }
}

# Service Bus Private Endpoint
resource "azurerm_private_endpoint" "servicebus" {
  name                = "tp-prod-sb-pe"
  resource_group_name = "travel-planner-prod-rg"
  location            = "Korea Central"
  subnet_id           = azurerm_subnet.data_subnet.id

  private_service_connection {
    name                           = "servicebus-connection"
    private_connection_resource_id = azurerm_servicebus_namespace.prod.id
    subresource_names              = ["namespace"]
    is_manual_connection           = false
  }
}

# Key Vault Private Endpoint
resource "azurerm_private_endpoint" "keyvault" {
  name                = "tp-prod-kv-pe"
  resource_group_name = "travel-planner-prod-rg"
  location            = "Korea Central"
  subnet_id           = azurerm_subnet.data_subnet.id

  private_service_connection {
    name                           = "keyvault-connection"
    private_connection_resource_id = azurerm_key_vault.prod.id
    subresource_names              = ["vault"]
    is_manual_connection           = false
  }
}
```

### 7.4 Azure Key Vault

#### 7.4.1 Key Vault Premium 구성

```yaml
# Key Vault Premium (HSM 보호)
resource "azurerm_key_vault" "prod" {
  name                = "tp-prod-kv"
  resource_group_name = "travel-planner-prod-rg"
  location            = "Korea Central"
  tenant_id           = data.azurerm_client_config.current.tenant_id
  sku_name            = "premium"    # HSM 지원

  # 삭제 보호
  soft_delete_retention_days = 90
  purge_protection_enabled   = true

  # 공개 접근 완전 차단 (Private Endpoint 전용)
  public_network_access_enabled = false

  # 네트워크 규칙
  network_acls {
    default_action = "Deny"
    bypass         = "AzureServices"
  }
}

# 관리하는 비밀 목록
key_vault_secrets:
  - name: postgresql-connection-string
    rotation_policy: 90days
    auto_rotation: true
  - name: redis-connection-string
    rotation_policy: 90days
    auto_rotation: true
  - name: servicebus-connection-string
    rotation_policy: 90days
    auto_rotation: true
  - name: jwt-signing-key
    rotation_policy: 30days
    auto_rotation: false    # 수동 (JWT 무효화 전략 필요)
  - name: google-oauth-client-secret
    rotation_policy: 365days
    auto_rotation: false
  - name: apple-oauth-key
    rotation_policy: 365days
    auto_rotation: false
  - name: fcm-server-key
    rotation_policy: 180days
    auto_rotation: false
  - name: openweathermap-api-key
    rotation_policy: 365days
    auto_rotation: false
  - name: google-places-api-key
    rotation_policy: 365days
    auto_rotation: false

# 감사 로깅
key_vault_diagnostics:
  log_analytics_workspace: tp-prod-law
  logs:
    - AuditEvent      # 모든 키/비밀 접근 기록
    - AzurePolicyEvaluationDetails
  retention_days: 365   # 1년 보존 (규정 준수)
```

---

## 8. 모니터링 및 관측 가능성

### 8.1 Azure Monitor 통합

#### 8.1.1 관측 스택 구성

```yaml
# Log Analytics Workspace
resource "azurerm_log_analytics_workspace" "prod" {
  name                = "tp-prod-law"
  resource_group_name = "travel-planner-prod-rg"
  location            = "Korea Central"
  sku                 = "PerGB2018"
  retention_in_days   = 90    # 3개월 온라인 보존
  daily_quota_gb      = 10    # 비용 제한 (초과 시 알림)
}

# Application Insights
resource "azurerm_application_insights" "prod" {
  name                = "tp-prod-appinsights"
  resource_group_name = "travel-planner-prod-rg"
  location            = "Korea Central"
  workspace_id        = azurerm_log_analytics_workspace.prod.id
  application_type    = "java"
  sampling_percentage = 10    # 10% 샘플링 (비용 최적화)
}

# Container Insights (AKS)
resource "azurerm_monitor_diagnostic_setting" "aks" {
  name               = "aks-container-insights"
  target_resource_id = azurerm_kubernetes_cluster.prod.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.prod.id

  enabled_log {
    category = "kube-apiserver"
  }
  enabled_log {
    category = "kube-controller-manager"
  }
  enabled_log {
    category = "kube-scheduler"
  }
  metric {
    category = "AllMetrics"
    enabled  = true
  }
}
```

#### 8.1.2 메트릭 및 알림

```yaml
# Critical 알림 규칙 (즉시 대응 필요)
alert_rules_critical:
  - name: ServiceUnavailable
    description: 서비스 Pod 0개 → 서비스 중단
    condition: "avg(kube_deployment_status_replicas_available) by (deployment) == 0"
    window: 2min
    severity: Sev1
    action: PagerDuty + Slack #critical

  - name: HighErrorRate
    description: HTTP 5xx 비율 5% 초과
    condition: "rate(http_requests_total{status=~'5..'}[5m]) / rate(http_requests_total[5m]) > 0.05"
    window: 5min
    severity: Sev1
    action: PagerDuty + Slack #critical

  - name: DatabaseConnectionPoolExhausted
    description: DB 연결 풀 90% 소진
    condition: "pg_stat_activity_count / pg_settings_max_connections > 0.9"
    window: 3min
    severity: Sev1
    action: PagerDuty + Slack #critical

  - name: DLQMessageAccumulation
    description: DLQ 메시지 1개 이상 발생
    condition: "azure_servicebus_deadletteredmessages > 0"
    window: 5min
    severity: Sev2
    action: Slack #ops

# Warning 알림 규칙 (모니터링 필요)
alert_rules_warning:
  - name: HighCPUUtilization
    description: Pod CPU 80% 초과 (HPA 트리거 임박)
    condition: "avg(container_cpu_usage_seconds_total) by (pod) > 0.8"
    window: 10min
    severity: Sev3
    action: Slack #monitoring

  - name: RedisMemoryHigh
    description: Redis 메모리 사용 75% 초과
    condition: "redis_memory_used_bytes / redis_memory_max_bytes > 0.75"
    window: 15min
    severity: Sev3
    action: Slack #monitoring

  - name: ExternalAPICircuitOpen
    description: 외부 API Circuit Breaker OPEN 상태
    condition: "circuit_breaker_state == 'OPEN'"
    window: 1min
    severity: Sev2
    action: Slack #ops

  - name: P95ResponseTimeDegraded
    description: P95 응답시간 2초 초과
    condition: "histogram_quantile(0.95, http_request_duration_seconds_bucket) > 2"
    window: 5min
    severity: Sev3
    action: Slack #monitoring
```

### 8.2 로깅 및 APM

#### 8.2.1 중앙집중식 로깅

```yaml
# Log Analytics 핵심 쿼리 (KQL)
log_queries:
  # 서비스별 오류율 실시간 집계
  error_rate_by_service: |
    ContainerLog
    | where TimeGenerated > ago(5m)
    | where LogEntry contains "ERROR"
    | summarize ErrorCount=count() by ServiceName=tostring(split(ContainerName, "-")[0]), bin(TimeGenerated, 1m)
    | order by TimeGenerated desc

  # 외부 API 응답시간 추적
  external_api_latency: |
    AppDependencies
    | where TimeGenerated > ago(1h)
    | where Target in ("maps.googleapis.com", "api.openweathermap.org")
    | summarize
        P50=percentile(DurationMs, 50),
        P95=percentile(DurationMs, 95),
        P99=percentile(DurationMs, 99),
        ErrorRate=countif(Success == false) * 100.0 / count()
      by Target, bin(TimeGenerated, 5m)

  # 느린 쿼리 탐지 (PostgreSQL)
  slow_queries: |
    AzureDiagnostics
    | where Category == "PostgreSQLFlexQueryStoreRuntime"
    | where mean_time_ms > 1000
    | project TimeGenerated, query_id_s, calls_s, mean_time_ms, max_time_ms
    | order by mean_time_ms desc

  # 보안 이벤트 탐지 (WAF 차단)
  waf_blocked_requests: |
    AzureDiagnostics
    | where ResourceType == "APPLICATIONGATEWAYS"
    | where OperationName == "ApplicationGatewayFirewall"
    | where action_s == "Blocked"
    | summarize BlockedCount=count() by clientIp_s, ruleId_s, bin(TimeGenerated, 1h)
    | order by BlockedCount desc
```

#### 8.2.2 APM 구성

```yaml
# Application Insights Java Agent 설정 (각 서비스 공통)
# applicationinsights.json
{
  "connectionString": "${APPLICATIONINSIGHTS_CONNECTION_STRING}",
  "role": {
    "name": "${SERVICE_NAME}",
    "instance": "${POD_NAME}"
  },
  "sampling": {
    "percentage": 10
  },
  "instrumentation": {
    "logging": {
      "level": "WARN"
    },
    "micrometer": {
      "enabled": true
    }
  },
  "customDimensions": {
    "environment": "production",
    "version": "${APP_VERSION}"
  }
}

# Custom Metrics (Spring Boot Actuator + Micrometer)
custom_metrics:
  # MNTR 서비스
  - name: monitoring.collection.duration
    description: 외부 API 수집 소요 시간
    unit: milliseconds
    type: Timer

  - name: monitoring.status.change.count
    description: 상태 변경 발생 횟수
    unit: count
    type: Counter
    tags: [from_status, to_status]

  # BRIF 서비스
  - name: briefing.push.sent.count
    description: Push 알림 발송 수
    unit: count
    type: Counter
    tags: [tier, result]

  # ALTN 서비스
  - name: alternative.card.generation.duration
    description: 대안 카드 생성 소요 시간
    unit: milliseconds
    type: Timer
    tags: [category]

  # PAY 서비스
  - name: payment.transaction.count
    description: 결제 트랜잭션 수
    unit: count
    type: Counter
    tags: [plan, result]
```

---

## 9. 배포 관련 컴포넌트

| 컴포넌트 | 선택 | 구성 | 역할 |
|---------|------|------|------|
| CI/CD | GitHub Actions | Self-hosted Runner on AKS | 빌드/테스트/이미지 빌드 |
| 이미지 레지스트리 | Azure Container Registry Premium | Geo-replicated (Korea Central + Korea South) | 컨테이너 이미지 저장/배포 |
| GitOps 배포 | ArgoCD HA | System Node Pool, 3 replicas | K8s 매니페스트 동기화 |
| 보안 스캔 | Trivy (이미지) + OWASP ZAP (DAST) | GitHub Actions 통합 | 취약점 자동 탐지 |
| 배포 전략 | Blue/Green (PAY) / Rolling Update (나머지) | ArgoCD Rollouts | 무중단 배포 |
| 롤백 | 자동 (헬스체크 실패) + 수동 (ArgoCD UI) | ArgoCD 이전 릴리즈로 1클릭 롤백 | 장애 시 신속 복구 |
| 시크릿 관리 | External Secrets Operator | Key Vault → K8s Secret 동기화 | 코드베이스 시크릿 배제 |
| 정책 관리 | Azure Policy + OPA Gatekeeper | 보안 정책 코드화 | 컴플라이언스 자동 검증 |

```yaml
# GitHub Actions CI/CD 파이프라인 (간소화)
# .github/workflows/deploy-prod.yml
name: Deploy to Production
on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  security-scan:
    runs-on: self-hosted
    steps:
      - uses: actions/checkout@v4
      - name: Trivy 이미지 스캔
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: fs
          severity: CRITICAL,HIGH
          exit-code: 1   # Critical/High 취약점 시 빌드 중단

  build-and-push:
    needs: security-scan
    runs-on: self-hosted
    steps:
      - name: ACR 로그인 및 이미지 빌드/푸시
        run: |
          az acr build \
            --registry travelplanneracr \
            --image $SERVICE_NAME:${{ github.sha }} \
            --build-arg BUILD_VERSION=${{ github.sha }} .

  deploy:
    needs: build-and-push
    runs-on: self-hosted
    steps:
      - name: ArgoCD 동기화 트리거
        run: |
          argocd app sync travel-planner-$SERVICE_NAME \
            --revision ${{ github.sha }} \
            --prune \
            --timeout 300
```

---

## 10. 재해복구 및 고가용성

### 10.1 복구 목표 및 자동 장애조치

#### 10.1.1 백업/복구 목표

```yaml
# RTO/RPO 목표 및 복구 절차
disaster_recovery_objectives:
  rto: "4h"    # Recovery Time Objective: 4시간 이내 서비스 복구
  rpo: "1h"    # Recovery Point Objective: 최대 1시간 데이터 손실 허용

  tier_classification:
    tier1_critical:       # RTO 30min, RPO 0
      services: [AUTH, SCHD, PLCE]
      strategy: Multi-Zone Active-Active
      failover: Automatic

    tier2_important:      # RTO 2h, RPO 15min
      services: [MNTR, BRIF, ALTN]
      strategy: Multi-Zone Active-Active
      failover: Automatic

    tier3_standard:       # RTO 4h, RPO 1h
      services: [PAY]
      strategy: Multi-Zone Active-Passive
      failover: Semi-automatic

  cross_region_dr:
    primary: Korea Central
    secondary: Korea South
    scope: DB Geo-redundant Backup + ACR Geo-replication
    activation: 수동 (RTO 4h 목표 달성 불가 시)
```

#### 10.1.2 자동 장애조치

```yaml
# DB 자동 장애조치 (PostgreSQL Zone Redundant HA)
postgresql_auto_failover:
  trigger: Primary 서버 응답 없음 (30초 감지)
  mechanism: Azure 관리형 자동 프로모션
  failover_time: "< 120초"
  data_loss: "0 (동기 복제)"
  connection_string_update: Azure DNS 자동 업데이트
  application_impact: 연결 재시도 필요 (Spring Boot 자동 재연결)

# Redis 자동 장애조치 (Zone Redundant Premium)
redis_auto_failover:
  trigger: Primary 노드 응답 없음 (15초 감지)
  mechanism: Sentinel 기반 자동 프로모션
  failover_time: "< 30초"
  data_loss: "RDB 마지막 스냅샷 이후 변경 (최대 15분)"
  application_impact: 연결 재시도 필요

# Application 자동 장애조치 (AKS Pod)
app_auto_failover:
  pod_failure:
    detection: kubelet liveness probe (초기 30초, 이후 10초 주기, 3회 실패)
    action: Pod 자동 재시작 (RestartPolicy: Always)
    recovery_time: "< 30초"

  node_failure:
    detection: Node NotReady (5분 임계값)
    action: Cluster Autoscaler 신규 노드 프로비저닝 + Pod 재스케줄링
    recovery_time: "< 10분"

  zone_failure:
    detection: 자동 (Azure 인프라)
    action: Anti-Affinity 규칙으로 나머지 Zone에 Pod 재배치
    recovery_time: "< 5분 (기존 여유 노드 활용 시)"
```

### 10.2 운영 절차

#### 10.2.1 표준 운영 절차

```yaml
# 인시던트 대응 절차
incident_response:
  detection:
    - Azure Monitor 자동 알림 수신
    - 사용자 신고 (고객센터 채널)
    - 내부 모니터링 대시보드 확인
  classification:
    Sev1: 서비스 전체 중단 → 즉시 대응 (15분 이내 초동)
    Sev2: 주요 기능 장애 → 30분 이내 초동
    Sev3: 성능 저하 → 2시간 이내 대응
    Sev4: 경미한 문제 → 다음 영업일 대응
  escalation:
    - L1: 온콜 엔지니어 (1차 대응)
    - L2: 서비스 오너 (기능 판단)
    - L3: 아키텍트 (아키텍처 레벨 결정)
  communication:
    internal: Slack #incident 채널
    external: 상태 페이지 (status.travel-planner.io) 업데이트

# 유지보수 윈도우
maintenance_window:
  regular: "매주 화요일 02:00~04:00 KST"
  emergency: "즉시 (Sev1 한정, 서비스 중단 최소화)"
  notification: "정기 유지보수 72시간 전 사전 공지"
  rollback_ready: "모든 배포 전 롤백 플랜 수립 필수"

# 변경 관리
change_management:
  production_deploy:
    approval: PO 승인 + 아키 리뷰 (중요 변경)
    process: PR → Code Review → 스테이징 검증 → 프로덕션 배포
    timing: 유지보수 윈도우 내 권장 (긴급 제외)
  database_schema_change:
    tool: Flyway (마이그레이션 버전 관리)
    approval: 아키텍트 필수 승인
    rollback: 모든 마이그레이션 DOWN 스크립트 작성 필수
```

---

## 11. 비용 최적화

### 11.1 비용 현황

#### 11.1.1 월간 예상 비용

| 항목 | 서비스 | 구성 | 월간 예상 비용 |
|------|--------|------|:-------------:|
| 컴퓨팅 | AKS (System D2s_v3 x3 + App D4s_v3 x3~10) | 평균 6노드 | ~$800 |
| 데이터베이스 | Azure PostgreSQL Flexible GP_D4s_v3 HA | Zone Redundant | ~$500 |
| 캐시 | Azure Cache for Redis Premium P2 | Zone Redundant | ~$400 |
| 메시징 | Azure Service Bus Premium 1MU | 3개 큐 | ~$300 |
| 네트워크 | Application Gateway WAF v2 + Azure Front Door | Zone Redundant | ~$200 |
| 모니터링 | Log Analytics + App Insights + Container Insights | 10GB/일 | ~$150 |
| 보안 | Key Vault Premium + DDoS Standard + ACR Premium | 기본 구성 | ~$100 |
| **합계** | | | **~$2,450~3,000** |

#### 11.1.2 비용 최적화 전략

```yaml
# Reserved Instance 전략
reserved_instances:
  postgresql:
    type: 1년 예약
    savings: 약 40% (On-Demand 대비)
    commitment: "tp-prod-pg GP_Standard_D4s_v3"

  redis:
    type: 1년 예약
    savings: 약 33%
    commitment: "tp-prod-redis Premium P2"

  aks_system_pool:
    type: 1년 예약 (고정 3노드)
    savings: 약 40%
    commitment: "Standard_D2s_v3 x 3"

# 자동 스케일링 비용 최적화
auto_scaling_cost:
  aks_app_pool:
    min_nodes: 3     # 평시 최소
    max_nodes: 10    # 피크 최대
    scale_down_delay: 10min    # 조급한 스케일 다운 방지
    estimated_saving: "피크 외 시간 30~50% 절감"

  spot_instance_option:
    pool_name: spot-pool
    vm_sku: Standard_D4s_v3
    eviction_policy: Delete
    max_price: -1    # 현재 스팟 가격 수용
    use_case: "MNTR 배치 처리, AI Pipeline (Phase 2)"
    estimated_saving: "On-Demand 대비 60~80% 절감"
```

### 11.2 Auto Scaling 최적화

#### 11.2.1 예측 스케일링 구성

```yaml
# KEDA 기반 예측 스케일링 (MNTR 서비스)
# 15분 주기 수집 작업 피크 예측 스케일업
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: mntr-predictive-scaling
  namespace: travel-planner
spec:
  scaleTargetRef:
    name: mntr-service
  minReplicaCount: 2
  maxReplicaCount: 6
  advanced:
    horizontalPodAutoscalerConfig:
      behavior:
        scaleUp:
          stabilizationWindowSeconds: 0    # 즉시 스케일업 (수집 피크 대비)
          policies:
            - type: Percent
              value: 100
              periodSeconds: 60
        scaleDown:
          stabilizationWindowSeconds: 600  # 10분 대기 후 스케일다운
  triggers:
    - type: cron
      metadata:
        timezone: "Asia/Seoul"
        start: "*/15 * * * *"    # 15분마다 수집 시작 직전
        end: "*/15 * * * *"      # 수집 완료 후
        desiredReplicas: "4"     # 수집 중 4 pods

# 비용 인식 HPA (야간 최소화)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: plce-hpa-cost-aware
  namespace: travel-planner
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: plce-service
  minReplicas: 2    # 야간(00:00~06:00) 최소값
  maxReplicas: 8
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
```

---

## 12. 운영 가이드

### 12.1 정기 점검

#### 12.1.1 점검 체크리스트

```yaml
# 일일 점검 (매일 09:00 KST)
daily_checklist:
  - check: 전체 서비스 Pod 정상 (Running, Ready)
    tool: "kubectl get pods -n travel-planner"
    threshold: 모든 Pod Running
  - check: 어제 오류율 리뷰
    tool: Log Analytics 쿼리 (error_rate_by_service)
    threshold: 5xx 비율 < 1%
  - check: DLQ 메시지 잔류 여부
    tool: Azure Portal → Service Bus → DLQ
    threshold: 0개
  - check: 자동 백업 성공 여부
    tool: Azure Portal → PostgreSQL → 백업 상태
    threshold: 최근 백업 성공
  - check: 보안 알림 확인
    tool: Microsoft Defender for Cloud
    threshold: Critical 0개

# 주간 점검 (매주 월요일 10:00 KST)
weekly_checklist:
  - check: HPA 스케일 이벤트 리뷰
    tool: "kubectl describe hpa -n travel-planner"
    action: 잦은 스케일링 시 임계값 조정 검토
  - check: P95 응답시간 트렌드
    tool: App Insights → 성능 대시보드
    threshold: 모든 서비스 < 2초
  - check: 비용 현황 리뷰
    tool: Azure Cost Management
    threshold: 월 예산 대비 ±10% 이내
  - check: 취약점 스캔 결과 리뷰
    tool: ACR → 이미지 취약점 스캔
    action: Critical/High 즉시 패치 계획 수립
  - check: 인증서 만료 확인
    tool: Key Vault → 인증서 만료일
    threshold: 만료 30일 이전 갱신

# 월간 점검 (매월 첫째 주 수요일)
monthly_checklist:
  - check: DR 복구 훈련
    action: PostgreSQL PITR 테스트 복구 실시
    duration: 4시간
  - check: 보안 감사 로그 리뷰
    tool: Key Vault 감사 로그, NSG 흐름 로그
    duration: 2시간
  - check: 용량 계획 리뷰
    action: 사용량 트렌드 분석 → 다음 달 스케일링 계획
    tool: Azure Monitor → 용량 리포트
  - check: 비용 최적화 리뷰
    action: Reserved Instance 추가/변경 검토
    tool: Azure Advisor
  - check: 의존성 업데이트
    action: K8s 버전, Java 버전, 라이브러리 보안 패치
    tool: Dependabot + Azure AKS 업그레이드 채널
```

### 12.2 장애 대응

#### 12.2.1 심각도별 대응 절차

```yaml
# Sev1: 서비스 전체 중단
sev1_procedure:
  trigger: 서비스 Pod 0개 OR 5xx 비율 > 50%
  response_time: 15분 이내 초동
  steps:
    - 1. PagerDuty 알림 수신 → 온콜 엔지니어 즉시 확인
    - 2. Slack #incident 채널 개설 및 관계자 소집
    - 3. "kubectl get pods -n travel-planner" 상태 확인
    - 4. 최근 배포 여부 확인 → 문제 시 ArgoCD 즉시 롤백
    - 5. DB/Redis/Service Bus 상태 확인
    - 6. Application Gateway 헬스 확인
    - 7. 30분 내 미해결 시 아키텍트 에스컬레이션
    - 8. 1시간 내 미해결 시 DR 발동 검토
  communication:
    - 15분: 내부 Slack 인시던트 개설
    - 30분: 상태 페이지 "서비스 장애" 게시
    - 1시간: 고객 공지 (이메일/앱 내 배너)

# Sev2: 주요 기능 장애
sev2_procedure:
  trigger: 특정 서비스 기능 불가 OR 5xx 비율 5~50%
  response_time: 30분 이내 초동
  steps:
    - 1. 영향 서비스 특정 (App Insights 의존성 맵 확인)
    - 2. 외부 API Circuit Breaker 상태 확인
    - 3. 해당 서비스 Pod 로그 집중 분석
    - 4. Fallback 동작 확인 (Redis 캐시 Fallback 활성화 여부)
    - 5. 필요 시 해당 서비스 Pod 재시작 또는 롤백

# Sev3: 성능 저하
sev3_procedure:
  trigger: P95 응답시간 > 2초 OR CPU 지속 80% 초과
  response_time: 2시간 이내 대응
  steps:
    - 1. 병목 서비스 특정 (App Insights → 성능 프로파일러)
    - 2. HPA 스케일 상태 확인 → 수동 스케일업 검토
    - 3. DB 슬로우 쿼리 확인 (Log Analytics 슬로우 쿼리 쿼리)
    - 4. Redis 캐시 히트율 확인

# Sev4: 경미한 문제
sev4_procedure:
  trigger: Warning 알림 OR 기능 일부 제한
  response_time: 다음 영업일 대응
  steps:
    - 1. 이슈 티켓 등록 (GitHub Issues)
    - 2. 다음 스프린트 계획에 포함
```

#### 12.2.2 자동 복구 구성

```yaml
# Pod 자동 재시작 (Liveness/Readiness Probe)
pod_auto_recovery:
  liveness_probe:
    http_get_path: /actuator/health/liveness
    port: 8080
    initial_delay_seconds: 30
    period_seconds: 10
    failure_threshold: 3
    success_threshold: 1
    timeout_seconds: 5

  readiness_probe:
    http_get_path: /actuator/health/readiness
    port: 8080
    initial_delay_seconds: 15
    period_seconds: 5
    failure_threshold: 3
    success_threshold: 1
    timeout_seconds: 3

  restart_policy: Always
  termination_grace_period_seconds: 30

# 노드 자동 교체 (Cluster Autoscaler)
node_auto_recovery:
  unhealthy_node_detection: 5min
  action: 새 노드 프로비저닝 + 장애 노드 Pod 재스케줄
  max_node_provision_time: 10min
  drain_timeout: 5min

# 트래픽 자동 라우팅 (Application Gateway Health Probe)
traffic_auto_routing:
  health_probe:
    path: /actuator/health
    interval: 30s
    timeout: 30s
    unhealthy_threshold: 3
  action: 비정상 백엔드 자동 제외 → 정상 Pod로 라우팅
  reintegration: 헬스체크 연속 2회 성공 시 자동 복귀
```

---

## 13. 확장 계획

### 13.1 단계별 확장 로드맵

#### 13.1.1 Phase별 목표

```yaml
phases:
  phase1_mvp:
    period: "2026 Q2~Q3"
    goal: "MVP 출시 및 초기 사용자 검증"
    target_users: "1,000~10,000명"
    scope:
      - 7개 마이크로서비스 안정화
      - Korea Central 단일 리전
      - 인메모리 이벤트 버스 → Azure Service Bus 전환
      - 기본 모니터링/알림 체계 구축
    deliverables:
      - 운영 AKS 클러스터 (3노드 고정)
      - PostgreSQL HA, Redis Premium
      - GitHub Actions CI/CD
      - 기본 관측 스택 (Log Analytics + App Insights)

  phase2_growth:
    period: "2026 Q4~2027 Q1"
    goal: "성장 대응 및 AI 기능 도입"
    target_users: "10,000~100,000명"
    scope:
      - AI Pipeline 서비스 추가 (Python/FastAPI)
      - GPU 노드 풀 (NC6s_v3) 추가
      - KEDA 기반 이벤트 드리븐 스케일링
      - Service Bus 메시지 처리 확장
      - 읽기 복제본 활성화
    deliverables:
      - AI Pipeline 서비스 배포
      - GPU 노드 풀 구성
      - 예측 스케일링 도입
      - 고급 APM (분산 트레이싱 전면 적용)

  phase3_global:
    period: "2027 Q2+"
    goal: "글로벌 확장 및 멀티 리전"
    target_users: "100,000명+"
    scope:
      - 멀티 리전 배포 (Japan East, Southeast Asia)
      - Azure Front Door 글로벌 로드밸런싱
      - Redis 지역 복제
      - PostgreSQL 멀티 리전 읽기 복제본
      - 서비스 메시 (Istio/Linkerd) 도입
    deliverables:
      - 멀티 리전 AKS 클러스터
      - Azure Traffic Manager / Front Door 글로벌 라우팅
      - 글로벌 CDN (정적 자산)
      - GDPR 대응 데이터 레지던시 분리
```

### 13.2 수평 확장 전략

#### 13.2.1 티어별 확장 전략

```yaml
horizontal_scaling:
  application_tier:
    current: "D4s_v3 x 3~10 노드 (HPA)"
    phase2_scale: "D4s_v3 x 5~20 노드 (트래픽 10배 증가 대비)"
    phase3_scale: "멀티 리전 AKS, 리전당 D4s_v3 x 3~10"
    strategy: "HPA + KEDA 이벤트 드리븐 스케일링"

  database_tier:
    current: "GP_Standard_D4s_v3 단일 Primary + Zone Redundant Standby"
    phase2_scale: "GP_Standard_D8s_v3 업그레이드 + 읽기 복제본 활성화"
    phase3_scale: "멀티 리전 읽기 복제본 (Japan, Southeast Asia)"
    strategy: "읽기/쓰기 분리, PgBouncer 연결 풀링 확장"

  cache_tier:
    current: "Redis Premium P2 6GB (단일 인스턴스)"
    phase2_scale: "Redis Premium P3 13GB 또는 클러스터 모드 (3 샤드)"
    phase3_scale: "Active Geo Replication (멀티 리전 Redis)"
    strategy: "메모리 사용률 75% 도달 시 업그레이드, 클러스터 전환 검토"

  messaging_tier:
    current: "Service Bus Premium 1MU"
    phase2_scale: "Service Bus Premium 2MU (메시지 처리량 2배)"
    phase3_scale: "Service Bus Premium 4MU + 멀티 리전 네임스페이스"
    strategy: "MU당 1,000 connections, 1GB RAM 기준 용량 계획"
```

---

## 14. 운영환경 특성 요약

| 항목 | 값 | 비고 |
|------|-----|------|
| **클라우드** | Microsoft Azure | Korea Central 주 리전 |
| **컨테이너 오케스트레이션** | AKS 1.29 Standard (Private Cluster) | Multi-Zone |
| **백엔드 런타임** | Java 21 + Spring Boot 3.4.x | 7개 마이크로서비스 |
| **프론트엔드** | Flutter 3.x (모바일) | iOS/Android |
| **데이터베이스** | Azure PostgreSQL Flexible GP_D4s_v3 | Zone Redundant HA |
| **캐시** | Azure Cache for Redis Premium P2 | Zone Redundant |
| **메시징** | Azure Service Bus Premium 1MU | 3개 큐 |
| **인그레스** | Application Gateway WAF v2 + AGIC | Zone Redundant |
| **CDN/글로벌** | Azure Front Door | 글로벌 POP |
| **이미지 레지스트리** | ACR Premium | Geo-replicated |
| **GitOps** | ArgoCD HA | Blue/Green 배포 |
| **시크릿 관리** | Azure Key Vault Premium (HSM) | External Secrets Operator |
| **모니터링** | Azure Monitor + App Insights + Container Insights | |
| **보안** | WAF + Private Endpoint + Managed Identity + RBAC | 다층 방어 |
| **목표 가용성** | 99.9% | 월 43분 허용 |
| **RTO / RPO** | 4시간 / 1시간 | Tier1 서비스: 30분/0 |
| **대상 사용자** | 1만~10만 명 | Phase 2에서 10만+ 대응 |
| **월간 예상 비용** | $2,450~3,000 | Reserved Instance 적용 시 절감 가능 |

---

*본 문서는 홍길동/아키에 의해 작성되었으며, 주요 변경 시 아키텍처 리뷰를 거쳐 갱신한다.*
