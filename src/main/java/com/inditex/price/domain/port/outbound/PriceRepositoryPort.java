package com.inditex.price.domain.port.outbound;

import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;

import java.util.Optional;

public interface PriceRepositoryPort {
    Optional<Price> findApplicablePrice(PriceQuery query);
}
