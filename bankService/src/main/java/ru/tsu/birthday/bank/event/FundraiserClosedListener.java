package ru.tsu.birthday.bank.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.tsu.birthday.bank.service.BankService;
import tools.jackson.databind.json.JsonMapper;

@Component
public class FundraiserClosedListener {
    private final BankService service;
    private final JsonMapper json;

    public FundraiserClosedListener(BankService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = FundraiserClosed.TOPIC)
    public void onClosed(String message) {
        service.closeCollection(json.readValue(message, FundraiserClosed.class));
    }
}
