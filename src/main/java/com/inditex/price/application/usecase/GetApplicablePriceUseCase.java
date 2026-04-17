package com.inditex.price.application.usecase;

import com.inditex.price.domain.exception.PriceNotFoundException;
import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;
import com.inditex.price.domain.port.inbound.GetApplicablePricePort;
import com.inditex.price.domain.port.outbound.PriceRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class GetApplicablePriceUseCase implements GetApplicablePricePort {

    private final PriceRepositoryPort priceRepositoryPort;

    public GetApplicablePriceUseCase(PriceRepositoryPort priceRepositoryPort) {
        this.priceRepositoryPort = priceRepositoryPort;
    }

    @Override
    public Price execute(PriceQuery query) {
        return priceRepositoryPort.findApplicablePrice(query)
                .orElseThrow(() -> new PriceNotFoundException(
                        query.productId(), query.brandId(), query.applicationDate()
                ));
    }
}
