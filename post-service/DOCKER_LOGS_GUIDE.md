# Hướng dẫn xem Logs Docker

## 1. Xem logs Kafka

### A. Xem logs real-time (follow mode)
```bash
# Xem logs liên tục, tự động cập nhật khi có log mới
docker logs kafka -f

# Hoặc với tail (chỉ xem 30 dòng cuối)
docker logs kafka --tail 30 -f
```

### B. Xem logs từ đầu đến giờ
```bash
# Xem tất cả logs
docker logs kafka

# Xem 100 dòng cuối
docker logs kafka --tail 100

# Xem từ dòng 50 đến 100
docker logs kafka --tail 100 | Select-Object -Last 50
```

### C. Xem logs với timestamp
```bash
# Xem logs kèm timestamp
docker logs kafka -t

# Xem logs real-time với timestamp
docker logs kafka -f -t
```

### D. Xem logs từ thời điểm cụ thể
```bash
# Xem logs từ 10 phút trước
docker logs kafka --since 10m

# Xem logs từ 1 giờ trước
docker logs kafka --since 1h

# Xem logs từ thời điểm cụ thể
docker logs kafka --since "2026-01-11T10:00:00"
```

## 2. Xem logs Zookeeper

```bash
# Real-time
docker logs zookeeper -f

# 50 dòng cuối
docker logs zookeeper --tail 50

# Với timestamp
docker logs zookeeper -f -t
```

## 3. Xem logs nhiều containers cùng lúc

### PowerShell (Windows)
```powershell
# Xem logs Kafka và Zookeeper song song
docker logs kafka -f | Out-File -FilePath kafka.log
docker logs zookeeper -f | Out-File -FilePath zookeeper.log

# Hoặc dùng 2 terminal riêng
```

### Docker Compose
```bash
# Xem logs tất cả services trong docker-compose
docker-compose -f docker-compose-kafka.yml logs -f

# Xem logs chỉ Kafka
docker-compose -f docker-compose-kafka.yml logs -f kafka

# Xem logs chỉ Zookeeper
docker-compose -f docker-compose-kafka.yml logs -f zookeeper
```

## 4. Lọc logs (grep/findstr)

### Windows PowerShell
```powershell
# Tìm "ERROR" trong logs
docker logs kafka | Select-String "ERROR"

# Tìm "started" trong logs
docker logs kafka | Select-String "started"

# Real-time với filter
docker logs kafka -f | Select-String "ERROR"
```

### CMD
```cmd
# Tìm "ERROR" trong logs
docker logs kafka | findstr "ERROR"

# Tìm "started" trong logs
docker logs kafka | findstr "started"
```

## 5. Export logs ra file

```bash
# Export logs ra file
docker logs kafka > kafka.log

# Export logs với timestamp
docker logs kafka -t > kafka-with-timestamp.log

# Append vào file (thêm vào cuối file)
docker logs kafka >> kafka.log
```

## 6. Xem logs trong Docker Desktop

1. Mở Docker Desktop
2. Vào tab "Containers"
3. Click vào container `kafka` hoặc `zookeeper`
4. Tab "Logs" sẽ hiển thị logs real-time
5. Có thể filter, search, export logs

## 7. Các lệnh hữu ích

```bash
# Xem logs của container đã stop
docker logs kafka

# Xem logs với giới hạn số dòng
docker logs kafka --tail 50

# Xem logs từ container ID
docker logs f9892f6c1ac7

# Xem logs và pipe vào file
docker logs kafka -f > kafka-realtime.log
```

## 8. Troubleshooting với logs

### Kiểm tra Kafka đã start chưa
```bash
docker logs kafka | findstr "started"
# Kết quả mong đợi: "KafkaServer id=1] started"
```

### Kiểm tra lỗi
```bash
docker logs kafka | findstr "ERROR"
docker logs kafka | findstr "Exception"
```

### Kiểm tra kết nối Zookeeper
```bash
docker logs kafka | findstr "Zookeeper"
# Kết quả mong đợi: "Connected to Zookeeper"
```
