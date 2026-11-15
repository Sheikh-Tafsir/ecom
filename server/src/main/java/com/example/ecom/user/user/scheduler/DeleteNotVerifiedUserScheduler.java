package com.example.ecom.user.user.scheduler;

import com.example.ecom.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteNotVerifiedUserScheduler {

    private final UserService userService;

    @Scheduled(cron = "0 0 0 * * ?")
    @SchedulerLock(name = "deleteNotVerifiedUsers", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void run() {
        userService.deleteNotVerifiedUsers();
    }
}
