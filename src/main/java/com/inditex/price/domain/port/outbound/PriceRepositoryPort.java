package com.inditex.price.domain.port.outbound;

import com.inditex.price.domain.model.PriceDto;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PriceRepositoryPort {
    Optional<PriceDto> findApplicablePrice(
            Long productId,
            Long brandId,
            LocalDateTime applicationDate
    );
}
