package com.inditex.price.infrastructure.adapter.inbound.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceResponse(

        @Schema(description = "Product identifier", example = "35455")
        Integer productId,

        @Schema(description = "Brand identifier", example = "1")
        Integer brandId,

        @Schema(description = "Applicable price list id", example = "1")
        Integer priceList,

        @Schema(description = "Price validity start date", example = "2020-06-14T00:00:00")
        LocalDateTime startDate,

        @Schema(description = "Price validity end date", example = "2020-12-31T23:59:59")
        LocalDateTime endDate,

        @Schema(description = "Final applicable price", example = "35.50")
        BigDecimal price,

        @Schema(description = "ISO 4217 currency code", example = "EUR")
        String currency

) {
}