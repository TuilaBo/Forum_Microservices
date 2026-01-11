# Script kiểm tra và xử lý Kafka containers

Write-Host "=== KIỂM TRA KAFKA CONTAINERS ===" -ForegroundColor Cyan

# 1. Kiểm tra tất cả containers có tên chứa "kafka"
Write-Host "`n1. Containers có tên chứa 'kafka':" -ForegroundColor Yellow
docker ps -a --filter "name=kafka" --format "table {{.ID}}\t{{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}"

# 2. Kiểm tra port 9092
Write-Host "`n2. Kiểm tra port 9092:" -ForegroundColor Yellow
$port9092 = netstat -ano | findstr :9092
if ($port9092) {
    Write-Host "Port 9092 đang được sử dụng:" -ForegroundColor Red
    Write-Host $port9092
} else {
    Write-Host "Port 9092 trống" -ForegroundColor Green
}

# 3. Kiểm tra port 2181 (Zookeeper)
Write-Host "`n3. Kiểm tra port 2181 (Zookeeper):" -ForegroundColor Yellow
$port2181 = netstat -ano | findstr :2181
if ($port2181) {
    Write-Host "Port 2181 đang được sử dụng:" -ForegroundColor Red
    Write-Host $port2181
} else {
    Write-Host "Port 2181 trống" -ForegroundColor Green
}

# 4. Hướng dẫn
Write-Host "`n=== HƯỚNG DẪN ===" -ForegroundColor Cyan
Write-Host "`nNếu đã có Kafka container chạy trước đó:" -ForegroundColor Yellow
Write-Host "  Option 1: Dùng container cũ (khuyến nghị)" -ForegroundColor Green
Write-Host "    - Kiểm tra container name: docker ps -a | findstr kafka"
Write-Host "    - Start container cũ: docker start <container-name>"
Write-Host "    - Không cần chạy docker-compose"
Write-Host ""
Write-Host "  Option 2: Dừng container cũ và dùng docker-compose" -ForegroundColor Yellow
Write-Host "    - Stop container cũ: docker stop <container-name>"
Write-Host "    - Remove container cũ: docker rm <container-name>"
Write-Host "    - Chạy docker-compose: docker-compose -f docker-compose-kafka.yml up -d"
Write-Host ""
Write-Host "  Option 3: Dùng container cũ nhưng đổi port trong docker-compose" -ForegroundColor Yellow
Write-Host "    - Sửa port trong docker-compose-kafka.yml (ví dụ: 9093:9092)"
Write-Host "    - Sửa application.properties: spring.kafka.bootstrap-servers=localhost:9093"
