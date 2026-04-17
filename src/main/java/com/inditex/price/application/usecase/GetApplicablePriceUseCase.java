package com.inditex.price.application.usecase;

import com.inditex.price.domain.port.inbound.GetApplicablePricePort;
import com.inditex.price.domain.port.outbound.PriceRepositoryPort;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceRequest;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceResponse;
import org.springframework.stereotype.Service;

@Service
public class GetApplicablePriceUseCase implements GetApplicablePricePort {

    private final PriceRepositoryPort priceRepositoryPort;

    public GetApplicablePriceUseCase(PriceRepositoryPort priceRepositoryPort) {
        this.priceRepositoryPort = priceRepositoryPort;
    }

    @Override
    public PriceResponse execute(PriceRequest request) {
        return null;
    }
}
