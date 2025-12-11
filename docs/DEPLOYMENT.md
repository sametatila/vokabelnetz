# 🚀 Deployment

This document describes how to deploy Vokabelnetz to production.

## Table of Contents

- [Architecture](#architecture)
- [Docker Compose](#docker-compose)
- [Nginx Configuration](#nginx-configuration)
- [Environment Variables](#environment-variables)
- [CI/CD Pipeline](#cicd-pipeline)
- [Database Backup](#database-backup)
- [Monitoring](#monitoring)
- [Deployment Checklist](#deployment-checklist)

---

## Architecture

### Production Deployment

```
┌────────────────────────────────────────────────────────────────────────────┐
│                      PRODUCTION DEPLOYMENT ARCHITECTURE                    │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│                            ┌─────────────────┐                             │
│                            │   Cloudflare    │                             │
│                            │   (CDN + SSL)   │                             │
│                            └────────┬────────┘                             │
│                                     │                                      │
│                                     ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                          NGINX REVERSE PROXY                         │  │
│  │                           (Port 80/443)                              │  │
│  │                                                                      │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │  │
│  │  │  Location /         → Angular Static Files (SPA)                │ │  │
│  │  │  Location /api/*    → Spring Boot Backend (proxy_pass)          │ │  │
│  │  │  Location /assets/* → Static Assets (caching)                   │ │  │
│  │  └─────────────────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                     │                              │                       │
│                     ▼                              ▼                       │
│  ┌─────────────────────────────┐  ┌─────────────────────────────────────┐  │
│  │     Angular Frontend        │  │      Spring Boot Backend            │  │
│  │     (Static Build)          │  │        (Port 8080)                  │  │
│  │                             │  │                                     │  │
│  │  /usr/share/nginx/html/     │  │  - REST API                         │  │
│  │  ├── index.html             │  │  - JWT Authentication               │  │
│  │  ├── main.js                │  │  - Business Logic                   │  │
│  │  ├── styles.css             │  │                                     │  │
│  │  └── assets/                │  │                                     │  │
│  └─────────────────────────────┘  └──────────────┬──────────────────────┘  │
│                                                   │                        │
│                                                   ▼                        │
│                                   ┌─────────────────────────────────────┐  │
│                                   │         PostgreSQL 18               │  │
│                                   │          (Port 5432)                │  │
│                                   │                                     │  │
│                                   │  - Persistent Volume                │  │
│                                   │  - Daily Backups                    │  │
│                                   └─────────────────────────────────────┘  │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## Docker Compose

### Development (docker-compose.yml)

```yaml
version: '3.9'

# ⚠️ SECURITY WARNING:
# Default passwords below are EXAMPLES ONLY. Even for development:
# 1. Copy .env.example to .env
# 2. Change ALL passwords before running docker-compose up
# 3. NEVER commit .env file to version control

services:
  # PostgreSQL Database
  postgres:
    image: postgres:18-alpine
    container_name: vokabelnetz-db
    environment:
      POSTGRES_DB: vokabelnetz
      POSTGRES_USER: ${DB_USER:-vokabelnetz}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-CHANGE_ME_IN_ENV_FILE}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U vokabelnetz"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - vokabelnetz-network

  # Spring Boot Backend
  backend:
    build:
      context: ./vokabelnetz-backend
      dockerfile: Dockerfile
    container_name: vokabelnetz-backend
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/vokabelnetz
      SPRING_DATASOURCE_USERNAME: ${DB_USER:-vokabelnetz}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-CHANGE_ME_IN_ENV_FILE}
      JWT_SECRET: ${JWT_SECRET:-CHANGE_ME_MIN_32_CHARACTERS_LONG}
      JWT_EXPIRATION: 900000
      JWT_REFRESH_EXPIRATION: 604800000
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - vokabelnetz-network

  # Angular Frontend
  frontend:
    build:
      context: ./vokabelnetz-frontend
      dockerfile: Dockerfile
      args:
        - CONFIGURATION=development
    container_name: vokabelnetz-frontend
    ports:
      - "4200:80"
    depends_on:
      - backend
    networks:
      - vokabelnetz-network

volumes:
  postgres_data:

networks:
  vokabelnetz-network:
    driver: bridge
```

### Production (docker-compose.prod.yml)

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:18-alpine
    container_name: vokabelnetz-db-prod
    environment:
      POSTGRES_DB: vokabelnetz
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_prod_data:/var/lib/postgresql/data
      - ./backups:/backups
    restart: unless-stopped
    networks:
      - vokabelnetz-prod

  backend:
    image: vokabelnetz/backend:${VERSION:-latest}
    container_name: vokabelnetz-backend-prod
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/vokabelnetz
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      CORS_ALLOWED_ORIGINS: https://vokabelnetz.com
    restart: unless-stopped
    depends_on:
      - postgres
    networks:
      - vokabelnetz-prod

  nginx:
    image: nginx:alpine
    container_name: vokabelnetz-nginx
    volumes:
      - ./docker/nginx/nginx.prod.conf:/etc/nginx/nginx.conf:ro
      - ./docker/nginx/ssl:/etc/nginx/ssl:ro
      - frontend_build:/usr/share/nginx/html:ro
    ports:
      - "80:80"
      - "443:443"
    restart: unless-stopped
    depends_on:
      - backend
    networks:
      - vokabelnetz-prod

volumes:
  postgres_prod_data:
  frontend_build:

networks:
  vokabelnetz-prod:
    driver: bridge
```

### Backend Dockerfile

```dockerfile
# vokabelnetz-backend/Dockerfile
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Dockerfile

```dockerfile
# vokabelnetz-frontend/Dockerfile
FROM node:24-alpine AS build

ARG CONFIGURATION=production

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration=$CONFIGURATION

FROM nginx:alpine

COPY --from=build /app/dist/vokabelnetz-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

---

## Nginx Configuration

### Development (nginx.conf)

```nginx
events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;
    
    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript 
               text/xml application/xml application/xml+rss text/javascript;
    
    server {
        listen 80;
        server_name localhost;
        root /usr/share/nginx/html;
        index index.html;
        
        # Angular SPA routing
        location / {
            try_files $uri $uri/ /index.html;
        }
        
        # API Proxy to Spring Boot
        location /api/ {
            proxy_pass http://backend:8080/api/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        # Static assets caching
        location /assets/ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}
```

### Production with SSL (nginx.prod.conf)

```nginx
events {
    worker_connections 2048;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;
    
    # =========================================================================
    # SECURITY HEADERS
    # See docs/SECURITY.md for detailed explanation
    # =========================================================================
    
    # Prevent clickjacking
    add_header X-Frame-Options "DENY" always;
    
    # Prevent MIME type sniffing
    add_header X-Content-Type-Options "nosniff" always;
    
    # XSS protection (legacy browsers)
    add_header X-XSS-Protection "1; mode=block" always;
    
    # Control referrer information
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    
    # Disable unnecessary browser features
    add_header Permissions-Policy "geolocation=(), microphone=(), camera=(), payment=()" always;
    
    # Content Security Policy - STRICT (no unsafe-inline)
    # Angular apps don't need inline scripts/styles when properly built
    # If you MUST use inline styles, use nonce-based CSP instead
    add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: https://cdn.vokabelnetz.com; font-src 'self' https://fonts.gstatic.com; connect-src 'self' https://api.vokabelnetz.com; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; upgrade-insecure-requests;" always;
    
    # HSTS - Force HTTPS (enable after confirming HTTPS works correctly)
    # max-age=31536000 = 1 year
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    
    # =========================================================================
    # RATE LIMITING ZONES
    # Layer 1: IP-based limits at Nginx level
    # Layer 2: User-based limits in application (see SECURITY.md)
    # =========================================================================
    
    # Auth endpoints: Strict limit (5 requests per minute per IP)
    limit_req_zone $binary_remote_addr zone=auth_limit:10m rate=5r/m;
    
    # API endpoints: Standard limit (60 requests per minute per IP)
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=60r/m;
    
    # Learning endpoints: Higher limit for active learners (100 requests per minute)
    limit_req_zone $binary_remote_addr zone=learning_limit:10m rate=100r/m;
    
    # =========================================================================
    
    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript 
               text/xml application/xml application/xml+rss text/javascript
               image/svg+xml;
    
    # HTTP → HTTPS redirect
    server {
        listen 80;
        server_name vokabelnetz.com www.vokabelnetz.com;
        return 301 https://$server_name$request_uri;
    }
    
    # HTTPS server
    server {
        listen 443 ssl http2;
        server_name vokabelnetz.com www.vokabelnetz.com;
        
        # SSL Configuration
        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;
        ssl_session_timeout 1d;
        ssl_session_cache shared:SSL:50m;
        ssl_session_tickets off;
        
        # Modern SSL configuration
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
        ssl_prefer_server_ciphers off;
        
        # HSTS
        add_header Strict-Transport-Security "max-age=63072000" always;
        
        root /usr/share/nginx/html;
        index index.html;
        
        # Angular SPA
        location / {
            try_files $uri $uri/ /index.html;
            
            # Cache HTML files for short time
            location ~* \.html$ {
                expires 1h;
                add_header Cache-Control "public, must-revalidate";
            }
        }
        
        # API with rate limiting
        location /api/ {
            limit_req zone=api_limit burst=20 nodelay;
            
            proxy_pass http://backend:8080/api/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        # Auth endpoints with stricter rate limiting
        location /api/auth/ {
            limit_req zone=auth_limit burst=5 nodelay;
            
            proxy_pass http://backend:8080/api/auth/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        # Static assets with long cache
        location /assets/ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
        
        # JS/CSS files with content hash
        location ~* \.(js|css)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}
```

---

## Environment Variables

### .env.example

```bash
# ===========================================
# ⚠️ SECURITY WARNING
# ===========================================
# 1. Copy this file to .env
# 2. Change ALL placeholder values
# 3. NEVER commit .env to version control
# 4. Use strong, unique passwords (min 16 chars)

# ===========================================
# DATABASE
# ===========================================
DB_USER=vokabelnetz
DB_PASSWORD=CHANGE_ME_use_strong_password_min_16_chars
DB_NAME=vokabelnetz

# ===========================================
# JWT CONFIGURATION
# ===========================================
# Must be at least 256 bits (32 characters)
# Generate with: openssl rand -base64 32
JWT_SECRET=CHANGE_ME_generate_with_openssl_rand_base64_32
JWT_EXPIRATION=900000          # 15 minutes in milliseconds
JWT_REFRESH_EXPIRATION=604800000  # 7 days in milliseconds

# ===========================================
# CORS
# ===========================================
CORS_ALLOWED_ORIGINS=https://vokabelnetz.com,https://www.vokabelnetz.com

# ===========================================
# SPRING PROFILES
# ===========================================
SPRING_PROFILES_ACTIVE=prod

# ===========================================
# DATA SEEDING
# ===========================================
APP_DATA_SEED_MODE=UPDATE  # INIT, UPDATE, or VALIDATE

# ===========================================
# EMAIL (Required for password reset)
# ===========================================
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@vokabelnetz.com
MAIL_PASSWORD=your-app-password

# ===========================================
# MONITORING & ALERTS
# ===========================================
# Sentry for error tracking
SENTRY_DSN=https://xxx@sentry.io/xxx

# Slack webhook for security alerts
# Create at: https://api.slack.com/messaging/webhooks
SLACK_SECURITY_WEBHOOK=https://hooks.slack.com/services/xxx/xxx/xxx

# PagerDuty for critical incidents (optional)
PAGERDUTY_ROUTING_KEY=your-routing-key
```

---

## CI/CD Pipeline

### GitHub Actions - CI (.github/workflows/ci.yml)

```yaml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  # Backend Tests
  backend-test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:18
        env:
          POSTGRES_DB: vokabelnetz_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: maven
      
      - name: Run Backend Tests
        working-directory: ./vokabelnetz-backend
        run: mvn clean verify
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/vokabelnetz_test
          SPRING_DATASOURCE_USERNAME: test
          SPRING_DATASOURCE_PASSWORD: test
      
      - name: Upload Coverage Report
        uses: codecov/codecov-action@v3
        with:
          files: ./vokabelnetz-backend/target/site/jacoco/jacoco.xml

  # Frontend Tests
  frontend-test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '24'
          cache: 'npm'
          cache-dependency-path: vokabelnetz-frontend/package-lock.json
      
      - name: Install Dependencies
        working-directory: ./vokabelnetz-frontend
        run: npm ci
      
      - name: Run Lint
        working-directory: ./vokabelnetz-frontend
        run: npm run lint
      
      - name: Run Tests
        working-directory: ./vokabelnetz-frontend
        run: npm run test:ci
      
      - name: Build
        working-directory: ./vokabelnetz-frontend
        run: npm run build -- --configuration=production

  # Docker Build
  docker-build:
    needs: [backend-test, frontend-test]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      
      - name: Build and Push Backend
        uses: docker/build-push-action@v5
        with:
          context: ./vokabelnetz-backend
          push: true
          tags: |
            vokabelnetz/backend:latest
            vokabelnetz/backend:${{ github.sha }}
      
      - name: Build and Push Frontend
        uses: docker/build-push-action@v5
        with:
          context: ./vokabelnetz-frontend
          push: true
          tags: |
            vokabelnetz/frontend:latest
            vokabelnetz/frontend:${{ github.sha }}
```

### GitHub Actions - CD (.github/workflows/cd.yml)

```yaml
name: CD Pipeline

on:
  workflow_run:
    workflows: ["CI Pipeline"]
    types: [completed]
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Deploy to Production
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd /opt/vokabelnetz
            
            # Pull latest images
            docker-compose -f docker-compose.prod.yml pull
            
            # Run database migrations
            docker-compose -f docker-compose.prod.yml run --rm backend \
              java -jar app.jar --spring.flyway.migrate=true
            
            # Restart services
            docker-compose -f docker-compose.prod.yml up -d
            
            # Clean up old images
            docker image prune -f
            
      - name: Health Check
        run: |
          sleep 30
          curl -f https://vokabelnetz.com/api/actuator/health || exit 1
      
      - name: Notify Slack
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          fields: repo,message,commit,author,action
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
        if: always()
```

---

## Database Backup

### Backup Script (scripts/backup.sh)

```bash
#!/bin/bash

# Configuration
BACKUP_DIR="/backups"
DB_CONTAINER="vokabelnetz-db-prod"
DB_NAME="vokabelnetz"
DB_USER="vokabelnetz"
RETENTION_DAYS=30
S3_BUCKET="vokabelnetz-backups"

# Create backup filename with timestamp
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/vokabelnetz_${TIMESTAMP}.sql.gz"

echo "Starting backup: ${BACKUP_FILE}"

# Create backup
docker exec ${DB_CONTAINER} pg_dump -U ${DB_USER} ${DB_NAME} | gzip > ${BACKUP_FILE}

# Check if backup was successful
if [ $? -eq 0 ]; then
    echo "Backup successful: ${BACKUP_FILE}"
    
    # Get file size
    SIZE=$(du -h ${BACKUP_FILE} | cut -f1)
    echo "Backup size: ${SIZE}"
    
    # Upload to S3 (optional)
    if command -v aws &> /dev/null; then
        aws s3 cp ${BACKUP_FILE} s3://${S3_BUCKET}/
        echo "Uploaded to S3: s3://${S3_BUCKET}/"
    fi
    
    # Remove old backups
    find ${BACKUP_DIR} -name "vokabelnetz_*.sql.gz" -mtime +${RETENTION_DAYS} -delete
    echo "Cleaned up backups older than ${RETENTION_DAYS} days"
else
    echo "Backup failed!"
    exit 1
fi
```

### Cron Job

```bash
# Add to crontab (crontab -e)
# Daily backup at 3:00 AM
0 3 * * * /opt/vokabelnetz/scripts/backup.sh >> /var/log/vokabelnetz-backup.log 2>&1

# Daily hard delete cleanup at 4:00 AM (GDPR compliance)
0 4 * * * /opt/vokabelnetz/scripts/cleanup-deleted-users.sh >> /var/log/vokabelnetz-cleanup.log 2>&1
```

### Hard Delete Cleanup Script (scripts/cleanup-deleted-users.sh)

```bash
#!/bin/bash

# GDPR Compliance: Permanently delete users marked for deletion > 30 days ago
# This script runs daily via cron

DB_CONTAINER="vokabelnetz-db-prod"
DB_NAME="vokabelnetz"
DB_USER="vokabelnetz"
RETENTION_DAYS=30

echo "$(date): Starting hard delete cleanup..."

# Count users pending deletion
PENDING_COUNT=$(docker exec ${DB_CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -t -c \
  "SELECT COUNT(*) FROM users WHERE deleted_at IS NOT NULL AND deleted_at < NOW() - INTERVAL '${RETENTION_DAYS} days';")

echo "Found ${PENDING_COUNT} users pending hard deletion"

if [ "$PENDING_COUNT" -gt 0 ]; then
  # Perform hard delete (CASCADE will remove related records)
  docker exec ${DB_CONTAINER} psql -U ${DB_USER} -d ${DB_NAME} -c \
    "DELETE FROM users WHERE deleted_at IS NOT NULL AND deleted_at < NOW() - INTERVAL '${RETENTION_DAYS} days';"
  
  echo "$(date): Hard deleted ${PENDING_COUNT} users and their related data"
else
  echo "$(date): No users to delete"
fi
```

---

## Monitoring

### Health Check Endpoint

```
GET /api/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### Prometheus Metrics

Enable in `application-prod.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

### Security Alerts

Security events are monitored and alerts sent via multiple channels:

#### Alert Channels

| Channel | Purpose | Events |
|---------|---------|--------|
| **Slack** (`#security-alerts`) | Real-time team notification | Brute force, token theft, suspicious login |
| **Sentry** (tag: `security`) | Error tracking + context | All security exceptions |
| **Email** (`security@vokabelnetz.com`) | Audit trail + escalation | Critical events only |
| **PagerDuty** | On-call alerting | Critical production incidents |

#### Alert Configuration

```yaml
# application-prod.yml

security:
  alerts:
    slack:
      enabled: true
      webhook-url: ${SLACK_SECURITY_WEBHOOK}
      channel: "#security-alerts"
    
    sentry:
      enabled: true
      dsn: ${SENTRY_DSN}
      environment: production
      
    email:
      enabled: true
      recipients:
        - security@vokabelnetz.com
        - oncall@vokabelnetz.com
      # Only critical events
      min-severity: CRITICAL
```

#### SecurityAlertService Implementation

```java
@Service
@Slf4j
public class SecurityAlertService {
    
    private final SlackClient slackClient;
    private final SentryClient sentryClient;
    private final EmailService emailService;
    
    public void sendAlert(SecurityEvent event) {
        String message = formatAlertMessage(event);
        
        // Always log
        log.warn("SECURITY_ALERT: type={}, userId={}, ip={}, details={}", 
            event.getType(), event.getUserId(), event.getIpAddress(), event.getDetails());
        
        // Slack - real-time notification
        if (slackEnabled) {
            slackClient.sendMessage(securityChannel, message, event.getSeverity().getColor());
        }
        
        // Sentry - for tracking and context
        sentryClient.captureEvent(event, Map.of("tag", "security"));
        
        // Email - critical events only
        if (event.getSeverity() == Severity.CRITICAL) {
            emailService.sendSecurityAlert(securityRecipients, event);
        }
    }
}
```

#### Security Event Types

| Event | Severity | Action |
|-------|----------|--------|
| `BRUTE_FORCE_DETECTED` | HIGH | Alert + account lockout |
| `TOKEN_REUSE_DETECTED` | CRITICAL | Alert + revoke all sessions |
| `DISTRIBUTED_ATTACK` | CRITICAL | Alert + IP blocking |
| `SUSPICIOUS_LOGIN` | MEDIUM | Alert + email to user |
| `PASSWORD_CHANGED` | INFO | Email to user |
| `MULTIPLE_FAILED_LOGINS` | MEDIUM | Alert |
| `ADMIN_ACTION` | INFO | Audit log |

#### Example Slack Alert

```
🚨 SECURITY ALERT: BRUTE_FORCE_DETECTED

User: j***n@e***.com (ID: 12345)
IP: 192.168.1.100
Location: Istanbul, TR
Time: 2025-01-09 14:30:00 UTC

Details: 10 failed login attempts in 5 minutes

Action Taken: Account locked for 15 minutes

[View in Sentry] [View User] [Block IP]
```

---

## Deployment Checklist

```
┌──────────────────────────────────────────────────────────────────────────┐
│                       PRODUCTION DEPLOYMENT CHECKLIST                    │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Pre-Deployment:                                                         │
│  □ All tests passing (CI green)                                          │
│  □ Environment variables configured                                      │
│  □ SSL certificates valid (check expiry)                                 │
│  □ Database migrations reviewed                                          │
│  □ Backup created and verified                                           │
│  □ Rollback plan documented                                              │
│                                                                          │
│  Deployment:                                                             │
│  □ Pull latest Docker images                                             │
│  □ Run database migrations                                               │
│  □ Start containers                                                      │
│  □ Verify health check endpoints                                         │
│                                                                          │
│  Post-Deployment:                                                        │
│  □ Smoke test critical paths:                                            │
│      □ User registration                                                 │
│      □ User login                                                        │
│      □ Start learning session                                            │
│      □ Submit answer                                                     │
│  □ Monitor error rates (first 30 minutes)                                │
│  □ Check performance metrics                                             │
│  □ Verify SSL/HTTPS working                                              │
│  □ Test language switching (TR/EN)                                       │
│                                                                          │
│  Rollback (if needed):                                                   │
│  □ docker-compose -f docker-compose.prod.yml down                        │
│  □ docker tag vokabelnetz/backend:previous vokabelnetz/backend:latest    │
│  □ docker-compose -f docker-compose.prod.yml up -d                       │
│  □ Restore database from backup if needed                                │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Quick Commands

```bash
# Start development environment
docker-compose up -d

# Start production environment
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose logs -f backend

# Run database migrations
docker-compose exec backend java -jar app.jar --spring.flyway.migrate=true

# Create backup
./scripts/backup.sh

# Restore from backup
gunzip -c /backups/vokabelnetz_20250109.sql.gz | docker exec -i vokabelnetz-db psql -U vokabelnetz

# Scale backend (if using swarm)
docker service scale vokabelnetz_backend=3
```
