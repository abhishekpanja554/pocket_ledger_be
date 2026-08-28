package com.abhout.pocket_ledger_be.sync;

import com.abhout.pocket_ledger_be.setting.SettingService;
import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DriveSyncScheduler {
    private  static final Logger logger = LoggerFactory.getLogger(DriveSyncScheduler.class);
    private final DriveSyncRunner runner;
    private final SettingService settingService;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void runDailySync(){
        List<User> users = settingService.getUsersWithDriveFolderConfigured();
        for (User user : users) {
            try {
                runner.runSyncForUser(user);
            } catch ( RuntimeException e){
                logger.error("Drive sync failed for the user: {}", user.getId(), e);
            }
        }
    }
}
