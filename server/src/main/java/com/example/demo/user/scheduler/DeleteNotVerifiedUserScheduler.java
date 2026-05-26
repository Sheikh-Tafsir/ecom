package com.example.demo.user.scheduler;

import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteNotVerifiedUserScheduler {

    private final UserService userService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void run() {
        userService.deleteNotVerifiedUsers();
    }
}
