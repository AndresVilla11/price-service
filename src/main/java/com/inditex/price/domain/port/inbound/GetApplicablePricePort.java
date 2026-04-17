package com.inditex.price.domain.port.inbound;

import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceRequest;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceResponse;

public interface GetApplicablePricePort {
    PriceResponse execute(PriceRequest request);
}
