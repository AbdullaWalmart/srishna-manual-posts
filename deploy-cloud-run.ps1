# Build and deploy Srishna backend to Cloud Run
# DB: loaded from gs://prod_srishna_web/data/srishna.db on startup; synced to GCS after every write.

docker build -t gcr.io/global-repeater-479306-s2/srishna-image-upload:latest .
docker push gcr.io/global-repeater-479306-s2/srishna-image-upload:latest
gcloud run deploy srishna-image-upload `
  --image gcr.io/global-repeater-479306-s2/srishna-image-upload:latest `
  --platform managed `
  --region asia-south1 `
  --allow-unauthenticated `
  --port 8080 `
  --memory 2Gi `
  --cpu 2 `
  --timeout 3000 `
  --set-env-vars "SQLITE_PATH=/tmp/srishna.db,GCP_BUCKET=prod_srishna_web"
