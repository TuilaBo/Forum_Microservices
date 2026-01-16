# School Forum - Microservices Architecture

A comprehensive microservices-based forum application built with Spring Boot, featuring authentication, posts, comments, notifications, and real-time event processing.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Docker Images](#docker-images)
- [Keycloak Setup](#keycloak-setup)
- [Debezium Setup (CDC)](#debezium-setup-cdc)
- [Services](#services)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

This project is a microservices-based forum application that allows users to:
- Register and authenticate via Keycloak
- Create, read, update, and delete posts
- Comment on posts
- Receive real-time notifications
- Manage user profiles

### Key Features

- **Microservices Architecture**: 6 independent services
- **Event-Driven Communication**: Kafka for async messaging
- **Change Data Capture (CDC)**: Debezium for automatic cache invalidation
- **Caching**: Redis for high-performance data caching
- **Rate Limiting**: API Gateway with Redis-based rate limiting
- **Monitoring**: Prometheus and Grafana for metrics and visualization
- **Security**: Keycloak for authentication and authorization

## 🏗️ Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  API Gateway    │ ← Rate Limiting (Redis)
│   (Port 8088)   │
└──────┬──────────┘
       │
       ├──► Auth Service (8081) ──► Keycloak (8080)
       │
       ├──► Post Service (8082) ──► PostgreSQL ──► Debezium ──► Kafka
       │                              │                    │
       │                              └──► Redis Cache    │
       │                                                 │
       ├──► Comment Service (8083) ──► PostgreSQL      │
       │                              │                 │
       ├──► User Service (8084) ──► PostgreSQL          │
       │                              │                 │
       └──► Notification Service (8085) ──► PostgreSQL  │
                                          │             │
                                          └──► Kafka ◄──┘
```

## 🛠️ Tech Stack

### Backend
- **Spring Boot 3.x**: Framework
- **Spring Cloud Gateway**: API Gateway
- **Spring Security + OAuth2**: Authentication
- **Spring Data JPA**: Database access
- **Spring Kafka**: Event streaming
- **PostgreSQL**: Relational database
- **Redis**: Caching and rate limiting
- **Keycloak**: Identity and Access Management
- **Debezium**: Change Data Capture (CDC)
- **Kafka**: Message broker
- **Micrometer + Prometheus**: Metrics collection
- **Grafana**: Metrics visualization

### Infrastructure
- **Docker**: Containerization
- **Docker Compose**: Multi-container orchestration

## 📦 Prerequisites

Before running this project, ensure you have:

- **Java 17+**: [Download](https://adoptium.net/)
- **Maven 3.8+**: [Download](https://maven.apache.org/download.cgi)
- **PostgreSQL 14+**: [Download](https://www.postgresql.org/download/) or use Docker
- **Redis 7+**: [Download](https://redis.io/download) or use Docker
- **Docker Desktop**: [Download](https://www.docker.com/products/docker-desktop)
- **Keycloak**: Will be run via Docker (see setup below)

### Database Setup

Create the following databases in PostgreSQL:

```sql
CREATE DATABASE post_forum_db;
CREATE DATABASE comment_forum_db;
CREATE DATABASE user_forum_db;
CREATE DATABASE notification_forum_db;
```

## 🚀 Quick Start

### Step 1: Clone the Repository

```bash
git clone <your-repo-url>
cd CV
```

### Step 2: Start Infrastructure Services

#### Option A: Using Docker Compose (Recommended)

```bash
# Start Kafka, Zookeeper, and Debezium
cd post-service
docker-compose -f docker-compose-kafka.yml up -d

# Start Prometheus and Grafana (for monitoring)
cd ..
docker-compose -f docker-compose-monitoring.yml up -d
```

#### Option B: Manual Setup

You need to start the following services manually:

1. **PostgreSQL** (Port 5432)
2. **Redis** (Port 6379)
3. **Keycloak** (Port 8080)
4. **Kafka + Zookeeper** (Port 9092, 2181)
5. **Debezium** (Port 8086) - Optional, for CDC

### Step 3: Setup Keycloak

#### 3.1. Pull Keycloak Docker Image

```bash
docker pull quay.io/keycloak/keycloak:latest
```

#### 3.2. Start Keycloak

```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev
```

**Wait 30-60 seconds** for Keycloak to fully start.

#### 3.3. Access Keycloak Admin Console

1. Open browser: http://localhost:8080
2. Click **Administration Console**
3. Login:
   - Username: `admin`
   - Password: `admin`

#### 3.4. Create Realm

1. Hover over **Master** (top-left) → Click **Create Realm**
2. Realm name: `school-forum`
3. Click **Create**

#### 3.5. Create Client

1. Go to **Clients** → Click **Create client**
2. **Client type**: `OpenID Connect`
3. **Client ID**: `forum-frontend`
4. Click **Next**
5. **Client authentication**: `OFF` (Public client)
6. **Authorization**: `OFF`
7. **Authentication flow**: Enable **Direct access grants** ⚠️ (Critical!)
8. Click **Next** → **Save**

#### 3.6. Create Users

1. Go to **Users** → Click **Create new user**
2. **Username**: `student1`
3. **Email**: `student1@example.com`
4. Click **Create**
5. Go to **Credentials** tab
6. Click **Set password**
7. **Password**: `password123`
8. **Temporary**: `OFF`
9. Click **Save**

Repeat for additional users: `student2`, `student3`, etc.

#### 3.7. Test Keycloak

Test token endpoint:

```bash
curl -X POST http://localhost:8080/realms/school-forum/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=forum-frontend&username=student1&password=password123"
```

**Expected**: JSON response with `access_token`

### Step 4: Start Microservices

Start each service in order:

```bash
# 1. API Gateway
cd api-gateway
mvn spring-boot:run

# 2. Auth Service (new terminal)
cd auth-service
mvn spring-boot:run

# 3. Post Service (new terminal)
cd post-service
mvn spring-boot:run

# 4. Comment Service (new terminal)
cd comment-service
mvn spring-boot:run

# 5. User Service (new terminal)
cd user-service
mvn spring-boot:run

# 6. Notification Service (new terminal)
cd notification-service
mvn spring-boot:run
```

### Step 5: Verify Services

Check if all services are running:

```bash
# API Gateway
curl http://localhost:8088/actuator/health

# Auth Service
curl http://localhost:8081/actuator/health

# Post Service
curl http://localhost:8082/actuator/health

# Comment Service
curl http://localhost:8083/actuator/health

# User Service
curl http://localhost:8084/actuator/health

# Notification Service
curl http://localhost:8085/actuator/health
```

## 🐳 Docker Images

### Pull All Required Images

```bash
# Pull all required images
docker pull postgres:14
docker pull redis:7-alpine
docker pull quay.io/keycloak/keycloak:latest
docker pull confluentinc/cp-kafka:7.4.0
docker pull confluentinc/cp-zookeeper:latest
docker pull debezium/connect:2.5
docker pull prom/prometheus:latest
docker pull grafana/grafana:latest
```

### Using Docker Compose

The project includes Docker Compose files for easy setup:

```bash
# Start Kafka infrastructure
cd post-service
docker-compose -f docker-compose-kafka.yml up -d

# Start monitoring stack
cd ..
docker-compose -f docker-compose-monitoring.yml up -d
```

## 🔐 Keycloak Setup

Keycloak is the Identity and Access Management (IAM) system used for authentication and authorization.

### Quick Setup

1. **Start Keycloak**:
   ```bash
   docker run -d --name keycloak -p 8080:8080 \
     -e KEYCLOAK_ADMIN=admin \
     -e KEYCLOAK_ADMIN_PASSWORD=admin \
     quay.io/keycloak/keycloak:latest start-dev
   ```

2. **Access Admin Console**: http://localhost:8080
   - Username: `admin`
   - Password: `admin`

3. **Create Realm**: `school-forum`
4. **Create Client**: `forum-frontend` (see detailed steps below)
5. **Create Users**: `student1`, `student2`, etc.

### Detailed Keycloak Configuration

See [Step 3](#step-3-setup-keycloak) in Quick Start for detailed instructions.

### Keycloak Configuration Checklist

- [ ] Keycloak running on port 8080
- [ ] Realm `school-forum` created
- [ ] Client `forum-frontend` created
- [ ] **Direct access grants** enabled ⚠️
- [ ] Valid redirect URIs configured
- [ ] Users created with passwords
- [ ] Users enabled

### Common Keycloak Issues

**Issue**: "Invalid grant" error when logging in

**Solution**: 
- Ensure **Direct access grants** is **ON** in client settings
- Verify user has password set
- Check user is enabled

**Issue**: "401 Unauthorized" when accessing protected endpoints

**Solution**:
- Verify token is valid and not expired
- Check token includes required roles
- Ensure service can reach Keycloak on port 8080

## 🔄 Debezium Setup (CDC)

Debezium is used for Change Data Capture (CDC) to automatically invalidate Redis cache when database changes occur.

### What is CDC?

Change Data Capture captures database changes (INSERT, UPDATE, DELETE) and publishes them to Kafka topics, enabling real-time cache invalidation.

### Architecture

```
PostgreSQL → Debezium Connector → Kafka Topic → Post Service → Redis Cache Invalidation
```

### Prerequisites

1. **PostgreSQL must have logical replication enabled**:
   ```sql
   -- Check if replication is enabled
   SHOW wal_level;
   -- Should return: logical
   
   -- If not, add to postgresql.conf:
   -- wal_level = logical
   ```

2. **Create Debezium user in PostgreSQL**:
   ```sql
   CREATE USER debezium WITH PASSWORD 'dbz';
   ALTER USER debezium WITH REPLICATION;
   GRANT CONNECT ON DATABASE post_forum_db TO debezium;
   GRANT USAGE ON SCHEMA public TO debezium;
   GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium;
   ```

3. **Create publication for Debezium**:
   ```sql
   CREATE PUBLICATION dbz_publication FOR TABLE posts;
   ```

### Step 1: Start Debezium

Debezium is already included in `post-service/docker-compose-kafka.yml`:

```bash
cd post-service
docker-compose -f docker-compose-kafka.yml up -d
```

This will start:
- **Zookeeper** (Port 2181)
- **Kafka** (Port 9092)
- **Debezium Connect** (Port 8086)

### Step 2: Verify Debezium is Running

```bash
# Check Debezium container
docker ps | grep debezium

# Check Debezium REST API
curl http://localhost:8086/connectors
# Should return: []
```

### Step 3: Create Debezium Connector

Use the provided connector configuration file:

```bash
# From project root
curl -X POST http://localhost:8086/connectors \
  -H "Content-Type: application/json" \
  -d @post-service/post-connector.json
```

**Or manually create connector**:

```bash
curl -X POST http://localhost:8086/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "postservice-posts-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "tasks.max": "1",
      "database.hostname": "host.docker.internal",
      "database.port": "5432",
      "database.user": "debezium",
      "database.password": "dbz",
      "database.dbname": "post_forum_db",
      "database.server.name": "dbserver1",
      "topic.prefix": "dbserver1",
      "slot.name": "postservice_slot",
      "plugin.name": "pgoutput",
      "publication.name": "dbz_publication",
      "table.include.list": "public.posts",
      "key.converter": "org.apache.kafka.connect.json.JsonConverter",
      "key.converter.schemas.enable": "false",
      "value.converter": "org.apache.kafka.connect.json.JsonConverter",
      "value.converter.schemas.enable": "false",
      "tombstones.on.delete": "false"
    }
  }'
```

### Step 4: Verify Connector Status

```bash
# Check connector status
curl http://localhost:8086/connectors/postservice-posts-connector/status

# Should return:
# {
#   "name": "postservice-posts-connector",
#   "connector": {
#     "state": "RUNNING",
#     "worker_id": "debezium:8083"
#   },
#   "tasks": [...]
# }
```

### Step 5: Test CDC

1. **Insert a new post** in PostgreSQL:
   ```sql
   INSERT INTO posts (title, content, author_id) 
   VALUES ('Test Post', 'Test Content', 'user-123');
   ```

2. **Check Kafka topic**:
   ```bash
   # List topics
   docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
   
   # Should see: dbserver1.public.posts
   
   # Consume messages
   docker exec -it kafka kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic dbserver1.public.posts \
     --from-beginning
   ```

3. **Check Post Service logs**:
   - Should see cache invalidation message
   - Redis cache for that post should be cleared

### Troubleshooting

#### Connector Status: FAILED

**Problem**: Connector fails to start

**Solutions**:
1. Check PostgreSQL connection:
   ```bash
   # From Debezium container
   docker exec -it debezium ping host.docker.internal
   ```

2. Verify PostgreSQL user permissions:
   ```sql
   -- Check user exists
   SELECT usename FROM pg_user WHERE usename = 'debezium';
   
   -- Check replication permission
   SELECT rolreplication FROM pg_roles WHERE rolname = 'debezium';
   -- Should return: true
   ```

3. Check publication exists:
   ```sql
   SELECT * FROM pg_publication WHERE pubname = 'dbz_publication';
   ```

#### No Messages in Kafka Topic

**Problem**: Database changes not captured

**Solutions**:
1. Verify logical replication slot exists:
   ```sql
   SELECT * FROM pg_replication_slots WHERE slot_name = 'postservice_slot';
   ```

2. Check PostgreSQL logs for errors

3. Verify table is included:
   ```sql
   -- Check if posts table exists
   SELECT * FROM information_schema.tables 
   WHERE table_schema = 'public' AND table_name = 'posts';
   ```

#### Cache Not Invalidated

**Problem**: Kafka messages received but cache not cleared

**Solutions**:
1. Check Post Service logs for consumer errors
2. Verify Kafka consumer group is running:
   ```bash
   # Check consumer lag
   docker exec -it kafka kafka-consumer-groups \
     --bootstrap-server localhost:9092 \
     --group post-cache-invalidation-group \
     --describe
   ```

3. Verify Redis connection in Post Service

### How It Works

1. **Database Change**: User updates a post in PostgreSQL
2. **Debezium Captures**: Debezium reads WAL (Write-Ahead Log) and captures the change
3. **Kafka Message**: Debezium publishes change event to Kafka topic `dbserver1.public.posts`
4. **Post Service Consumes**: Post Service Kafka consumer receives the message
5. **Cache Invalidation**: Post Service removes the cached post from Redis
6. **Next Request**: Cache miss → Query DB → Update cache

### Benefits

- ✅ **Automatic**: No manual cache management
- ✅ **Real-time**: Cache invalidated immediately after DB change
- ✅ **Reliable**: Captures ALL changes (even direct DB updates)
- ✅ **Scalable**: Works with multiple service instances

## 🔌 Services

| Service | Port | Description | Database |
|---------|------|-------------|----------|
| **API Gateway** | 8088 | Single entry point, rate limiting | - |
| **Auth Service** | 8081 | Authentication, OTP, user registration | - |
| **Post Service** | 8082 | Post CRUD operations | `post_forum_db` |
| **Comment Service** | 8083 | Comment management | `comment_forum_db` |
| **User Service** | 8084 | User profile management | `user_forum_db` |
| **Notification Service** | 8085 | Real-time notifications | `notification_forum_db` |

## ⚙️ Configuration

### Database Configuration

All services use PostgreSQL with the following pattern:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/{service}_forum_db
spring.datasource.username=postgres
spring.datasource.password=sa
```

### Keycloak Configuration

All services use the same Keycloak configuration:

```properties
keycloak.url=http://localhost:8080
keycloak.realm=school-forum
keycloak.client-id=forum-frontend
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/school-forum
```

### Redis Configuration

Used by:
- **API Gateway**: Rate limiting
- **Post Service**: Caching
- **Auth Service**: OTP storage

```properties
spring.redis.host=localhost
spring.redis.port=6379
```

### Kafka Configuration

Used by:
- **Post Service**: CDC events (Debezium)
- **Comment Service**: Comment events
- **Notification Service**: Event consumption

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

## 📚 API Documentation

### API Gateway Routes

All requests go through API Gateway (port 8088):

- `/auth/**` → Auth Service (8081)
- `/posts/**` → Post Service (8082)
- `/comments/**` → Comment Service (8083)
- `/users/**` → User Service (8084)
- `/notifications/**` → Notification Service (8085)

### Authentication

1. **Register**:
   ```bash
   POST http://localhost:8088/auth/register
   {
     "email": "user@example.com",
     "password": "password123",
     "username": "username"
   }
   ```

2. **Login**:
   ```bash
   POST http://localhost:8088/auth/login
   {
     "username": "student1",
     "password": "password123"
   }
   ```

3. **Use Token**:
   ```bash
   GET http://localhost:8088/posts
   Authorization: Bearer {access_token}
   ```

### Swagger UI

Some services expose Swagger UI:

- **Auth Service**: http://localhost:8081/swagger-ui.html

## 📊 Monitoring

### Prometheus

Prometheus scrapes metrics from services:

- **URL**: http://localhost:9090
- **Metrics Endpoint**: `/actuator/prometheus` (on each service)

### Grafana

Grafana visualizes Prometheus metrics:

- **URL**: http://localhost:3000
- **Default Login**: `admin` / `admin`

### Setup Monitoring

1. **Start Prometheus and Grafana**:
   ```bash
   docker-compose -f docker-compose-monitoring.yml up -d
   ```

2. **Configure Prometheus Data Source in Grafana**:
   - URL: `http://prometheus:9090`
   - Access: `Server (default)`

3. **Import Dashboards**:
   - Spring Boot Dashboard: ID `11378`
   - JVM Dashboard: ID `4701`

### Key Metrics

- **HTTP Request Rate**: `rate(http_server_requests_seconds_count[1m])`
- **Error Rate**: `rate(http_server_requests_seconds_count{status=~"5.."}[1m])`
- **Latency P95**: `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))`
- **JVM Heap**: `jvm_memory_used_bytes{area="heap"}`
- **GC Pause**: `jvm_gc_pause_seconds_sum`

## 🔧 Troubleshooting

### 1. Service Won't Start

**Problem**: Service fails to start

**Solutions**:
1. Check if port is already in use:
   ```bash
   netstat -ano | findstr :8082
   ```

2. Verify database connection:
   ```bash
   psql -h localhost -U postgres -d post_forum_db
   ```

3. Check logs:
   ```bash
   tail -f logs/application.log
   ```

### 2. Database Connection Failed

**Problem**: `Connection refused` or `Authentication failed`

**Solutions**:
1. Verify PostgreSQL is running:
   ```bash
   # Windows
   Get-Service postgresql*
   
   # Or check port
   netstat -ano | findstr :5432
   ```

2. Check database credentials in `application.properties`

3. Verify database exists:
   ```sql
   \l
   ```

### 3. Keycloak Connection Failed

**Problem**: `401 Unauthorized` when accessing services

**Solution**:
1. Ensure Keycloak is running on port 8080:
   ```bash
   docker ps | grep keycloak
   # Or check: http://localhost:8080
   ```

2. Verify realm `school-forum` exists:
   - Access http://localhost:8080
   - Login as admin/admin
   - Check if `school-forum` realm exists

3. Check client `forum-frontend` configuration:
   - Go to **Clients** → `forum-frontend`
   - Verify **Direct access grants** is **ON** ⚠️ (Critical!)
   - Verify **Client authentication** is **OFF** (Public client)

4. Verify user exists and has password:
   - Go to **Users** → Find your user
   - Check **Credentials** tab has password set
   - Verify user is **Enabled**

### 4. Redis Connection Failed

**Problem**: `Connection refused` to Redis

**Solutions**:
1. Verify Redis is running:
   ```bash
   # Windows
   redis-cli ping
   # Should return: PONG
   ```

2. Check Redis port (default: 6379)

3. Verify Redis configuration in `application.properties`

### 5. Kafka Connection Failed

**Problem**: `Connection refused` to Kafka

**Solutions**:
1. Verify Kafka is running:
   ```bash
   docker ps | grep kafka
   ```

2. Check Kafka port (default: 9092)

3. Verify Kafka bootstrap servers in `application.properties`

### 6. Debezium Connector Failed

See [Debezium Troubleshooting](#troubleshooting-1) section above.

## 📁 Project Structure

```
CV/
├── api-gateway/          # API Gateway service
├── auth-service/         # Authentication service
├── post-service/         # Post management service
├── comment-service/      # Comment management service
├── user-service/         # User profile service
├── notification-service/ # Notification service
├── moderation-service/   # Moderation service (future)
├── docker-compose-monitoring.yml  # Prometheus & Grafana
├── prometheus.yml        # Prometheus configuration
└── README.md            # This file
```

## 🔒 Security

- **Authentication**: Keycloak OAuth2
- **Authorization**: JWT tokens with roles
- **Rate Limiting**: Redis-based (API Gateway)
- **CORS**: Configured in API Gateway
- **HTTPS**: Recommended for production

## 📝 License

This project is for educational purposes.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Contact

For questions or issues, please open an issue on GitHub.

---

**Note**: This is a development setup. For production deployment, consider:
- Using environment variables for sensitive configuration
- Setting up proper SSL/TLS certificates
- Configuring production-grade database and Redis clusters
- Implementing proper logging and monitoring
- Setting up CI/CD pipelines
- Using Kubernetes for orchestration
