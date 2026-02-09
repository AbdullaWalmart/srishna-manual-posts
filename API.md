# Srishna API

## API behavior (recent optimisations)

The following **do not change** request/response contracts or existing behaviour:

- **Async GCS sync:** After a write (create/update/delete), the DB file is uploaded to GCS in the background. The API response is returned immediately with the same status and body as before; only the timing of the GCS upload changed.
- **Batched DB queries:** Post list APIs load uploader names in one batch instead of N queries. The JSON shape and values are unchanged.
- **Caching:** GET list responses may be served from an in-memory cache (short TTL). Any create/update/delete clears the cache so the next GET sees fresh data. Semantics remain the same.
- **Image URLs:** `imageUrl` in list responses is either a signed GCS URL (default) or a direct public GCS URL. Signed URLs are cached 23h so list APIs stay fast. For fastest image loading and browser/CDN caching, set `GCP_PUBLIC_URLS=true` and make the bucket (or `images/` objects) public; then responses use short URLs like `https://storage.googleapis.com/prod_srishna_web/images/xxx.jpeg`. No change to response shape or behaviour.
- **URL cache warming:** After startup (with a short delay), the backend pre-generates signed URLs for the first 100 posts (configurable). When 80% of that batch is done, it starts the next 100. This fills the URL cache so the first list API calls get imageUrl from cache immediately. Disable with `GCP_URL_CACHE_WARM_ENABLED=false`. No API or response change.

---

## Caching (for high request volume)

To support **3–4 crore (30–40M) requests**, list APIs use in-memory caching:

- **Cached:** `GET /api/posts/list`, `GET /api/posts/all`, `GET /api/posts/admin/list` — Caffeine cache, 60s TTL, evicted on any post write.
- **Eviction:** Any `POST/PATCH/DELETE` on posts clears the list cache so subsequent reads are up to date.
- **Config:** TTL and size are set in `application.yml` under `spring.cache.caffeine.spec` (e.g. `maximumSize=1000,expireAfterWrite=60s`). Tune for your load.

---

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

## HTML view (share preview)

**Endpoint:** `GET /post/{id}/view`  
**Returns:** HTML (not JSON). For link sharing so social crawlers get a rich preview.

- The backend loads the same post as `GET /api/posts/{id}` internally and renders a full HTML page.
- The page includes **Open Graph** and **Twitter Card** meta tags (`og:title`, `og:description`, `og:image`, `og:url`, `og:type`, `twitter:card`, etc.) so Facebook, Twitter, WhatsApp, etc. show title, description, and image when the link is shared.
- Title is derived from post text (first ~60 chars) or `"Post #id"`. `og:url` uses `app.base-url` + `/post/{id}/view` (set `APP_BASE_URL` to your backend’s public URL when this view is served from the backend).

Example: `https://your-backend.run.app/post/123/view`

---

## Database (GCS)

The database lives in the GCS bucket. **On every startup** the app loads `gs://prod_srishna_web/data/srishna.db` (or `GCP_BUCKET` / `GCP_DB_OBJECT`) to a runtime path. No DB file is stored in the project directory: the path defaults to the system temp dir (e.g. `/tmp/srishna.db` on Linux, `%TEMP%\srishna.db` on Windows) unless `SQLITE_PATH` is set.

- **Backup (upload DB to GCS):** `POST /api/admin/backup-db` — uploads the current DB to `gs://<bucket>/data/srishna.db`. Call after changes or before redeploy so the bucket has the latest.
- **Revert (download DB from GCS):** `POST /api/admin/revert-db` — downloads the DB from the bucket to the local path. **Restart the application** after calling to use the reverted data.

Config: `gcp.bucket-name` (default `prod_srishna_web`), `gcp.db-object-name` (default `data/srishna.db`). DB path: `SQLITE_PATH` (defaults to system temp; set e.g. `/tmp/srishna.db` on Cloud Run).
