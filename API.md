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
  "createdAt": "2025-02-02T14:30:00.123456789Z"
}
```

| Field         | Description |
|---------------|-------------|
| `id`          | Post ID     |
| `imageUrl`    | Full URL to access the image (signed or public) |
| `imagePath`   | Complete storage path in bucket (e.g. `images/uuid.jpg`) |
| `textContent` | Text/caption (or `null`) |
| `createdAt`   | Upload datetime (ISO-8601 UTC) |
