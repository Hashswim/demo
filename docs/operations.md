# 로컬 및 EKS 운영

## 로컬

Docker Compose는 localhost:8080 웹만 공개한다. PostgreSQL, FastAPI, Worker, 모델 서버 포트는 호스트에 게시하지 않는다. 최초 seed는 APP_DEMO_ENABLED=true인 로컬에서만 계정과 가상 인력을 만든다. 비밀번호는 .env에서 준비하고 Git에 넣지 않는다. `docker compose down`은 데이터 볼륨을 지우지 않는다.

백업: `scripts/backup-local.sh`. 복원은 `RESTORE_CONFIRM=I_HAVE_A_SEPARATE_EMPTY_DATABASE RESTORE_DATABASE=restore_trial scripts/restore-local.sh backups/파일.dump`로 별도 DB에 수행한다. 목록 검사만 통과했다고 전체 복원이 검증된 것은 아니다.

## EKS 배포 준비

Terraform persistent는 S3/ECR, runtime은 VPC/EKS/노드/EBS CSI IAM/백업 IAM/Budgets로 나뉜다. 각 폴더의 terraform.tfvars.example을 검토해서 값·리전·허용IP·표준지원 EKS버전·호환 CSI버전을 정한다. apply는 자동 실행하지 않는다. S3는 prevent_destroy와 퍼블릭 차단을 적용한다. runtime 삭제와 별개로 보관한다.

1. 견적과 월 사용 시간을 확인한다. 클러스터가 남으면 계속 과금된다. Budgets는 상한 자동 차단이 아니다.
2. persistent 준비 후 이미지(ECR)와 S3 ARN을 runtime에 전달한다. GitHub ECR OIDC role은 보호된 환경과 특정 저장소·브랜치 조건으로 별도 준비한다. 로컬 계정 키를 GitHub에 저장하지 않는다.
3. runtime 구성은 컨트롤 플레인 2개 AZ 서브넷, 워커 한 AZ, NAT 없이 퍼블릭 IPv4 사용. 사용자의 443과 운영자 EKS API IP를 제한한다.
4. TLS 인증서·도메인, namespace, Secret을 준비한다. Secret 키는 Helm values의 주석을 참고한다. DATABASE_URL은 클러스터 서비스 주소와 같은 자격증명을 써야 한다. 특수문자 비밀번호는 DSN에 URL 인코딩한다.
5. `kubectl -n staffing create configmap staffing-fixtures --from-file=data/employees.json --from-file=data/evaluation_requests.json`으로 **가상 자료만** 준비한다. 실제 개인정보는 ConfigMap에 넣지 않는다.
6. Helm values에 이미지 digest, 기존 Secret/TLS Secret, demo.enabled(가상 시연만), backup bucket/role, 모델 PVC를 설정한다. model.enabled=true 시 AI 이미지도 INSTALL_LOCAL_MODELS=true로 빌드해야 한다. 현재 EKS에 실배포하지 않았다.
7. `helm lint`, `helm template`, `helm upgrade --install staffing infra/helm/staffing -n staffing` 순서로 검토 후 배포한다. 릴리스·namespace를 바꾸면 Terraform의 백업 IRSA subject도 함께 바꿔야 한다.
8. 노드 IPv4와 TLS 도메인의 연결, 역할별 화면, 원문 로그 제외, DB 접근 제한, 모델 준비 상태를 점검한다.

## 로그와 후속 운영

컨트롤 플레인 로그 그룹과 보존 설정은 Terraform에 포함한다. Pod 로그를 CloudWatch로 수집하려면 Fluent Bit 설정 예시와 별도 최소권한 IAM·배포가 추가로 필요하다. 자동 설치가 끝났다고 가정하지 않는다. 기본 kubectl logs만으로도 로컬 진단이 가능하다.

## 종료

scripts/eks-end-session.sh는 점검만 하며 AWS 리소스를 삭제하지 않는다. 신규 작업 차단→진행 작업 정리→백업 Job→S3 무결성/전체 복원 시험→검토한 destroy 계획 실행→잔여 EBS·IPv4·스냅샷·로그 확인 순서다. 현재 접수 중단 API는 없으므로 사용 제한 창을 정해 수동으로 작업을 정리해야 한다. Retain EBS는 삭제 후에도 남을 수 있으며, 새 클러스터에는 검증된 S3 백업을 복원하는 경로를 기본으로 한다.
