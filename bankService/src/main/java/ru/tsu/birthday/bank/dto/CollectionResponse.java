package ru.tsu.birthday.bank.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CollectionResponse(UUID fundraiserId, BigDecimal targetAmount, BigDecimal paidAmount, boolean open) {}
