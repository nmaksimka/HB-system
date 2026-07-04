package ru.tsu.birthday.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.tsu.birthday.notification.client.IdentityClient;

@Component
public class ReminderJob {
    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);

    private final IdentityClient identity;
    private final ReminderService reminders;

    public ReminderJob(IdentityClient identity, ReminderService reminders) {
        this.identity = identity;
        this.reminders = reminders;
    }

    @Scheduled(cron = "${reminders.cron:0 0 6 * * *}", zone = "UTC")
    public void refreshAll() {
        for (var subscriberId : identity.subscribers()) {
            try {
                reminders.refresh(subscriberId);
            } catch (RuntimeException ex) {
                log.warn("Reminders for {} were not refreshed", subscriberId, ex);
            }
        }
    }
}
