package com.example.ecom.auth.service;

import com.example.ecom.auth.dto.Otp;
import com.example.ecom.auth.enums.OtpType;
import com.example.ecom.common.model.User;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

import static com.example.ecom.common.utils.CacheConstants.CACHE_OTPS;

@Component
@RequiredArgsConstructor
public class OtpService {

    public static final int MAX_TRIES = 3;

    private final CacheManager cacheManager;

    public Otp createOtp(User user, OtpType type) {
        Otp otp = new Otp();

        otp.setEmail(user.getEmail());
        otp.setType(type);
        otp.setValue(generateOtpValue());

        saveOtp(type, user.getEmail(), otp);

        return otp;
    }

    public Otp getOrCreateOtp(User user, OtpType type) {
        Otp otp = getOtp(type, user.getEmail());

        if (otp != null) {
            throw new ValidationException(type.name() + " OTP already sent to user with email: " + user.getEmail());
        }

        return createOtp(user, type);
    }

    public void verifyOtp(OtpType type, int otpInput, String email) {
        Otp otp = getOtp(type, email);

        if (otp == null) {
            throw new BadCredentialsException("No OTP found");
        }

        if (otp.getTries() >= MAX_TRIES) {
            deleteOtp(type, email);
            throw new BadCredentialsException("Max tries attempted. Please request a new OTP.");
        }

        if (otp.getValue() != otpInput) {
            int newTries = otp.getTries() + 1;
            if (newTries >= MAX_TRIES) {
                deleteOtp(type, email);
                throw new BadCredentialsException("Max tries attempted. Please request a new OTP.");
            }

            otp.setTries(newTries);
            saveOtp(type, email, otp);
            throw new BadCredentialsException("Invalid OTP");
        }

        deleteOtp(type, email);
    }

    private Otp getOtp(OtpType type, String email) {
        Cache cache = getCache();
        return cache != null ? cache.get(getKey(type, email), Otp.class) : null;
    }

    private void saveOtp(OtpType type, String email, Otp otp) {
        Cache cache = getCache();
        if (cache != null) {
            cache.put(getKey(type, email), otp);
        }
    }

    private int generateOtpValue() {
        SecureRandom secureRandom = new SecureRandom();
        return 100000 + secureRandom.nextInt(900000);
    }

    private void deleteOtp(OtpType type, String email) {
        Cache cache = getCache();
        if (cache != null) {
            cache.evict(getKey(type, email));
        }
    }

    private Cache getCache() {
        return cacheManager.getCache(CACHE_OTPS);
    }

    private String getKey(OtpType type, String email) {
        return type.name() + ":" + email;
    }
}
