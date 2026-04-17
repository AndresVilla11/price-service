package com.inditex.price.domain.port.inbound;

import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;

public interface GetApplicablePricePort {
    Price execute(PriceQuery query);
}
