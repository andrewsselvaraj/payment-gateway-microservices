# Payment Gateway Microservices

Payment gateway integration built as Java 21 / Spring Boot 3 microservices, packaged with Docker,
delivered by Jenkins and deployed to **Red Hat OpenShift Service on AWS (ROSA)**.

| Service | Port | Responsibility |
|---|---|---|
| `api-gateway` | 8080 | Spring Cloud Gateway edge: routing, correlation IDs, CORS. Only service exposed via an OpenShift Route. |
| `order-service` | 8081 | Creates orders and charges them through `payment-service`. |
| `payment-service` | 8082 | Gateway integration: pluggable providers (mock, Stripe), idempotency keys, refunds, signed webhooks. |
| `notification-service` | 8083 | Receives payment events and notifies customers. |

The full architecture, payment flow, security notes, CI/CD design and the ROSA deployment runbook are in
**[docs/REPORT.md](docs/REPORT.md)**.

## Quick start

```bash
# Build and run all tests
mvn clean verify

# Run the whole stack locally (PostgreSQL + 4 services)
docker compose up --build

# Place an order through the gateway
./scripts/smoke-test.sh http://localhost:8080
```

Test cards for the mock provider: any number succeeds, except `...0002` (declined) and `...9995` (insufficient funds).

## Deploy to ROSA

```bash
oc new-project payments-dev
oc apply -k k8s/overlays/dev
oc get route api-gateway -n payments-dev
```

Production uses `k8s/overlays/prod` with secrets created out of band (`k8s/base/secrets.example.yaml`).
The `Jenkinsfile` automates build, test, image push, dev deploy, smoke test and gated prod promotion.

## Repository layout

```
api-gateway/  order-service/  payment-service/  notification-service/   Spring Boot services
Dockerfile                    shared multi-stage build (UBI 9 OpenJDK 21 runtime)
docker-compose.yml            local stack
Jenkinsfile                   CI/CD pipeline
k8s/base, k8s/overlays/{dev,prod}   Kustomize manifests for OpenShift
k8s/jenkins-rbac.yaml         deployer service account for Jenkins
scripts/                      smoke test, DB init
docs/REPORT.md                project report
```
