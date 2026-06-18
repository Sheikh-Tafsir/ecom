package com.example.demo.auth.service;

import com.example.demo.auth.dto.Otp;
import com.example.demo.auth.enums.OtpType;
import com.example.demo.common.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OtpService {

    public static final int MAX_TRIES = 3;
    public static final int OTP_VALIDITY_INT_MINUTES = 5;

    private final RedisTemplate<String, Object> redisTemplate;

    public Otp createOtp(User user, OtpType type) {
        Otp otp = new Otp();

        otp.setEmail(user.getEmail());
        otp.setType(type);
        otp.setValue(generateOtpValue());

        redisTemplate.opsForValue().set(getKey(type, user.getEmail()), otp, Duration.ofMinutes(OTP_VALIDITY_INT_MINUTES));

        return otp;
    }

    public Otp getOrCreateOtp(User user, OtpType type) {
        Otp otp = getOtp(type, user.getEmail());

        if (otp != null) {
            return otp;
        }

        return createOtp(user, type);
    }

    public void verifyOtp(OtpType type, int otpInput, String email) {
        Otp otp = getOtp(type, email);

        if (otp == null) {
            throw new BadCredentialsException("No OTP found");
        }

        if (otp.getTries() >= MAX_TRIES) {
            throw new BadCredentialsException("Max tries attempted");
        }

        if (otp.getValue() != otpInput) {
            otp.setTries(otp.getTries() + 1);
            throw new BadCredentialsException("Invalid OTP");
        }

        deleteOtp(type, email);
    }

    private Otp getOtp(OtpType type, String email) {
        return (Otp) redisTemplate.opsForValue().get(getKey(type, email));
    }

    private int generateOtpValue() {
        SecureRandom secureRandom = new SecureRandom();
        return 100000 + secureRandom.nextInt(900000);
    }

    private void deleteOtp(OtpType type, String email) {
        redisTemplate.delete(getKey(type, email));
    }

    private String getKey(OtpType type, String email) {
        return "OTP:" + type.name() + ":" + email;
    }
}
