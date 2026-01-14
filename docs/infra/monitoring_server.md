# 모니터링 서버

## 개요
백엔드 애플리케이션의 메트릭과 로그를 수집하고 시각화하기 위한 전용 모니터링 서버

## 서버 스펙
- **인스턴스 타입**: t3.micro (2 vCPU, 1GB RAM)
- **OS**: Ubuntu 24.04 LTS
- **스토리지**: 8GB EBS
- **리전**: ap-southeast-1 (싱가포르)

## 네트워크 구성
```
VPC (10.0.0.0/16)
├── Public Subnet - Backend (10.0.1.0/24)
│   └── Backend Server
│
└── Public Subnet - Monitoring (10.0.2.0/24)
    └── Monitoring Server
```

- 백엔드 서버와 Private IP로 통신
- 보안그룹으로 접근 제어
- 추후에 private로 변환

## 보안그룹 규칙

### 인바운드
| 포트 | 용도 | 허용 대상 |
|-----|------|----------|
| 22 | SSH | 관리자 |
| 3000 | Grafana | 팀원 |
| 9090 | Prometheus | 관리자 |
| 3100 | Loki | 백엔드 서버 SG |

### 아웃바운드
- 모든 트래픽 허용

## 배포될 컴포넌트 (예정)
- **Prometheus**: 메트릭 수집 및 저장
- **Loki**: 로그 수집 및 저장
- **Grafana**: 시각화 대시보드

## 백엔드 서버 연결
백엔드 서버에 추가된 인바운드 규칙:
- **9100번 포트**: Node Exporter (시스템 메트릭)
- **8080번 포트**: Spring Boot Actuator (애플리케이션 메트릭)

출처: 모니터링 서버 보안그룹만 허용

## Terraform 관리
```bash
# 배포
terraform apply

# 정보 확인
terraform output monitoring_public_ip
terraform output grafana_url
```

생성된 리소스: 11개 (EC2, 서브넷, 보안그룹 및 규칙 등)

