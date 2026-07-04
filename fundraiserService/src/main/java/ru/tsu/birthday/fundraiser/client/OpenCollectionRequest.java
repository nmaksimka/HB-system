package ru.tsu.birthday.fundraiser.client;

import java.math.BigDecimal;
import java.util.UUID;

public record OpenCollectionRequest(UUID fundraiserId, UUID beneficiaryId, BigDecimal targetAmount) {}
