package com.inditex.price.domain.model;

import java.time.LocalDateTime;

public record PriceQuery(
        Integer productId,
        Integer brandId,
        LocalDateTime applicationDate
) {
}