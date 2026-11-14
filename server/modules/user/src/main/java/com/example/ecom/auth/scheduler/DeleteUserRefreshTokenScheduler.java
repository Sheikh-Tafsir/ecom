package com.example.ecom.auth.scheduler;

import com.example.ecom.auth.service.AuthTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteUserRefreshTokenScheduler {

    private final AuthTokenService authTokenService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void run() {
        authTokenService.deleteRevokedRefreshTokens(1);
    }
}
