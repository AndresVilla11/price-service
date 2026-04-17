package com.inditex.price.infrastructure.adapter.outbound.persistence;

import com.inditex.price.infrastructure.adapter.outbound.persistence.entity.PriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PriceJpaRepository extends JpaRepository<PriceEntity, Long> {

    @Query("""
        SELECT p FROM PriceEntity p
        WHERE p.productId = :productId
          AND p.brandId = :brandId
          AND :date BETWEEN p.startDate AND p.endDate
        ORDER BY p.priority DESC
        LIMIT 1
    """)
    Optional<PriceEntity> findTopByCriteria(
            Long productId,
            Long brandId,
            LocalDateTime date
    );
}