package com.inditex.price.infrastructure.adapter.outbound.persistence;

import com.inditex.price.domain.model.PriceDto;
import com.inditex.price.domain.port.outbound.PriceRepositoryPort;
import com.inditex.price.infrastructure.adapter.outbound.persistence.mapper.PriceMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class PriceRepositoryAdapter implements PriceRepositoryPort {

    private final PriceJpaRepository priceJpaRepository;
    private final PriceMapper priceMapper;

    public PriceRepositoryAdapter(PriceJpaRepository priceJpaRepository, PriceMapper priceMapper) {
        this.priceJpaRepository = priceJpaRepository;
        this.priceMapper = priceMapper;
    }

    @Override
    public Optional<PriceDto> findApplicablePrice(Long productId, Long brandId, LocalDateTime applicationDate) {
        priceJpaRepository.findTopByCriteria(productId, brandId, applicationDate)
                .map(priceMapper::toDomain);
        return Optional.empty();
    }
}
