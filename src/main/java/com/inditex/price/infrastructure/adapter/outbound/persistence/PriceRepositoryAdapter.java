package com.inditex.price.infrastructure.adapter.outbound.persistence;

import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;
import com.inditex.price.domain.port.outbound.PriceRepositoryPort;
import com.inditex.price.infrastructure.adapter.outbound.persistence.mapper.EntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public final class PriceRepositoryAdapter implements PriceRepositoryPort {

    private final PriceJpaRepository priceJpaRepository;
    private final EntityMapper entityMapper;

    public PriceRepositoryAdapter(PriceJpaRepository priceJpaRepository, EntityMapper entityMapper) {
        this.priceJpaRepository = priceJpaRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    public Optional<Price> findApplicablePrice(PriceQuery query) {
        return priceJpaRepository.findTopByCriteria(
                        query.productId(),
                        query.brandId(),
                        query.applicationDate())
                .map(entityMapper::toDomain);
    }
}
