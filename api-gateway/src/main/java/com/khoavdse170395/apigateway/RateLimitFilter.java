package com.khoavdse170395.apigateway;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final int LIMIT = 10; // 10 requests / minute / IP
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = getClientIp(exchange);
        String key = "rate:" + ip;

        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Boolean> ttlMono = count == 1
                            ? redisTemplate.expire(key, WINDOW)
                            : Mono.just(true);

                    return ttlMono.then(
                            count > LIMIT
                                    ? reject(exchange)
                                    : chain.filter(exchange)
                    );
                });
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    private String getClientIp(ServerWebExchange exchange) {
        if (exchange.getRequest().getHeaders().getFirst("X-Forwarded-For") != null) {
            return exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        }
        if (exchange.getRequest().getRemoteAddress() != null &&
                exchange.getRequest().getRemoteAddress().getAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        // Chạy khá sớm nhưng sau các filter nội bộ của Gateway
        return -1;
    }
}

