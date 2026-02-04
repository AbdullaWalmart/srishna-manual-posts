# Srishna Manual Posts (Backend)

Spring Boot API for manual posts: upload images/videos, store in GCP, track shares. Uses SQLite with optional sync to Google Cloud Storage.

---

## Prerequisites

- **Java 17**
- **Maven 3.6+**
- For Cloud Run deploy: **Docker**, **Google Cloud SDK** (`gcloud`), and a GCP project with Container Registry and Cloud Run enabled

---

## Run locally

```bash
mvn spring-boot:run
```

The API runs at **http://localhost:8080**.

- **Swagger UI:** http://localhost:8080/swagger-ui.html  
- **OpenAPI spec:** http://localhost:8080/v3/api-docs  

SQLite DB is created under `./data/srishna.db` by default (override with `SQLITE_PATH`).

---

## Build and deploy to Google Cloud Run

Build the Docker image, push to Google Container Registry, and deploy to Cloud Run.

### 1. Build the image

```bash
docker build -t gcr.io/global-repeater-479306-s2/srishna-image-upload:latest .
```

### 2. Push to Container Registry

```bash
docker push gcr.io/global-repeater-479306-s2/srishna-image-upload:latest
```

### 3. Deploy to Cloud Run

**PowerShell:**

```powershell
gcloud run deploy srishna-image-upload `
  --image gcr.io/global-repeater-479306-s2/srishna-image-upload:latest `
  --platform managed `
  --region asia-south1 `
  --allow-unauthenticated `
  --port 8080 `
  --memory 2Gi `
  --cpu 2 `
  --timeout 3000 `
  --set-env-vars "SQLITE_PATH=/tmp/srishna.db,GCP_DB_RESTORE=true,GCP_BUCKET=prod_srishna_web"
```

**Bash / Linux / macOS:** use backslashes for line continuation:

```bash
gcloud run deploy srishna-image-upload \
  --image gcr.io/global-repeater-479306-s2/srishna-image-upload:latest \
  --platform managed \
  --region asia-south1 \
  --allow-unauthenticated \
  --port 8080 \
  --memory 2Gi \
  --cpu 2 \
  --timeout 3000 \
  --set-env-vars "SQLITE_PATH=/tmp/srishna.db,GCP_DB_RESTORE=true,GCP_BUCKET=prod_srishna_web"
```

### Environment variables (Cloud Run)

| Variable         | Description |
|------------------|-------------|
| `SQLITE_PATH`    | Path for SQLite DB (e.g. `/tmp/srishna.db` on Cloud Run) |
| `GCP_DB_RESTORE` | Set to `true` to download DB from GCS on startup |
| `GCP_BUCKET`     | GCS bucket name (e.g. `prod_srishna_web`) |

After deploy, the service URL is shown in the command output (e.g. `https://srishna-image-upload-xxxxx.asia-south1.run.app`).

---

## API overview

- **Posts:** `GET /api/posts`, `GET /api/posts/list`, `GET /api/posts/all`, `POST /api/posts`, `PATCH /api/posts/{id}/active`, `DELETE /api/posts/{id}`
- **Auth:** `POST /api/auth/login`, `POST /api/auth/signup`, `GET /api/auth/me`, forgot/reset password
- **Admin:** `POST /api/admin/backup-db`, `POST /api/admin/revert-db` (DB sync with GCS)

See **[API.md](API.md)** and Swagger UI for full details.

---

## Frontend

UI repo: [srishna-manual-posts-ui](https://github.com/AbdullaWalmart/srishna-manual-posts-ui) (React + Vite).
