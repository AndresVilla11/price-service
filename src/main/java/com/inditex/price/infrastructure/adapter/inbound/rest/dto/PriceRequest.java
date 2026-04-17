package com.inditex.price.infrastructure.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record PriceRequest(
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime applicationDate,

        @NotNull
        @Positive
        Long productId,

        @NotNull
        @Positive
        Long brandId
) {
}