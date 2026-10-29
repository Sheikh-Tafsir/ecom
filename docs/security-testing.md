# Security Testing

How to run static analysis, dependency scanning, dynamic application testing, and manual OWASP Top 10 checks against the platform. For the security architecture itself, see [security.md](./security.md).

---

## Tools Installed

| Layer | Tool | Service | What It Finds |
|---|---|---|---|
| SAST | SpotBugs + FindSecBugs | Server, Gateway | Injection patterns, insecure crypto, hardcoded secrets, XXE, deserialization |
| SAST | ESLint + security plugin | Chat Server | `eval()`, non-literal `require()`, unsafe regex, prototype pollution |
| SCA | OWASP Dependency-Check | Server, Gateway | Known CVEs in Java dependencies (NVD database) |
| SCA | `npm audit` | Chat Server, Client | Known CVEs in npm packages |
| SCA | Trivy | All (filesystem + Docker images) | CVEs, leaked secrets, Dockerfile misconfigs |
| DAST | OWASP ZAP | Full app (via Nginx proxy) | XSS, SQL injection, path traversal, missing headers, auth bypass |
| Secrets | TruffleHog / Gitleaks | Git history | Committed API keys, passwords, tokens |

---

## 1. SAST (Static Analysis)

Scans source code without running the app.

### Java — SpotBugs + FindSecBugs (Server + Gateway)

Both `server/build.gradle` and `gateway/build.gradle` include the `com.github.spotbugs` plugin with the FindSecBugs extension.

```bash
cd server && ./gradlew spotbugsMain
# Report: server/build/reports/spotbugs/main.html

cd gateway && ./gradlew spotbugsMain
# Report: gateway/build/reports/spotbugs/main.html
```

Configuration (in each `build.gradle`):

```groovy
spotbugs {
    effort = MAX       // deepest analysis
    reportLevel = MEDIUM  // report medium and high confidence bugs
}
```

To fail CI on high-confidence bugs only, change `reportLevel` to `HIGH`.

### Node.js — ESLint Security (Chat Server)

The Chat Server has `eslint-plugin-security` configured in `.eslintrc.json`.

```bash
cd server_chat && npm run lint:security
```

Key rules enabled:
- `detect-eval-with-expression` → **error**
- `detect-no-csrf-before-method-override` → **error**
- `detect-object-injection` → warn
- `detect-non-literal-require` → warn
- `detect-non-literal-fs-filename` → warn
- `detect-child-process` → warn

### React — Manual Grep (Client)

No automated SAST plugin is installed for the client. Run a quick pattern search:

```bash
cd client
grep -rn "dangerouslySetInnerHTML" src/ --include="*.jsx" --include="*.tsx"
grep -rn "eval(" src/ --include="*.js" --include="*.jsx" --include="*.tsx"
grep -rn "innerHTML" src/ --include="*.js" --include="*.jsx" --include="*.tsx"
```

Any matches should be reviewed manually.

---

## 2. SCA (Dependency Scanning)

Checks dependencies against known vulnerability databases.

### Java — OWASP Dependency-Check (Server + Gateway)

Both `build.gradle` files include the `org.owasp.dependencycheck` plugin.

```bash
cd server && ./gradlew dependencyCheckAnalyze
# Report: server/build/reports/dependency-check-report.html

cd gateway && ./gradlew dependencyCheckAnalyze
# Report: gateway/build/reports/dependency-check-report.html
```

The first run downloads the NVD database (~5–10 minutes). Subsequent runs are fast.

Configuration:

```groovy
dependencyCheck {
    failBuildOnCVSS = 9       // fail on CRITICAL only
    analyzers.assemblyEnabled = false
}
```

Lower `failBuildOnCVSS` to `7` to also fail on HIGH severity.

### Node.js — npm audit (Chat Server + Client)

```bash
cd server_chat && npm audit
cd client && npm audit

# Only high/critical:
npm audit --audit-level=high

# Auto-fix safe updates:
npm audit fix
```

### Trivy — Full Filesystem + Docker Images

```bash
# Install via official Debian/Ubuntu APT repository:
sudo apt-get install wget gnupg
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor | sudo tee /usr/share/keyrings/trivy.gpg > /dev/null
echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update && sudo apt-get install trivy

# Scan the entire repo
trivy fs --scanners vuln,secret,misconfig .

# Only high/critical
trivy fs --severity HIGH,CRITICAL .

# Scan Docker images (after docker compose build)
docker compose build
trivy image ecom-server:latest
trivy image ecom-gateway:latest
trivy image ecom-chat-server:latest
trivy image ecom-client:latest
```

### Secrets in Git History

```bash
# TruffleHog (only verified secrets)
docker run --rm -v $(pwd):/repo trufflesecurity/trufflehog:latest git file:///repo --only-verified

# Gitleaks (broader scan)
docker run --rm -v $(pwd):/repo zricethezav/gitleaks:latest detect --source /repo
```

---

## 3. DAST (Dynamic Scanning) — OWASP ZAP

Attacks the running application. **Run only against dev/staging with test data.**

### Prerequisites

1. App running: `docker compose up -d`
2. Docker network name: `docker network ls | grep frontend` (usually `ecom_frontend`)
3. For authenticated scans: create test users in the dev database

### Baseline Scan (Passive — Safe)

Crawls the app and observes issues without attacking. Safe for any environment.

```bash
mkdir -p zap-reports

docker run --rm \
  --network ecom_frontend \
  -v $(pwd)/zap-reports:/zap/wrk \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py \
    -t https://nginx-proxy/ \
    -r baseline-report.html \
    -I
```

### API Scan (Active — Targets REST Endpoints)

Uses the OpenAPI spec to systematically test each endpoint.

```bash
# 1. Enable Swagger temporarily (SPRING_PROFILES_ACTIVE=dev)
# 2. Export the spec:
curl -k https://localhost/v3/api-docs > openapi.json

# 3. Run API scan:
docker run --rm \
  --network ecom_frontend \
  -v $(pwd)/zap-reports:/zap/wrk \
  -v $(pwd)/openapi.json:/zap/wrk/openapi.json \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-api-scan.py \
    -t /zap/wrk/openapi.json \
    -f openapi \
    -r api-scan-report.html
```

### Full Scan with Authentication (Active — Comprehensive)

Logs in as a test user, crawls authenticated routes, and actively attacks everything.

**Step 1:** Create `zap-config.yaml` in the repo root:

> [!WARNING]
> Do NOT commit `zap-config.yaml` with plaintext credentials to source control. Ensure `zap-config.yaml` is added to `.gitignore`, or pass credentials dynamically using environment variables (`${ZAP_TEST_USER}`, `${ZAP_TEST_PASS}`) in your CI pipeline.

```yaml
env:
  contexts:
    - name: "Ecom API"
      urls:
        - "https://nginx-proxy"
      includePaths:
        - "https://nginx-proxy/api/.*"
      excludePaths:
        - "https://nginx-proxy/api/payment/callback.*"
      authentication:
        method: "json"
        parameters:
          loginPageUrl: "https://nginx-proxy/api/v1/auth/login"
          loginRequestUrl: "https://nginx-proxy/api/v1/auth/login"
          loginRequestBody: '{"email":"{%username%}","password":"{%password%}"}'
        verification:
          method: "response"
          loggedInRegex: "\"success\":true"
      users:
        - name: "test-user"
          credentials:
            username: "zaptest@example.com"
            password: "ZapTest123!"
        - name: "test-admin"
          credentials:
            username: "zapadmin@example.com"
            password: "ZapAdmin123!"

jobs:
  - type: spider
    parameters:
      context: "Ecom API"
      user: "test-user"
      maxDuration: 5
  - type: spiderAjax
    parameters:
      context: "Ecom API"
      user: "test-user"
      maxDuration: 5
  - type: passiveScan-wait
  - type: activeScan
    parameters:
      context: "Ecom API"
      user: "test-user"
  - type: report
    parameters:
      template: "modern"
      reportDir: "/zap/wrk"
      reportFile: "zap-full-report"
```

**Step 2:** Run it:

```bash
docker run --rm \
  --network ecom_frontend \
  -v $(pwd)/zap-config.yaml:/zap/wrk/zap-config.yaml \
  -v $(pwd)/zap-reports:/zap/wrk \
  ghcr.io/zaproxy/zaproxy:stable \
  zap.sh -cmd -autorun /zap/wrk/zap-config.yaml
```

Reports land in `zap-reports/`.

---

## 4. Manual OWASP Top 10 Checks

Automated tools miss business logic flaws. Use `curl` or Burp Suite Community for these.

### Setup — Get Auth Tokens

```bash
USER_TOKEN=$(curl -sk -X POST https://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!"}' \
  | jq -r '.data.accessToken')

ADMIN_TOKEN=$(curl -sk -X POST https://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Password123!"}' \
  | jq -r '.data.accessToken')
```

### A01 — Broken Access Control (IDOR)

```bash
# Access another user's order
curl -sk -H "Authorization: Bearer $USER_TOKEN" \
  https://localhost/api/v1/orders/OTHER_USERS_ORDER_ID
# Expected: 403 or 404

# Admin endpoint with regular user token
curl -sk -X PATCH -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"CONFIRMED"}' \
  https://localhost/api/v1/orders/ANY_ORDER_ID/status
# Expected: 403

# Payment refund as regular user
curl -sk -X POST -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"paymentId":"any-id"}' \
  https://localhost/api/payment/refund
# Expected: 403
```

### A02 — Cryptographic Failures

```bash
# HSTS header present
curl -skI https://localhost/ | grep -i strict-transport
# Expected: Strict-Transport-Security: max-age=63072000; includeSubDomains

# Refresh token cookie flags
curl -sk -v -X POST https://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!"}' \
  2>&1 | grep -i "set-cookie"
# Expected: HttpOnly; Secure; SameSite=
```

### A03 — Injection

```bash
# SQL injection on login
curl -sk -X POST https://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin'\'' OR 1=1--","password":"x"}'
# Expected: 401 (not 500 or data leak)

# XSS in query params
curl -sk "https://localhost/api/v1/products?search=<script>alert(1)</script>"
# Expected: sanitized or 400

# SQL injection in pagination
curl -sk "https://localhost/api/v1/products?page=0&size=10;DROP TABLE products--"
# Expected: 400 or normal response
```

### A05 — Security Misconfiguration

```bash
# Actuator blocked (except health)
curl -sk https://localhost/actuator/prometheus    # Expected: 404
curl -sk https://localhost/actuator/env           # Expected: 404
curl -sk https://localhost/actuator/health        # Expected: 200

# Swagger blocked in prod
curl -sk https://localhost/swagger-ui/index.html  # Expected: 404
curl -sk https://localhost/v3/api-docs            # Expected: 404

# No stack traces in errors
curl -sk -X POST https://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d 'not json at all'
# Expected: ApiResponse JSON, not a Java stack trace
```

### A07 — Authentication Failures

```bash
# Token reuse after logout
TOKEN=$(curl -sk -X POST https://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"Password123!"}' \
  | jq -r '.data.accessToken')
curl -sk -X POST https://localhost/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
sleep 5
curl -sk -H "Authorization: Bearer $TOKEN" https://localhost/api/v1/orders
# Expected: 401

# No token
curl -sk https://localhost/api/v1/orders
# Expected: 401
```

---

## Quick-Run Cheat Sheet

```bash
# ── SAST ──
cd server  && ./gradlew spotbugsMain && cd ..
cd gateway && ./gradlew spotbugsMain && cd ..
cd server_chat && npm run lint:security && cd ..

# ── SCA ──
cd server  && ./gradlew dependencyCheckAnalyze && cd ..
cd gateway && ./gradlew dependencyCheckAnalyze && cd ..
cd server_chat && npm audit --audit-level=high && cd ..
cd client && npm audit --audit-level=high && cd ..

# ── Secrets ──
docker run --rm -v $(pwd):/repo trufflesecurity/trufflehog:latest git file:///repo --only-verified

# ── DAST (app must be running) ──
docker compose up -d
mkdir -p zap-reports
docker run --rm --network ecom_frontend \
  -v $(pwd)/zap-reports:/zap/wrk \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py -t https://nginx-proxy/ -r baseline.html -I

# ── Reports ──
xdg-open server/build/reports/spotbugs/main.html
xdg-open server/build/reports/dependency-check-report.html
xdg-open zap-reports/baseline.html
```

---

## CI Integration

To run security checks in GitHub Actions, add to the existing CI workflow:

```yaml
  security-scan:
    runs-on: ubuntu-latest
    needs: [server-build]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: cd server && ./gradlew spotbugsMain dependencyCheckAnalyze
      - run: cd gateway && ./gradlew spotbugsMain dependencyCheckAnalyze
      - uses: actions/setup-node@v4
        with: { node-version: '22' }
      - run: cd server_chat && npm ci && npm run lint:security
      - run: cd server_chat && npm audit --audit-level=high
      - run: cd client && npm ci && npm audit --audit-level=high
```

Add DAST (ZAP) as a separate scheduled workflow rather than on every push — it takes several minutes and needs a running app.
