# Deploy to the Red Hat OpenShift Developer Sandbox

This runbook deploys the backend (this repo) and the Angular frontend
([payment-gateway-fe](https://github.com/andrewsselvaraj/payment-gateway-fe)) into a Developer Sandbox
project such as `andrewsselvaraj-a-dev`.

The Sandbox is not the ROSA setup in [REPORT.md](REPORT.md): you get one fixed project, a resource quota,
no cluster-admin and no Jenkins. So both repos have a `k8s/overlays/sandbox` overlay that:

- deploys into whatever project is current (no `payments-dev` namespace),
- builds the images inside OpenShift from GitHub with each repo's `Dockerfile` (BuildConfig + ImageStream),
  so no quay.io account or registry secret is needed,
- runs one replica per service with explicit CPU/memory limits that fit the quota,
- drops the HPAs, PodDisruptionBudgets and default-deny NetworkPolicies,
- uses in-memory H2 databases and the mock payment provider (data is lost when a pod restarts).

## 1. Prerequisites

- `oc` CLI. Download it from the Sandbox console: **?** (top right) > **Command line tools**.
- `git`.

## 2. Log in

1. In the console, click your user name (top right) > **Copy login command** > **Display Token**.
2. Run the `oc login --token=... --server=https://api....openshiftapps.com:6443` line it shows.
3. Select your project:

```bash
oc project andrewsselvaraj-a-dev
```

## 3. Deploy the backend

```bash
git clone https://github.com/andrewsselvaraj/payment-gateway-microservices.git
cd payment-gateway-microservices
oc apply -k k8s/overlays/sandbox
```

This creates the ConfigMap, Secrets, 4 Deployments and Services, the `api-gateway` Route, and one
BuildConfig + ImageStream per service. The pods show `ImagePullBackOff` until their image is built; that is expected.

Now build the images. Run them **one at a time**: each Maven build needs up to 2 GiB and parallel builds
can exceed the Sandbox quota.

```bash
for s in notification-service payment-service order-service api-gateway; do
  oc start-build "$s" --follow || break
done
```

Each finished build rolls out its Deployment automatically (image-change trigger). Watch them come up:

```bash
oc get pods -w        # wait for 4 pods Running and 1/1 Ready (Spring Boot takes ~1-2 min on the Sandbox CPU share)
```

Smoke test through the gateway Route:

```bash
GW=https://$(oc get route api-gateway -o jsonpath='{.spec.host}')
./scripts/smoke-test.sh "$GW"   # prints "Smoke test PASSED"
```

## 4. Deploy the frontend

```bash
cd ..
git clone https://github.com/andrewsselvaraj/payment-gateway-fe.git
cd payment-gateway-fe
oc apply -k k8s/overlays/sandbox
oc start-build payment-gateway-fe --follow
```

nginx in the frontend pod proxies `/api/**` to the `api-gateway` Service in the same project, so no URL or CORS
setting is needed. Open the app:

```bash
echo https://$(oc get route payment-gateway-fe -o jsonpath='{.spec.host}')
```

Place a payment on **New payment** with card `4242 4242 4242 4242` (succeeds), `...0002` (declined) or
`...9995` (insufficient funds).

## 5. Redeploy after a code change

Push to `main`, then rebuild the service you changed; its Deployment rolls out on its own:

```bash
oc start-build order-service --follow
oc start-build payment-gateway-fe --follow
```

## Troubleshooting

| Symptom | Fix |
|---|---|
| `exceeded quota` when a build or pod starts | Run builds one at a time; check usage with `oc describe quota`. Delete finished build pods with `oc delete pod --field-selector=status.phase==Succeeded`. |
| Build fails pulling `maven`, `node` or `nginx-unprivileged` (`toomanyrequests`) | Docker Hub rate limit on the shared cluster. Wait a few minutes and run `oc start-build` again. |
| Build pod `OOMKilled` | Make sure nothing else is building, then retry. |
| Pod restarts during startup | `oc logs deploy/<service>`; a slow start is covered by a 5-minute startup probe. |
| App was working, now all pods are at 0 | The Sandbox idles workloads after ~12 hours. `oc scale deploy --all --replicas=1`. |
| Orders disappeared | H2 is in-memory; a pod restart clears it. |

## Clean up

```bash
oc delete all,cm,secret,is,bc -l app.kubernetes.io/part-of=payment-gateway
oc delete is,bc payment-gateway-fe api-gateway order-service payment-service notification-service --ignore-not-found
```
