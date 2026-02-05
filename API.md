# Srishna API

## Swagger UI

When the backend is running (default port **8080**):

- **Swagger UI:** http://localhost:8080/swagger-ui.html  
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs  

Replace `localhost:8080` with your host/port if different (e.g. `https://your-api.com`).

---

## Upload API – cURL

**Endpoint:** `POST /api/posts`  
**Auth:** None (upload is public for now).

### Example

```bash
curl -X POST "http://localhost:8080/api/posts" \
  -F "image=@/path/to/your/image.jpg" \
  -F "text=Optional caption or text"
```

### Windows (PowerShell)

```powershell
curl -X POST "http://localhost:8080/api/posts" `
  -F "image=@C:\path\to\image.jpg" `
  -F "text=Optional caption"
```

### Response (200 OK)

The upload API returns the created post with **complete image path**, **text**, and **uploaded datetime**:

```json
{
  "id": 1,
  "imageUrl": "https://storage.googleapis.com/...",
  "imagePath": "images/abc123.jpg",
  "textContent": "Optional caption",
  "active": true,
  "createdAt": "2025-02-02T14:30:00.123456789Z"
}
```

| Field         | Description |
|---------------|-------------|
| `id`          | Post ID     |
| `imageUrl`    | Full URL to access the image (signed or public) |
| `imagePath`   | Complete storage path in bucket (e.g. `images/uuid.jpg`) |
| `textContent` | Text/caption (or `null`) |
| `active`      | If `false`, post is hidden from list APIs |
| `createdAt`   | Upload datetime (ISO-8601 UTC) |

---

## List APIs – active posts only

`GET /api/posts` and `GET /api/posts/list` return only **active** posts. Inactive posts are excluded. To hide a post from the list, set `active` to `false` via the PATCH endpoint (auth required).

**Set post active/inactive:** `PATCH /api/posts/{id}/active?active=true|false` (authenticated).

---

## Database (GCS)

The database lives in the GCS bucket. **On every startup** the app loads `gs://prod_srishna_web/data/srishna.db` (or `GCP_BUCKET` / `GCP_DB_OBJECT`) to a runtime path. No DB file is stored in the project directory: the path defaults to the system temp dir (e.g. `/tmp/srishna.db` on Linux, `%TEMP%\srishna.db` on Windows) unless `SQLITE_PATH` is set.

- **Backup (upload DB to GCS):** `POST /api/admin/backup-db` — uploads the current DB to `gs://<bucket>/data/srishna.db`. Call after changes or before redeploy so the bucket has the latest.
- **Revert (download DB from GCS):** `POST /api/admin/revert-db` — downloads the DB from the bucket to the local path. **Restart the application** after calling to use the reverted data.

Config: `gcp.bucket-name` (default `prod_srishna_web`), `gcp.db-object-name` (default `data/srishna.db`). DB path: `SQLITE_PATH` (defaults to system temp; set e.g. `/tmp/srishna.db` on Cloud Run).
