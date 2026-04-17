package com.inditex.price.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceDto(
        Long brandId,
        Long productId,
        Integer priceList,
        Integer priority,
        LocalDateTime startDate,
        LocalDateTime endDate,
        BigDecimal price,
        String currency
) {}