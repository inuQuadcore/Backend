# k6 부하테스트 가이드

## 개요

Grafana k6를 사용해 EveryBuddy 백엔드 API의 성능을 측정한다.
주요 목적은 EC2 t3.medium 스펙이 실제 부하를 감당할 수 있는지 검증하고,
필요 시 t3.small로 다운그레이드 가능한지 판단하는 것이다.

k6 결과는 모니터링 서버의 Prometheus로 전송되고 Grafana 대시보드에서 시각화된다.

---

## 인프라 구성

```
로컬 PC (WSL)
  └─ k6 실행
       │ Prometheus remote write
       ▼
모니터링 서버 (t3.micro)
  ├─ Prometheus :9090
  └─ Grafana    :3000

백엔드 서버 (t3.medium)
  └─ Spring Boot :8080
       └─ /actuator/prometheus (메트릭 노출)
```

---

## 테스트 시나리오

### 1. Auth (`scripts/auth.js`)

로그인과 토큰 갱신의 처리 성능을 측정한다. 모든 API 요청의 전제조건인 JWT 인증 흐름이 부하 하에서도 안정적인지 확인한다.

**흐름**
```
로그인 (POST /api/v1/auth/login)
  → 1초 대기
  → 토큰 갱신 (POST /api/v1/auth/refresh)
  → 1초 대기
  → 반복
```

**부하 단계**

| 구간 | 시간 | 가상 유저 수 |
|------|------|-------------|
| 워밍업 | 0 ~ 30s | 0 → 10명 |
| 부하 증가 | 30s ~ 1m30s | 10 → 30명 |
| 유지 | 1m30s ~ 3m30s | 30명 |
| 쿨다운 | 3m30s ~ 4m | 30 → 0명 |

**통과 기준 (Threshold)**
- 응답시간 p(95) < 500ms
- 오류율 < 1%

---

### 2. Discover (`scripts/discover.js`)

사용자 발견 기능의 DB 조회 성능을 측정한다. 필터링 쿼리가 부하 하에서 얼마나 버티는지가 핵심이다.

**흐름**
```
랜덤 사용자 추천 (GET /api/v1/discover/random)
  → 1초 대기
  → 필터 사용자 검색 (GET /api/v1/discover/filter?country=KOREA&primaryLanguage=KOREAN)
  → 1초 대기
  → 반복
```

**부하 단계** (auth.js와 동일)

**통과 기준 (Threshold)**
- 응답시간 p(95) < 1000ms (DB 조회 포함이라 auth보다 여유)
- 오류율 < 1%

---

## 테스트 계정

| 항목 | 값 |
|------|-----|
| loginId | k6test@everybuddy.com |
| password | (팀 내부 공유) |
| provider | LOCAL |

> 테스트 전용 계정으로 DB에 등록되어 있다. Google OAuth가 아닌 로컬 계정을 사용하는 이유는 외부 의존성 없이 순수하게 백엔드 성능만 측정하기 위함이다.

---

## 실행 방법

### 사전 조건

- WSL (Ubuntu) 환경
- k6 v2.0.0 이상 설치

**k6 설치 (WSL Ubuntu)**

```bash
sudo gpg -k
sudo gpg --no-default-keyring \
  --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 \
  --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**설치 확인**

```bash
k6 version
# k6 v2.0.0 (commit/8c3be52cc1, go1.26.3, linux/amd64)
```

### 실행

```bash
cd k6

# auth 시나리오
./run.sh scripts/auth.js

# discover 시나리오
./run.sh scripts/discover.js
```

`run.sh`는 내부적으로 Prometheus remote write 옵션을 포함해서 실행한다.
실행 중 CLI에 실시간 결과가 출력되고, 동시에 Grafana 대시보드에도 데이터가 쌓인다.

### Prometheus remote write 없이 빠르게 확인할 때

```bash
cd k6
k6 run scripts/auth.js
```

---

## Grafana 대시보드

- 주소: 모니터링 서버 3000포트 (팀 내부 공유)
- 대시보드: **k6 Prometheus** (ID: 18030)

k6 메트릭 외에 같은 Grafana에서 백엔드 서버의 JVM 힙, GC, DB 커넥션 풀, CPU/메모리도 함께 볼 수 있다. 부하 중 서버 자원 상태를 동시에 확인하는 것이 핵심이다.

---

## 테스트 대상에서 제외한 항목

| 항목 | 제외 이유 |
|------|----------|
| 번역 API | Triton 외부 서버 성능에 결과가 오염됨 |
| 파일 업로드 | S3 비용 발생, 네트워크 병목이 주범이라 의미 없음 |
| Firebase 채팅 | Realtime DB는 WebSocket/SSE 기반으로 k6 기본 HTTP 모듈로 정확한 측정 불가 |
| Google OAuth | 외부 Google 서버 의존성으로 순수 백엔드 성능 측정 불가 |

---

## 결과 해석 기준

| 지표 | 의미 |
|------|------|
| `http_req_duration` | 요청 전체 응답시간 (핵심 지표) |
| `http_req_failed` | 오류율 (5xx, 네트워크 오류 등) |
| `http_reqs` | 초당 처리 요청 수 (RPS) |
| `p(95)` | 상위 5% 느린 요청을 제외한 95번째 백분위 응답시간 |
| `p(99)` | 최악의 케이스에 가까운 응답시간 |

t3.small 다운그레이드 판단 기준: 30 VU 기준으로 p(95) < 500ms를 유지하고 CPU/메모리가 70% 이하면 다운그레이드 검토 가능하다.
