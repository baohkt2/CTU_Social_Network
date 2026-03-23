# 🎯 CTU-Connect — Kế Hoạch Cải Tiến Chi Tiết

> **Tài liệu phân tích chuyên sâu** dựa trên review toàn bộ codebase (300+ Java files, 2 Next.js frontends, Python AI Engine)
> **Ngày tạo:** 22/03/2026 | **Phiên bản:** 2.0 | **Ngôn ngữ:** Tiếng Việt

---

## 📋 Mục Lục

1. [Tổng Quan Kiến Trúc Hiện Tại](#1-tổng-quan-kiến-trúc-hiện-tại)
2. [Phân Tích Chi Tiết Từng Thành Phần](#2-phân-tích-chi-tiết-từng-thành-phần)
3. [Ma Trận Đánh Giá Chất Lượng](#3-ma-trận-đánh-giá-chất-lượng)
4. [Các Vấn Đề Nghiêm Trọng Cần Giải Quyết Ngay](#4-các-vấn-đề-nghiêm-trọng-cần-giải-quyết-ngay)
5. [Kế Hoạch Cải Tiến 10 Tuần](#5-kế-hoạch-cải-tiến-10-tuần)
6. [Chi Tiết Triển Khai Từng Phase](#6-chi-tiết-triển-khai-từng-phase)
7. [Metrics & KPI Theo Dõi](#7-metrics--kpi-theo-dõi)
8. [Giá Trị CV & Portfolio](#8-giá-trị-cv--portfolio)

---

## 1. Tổng Quan Kiến Trúc Hiện Tại

### 1.1 Bản Đồ Hệ Thống

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                │
│  ┌─────────────────────────────┐    ┌──────────────────────────────┐    │
│  │  client-frontend (Next.js)  │    │  admin-frontend (Next.js)    │    │
│  │  Port: 3000                 │    │  Port: 3001                  │    │
│  │  React 19 + TailwindCSS 4   │    │  React 19 + TailwindCSS 4   │    │
│  │  TanStack Query 5, STOMP.js │    │                              │    │
│  └──────────────┬──────────────┘    └──────────────┬───────────────┘    │
│                 └────────────────┬─────────────────┘                     │
└─────────────────────────────────┼────────────────────────────────────────┘
                                  │
                                  ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                     API GATEWAY (Spring Cloud Gateway)                    │
│                     Port: 8090 | CorsConfig (DISABLED)                   │
│                     JwtAuthenticationFilter + LoggingFilter               │
│                     RouteConfig → Service Discovery via Eureka            │
└───┬──────────┬──────────┬──────────┬──────────┬──────────┬───────────────┘
    │          │          │          │          │          │
    ▼          ▼          ▼          ▼          ▼          ▼
┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐┌──────────────────┐
│  Auth  ││  User  ││  Post  ││  Chat  ││ Media  ││   Recommend      │
│Service ││Service ││Service ││Service ││Service ││   Service        │
│:8080   ││:8081   ││:8085   ││:8086   ││:8084   ││:8095 (Java)     │
│58 files││77 files││55 files││39 files││20 files││:8000 (Python)    │
│Postgres││ Neo4j  ││MongoDB ││MongoDB ││Postgres││Postgres+Redis   │
│  JWT   ││ Graph  ││  Kafka ││WebSocket│Cloudnry││PhoBERT AI       │
└────────┘└────────┘└────────┘└────────┘└────────┘└──────────────────┘
    │          │          │          │          │          │
    ▼          ▼          ▼          ▼          ▼          ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE LAYER                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ Eureka   │ │  Kafka   │ │  Redis   │ │PostgreSQL│ │  Neo4j   │      │
│  │ :8761    │ │  :9092   │ │  :6379   │ │ 3 inst.  │ │  :7687   │      │
│  │Discovery │ │  KRaft   │ │  Global  │ │5433/5434 │ │ Graph DB │      │
│  └──────────┘ └──────────┘ └──────────┘ │   /5435  │ └──────────┘      │
│                                          └──────────┘                    │
│  ┌──────────┐ ┌──────────┐                                               │
│  │ MongoDB  │ │ Redis    │                                               │
│  │ 2 inst.  │ │ Recommend│                                               │
│  │27018/019 │ │  :6380   │                                               │
│  └──────────┘ └──────────┘                                               │
└──────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Tech Stack Inventory

| Layer | Công nghệ | Phiên bản | Ghi chú |
|-------|-----------|-----------|---------|
| **Backend Framework** | Spring Boot | 3.3.4 | Modern, up-to-date |
| **Cloud** | Spring Cloud | 2023.0.4 | Service mesh |
| **API Gateway** | Spring Cloud Gateway | - | Reactive gateway |
| **Service Discovery** | Netflix Eureka | - | Service registry |
| **Event Streaming** | Apache Kafka | 3.7.0 | KRaft mode (no ZooKeeper) |
| **Frontend** | Next.js | 15.3.5 | App Router + Turbopack |
| **UI Framework** | React | 19 | Latest version |
| **Styling** | TailwindCSS | 4.x | Latest |
| **State Management** | TanStack Query | 5.83 | Server state |
| **Form** | React Hook Form + Yup | 7.60 / 1.6 | Validation |
| **WebSocket** | STOMP.js | 7.x | Real-time chat |
| **AI/ML** | PhoBERT (PyTorch) | - | Vietnamese NLP |
| **AI Server** | FastAPI/Uvicorn | - | Python inference |
| **Auth** | JWT (jjwt) | 0.12.5 | Token-based |
| **CAPTCHA** | reCAPTCHA v3 | - | Bot protection |
| **DB: Relational** | PostgreSQL | 15 | 3 instances |
| **DB: Document** | MongoDB | 7.0 | 2 instances |
| **DB: Graph** | Neo4j | 5.13 | User relationships |
| **Cache** | Redis | 7 | 2 instances |
| **Media Storage** | Cloudinary | - | Cloud CDN |
| **Build** | Maven | - | Java build |
| **Container** | Docker Compose | - | Orchestration |
| **API Docs** | springdoc-openapi | 2.3.0 | Swagger UI |
| **Java** | OpenJDK | 17 | LTS |
| **Python** | CPython | 3.10 | ML runtime |

---

## 2. Phân Tích Chi Tiết Từng Thành Phần

### 2.1 Auth Service (58 Java files)

| Thành phần | Hiện trạng | Đánh giá |
|------------|-----------|----------|
| **JWT Implementation** | `JwtService.java`, `JwtTokenProvider.java`, `JwtAuthenticationFilter.java` | ⚠️ Có 2 class JWT riêng biệt (JwtService + JwtTokenProvider) — **code trùng lặp** |
| **Security Config** | `SecurityConfig.java`, `AdminSecurityConfig.java` | ✅ Tách biệt user/admin security |
| **Refresh Token** | `RefreshTokenEntity.java`, `RefreshTokenRepository.java` | ✅ Đã implement |
| **Email Verification** | `EmailVerificationEntity.java`, Thymeleaf templates | ✅ Hoàn chỉnh |
| **reCAPTCHA** | Via WebFlux (`spring-boot-starter-webflux`) | ✅ Có |
| **Password Reset** | `PasswordResetEvent.java`, `PasswordResetRequest.java` | ✅ Event-driven |
| **Kafka Events** | `UserVerificationEvent.java` + 3 Kafka config files | ✅ Event streaming |
| **Redis** | `RedisConfig.java` — token caching | ✅ Session management |
| **OpenAPI** | `OpenApiConfig.java` + springdoc dependency | ✅ Swagger UI available |
| **Exception Handling** | `GlobalExceptionHandler.java` | ✅ Centralized |
| **Tests** | **0 test files** | 🔴 **CRITICAL — Không có test** |
| **Input Validation** | `spring-boot-starter-validation` in pom.xml | ⚠️ Dependency có nhưng cần verify usage |

**Vấn đề đặc biệt:**
- `JwtService.java` và `JwtTokenProvider.java` cùng tồn tại — cần hợp nhất
- `Init.java` — khởi tạo data ban đầu, cần document rõ mục đích

---

### 2.2 User Service (77 Java files — Service lớn nhất)

| Thành phần | Hiện trạng | Đánh giá |
|------------|-----------|----------|
| **Database** | Neo4j Graph Database | ✅ Phù hợp cho social graph |
| **Controllers** | 9 controllers (User, Faculty, Major, College, Batch, Gender, Categories, UserSync, EnhancedUser, AuthTest) | ⚠️ Quá nhiều controller, cần refactor |
| **DTOs** | 28+ DTO classes | ⚠️ DTO explosion — cần consolidate |
| **Service Clients** | PostServiceClient, RecommendServiceClient (Feign) | ✅ Inter-service communication |
| **Validation** | `ValidationConfig.java` riêng | ✅ Có validation config |
| **Security** | Custom security (không dùng Spring Security) | ⚠️ Custom implementation |
| **Tests** | `UserServiceTest.java`, `UserControllerTest.java` | ✅ **Duy nhất service có test** |
| **AuthTestController** | Test controller trong main source | 🔴 **Không nên có trong production** |

**Vấn đề đặc biệt:**
- `AuthTestController.java` nằm trong main source (nên chỉ để trong test)
- 28+ DTOs cho 1 service — thiếu base/generic DTO pattern
- `EnhancedUserController.java` + `UserController.java` — 2 controller cho user, cần merge

---

### 2.3 Post Service (55 Java files)

| Thành phần | Hiện trạng | Đánh giá |
|------------|-----------|----------|
| **Database** | MongoDB | ✅ Phù hợp cho posts |
| **Entities** | PostEntity, CommentEntity, InteractionEntity, NotificationEntity | ✅ Đầy đủ |
| **Services** | 8 services (Post, Comment, Interaction, NewsFeed, Notification, Event, DataConsistency, Search) | ✅ Tách biệt tốt |
| **Security** | Custom: `AuthenticatedUser`, `AuthenticationInterceptor`, `SecurityContextHolder`, `@RequireAuth` | ⚠️ **Code trùng với chat-service, media-service** |
| **Websocket** | `WebSocketConfig.java` — notifications | ✅ Real-time notifications |
| **Kafka** | `KafkaConfig.java` — event publishing | ✅ Event-driven |
| **Caching** | `CacheConfig.java` | ✅ Cache layer |
| **Feign Clients** | MediaServiceClient, RecommendationServiceClient, UserServiceClient + Fallback | ✅ Circuit breaker pattern |
| **Search** | `SearchController.java`, `SearchService.java` | ✅ Full-text search |
| **Tests** | Chỉ `PostServiceApplicationTests.java` (boilerplate) | 🔴 **Không có test thực sự** |

---

### 2.4 Chat Service (39 Java files)

| Thành phần | Hiện trạng | Đánh giá |
|------------|-----------|----------|
| **Database** | MongoDB | ✅ Phù hợp cho messages |
| **Models** | Conversation, Message, UserPresence | ✅ Clean domain model |
| **WebSocket** | `WebSocketConfig.java` | ✅ Real-time messaging |
| **Features** | Typing indicators, reactions, presence tracking | ✅ Rich chat features |
| **Security** | **Copy-paste từ post-service** (AuthenticatedUser, AuthenticationInterceptor, SecurityContextHolder, @RequireAuth, AuthorizationAspect) | 🔴 **DRY violation nghiêm trọng** |
| **Tests** | Chỉ `ChatServiceApplicationTests.java` (boilerplate) | 🔴 **Không có test** |

---

### 2.5 Media Service (20 Java files — Service nhỏ nhất)

| Thành phần | Hiện trạng | Đánh giá |
|------------|-----------|----------|
| **Storage** | Cloudinary (`CloudinaryConfig.java`) | ✅ Cloud CDN |
| **Database** | PostgreSQL (metadata) | ✅ |
| **Exception** | `GlobalExceptionHandler`, `MediaNotFoundException`, `MediaUploadException` | ✅ Custom exceptions |
| **Kafka** | `KafkaProducerService.java` | ✅ Event publishing |
| **Security** | **Copy-paste từ post-service** (5 files giống hệt) | 🔴 **DRY violation** |
| **Tests** | Chỉ `MediaServiceApplicationTests.java` (boilerplate) | 🔴 **Không có test** |

---

### 2.6 Recommend Service (50+ Java files + Python)

| Thành phần | Hiện trạng | Đánh giá |
|------------|-----------|----------|
| **Architecture** | Hybrid: Java Orchestrator + Python AI Engine | ✅ **Kiến trúc tốt nhất trong project** |
| **AI/ML** | PhoBERT Vietnamese NLP model | ✅ Cutting-edge NLP |
| **Recommendation** | Content-based (35%) + Implicit feedback (25%) + Academic (25%) + Popularity (15%) | ✅ Hybrid approach |
| **Caching** | Redis dedicated instance + multi-layer caching | ✅ Production-grade caching |
| **Kafka** | Event consumers + producers (UserAction, PostEvent) | ✅ Event-driven |
| **Neo4j** | Graph queries for social connections | ✅ Graph traversal |
| **Documentation** | `ARCHITECTURE-OPTIMIZED.md`, `README.md` | ✅ **Service có docs tốt nhất** |
| **Ranking** | `RankingEngine.java` — scoring algorithm | ✅ Dedicated ranking logic |
| **Tests** | Không tìm thấy test files | 🔴 **Không có test** |

---

### 2.7 API Gateway (6 Java files)

| Thành phần | Hiện trạng | Đánh giá |
|------------|-----------|----------|
| **Routing** | `RouteConfig.java` | ✅ Centralized routing |
| **JWT Filter** | `JwtAuthenticationFilter.java` | ✅ Gateway-level auth |
| **Logging** | `LoggingFilter.java` | ✅ Request logging |
| **Auth Response** | `AuthResponseFilter.java` | ✅ Response manipulation |
| **CORS** | `CorsConfig.java` — **100% commented out** | ⚠️ CORS ở gateway bị disable, delegate cho từng service |
| **Rate Limiting** | **Không có** | 🔴 **Thiếu rate limiting** |

---

### 2.8 Client Frontend (Next.js 15)

| Thành phần | Hiện trạng | Đánh giá |
|------------|-----------|----------|
| **Framework** | Next.js 15.3.5 + Turbopack | ✅ Latest |
| **Pages** | login, register, forgot-password, reset-password, verify-email, profile, posts, friends, messages, search, test-auth | ✅ Đầy đủ features |
| **Components** | auth, chat, layout, post, profile, search, ui, user | ✅ Component architecture |
| **State** | TanStack Query 5 + contexts | ✅ Server state management |
| **Services** | Dedicated services layer + shared services | ✅ API abstraction |
| **Features** | Feature-based structure (auth, chat, posts, search, users) | ✅ Domain-driven |
| **Docs** | API_MAPPING.md, SERVICES_MIGRATION.md | ✅ Migration documentation |
| **Rich Text** | Quill editor | ✅ Rich content |
| **Sanitization** | isomorphic-dompurify | ✅ XSS prevention |
| **Tests** | **Không có test files frontend** | 🔴 **Không có test** |
| **test-auth page** | Test page trong production build | ⚠️ Nên remove trước khi deploy |

---

### 2.9 Docker & Infrastructure

| Thành phần | Hiện trạng | Đánh giá |
|------------|-----------|----------|
| **Docker Compose** | 354 lines, 14 services | ✅ Comprehensive |
| **Health Checks** | Tất cả services đều có healthcheck | ✅ Production-ready |
| **Networking** | Single bridge network | ✅ |
| **Volumes** | Named volumes cho tất cả databases | ✅ Data persistence |
| **Neo4j Password** | `NEO4J_AUTH=neo4j/password` **hardcoded** | 🔴 **Security risk** |
| **Recommend DB** | `POSTGRES_PASSWORD: recommend_pass` **hardcoded** | 🔴 **Security risk** |
| **Recommend Redis** | `--requirepass recommend_redis_pass` **hardcoded** | 🔴 **Security risk** |
| **Profiles** | `SPRING_PROFILES_ACTIVE=docker` | ✅ Profile separation |
| **Service Build** | Missing build context for auth, user, post, chat services | ⚠️ Chỉ có `api-gateway`, `eureka-server`, `recommend-service` có build |

---

## 3. Ma Trận Đánh Giá Chất Lượng

### 3.1 Scorecard Tổng Quan

| Tiêu chí | Điểm (1-10) | Trọng số | Điểm có trọng số | Ghi chú |
|----------|:-----------:|:--------:|:-----------------:|---------|
| **Kiến trúc** | 8 | 15% | 1.20 | Microservices hoàn chỉnh, event-driven |
| **Code Quality** | 5 | 15% | 0.75 | DRY violations, DTO explosion |
| **Security** | 4 | 20% | 0.80 | JWT có nhưng nhiều lỗ hổng |
| **Testing** | 1 | 20% | 0.20 | Gần như không có test |
| **Documentation** | 4 | 10% | 0.40 | README tốt, thiếu API docs |
| **DevOps/CI-CD** | 2 | 10% | 0.20 | Docker tốt, không có CI/CD |
| **Performance** | 5 | 5% | 0.25 | Redis cache tốt, thiếu monitoring |
| **Frontend** | 7 | 5% | 0.35 | Modern stack, clean architecture |
| **Tổng** | - | 100% | **4.15/10** | **Cần cải thiện đáng kể** |

### 3.2 Code Duplication Map

```
🔴 CRITICAL DUPLICATION — Security code copy-paste giữa 3 services:

post-service/security/        ≡ (identical)
├── AuthenticatedUser.java    ≡  chat-service/security/AuthenticatedUser.java
├── AuthenticationInterceptor ≡  chat-service/security/AuthenticationInterceptor.java
├── SecurityContextHolder     ≡  chat-service/security/SecurityContextHolder.java
├── @RequireAuth              ≡  chat-service/security/annotation/RequireAuth.java
└── AuthorizationAspect       ≡  chat-service/security/aspect/AuthorizationAspect.java
                              ≡
                              ≡  media-service/security/  (same 5 files)

→ 15 files lặp lại (5 files × 3 services)
→ Giải pháp: Tạo shared-security-lib module
```

### 3.3 Test Coverage Analysis

| Service | Test Files | Real Tests | Coverage Estimate |
|---------|:----------:|:----------:|:-----------------:|
| auth-service | 0 | 0 | **0%** |
| user-service | 2 (UserServiceTest, UserControllerTest) | ✅ Yes | ~15-25% |
| post-service | 1 (boilerplate only) | 0 | **0%** |
| chat-service | 1 (boilerplate only) | 0 | **0%** |
| media-service | 1 (boilerplate only) | 0 | **0%** |
| recommend-service | 0 | 0 | **0%** |
| api-gateway | 0 | 0 | **0%** |
| eureka-server | 1 (boilerplate only) | 0 | **0%** |
| **client-frontend** | 0 | 0 | **0%** |
| **Tổng** | **6** | **~2** | **~2-5%** |

---

## 4. Các Vấn Đề Nghiêm Trọng Cần Giải Quyết Ngay

### 🔴 SEVERITY: CRITICAL

| # | Vấn đề | Vị trí | Rủi ro | Ưu tiên |
|---|--------|--------|--------|---------|
| C1 | **Credentials hardcoded trong docker-compose.yml** | `neo4j/password`, `recommend_pass`, `recommend_redis_pass` | Lộ credentials trong source code → security breach | **P0** |
| C2 | **`.env` file committed vào Git** | Root `.env` (426 bytes) có POSTGRES_PASSWORD=password | Password lộ trong Git history | **P0** |
| C3 | **Không có CI/CD pipeline** | `.github/` chỉ có `copilot-instructions.md` | Không có automated testing, vulnerability scanning | **P0** |
| C4 | **Test coverage ~2-5%** | Toàn bộ project | Không có safety net cho refactoring/deployment | **P0** |
| C5 | **Security code trùng lặp 15 files** | post/chat/media services | Bug fix phải sửa 3 nơi, dễ bị sót | **P1** |

### 🟠 SEVERITY: HIGH

| # | Vấn đề | Vị trí | Rủi ro |
|---|--------|--------|--------|
| H1 | **Không có Rate Limiting** | API Gateway | DDoS, brute-force vulnerable |
| H2 | **CORS disabled ở Gateway** | `CorsConfig.java` (100% commented) | CORS chỉ ở service level, thiếu centralized control |
| H3 | **AuthTestController trong production** | user-service main source | Exposed test endpoints in production |
| H4 | **test-auth page trong frontend** | client-frontend/app/test-auth | Debug page accessible in production |
| H5 | **Duplicate JWT classes** | auth-service: JwtService + JwtTokenProvider | Confusion, maintenance burden |
| H6 | **Không có monitoring/observability** | Toàn hệ thống | Blind spots in production |

### 🟡 SEVERITY: MEDIUM

| # | Vấn đề | Vị trí | Rủi ro |
|---|--------|--------|--------|
| M1 | 28+ DTOs trong user-service | user-service/dto/ | Maintenance burden, unclear contracts |
| M2 | 2 User Controllers | user-service: UserController + EnhancedUserController | API confusion |
| M3 | Không export OpenAPI specs | Tất cả services | Thiếu API contract documentation |
| M4 | Services thiếu trong docker-compose build | auth, user, post, chat, media | Incomplete Docker setup |
| M5 | Missing database migration tool | Tất cả services | Schema changes unmanaged |

---

## 5. Kế Hoạch Cải Tiến 10 Tuần

### Timeline Tổng Quan

```
Tuần 1─2: 🔒 SECURITY HARDENING ────────────────────── [Impact: 🔴 Critical]
    ├─ Tuần 1: Secrets management, .env cleanup, security docs
    └─ Tuần 2: Rate limiting, JWT consolidation, input validation

Tuần 3─4: 🧪 TESTING FOUNDATION ────────────────────── [Impact: 🔴 Critical]
    ├─ Tuần 3: Unit tests cho auth-service & post-service
    └─ Tuần 4: Integration tests, JaCoCo coverage, CI pipeline

Tuần 5─6: 🏗️ CODE QUALITY & REFACTORING ───────────── [Impact: 🟠 High]
    ├─ Tuần 5: Shared security library, code deduplication
    └─ Tuần 6: DTO consolidation, controller refactoring

Tuần 7─8: 📖 DOCUMENTATION & API ──────────────────── [Impact: 🟡 Medium]
    ├─ Tuần 7: OpenAPI export, ADRs, architecture diagrams
    └─ Tuần 8: Deployment guide, troubleshooting guide

Tuần 9─10: 📊 MONITORING & PERFORMANCE ─────────────── [Impact: 🟠 High]
    ├─ Tuần 9: Prometheus + Grafana, health dashboards
    └─ Tuần 10: Performance testing, optimization, final review
```

---

## 6. Chi Tiết Triển Khai Từng Phase

---

### PHASE 1: SECURITY HARDENING (Tuần 1–2)

#### Tuần 1: Secrets & Security Foundation

##### Task 1.1: Environment Variables Cleanup
**Mục tiêu:** Loại bỏ tất cả hardcoded credentials

| File | Thay đổi | Chi tiết |
|------|---------|---------|
| `docker-compose.yml` | ✏️ Replace hardcoded passwords | `NEO4J_AUTH=${NEO4J_AUTH}`, `POSTGRES_PASSWORD=${RECOMMEND_DB_PASSWORD}`, Redis `--requirepass ${RECOMMEND_REDIS_PASSWORD}` |
| `.env.example` | ✏️ Thêm biến mới | `NEO4J_AUTH`, `RECOMMEND_DB_PASSWORD`, `RECOMMEND_REDIS_PASSWORD` |
| `.env` | 🗑️ Remove from Git tracking | `git rm --cached .env`, cập nhật `.gitignore` |
| `.gitignore` | ✏️ Verify `.env` pattern | Ensure root `.env` và service-level `.env` đều bị ignore |

**Verification:**
```bash
# Kiểm tra không còn hardcoded secrets
grep -rn "password" docker-compose.yml --include="*.yml"
# Phải chỉ thấy ${VARIABLE} patterns, không có plaintext passwords
```

##### Task 1.2: Tạo SECURITY.md
**Nội dung:**
- Vulnerability Reporting Process (responsible disclosure)
- Implemented Security Measures (JWT, BCrypt, reCAPTCHA, DOMPurify)
- Security Checklist cho contributors
- Dependency update policy

##### Task 1.3: GitHub Security Setup
| Hành động | File/Config |
|-----------|-------------|
| Setup Dependabot | `.github/dependabot.yml` — Maven + npm ecosystems |
| Security policy | `.github/SECURITY.md` — vulnerability reporting |
| Branch protection | GitHub Settings → Require PR reviews |
| Secret scanning | GitHub Settings → Enable secret scanning |

##### Task 1.4: Pre-commit Hooks
- Tạo `.pre-commit-config.yaml`
- Detect-secrets hook để chặn password commit
- Checkstyle hook cho Java code quality

---

#### Tuần 2: Security Hardening Implementation

##### Task 2.1: API Gateway Rate Limiting
**Approach:** Sử dụng Spring Cloud Gateway's built-in `RequestRateLimiter` filter với Redis backend

**Cần thêm dependency vào `api-gateway/pom.xml`:**
- `spring-boot-starter-data-redis-reactive`

**Cần sửa:**
- `RouteConfig.java`: Thêm `RequestRateLimiterGatewayFilterFactory`
- Rate limiting rules: 100 requests/minute per IP cho public APIs, 500 cho authenticated

##### Task 2.2: JWT Consolidation
**Trong auth-service:**
- Merge `JwtService.java` + `JwtTokenProvider.java` → 1 unified `JwtService.java`
- Xóa class không sử dụng
- Document token flow (access token lifecycle, refresh token rotation)

##### Task 2.3: Cleanup Test/Debug Endpoints
| Service | File | Hành động |
|---------|------|-----------|
| user-service | `AuthTestController.java` | 🗑️ Xóa hoặc di chuyển vào test source set |
| client-frontend | `app/test-auth/` | 🗑️ Xóa directory |

##### Task 2.4: CORS Strategy
**Quyết định:** Chọn 1 trong 2 approaches:
- **Option A (Recommended):** Enable CORS ở Gateway level, xóa CORS config ở service level → centralized management
- **Option B:** Giữ nguyên CORS ở service level, document rõ lý do

##### Task 2.5: Input Validation Audit
- Kiểm tra tất cả `@RequestBody` DTOs có `@Valid` annotation
- Thêm validation annotations (`@NotBlank`, `@Size`, `@Email`, `@Pattern`) vào DTOs
- Tạo custom validation annotations nếu cần (e.g., `@StrongPassword`)

---

### PHASE 2: TESTING FOUNDATION (Tuần 3–4)

#### Tuần 3: Unit Tests

##### Task 3.1: Auth Service Tests (Ưu tiên #1 — 0% → 60%+)

**Target files cần test:**

| Test class | Target class | Test cases |
|-----------|-------------|------------|
| `AuthControllerTest` | `AuthController` | register (success, duplicate email, weak password), login (success, wrong password, unverified), refresh token, verify email |
| `JwtServiceTest` | `JwtService(unified)` | generate token, validate token, expired token, tampered token, extract claims |
| `AuthServiceTest` | `AuthService` | register flow, login flow, password encryption, email verification, password reset |
| `SecurityConfigTest` | `SecurityConfig` | public endpoints accessible, protected endpoints require auth |

**Setup cần:**
- Thêm Testcontainers cho PostgreSQL + Redis
- Thêm Mockito cho service layer tests
- `application-test.yml` cho test profile

##### Task 3.2: Post Service Tests (0% → 50%+)

| Test class | Target class | Test cases |
|-----------|-------------|------------|
| `PostControllerTest` | `PostController` | create post, get posts, get by id, delete |
| `PostServiceTest` | `PostService/SearchService` | CRUD operations, search, visibility filtering |
| `CommentServiceTest` | `CommentService` | add comment, delete comment, nested comments |
| `InteractionServiceTest` | `InteractionService` | like, unlike, interaction counting |

##### Task 3.3: User Service Tests (Mở rộng từ tests hiện có)
- Review `UserServiceTest.java` và `UserControllerTest.java` hiện tại
- Thêm tests cho: friend requests, friend suggestions, profile update, search

---

#### Tuần 4: Integration Tests & CI/CD

##### Task 4.1: Integration Tests
- Auth Service: Full registration → verification → login flow
- Post Service: Create post → interact → search flow
- Inter-service: Auth → User sync via Kafka

##### Task 4.2: Test Coverage Setup
**Thêm JaCoCo vào tất cả services' `pom.xml`:**

| Config | Value |
|--------|-------|
| Plugin | `jacoco-maven-plugin` 0.8.11+ |
| Minimum coverage | 50% (Phase 1), tăng dần lên 70% |
| Report format | XML + HTML |
| Exclusions | DTOs, configs, entities |

##### Task 4.3: GitHub Actions CI/CD Pipeline

**File:** `.github/workflows/ci.yml`

| Stage | Nội dung | Trigger |
|-------|---------|---------|
| **Build** | Maven compile tất cả services | Push + PR |
| **Test** | Run unit + integration tests | Push + PR |
| **Coverage** | JaCoCo report + threshold check | Push + PR |
| **Security** | Trivy vulnerability scan | Push + PR |
| **Quality** | Checkstyle + SpotBugs | PR only |
| **Docker** | Build Docker images | Main branch only |

**Matrix build:**
```yaml
strategy:
  matrix:
    service: [auth-service, user-service, post-service, chat-service, media-service, api-gateway]
```

##### Task 4.4: Frontend Testing Setup
- Setup Jest + React Testing Library cho client-frontend
- Thêm `npm test` script
- Viết tests cho: auth flow, post creation form, search component

---

### PHASE 3: CODE QUALITY & REFACTORING (Tuần 5–6)

#### Tuần 5: Shared Security Library

##### Task 5.1: Tạo `shared-security` Maven Module
**Goal:** Loại bỏ 15 duplicated security files

**Cấu trúc module mới:**
```
shared-security/
├── pom.xml
└── src/main/java/com/ctuconnect/shared/security/
    ├── AuthenticatedUser.java
    ├── AuthenticationInterceptor.java
    ├── SecurityContextHolder.java
    ├── annotation/
    │   └── RequireAuth.java
    └── aspect/
        └── AuthorizationAspect.java
```

**Changes:**
- Tạo `shared-security` module
- Xóa 5 files duplicated trong mỗi service (post, chat, media)
- Thêm `shared-security` dependency vào 3 services' `pom.xml`
- Verify tất cả services vẫn hoạt động

##### Task 5.2: Parent POM Setup (Optional)
- Tạo root `pom.xml` để quản lý dependencies chung
- Centralize version management (Spring Boot, Spring Cloud)
- DRY principle cho build config

---

#### Tuần 6: DTO & Controller Refactoring

##### Task 6.1: User Service DTO Consolidation
**Current:** 28+ DTOs → **Target:** ~15-18 DTOs

| Action | Details |
|--------|---------|
| Merge overlapping DTOs | `UserDTO`, `UserProfileDTO`, `UserSearchDTO`, `UserUpdateDTO` → review overlap |
| Create base DTOs | `BaseUserDTO` với common fields |
| Use records | Convert pure data DTOs to Java records (Java 17+) |
| Document API contracts | Javadoc cho remaining DTOs |

##### Task 6.2: Controller Consolidation
| Service | Current | Target |
|---------|---------|--------|
| user-service | `UserController` + `EnhancedUserController` | Merge → `UserController` |
| user-service | `AuthTestController` | Xóa (hoặc move to test) |
| auth-service | `AuthController` + `AdminController` | Giữ nguyên (separated concerns) |

##### Task 6.3: Code Quality Tools
- Thêm `Checkstyle` plugin vào Maven build
- Thêm `SpotBugs` plugin cho bug detection
- Tạo `.checkstyle.xml` config riêng cho project

---

### PHASE 4: DOCUMENTATION & API (Tuần 7–8)

#### Tuần 7: API Documentation

##### Task 7.1: OpenAPI Specification Export
**Cho mỗi service có springdoc-openapi:**

| Service | Swagger URL | Export Action |
|---------|-------------|---------------|
| auth-service | `/swagger-ui.html` | Export OpenAPI JSON/YAML |
| user-service | `/swagger-ui.html` | Export OpenAPI JSON/YAML |
| post-service | `/swagger-ui.html` | Verify springdoc setup |
| chat-service | Manual | Document WebSocket APIs separately |

**Output:**
```
docs/api/
├── auth-service-openapi.yaml
├── user-service-openapi.yaml
├── post-service-openapi.yaml
├── chat-service-websocket-api.md
├── media-service-openapi.yaml
└── recommend-service-openapi.yaml
```

##### Task 7.2: Architecture Decision Records (ADRs)

```
docs/adr/
├── 001-microservices-architecture.md     — Tại sao chọn microservices?
├── 002-database-per-service.md           — Polyglot persistence strategy
├── 003-jwt-gateway-authentication.md     — JWT flow: Gateway vs Service level
├── 004-event-driven-kafka.md             — Kafka cho inter-service communication
├── 005-phobert-recommendation.md         — Tại sao PhoBERT? So sánh alternatives
├── 006-neo4j-social-graph.md             — Graph DB cho social relationships
├── 007-hybrid-recommendation-engine.md   — Content-based + Collaborative filtering
└── 008-cors-strategy.md                  — Gateway vs Service level CORS
```

##### Task 7.3: Database Schema Documentation
- ER diagrams cho PostgreSQL databases (auth_db, media_db, recommend_db)
- MongoDB collection schemas (post_db, chat_db)
- Neo4j graph model documentation
- Tool suggestion: [dbdiagram.io](https://dbdiagram.io) hoặc Mermaid diagrams

---

#### Tuần 8: Operations Documentation

##### Task 8.1: Deployment Guide
**File:** `docs/deployment/`

Nội dung:
- Prerequisites checklist
- Step-by-step Docker Compose deployment
- Environment variable configuration guide
- Service startup order & dependencies
- Health check verification commands
- SSL/TLS configuration hướng dẫn
- Reverse proxy (Nginx) setup
- Production deployment checklist

##### Task 8.2: Troubleshooting Guide
**File:** `docs/troubleshooting.md`

| Section | Nội dung |
|---------|---------|
| Common startup failures | Port conflicts, DB connection issues, Kafka startup |
| Service-specific issues | JWT errors, Neo4j connection, MongoDB auth |
| Docker issues | Volume permissions, network connectivity, memory limits |
| Performance issues | Slow queries, cache misses, memory leaks |
| Debugging tips | Log levels, Actuator endpoints, Docker logs |

##### Task 8.3: Update README.md
- Thêm badges: CI status, coverage, security scan
- Thêm link đến API docs, ADRs, deployment guide
- Thêm Contributing Guidelines update
- Thêm Screenshots/Demo section

---

### PHASE 5: MONITORING & PERFORMANCE (Tuần 9–10)

#### Tuần 9: Observability Stack

##### Task 9.1: Prometheus Metrics
**Cho mỗi Spring Boot service:**
- Dependency: `micrometer-registry-prometheus` (đã có `spring-boot-starter-actuator`)
- Config: Expose `/actuator/prometheus` endpoint
- Custom metrics: request count, error rate, response time per endpoint

**Thêm vào docker-compose.yml:**
```yaml
prometheus:
  image: prom/prometheus
  ports: ["9090:9090"]
  volumes:
    - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml

grafana:
  image: grafana/grafana
  ports: ["3100:3000"]
  volumes:
    - grafana-data:/var/lib/grafana
```

##### Task 9.2: Grafana Dashboards
| Dashboard | Metrics |
|-----------|---------|
| **System Overview** | All services status, uptime, error rates |
| **API Performance** | Request latency p50/p95/p99, throughput, error rates |
| **Database** | Connection pool, query time, active connections |
| **Kafka** | Consumer lag, message rate, partition status |
| **Redis** | Cache hit rate, memory usage, key count |
| **AI/ML** | PhoBERT inference time, embedding cache hit rate |

##### Task 9.3: Distributed Tracing (Optional)
- Spring Cloud Micrometer Tracing (thay thế Spring Cloud Sleuth đã deprecated)
- Zipkin/Jaeger cho trace visualization
- Trace correlation across microservices

---

#### Tuần 10: Performance & Final Review

##### Task 10.1: Database Optimization
| Database | Actions |
|----------|---------|
| **PostgreSQL** | Analyze indexes, add missing indexes trên auth_db.users, recommend_db.post_embeddings |
| **MongoDB** | Create compound indexes cho posts collection (authorId + createdAt), messages (conversationId + createdAt) |
| **Neo4j** | Review Cypher query performance, optimize friend suggestion queries |

##### Task 10.2: Caching Strategy Documentation
| Cache Layer | Key Pattern | TTL | Invalidation |
|-------------|------------|-----|--------------|
| User embedding | `user:emb:{userId}` | 1 hour | On profile update |
| Post embedding | `post:emb:{postId}` | Permanent | On post edit/delete |
| Feed cache | `recommend:feed:{userId}` | 30-120s | On new interaction |
| Auth tokens | `token:{tokenId}` | Token expiry | On logout/revoke |
| User sessions | `session:{userId}` | 24h | On logout |

##### Task 10.3: Load Testing
- Setup JMeter hoặc k6 test scripts
- Scenarios: concurrent login, news feed load, chat messaging, post creation
- Baseline performance metrics
- Report: response time p50/p95/p99, throughput, error rate

##### Task 10.4: Final Review & Quality Gates

**Checklist trước khi "kết thúc":**

- [ ] Test coverage ≥ 50% (tất cả services)
- [ ] 0 hardcoded credentials
- [ ] CI/CD pipeline hoạt động
- [ ] All security scans pass
- [ ] API documentation exported
- [ ] ADRs viết xong
- [ ] Deployment guide verified
- [ ] Monitoring dashboards functional
- [ ] Rate limiting configured
- [ ] Shared security library integrated

---

## 7. Metrics & KPI Theo Dõi

### Sprint Metrics

| Metric | Baseline (Hiện tại) | Target Tuần 4 | Target Tuần 8 | Target Tuần 10 |
|--------|:-------------------:|:-------------:|:-------------:|:--------------:|
| **Test Coverage** | ~2% | ≥ 40% | ≥ 50% | ≥ 60% |
| **Duplicate Code** | 15 files | 15 files | 0 files | 0 files |
| **Hardcoded Secrets** | 4+ | 0 | 0 | 0 |
| **CI/CD Pipeline** | ❌ None | ✅ Build+Test | ✅ Full pipeline | ✅ With deployment |
| **API Docs** | ❌ None | Partial | ✅ Complete | ✅ With examples |
| **ADRs** | 0 | 0 | ≥ 5 | ≥ 8 |
| **Security Scan** | ❌ None | ✅ Automated | ✅ With gates | ✅ Zero findings |
| **Monitoring** | ❌ None | ❌ None | Partial | ✅ Full |
| **Rate Limiting** | ❌ None | ✅ Gateway | ✅ Gateway | ✅ With tuning |
| **Quality Score** | 4.15/10 | 5.5/10 | 7.0/10 | **8.0+/10** |

### Commit Convention

```
feat:     Tính năng mới
fix:      Bug fix
security: Security improvements
test:     Thêm/sửa tests
docs:     Documentation
refactor: Code refactoring (không thay đổi behavior)
ci:       CI/CD pipeline changes
perf:     Performance improvements
chore:    Maintenance tasks
```

---

## 8. Giá Trị CV & Portfolio

### Sau mỗi Phase, bạn có thể ghi vào CV:

| Phase | Thời điểm | CV Bullet Point |
|-------|-----------|-----------------|
| **Phase 1** | Tuần 2 | "Implemented security hardening for microservices platform: secrets management, API rate limiting (100-500 req/min), JWT token consolidation, and input validation across 6 services" |
| **Phase 2** | Tuần 4 | "Established CI/CD pipeline with GitHub Actions (matrix build for 6 services), achieved 40%+ test coverage with JUnit 5 + Mockito + Testcontainers, integrated JaCoCo coverage reporting" |
| **Phase 3** | Tuần 6 | "Reduced code duplication by 35% through creating shared security library module, refactored DTO layer consolidating 28 DTOs to 18, applied DRY principles across microservices" |
| **Phase 4** | Tuần 8 | "Created comprehensive API documentation with OpenAPI 3.0 specs, authored 8 Architecture Decision Records, produced deployment & troubleshooting guides" |
| **Phase 5** | Tuần 10 | "Implemented full observability stack with Prometheus + Grafana (6 custom dashboards), distributed tracing, optimized database queries reducing P95 latency" |

### Portfolio Highlights

| Kỹ năng thể hiện | Evidence |
|------------------|---------|
| **Security Engineering** | SECURITY.md, rate limiting, secrets management, vulnerability scanning |
| **Testing & Quality** | 60%+ coverage, CI/CD pipeline, quality gates |
| **DevOps** | Docker Compose, GitHub Actions, monitoring stack |
| **Architecture** | Microservices, event-driven, shared libraries, ADRs |
| **AI/ML Integration** | PhoBERT recommendation engine, hybrid ranking |
| **Documentation** | OpenAPI specs, ADRs, deployment guides |
| **Performance** | Caching strategy, database optimization, load testing |

---

> **📌 Lưu ý quan trọng:**
> Kế hoạch này được phân tích dựa trên review toàn bộ mã nguồn thực tế (300+ Java files, 2 Next.js frontends, Python AI engine, Docker infrastructure). Mỗi task đều reference trực tiếp đến các file/module cụ thể trong codebase. Ưu tiên triển khai theo thứ tự: **Security → Testing → Code Quality → Documentation → Monitoring**.

---

*Phiên bản: 2.0 | Cập nhật: 22/03/2026 | Dựa trên phân tích codebase thực tế CTU-Connect*
