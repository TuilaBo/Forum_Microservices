# Redis Implementation Plan - Kế Hoạch Áp Dụng Redis

## 🎯 Mục Tiêu
Áp dụng Redis vào dự án để thể hiện hiểu biết về caching, performance optimization và distributed systems - **ăn điểm với nhà tuyển dụng**.

## 📊 Use Cases Được Đề Xuất (Theo Độ Ưu Tiên)

### 🥇 **1. API Gateway - Rate Limiting** (Ưu tiên cao nhất)

**Tại sao quan trọng:**
- ✅ **Thực tế và cần thiết**: Bảo vệ API khỏi abuse
- ✅ **Thể hiện hiểu biết**: Rate limiting là use case phổ biến của Redis
- ✅ **Dễ demo**: Có thể test ngay với nhiều requests
- ✅ **Production-ready**: Luôn cần trong production

**Implementation:**
```java
// api-gateway/src/main/java/com/khoavdse170395/apigateway/filter/RateLimitFilter.java
@Component
public class RateLimitFilter implements GatewayFilter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientId = getClientId(exchange); // IP hoặc userId
        String key = "rate_limit:" + clientId;
        
        // Redis INCR với TTL
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1)); // 1 phút window
        }
        
        if (count > 100) { // 100 requests/phút
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        
        return chain.filter(exchange);
    }
}
```

**Redis Commands sử dụng:**
- `INCR` - Đếm số requests
- `EXPIRE` - Set TTL cho key
- `TTL` - Kiểm tra thời gian còn lại

**Giá trị cho interview:**
- Hiểu về distributed rate limiting
- Biết cách dùng Redis atomic operations
- Hiểu về sliding window vs fixed window

---

### 🥈 **2. User Service - Cache User Profiles** (Ưu tiên cao)

**Tại sao quan trọng:**
- ✅ **High hit rate**: User profiles được query nhiều lần
- ✅ **Dễ implement**: Wrap existing service methods
- ✅ **Performance impact rõ ràng**: Giảm DB queries đáng kể
- ✅ **Thể hiện pattern**: Cache-aside pattern

**Implementation:**
```java
// user-service/src/main/java/com/khoavdse170395/userservice/service/impl/UserServiceImpl.java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private RedisTemplate<String, UserResponse> redisTemplate;
    
    private static final String CACHE_KEY_PREFIX = "user:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    
    @Override
    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(String id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // 1. Check cache
        UserResponse cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 2. Query database
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        UserResponse response = mapToResponse(user);
        
        // 3. Update cache
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);
        
        return response;
    }
    
    @Override
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        // Update database
        UserResponse updated = ...;
        
        // Invalidate cache
        String cacheKey = CACHE_KEY_PREFIX + userId;
        redisTemplate.delete(cacheKey);
        
        return updated;
    }
}
```

**Redis Commands sử dụng:**
- `GET` - Lấy từ cache
- `SET` với TTL - Lưu vào cache
- `DEL` - Xóa cache khi update

**Giá trị cho interview:**
- Hiểu về cache-aside pattern
- Biết cách invalidate cache
- Hiểu về cache hit/miss ratio

---

### 🥉 **3. Post Service - Cache Posts** (Ưu tiên cao)

**Tại sao quan trọng:**
- ✅ **High traffic**: Posts được đọc nhiều hơn viết
- ✅ **Performance critical**: Giảm load database đáng kể
- ✅ **Thể hiện pattern**: Cache invalidation với Kafka

**Implementation:**
```java
// post-service/src/main/java/com/khoavdse170395/postservice/service/impl/PostServiceImpl.java
@Service
public class PostServiceImpl implements PostService {
    
    @Autowired
    private RedisTemplate<String, PostResponse> redisTemplate;
    
    private static final String CACHE_KEY_PREFIX = "post:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    
    @Override
    public PostResponse getPostById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // Check cache
        PostResponse cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Query database
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        PostResponse response = mapToResponse(post);
        
        // Update cache
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);
        
        return response;
    }
    
    @Override
    public PostResponse updatePost(Long id, UpdatePostRequest request, String userId) {
        // Update database
        PostResponse updated = ...;
        
        // Invalidate cache
        String cacheKey = CACHE_KEY_PREFIX + id;
        redisTemplate.delete(cacheKey);
        
        // Publish event để các service khác invalidate cache
        postEventProducer.publishPostUpdated(...);
        
        return updated;
    }
}
```

**Cache Invalidation với Kafka:**
```java
// post-service/src/main/java/com/khoavdse170395/postservice/consumer/CacheInvalidationConsumer.java
@Component
public class CacheInvalidationConsumer {
    
    @KafkaListener(topics = "post-updated")
    public void handlePostUpdated(@Payload PostUpdatedEvent event) {
        // Invalidate cache khi post được update
        String cacheKey = "post:" + event.getPostId();
        redisTemplate.delete(cacheKey);
    }
}
```

**Giá trị cho interview:**
- Hiểu về cache invalidation strategies
- Biết cách sync cache giữa services với Kafka
- Hiểu về eventual consistency

---

### 4. **Auth Service - Token Blacklist** (Ưu tiên trung bình)

**Tại sao quan trọng:**
- ✅ **Security**: Cần thiết cho logout functionality
- ✅ **Thể hiện hiểu biết**: Token management với Redis
- ✅ **Real-world**: Luôn cần trong production

**Implementation:**
```java
// auth-service/src/main/java/com/khoavdse170395/authservice/service/TokenBlacklistService.java
@Service
public class TokenBlacklistService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public void blacklistToken(String token, Duration ttl) {
        String key = "blacklist:token:" + token;
        redisTemplate.opsForValue().set(key, "1", ttl);
    }
    
    public boolean isBlacklisted(String token) {
        String key = "blacklist:token:" + token;
        return redisTemplate.hasKey(key);
    }
}

// SecurityConfig - Check blacklist trong JWT filter
@Component
public class JwtTokenFilter implements Filter {
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    @Override
    public void doFilter(...) {
        String token = extractToken(request);
        
        if (tokenBlacklistService.isBlacklisted(token)) {
            response.setStatus(401);
            return;
        }
        
        // Continue với JWT validation
    }
}
```

**Redis Commands sử dụng:**
- `SET` với TTL - Lưu blacklisted token
- `EXISTS` - Kiểm tra token có trong blacklist

**Giá trị cho interview:**
- Hiểu về token management
- Biết cách implement logout với stateless JWT
- Hiểu về security best practices

---

### 5. **Notification Service - Real-time với Pub/Sub** (Ưu tiên trung bình)

**Tại sao quan trọng:**
- ✅ **Thể hiện hiểu biết**: Redis Pub/Sub là advanced feature
- ✅ **Real-time**: Push notifications đến clients
- ✅ **Scalable**: Có thể scale notification service

**Implementation:**
```java
// notification-service/src/main/java/com/khoavdse170395/notificationservice/service/RedisNotificationPublisher.java
@Service
public class RedisNotificationPublisher {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void publishNotification(String userId, Notification notification) {
        String channel = "notifications:" + userId;
        redisTemplate.convertAndSend(channel, notification);
    }
}

// WebSocket handler subscribe Redis channel
@Component
public class NotificationWebSocketHandler {
    
    @Autowired
    private RedisMessageListenerContainer messageListenerContainer;
    
    public void subscribeToUserNotifications(String userId, WebSocketSession session) {
        String channel = "notifications:" + userId;
        
        messageListenerContainer.addMessageListener(
            (message, pattern) -> {
                Notification notification = (Notification) message.getBody();
                session.sendMessage(new TextMessage(notification.toJson()));
            },
            new ChannelTopic(channel)
        );
    }
}
```

**Redis Commands sử dụng:**
- `PUBLISH` - Gửi message đến channel
- `SUBSCRIBE` - Subscribe channel để nhận messages

**Giá trị cho interview:**
- Hiểu về Redis Pub/Sub
- Biết cách implement real-time features
- Hiểu về message queue patterns

---

### 6. **Comment Service - Cache Comments** (Ưu tiên thấp)

**Tại sao quan trọng:**
- ✅ **Performance**: Comments được load nhiều
- ✅ **Pattern**: Cache với pagination

**Implementation:**
```java
// comment-service/src/main/java/com/khoavdse170395/commentservice/service/impl/CommentServiceImpl.java
@Override
public Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable) {
    String cacheKey = "comments:post:" + postId + ":page:" + pageable.getPageNumber();
    
    // Cache page results
    Page<CommentResponse> cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
        return cached;
    }
    
    Page<CommentResponse> comments = commentRepository.findByPostId(postId, pageable)
        .map(this::mapToResponse);
    
    redisTemplate.opsForValue().set(cacheKey, comments, Duration.ofMinutes(10));
    
    return comments;
}
```

---

## 🎯 Kế Hoạch Triển Khai (Theo Thứ Tự)

### Phase 1: Rate Limiting (1-2 ngày)
1. Setup Redis trong API Gateway
2. Implement RateLimitFilter
3. Test với nhiều concurrent requests
4. **Demo**: Show rate limiting hoạt động

### Phase 2: User Profile Caching (1 ngày)
1. Setup Redis trong User Service
2. Implement cache cho getUserById, getUserByUsername
3. Implement cache invalidation
4. **Demo**: Show cache hit/miss, performance improvement

### Phase 3: Post Caching (1-2 ngày)
1. Setup Redis trong Post Service
2. Implement cache cho getPostById
3. Implement cache invalidation với Kafka
4. **Demo**: Show cache invalidation khi update post

### Phase 4: Token Blacklist (1 ngày)
1. Setup Redis trong Auth Service
2. Implement TokenBlacklistService
3. Integrate với logout endpoint
4. **Demo**: Show logout invalidates token

### Phase 5: Real-time Notifications (Optional - 2-3 ngày)
1. Setup Redis Pub/Sub
2. Implement WebSocket handler
3. Test real-time notifications
4. **Demo**: Show real-time notifications

---

## 📝 Dependencies Cần Thêm

### API Gateway
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

### User Service, Post Service, Auth Service
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### Notification Service
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

---

## 🎤 Câu Trả Lời Cho Interview

### "Tại sao bạn chọn Redis?"

**Trả lời:**
1. **Performance**: Redis là in-memory database, tốc độ rất nhanh (sub-millisecond)
2. **Data Structures**: Hỗ trợ nhiều data structures (String, Hash, List, Set, Sorted Set) phù hợp với nhiều use cases
3. **Atomic Operations**: Đảm bảo consistency trong distributed systems
4. **Pub/Sub**: Hỗ trợ real-time messaging
5. **TTL**: Tự động expire data, không cần cleanup manual

### "Bạn đã áp dụng Redis ở đâu?"

**Trả lời:**
1. **API Gateway - Rate Limiting**: Bảo vệ API khỏi abuse, sử dụng INCR với TTL
2. **User Service - Caching**: Cache user profiles để giảm database load, improve response time
3. **Post Service - Caching**: Cache posts với invalidation qua Kafka events
4. **Auth Service - Token Blacklist**: Quản lý blacklisted tokens khi logout
5. **Notification Service - Pub/Sub**: Real-time notifications với Redis Pub/Sub

### "Làm thế nào bạn đảm bảo cache consistency?"

**Trả lời:**
1. **Cache Invalidation**: Xóa cache khi data được update
2. **TTL**: Set expiration time để đảm bảo data không quá cũ
3. **Event-driven**: Sử dụng Kafka events để sync cache giữa services
4. **Cache-aside Pattern**: Luôn check cache trước, update cache sau khi query DB

---

## ✅ Checklist Trước Khi Demo

- [ ] Redis đã được setup và chạy
- [ ] Rate limiting hoạt động (test với nhiều requests)
- [ ] User caching hoạt động (check cache hit/miss)
- [ ] Post caching với invalidation hoạt động
- [ ] Token blacklist hoạt động (logout test)
- [ ] Metrics/logs để show performance improvement
- [ ] Documentation về Redis implementation

---

## 🎯 Kết Luận

**Ưu tiên implement:**
1. **Rate Limiting** - Dễ, thực tế, dễ demo
2. **User/Post Caching** - High impact, thể hiện pattern
3. **Token Blacklist** - Security, production-ready
4. **Pub/Sub** - Advanced, impressive

**Tổng thời gian:** 5-7 ngày để implement tất cả

**Giá trị cho interview:** ⭐⭐⭐⭐⭐
- Thể hiện hiểu biết về caching strategies
- Biết cách optimize performance
- Hiểu về distributed systems
- Có kinh nghiệm với Redis trong production
