package ru.tsu.birthday.notification.mapper;

import org.springframework.stereotype.Component;
import ru.tsu.birthday.notification.dto.ReminderResponse;
import ru.tsu.birthday.notification.entity.Reminder;

@Component
public class ReminderMapper {
    public ReminderResponse toResponse(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getBirthdayUserId(),
                reminder.getBirthdayUserName(),
                reminder.getBirthdayDate(),
                reminder.getThresholdDays(),
                reminder.getCreatedAt(),
                reminder.getReadAt()
        );
    }
}
