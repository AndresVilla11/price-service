package com.inditex.price.utils;

import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceResponse;
import com.inditex.price.infrastructure.adapter.outbound.persistence.entity.PriceEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ObjectMock {

    public static final LocalDateTime DATE_14_AT_16 = LocalDateTime.of(2020, 6, 14, 16, 0);
    public static final LocalDateTime DATE_14_AT_10 = LocalDateTime.of(2020, 6, 14, 10, 0);

    public static PriceEntity buildEntity(int priceList, int priority, String price,
                                          LocalDateTime start, LocalDateTime end) {
        return PriceEntity.builder()
                .productId(35455)
                .brandId(1)
                .priceList(priceList)
                .priority(priority)
                .startDate(start)
                .endDate(end)
                .price(new BigDecimal(price))
                .currency("EUR")
                .build();
    }

    public static Price buildDomain(int priceList, String price,
                                    LocalDateTime start, LocalDateTime end) {
        return new Price(35455, 1, priceList, start, end, new BigDecimal(price), "EUR");
    }

    public static Price priceWith(int priceList, String amount) {
        return new Price(
                35455,
                1,
                priceList,
                LocalDateTime.of(2020, 6, 14, 0, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59, 59),
                new BigDecimal(amount),
                "EUR"
        );
    }

    public static PriceQuery priceQueryFull200Ok() {
        return new PriceQuery(
                35455,
                1,
                LocalDateTime.of(2020, 6, 14, 10, 0));
    }

    public static PriceResponse priceResponseFull200Ok() {
        return new PriceResponse(
                35455,
                1,
                1,
                LocalDateTime.of(2020, 6, 14, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59, 59),
                new BigDecimal("35.50"),
                "EUR");
    }

    public static PriceResponse priceResponse200Ok() {
        return new PriceResponse(
                35455,
                1,
                2,
                LocalDateTime.of(2020, 6, 14, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59, 59),
                new BigDecimal("25.45"),
                "EUR");
    }
}
