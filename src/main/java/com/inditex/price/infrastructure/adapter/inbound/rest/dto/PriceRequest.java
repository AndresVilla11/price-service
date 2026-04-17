package com.inditex.price.infrastructure.adapter.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record PriceRequest(
        @Schema(description = "Product identifier", example = "35455", minimum = "1")
        @NotNull @Positive
        Integer productId,

        @Schema(description = "Brand identifier (1 = ZARA)", example = "1", minimum = "1")
        @NotNull @Positive
        Integer brandId,

        @Schema(description = "Date to evaluate applicable price", example = "2020-06-14T10:00:00")
        @NotNull
        LocalDateTime applicationDate

) {
}