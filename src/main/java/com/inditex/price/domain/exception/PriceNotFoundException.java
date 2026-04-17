package com.inditex.price.domain.exception;

import java.time.LocalDateTime;

public class PriceNotFoundException extends RuntimeException {

    public PriceNotFoundException(Integer productId, Integer brandId, LocalDateTime date) {
        super("No applicable price found for productId=%d, brandId=%d, date=%s"
                .formatted(productId, brandId, date));
    }
}