package ru.tsu.birthday.bank.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.tsu.birthday.bank.service.BankService;
import tools.jackson.databind.json.JsonMapper;

@Component
public class FundraiserEventsListener {
    private final BankService service;
    private final JsonMapper json;

    public FundraiserEventsListener(BankService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = FundraiserClosed.TOPIC)
    public void onClosed(String message) {
        service.closeCollection(json.readValue(message, FundraiserClosed.class));
    }

    @KafkaListener(topics = PaymentRejected.TOPIC)
    public void onRejected(String message) {
        service.refund(json.readValue(message, PaymentRejected.class));
    }
}
