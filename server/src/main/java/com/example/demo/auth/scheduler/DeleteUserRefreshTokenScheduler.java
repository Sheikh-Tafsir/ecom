package com.example.demo.auth.scheduler;

import com.example.demo.auth.service.UserRefreshTokenService;
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
