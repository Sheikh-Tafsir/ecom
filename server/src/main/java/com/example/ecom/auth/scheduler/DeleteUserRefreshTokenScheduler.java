package com.example.ecom.auth.scheduler;

import com.example.ecom.auth.service.UserRefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteUserRefreshTokenScheduler {

    private final UserRefreshTokenService service;

    @Scheduled(cron = "0 0 0 * * ?")
    public void run() {
        service.deleteRevoked();
    }
}
