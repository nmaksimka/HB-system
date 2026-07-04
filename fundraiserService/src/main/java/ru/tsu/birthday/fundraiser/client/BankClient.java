package ru.tsu.birthday.fundraiser.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "bankClient", url = "${clients.bank.url}")
public interface BankClient {
    @PostMapping("/internal/collections")
    void openCollection(@RequestBody OpenCollectionRequest request);
}
