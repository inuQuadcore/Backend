# 백엔드 서버 운영 가이드

## 서버 사양
- **인스턴스 타입**: t3.small
- **vCPU**: 2 core
- **메모리**: 2GB RAM
- **스토리지**: 8GB EBS (gp3)
- **OS**: Ubuntu 22.04 LTS
- **리전**: ap-southeast-1 (Singapore)
- **가용영역**: ap-southeast-1a

## 아키텍처
```
┌─────────────────────────────────────┐
│         EC2 Instance (t3.small)     │
│  ┌───────────────────────────────┐  │
│  │  Docker Compose               │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │ MySQL Container         │  │  │
│  │  │ - Port: 3306            │  │  │
│  │  │ - Network: everybuddy   │  │  │
│  │  └─────────────────────────┘  │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │ Backend Container       │  │  │
│  │  │ - Port: 8080            │  │  │
│  │  │ - Spring Boot 3.5.6     │  │  │
│  │  │ - Network: everybuddy   │  │  │
│  │  └─────────────────────────┘  │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

## 네트워크 구성

### VPC
- **CIDR**: 10.0.0.0/16
- **Public Subnet**: 10.0.1.0/24

### 보안 그룹 (Inbound Rules)
| 포트 | 프로토콜 | 소스 | 용도 |
|------|----------|------|------|
| 22 | TCP | 0.0.0.0/0 | SSH |
| 80 | TCP | 0.0.0.0/0 | HTTP |
| 443 | TCP | 0.0.0.0/0 | HTTPS |
| 8080 | TCP | 0.0.0.0/0 | Application |
| 9100 | TCP | Monitoring SG | Node Exporter |
| 9404 | TCP | Monitoring SG | Spring Actuator |

## 디렉토리 구조
```
~/everybuddy/
├── docker-compose.yml          # Docker Compose 설정
├── init.sql                    # MySQL 초기화 스크립트
└── DEPLOYMENT.md              # 이 문서
```

## 운영 명령어

### 서버 접속
```bash
ssh -i ~/.ssh/everybuddy-key.pem ubuntu@<EC2_PUBLIC_IP>
```

### 컨테이너 관리
```bash
# 전체 상태 확인
docker compose ps

# 컨테이너 시작
docker compose up -d

# 컨테이너 중지
docker compose down

# 특정 컨테이너만 재시작
docker compose restart backend
docker compose restart mysql

# 로그 확인
docker compose logs -f backend
docker compose logs -f mysql
docker compose logs --tail 100 backend
```


### 헬스체크
```bash
# 애플리케이션 상태
curl http://localhost:8080/actuator/health

# MySQL 연결 확인
docker exec everybuddy-mysql mysql -uroot -p -e "SELECT 1"

# 컨테이너 상태 확인
docker compose ps
```

## 배포 프로세스

### 자동 배포 (CI/CD)
1. `dev` 브랜치에 코드 push
2. GitHub Actions 자동 실행
3. Docker 이미지 빌드 및 Docker Hub 푸시
4. EC2 서버에 자동 배포

### 수동 배포 트리거
```bash
# 로컬에서 빈 커밋으로 배포 트리거
git commit --allow-empty -m "chore: trigger deployment"
git push origin dev
```

### 배포 확인
```bash
# GitHub Actions 로그 확인
# Repository → Actions → 최근 workflow

# 서버에서 확인
docker compose ps
docker compose logs backend --tail 50
curl http://localhost:8080/actuator/health
```

## 트러블슈팅

### 2026-01-21 백엔드 서버 다운
```bash
# aws 콘솔에서 "미달 프로비저닝" 상태 확인
t3.micro -> t3.small 로 변경
