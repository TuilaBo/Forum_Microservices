package com.khoavdse170395.authservice.service.impl;

import com.khoavdse170395.authservice.service.OtpService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public OtpServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String type, String email) {
        return "otp:" + type + ":" + email;
    }

    private String generateOtp() {
        int num = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%06d", num);
    }

    @Override
    public String generateRegisterOtp(String email) {
        String otp = generateOtp();
        redisTemplate.opsForValue().set(key("register", email), otp, OTP_TTL);
        return otp;
    }

    @Override
    public boolean verifyRegisterOtp(String email, String otp) {
        String stored = redisTemplate.opsForValue().get(key("register", email));
        if (stored == null) return false;
        boolean ok = stored.equals(otp);
        if (ok) {
            redisTemplate.delete(key("register", email));
        }
        return ok;
    }

    @Override
    public String generateForgotPasswordOtp(String email) {
        String otp = generateOtp();
        redisTemplate.opsForValue().set(key("forgot", email), otp, OTP_TTL);
        return otp;
    }

    @Override
    public boolean verifyForgotPasswordOtp(String email, String otp) {
        String stored = redisTemplate.opsForValue().get(key("forgot", email));
        if (stored == null) return false;
        boolean ok = stored.equals(otp);
        if (ok) {
            redisTemplate.delete(key("forgot", email));
        }
        return ok;
    }
}

