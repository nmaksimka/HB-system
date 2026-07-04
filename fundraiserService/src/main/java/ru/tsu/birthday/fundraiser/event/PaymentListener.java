package ru.tsu.birthday.fundraiser.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.tsu.birthday.fundraiser.service.FundraiserService;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PaymentListener {
    private final FundraiserService service;
    private final JsonMapper json;

    public PaymentListener(FundraiserService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = PaymentSucceeded.TOPIC)
    public void onPayment(String message) {
        service.applyPayment(json.readValue(message, PaymentSucceeded.class));
    }
}
