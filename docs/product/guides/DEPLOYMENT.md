# StreamGuard Deployment Guide

Production deployment guide for StreamGuard with Docker, Kubernetes, and cloud platforms.

## Deployment Options

1. **Docker Compose** - Single-node deployment
2. **Kubernetes** - Multi-node orchestration
3. **AWS/GCP/Azure** - Cloud-native deployment

---

## Docker Compose Deployment

### Production docker-compose.yml

```yaml
version: '3.8'

services:
  zookeeper:
    image: zookeeper:3.8
    container_name: streamguard-zk-prod
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zk-data:/data
      - zk-logs:/datalog
    restart: unless-stopped

  kafka:
    image: wurstmeister/kafka:latest
    container_name: streamguard-kafka-prod
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_HOST_NAME: kafka
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_CREATE_TOPICS: "security-events:4:2"  # 4 partitions, 2 replicas
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'false'
      KAFKA_LOG_RETENTION_HOURS: 168  # 7 days
      KAFKA_LOG_SEGMENT_BYTES: 1073741824  # 1GB
      KAFKA_COMPRESSION_TYPE: lz4
    volumes:
      - kafka-data:/kafka
    depends_on:
      - zookeeper
    restart: unless-stopped

  prometheus:
    image: prom/prometheus:v2.48.0
    container_name: streamguard-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
    restart: unless-stopped

  grafana:
    image: grafana/grafana:10.2.0
    container_name: streamguard-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:-admin}
      GF_INSTALL_PLUGINS: grafana-piechart-panel
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
    depends_on:
      - prometheus
    restart: unless-stopped

volumes:
  zk-data:
  zk-logs:
  kafka-data:
  prometheus-data:
  grafana-data:
```

### Prometheus Configuration

**File:** `prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'streamguard-processor'
    static_configs:
      - targets: ['host.docker.internal:8080']
        labels:
          instance: 'processor-1'

  - job_name: 'streamguard-query-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8081']
        labels:
          instance: 'api-1'

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

rule_files:
  - 'alerts.yml'
```

### Alert Rules

**File:** `alerts.yml`

```yaml
groups:
  - name: streamguard
    interval: 30s
    rules:
      - alert: HighAnomalyRate
        expr: rate(streamguard_anomalies_detected_total[5m]) > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High anomaly detection rate"
          description: "Anomaly rate {{ $value }}/s exceeds threshold"

      - alert: ProcessorDown
        expr: up{job="streamguard-processor"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Stream processor is down"

      - alert: HighProcessingLatency
        expr: histogram_quantile(0.95, rate(streamguard_processing_latency_seconds_bucket[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High processing latency"
```

### Starting Production Stack

```bash
# Create required files
mkdir -p grafana/{dashboards,datasources}

# Start services
docker-compose -f docker-compose.prod.yml up -d

# Verify all services running
docker-compose ps

# Check logs
docker-compose logs -f kafka
docker-compose logs -f prometheus
```

---

## Kubernetes Deployment

### Prerequisites

```bash
# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

# Install Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Verify cluster access
kubectl cluster-info
```

### Namespace Setup

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: streamguard
  labels:
    name: streamguard
```

```bash
kubectl apply -f namespace.yaml
```

### Kafka Deployment (Helm)

```bash
# Add Bitnami repo
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Install Kafka
helm install kafka bitnami/kafka \
  --namespace streamguard \
  --set replicaCount=3 \
  --set persistence.size=100Gi \
  --set logRetentionHours=168 \
  --set numPartitions=8 \
  --set defaultReplicationFactor=2
```

### Stream Processor Deployment

**File:** `k8s/stream-processor-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: streamguard-processor
  namespace: streamguard
spec:
  replicas: 3
  selector:
    matchLabels:
      app: streamguard-processor
  template:
    metadata:
      labels:
        app: streamguard-processor
    spec:
      containers:
      - name: processor
        image: streamguard/stream-processor:latest
        env:
        - name: KAFKA_BROKER
          value: "kafka.streamguard.svc.cluster.local:9092"
        - name: KAFKA_TOPIC
          value: "security-events"
        - name: CONSUMER_GROUP
          value: "streamguard-processors"
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: streamguard-secrets
              key: openai-api-key
        - name: DB_PATH
          value: "/data/events.db"
        - name: METRICS_PORT
          value: "8080"
        ports:
        - containerPort: 8080
          name: metrics
        volumeMounts:
        - name: data
          mountPath: /data
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /metrics
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /metrics
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
      volumes:
      - name: data
        persistentVolumeClaim:
          claimName: processor-data-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: streamguard-processor
  namespace: streamguard
spec:
  selector:
    app: streamguard-processor
  ports:
  - port: 8080
    targetPort: 8080
    name: metrics
  type: ClusterIP
```

### Query API Deployment

**File:** `k8s/query-api-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: streamguard-query-api
  namespace: streamguard
spec:
  replicas: 2
  selector:
    matchLabels:
      app: streamguard-query-api
  template:
    metadata:
      labels:
        app: streamguard-query-api
    spec:
      containers:
      - name: query-api
        image: streamguard/query-api:latest
        env:
        - name: ROCKSDB_PATH
          value: "/data/events.db"
        - name: SERVER_PORT
          value: "8081"
        - name: ROCKSDB_READ_ONLY
          value: "true"
        ports:
        - containerPort: 8081
          name: http
        volumeMounts:
        - name: data
          mountPath: /data
          readOnly: true
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 10
          periodSeconds: 5
      volumes:
      - name: data
        persistentVolumeClaim:
          claimName: processor-data-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: streamguard-query-api
  namespace: streamguard
spec:
  selector:
    app: streamguard-query-api
  ports:
  - port: 80
    targetPort: 8081
    name: http
  type: LoadBalancer
```

### Persistent Volume

**File:** `k8s/persistent-volume.yaml`

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: processor-data-pvc
  namespace: streamguard
spec:
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: 500Gi
  storageClassName: fast-ssd
```

### Secrets

```bash
# Create secret for OpenAI API key
kubectl create secret generic streamguard-secrets \
  --from-literal=openai-api-key='sk-proj-...' \
  --namespace streamguard
```

### Deploy All Resources

```bash
# Apply all manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/persistent-volume.yaml
kubectl apply -f k8s/stream-processor-deployment.yaml
kubectl apply -f k8s/query-api-deployment.yaml

# Verify deployments
kubectl get pods -n streamguard
kubectl get svc -n streamguard

# Check logs
kubectl logs -f -n streamguard -l app=streamguard-processor
```

---

## AWS Deployment

### Architecture

```
VPC
├── Public Subnet (us-east-1a)
│   ├── NAT Gateway
│   └── Application Load Balancer
│
├── Private Subnet (us-east-1a)
│   ├── EKS Worker Nodes
│   ├── MSK Kafka Cluster
│   └── EFS (RocksDB storage)
│
└── Private Subnet (us-east-1b)
    ├── EKS Worker Nodes
    ├── MSK Kafka Cluster
    └── EFS Mount Targets
```

### EKS Cluster Setup

```bash
# Install eksctl
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin

# Create cluster
eksctl create cluster \
  --name streamguard \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type m5.xlarge \
  --nodes 3 \
  --nodes-min 2 \
  --nodes-max 6 \
  --managed

# Configure kubectl
aws eks update-kubeconfig --name streamguard --region us-east-1
```

### Amazon MSK (Kafka)

```bash
# Create MSK cluster via AWS Console or CLI
aws kafka create-cluster \
  --cluster-name streamguard-kafka \
  --broker-node-group-info file://broker-info.json \
  --kafka-version 3.6.0 \
  --number-of-broker-nodes 3

# Get bootstrap servers
aws kafka get-bootstrap-brokers --cluster-arn <cluster-arn>
```

### EFS for RocksDB

```bash
# Create EFS file system
aws efs create-file-system \
  --performance-mode generalPurpose \
  --throughput-mode bursting \
  --tags Key=Name,Value=streamguard-rocksdb

# Create mount targets in each AZ
aws efs create-mount-target \
  --file-system-id <fs-id> \
  --subnet-id <subnet-id> \
  --security-groups <sg-id>

# Install EFS CSI driver in EKS
kubectl apply -k "github.com/kubernetes-sigs/aws-efs-csi-driver/deploy/kubernetes/overlays/stable/?ref=release-1.7"
```

---

## Monitoring Setup

### Grafana Dashboards

Import pre-built dashboard JSON:

```bash
# StreamGuard Overview Dashboard
curl -o dashboard.json https://raw.githubusercontent.com/yourusername/streamguard/main/grafana/dashboards/overview.json

# Import via Grafana UI or API
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @dashboard.json
```

### Key Panels

1. **Event Processing Rate** - Events/second
2. **Anomaly Detection Rate** - Anomalies/second by severity
3. **AI Analysis Latency** - p50, p95, p99
4. **Storage Growth** - RocksDB size over time
5. **Consumer Lag** - Kafka offset lag
6. **Error Rates** - Failed events, API errors

---

## Backup & Disaster Recovery

### RocksDB Backup

```bash
#!/bin/bash
# backup-rocksdb.sh

BACKUP_DIR="/backups/rocksdb/$(date +%Y%m%d-%H%M%S)"
DB_PATH="/data/events.db"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Stop writes (optional, for consistency)
# kill -SIGSTOP $(pgrep stream-processor)

# Copy database files
cp -r "$DB_PATH" "$BACKUP_DIR/"

# Resume writes
# kill -SIGCONT $(pgrep stream-processor)

# Compress backup
tar -czf "$BACKUP_DIR.tar.gz" "$BACKUP_DIR"
rm -rf "$BACKUP_DIR"

# Upload to S3
aws s3 cp "$BACKUP_DIR.tar.gz" s3://streamguard-backups/

echo "Backup completed: $BACKUP_DIR.tar.gz"
```

### Kafka Offset Backup

```bash
# Export consumer group offsets
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group streamguard-processors \
  --describe \
  --offsets > offsets-backup.txt
```

### Restore Procedure

```bash
# 1. Stop all processors
kubectl scale deployment/streamguard-processor --replicas=0

# 2. Download backup
aws s3 cp s3://streamguard-backups/20241008-120000.tar.gz .

# 3. Extract and restore
tar -xzf 20241008-120000.tar.gz
cp -r 20241008-120000/events.db /data/

# 4. Reset Kafka offsets (if needed)
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group streamguard-processors \
  --topic security-events \
  --reset-offsets --to-offset 12345 \
  --execute

# 5. Restart processors
kubectl scale deployment/streamguard-processor --replicas=3
```

---

## Performance Tuning

### Kafka Optimization

```properties
# Producer settings (event sources)
compression.type=lz4
batch.size=32768
linger.ms=10
buffer.memory=67108864

# Consumer settings (stream processor)
fetch.min.bytes=1024
fetch.max.wait.ms=100
max.poll.records=500
session.timeout.ms=30000
```

### RocksDB Tuning

```cpp
rocksdb::Options options;

// Write optimization
options.write_buffer_size = 128 * 1024 * 1024;  // 128MB
options.max_write_buffer_number = 4;
options.min_write_buffer_number_to_merge = 2;

// Read optimization
options.block_cache = rocksdb::NewLRUCache(1024 * 1024 * 1024);  // 1GB
options.bloom_filter_bits_per_key = 10;

// Compaction optimization
options.max_background_jobs = 8;
options.level0_file_num_compaction_trigger = 2;
```

### JVM Tuning (Query API)

```bash
java -jar query-api-1.0.0.jar \
  -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -XX:+UseStringDeduplication
```

---

## Security Best Practices

1. **Network Security:**
   - Use VPC with private subnets
   - Enable encryption in transit (TLS)
   - Configure security groups restrictively

2. **Secrets Management:**
   - Use AWS Secrets Manager or HashiCorp Vault
   - Rotate API keys regularly
   - Never commit secrets to Git

3. **Authentication:**
   - Implement API key authentication
   - Use mTLS for internal communication
   - Enable Kafka SASL/SCRAM

4. **Monitoring:**
   - Enable CloudWatch/Stackdriver logs
   - Set up security alerts
   - Monitor unauthorized access attempts

---

## Scaling Guidelines

| Load (events/sec) | Processors | Kafka Partitions | Query API Replicas |
|-------------------|------------|------------------|-------------------|
| 1K - 10K | 1-2 | 4 | 2 |
| 10K - 50K | 2-4 | 8 | 3 |
| 50K - 100K | 4-8 | 16 | 4 |
| 100K+ | 8+ | 32+ | 6+ |

**Auto-scaling (Kubernetes):**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: streamguard-processor-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: streamguard-processor
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: kafka_consumer_lag
      target:
        type: AverageValue
        averageValue: "1000"
```
