package com.inditex.price.infrastructure.adapter.outbound.persistence;

import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;
import com.inditex.price.infrastructure.adapter.outbound.persistence.entity.PriceEntity;
import com.inditex.price.infrastructure.adapter.outbound.persistence.mapper.EntityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.inditex.price.utils.ObjectMock.DATE_14_AT_10;
import static com.inditex.price.utils.ObjectMock.DATE_14_AT_16;
import static com.inditex.price.utils.ObjectMock.buildDomain;
import static com.inditex.price.utils.ObjectMock.buildEntity;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceRepositoryAdapter — unit")
class PriceRepositoryAdapterTest {

    @Mock
    private PriceJpaRepository priceJpaRepository;

    @Mock
    private EntityMapper entityMapper;

    @InjectMocks
    private PriceRepositoryAdapter adapter;

    @Nested
    @DisplayName("when JPA returns a result")
    class WhenJpaReturnsResult {

        @Test
        @DisplayName("delegates to JPA repository with exact query parameters")
        void delegatesExactParametersToJpa() {
            PriceEntity entity = buildEntity(2, 1, "25.45",
                    LocalDateTime.of(2020, 6, 14, 15, 0),
                    LocalDateTime.of(2020, 6, 14, 18, 30));
            when(priceJpaRepository.findTopByCriteria(35455, 1, DATE_14_AT_16))
                    .thenReturn(Optional.of(entity));
            when(entityMapper.toDomain(entity))
                    .thenReturn(buildDomain(2, "25.45",
                            LocalDateTime.of(2020, 6, 14, 15, 0),
                            LocalDateTime.of(2020, 6, 14, 18, 30)));

            adapter.findApplicablePrice(new PriceQuery(35455, 1, DATE_14_AT_16));

            verify(priceJpaRepository, times(1)).findTopByCriteria(35455, 1, DATE_14_AT_16);
            verifyNoMoreInteractions(priceJpaRepository);
        }

        @Test
        @DisplayName("maps JPA entity to domain model via EntityMapper")
        void mapsEntityToDomainViaMapper() {
            PriceEntity entity = buildEntity(2, 1, "25.45",
                    LocalDateTime.of(2020, 6, 14, 15, 0),
                    LocalDateTime.of(2020, 6, 14, 18, 30));
            Price expected = buildDomain(2, "25.45",
                    LocalDateTime.of(2020, 6, 14, 15, 0),
                    LocalDateTime.of(2020, 6, 14, 18, 30));

            when(priceJpaRepository.findTopByCriteria(any(), any(), any()))
                    .thenReturn(Optional.of(entity));
            when(entityMapper.toDomain(entity)).thenReturn(expected);

            Optional<Price> result = adapter.findApplicablePrice(
                    new PriceQuery(35455, 1, DATE_14_AT_16));

            assertThat(result).isPresent().contains(expected);
            verify(entityMapper, times(1)).toDomain(entity);
        }

        @Test
        @DisplayName("returns the exact domain object produced by the mapper")
        void returnsExactDomainObjectFromMapper() {
            PriceEntity entity = buildEntity(1, 0, "35.50",
                    LocalDateTime.of(2020, 6, 14, 0, 0),
                    LocalDateTime.of(2020, 12, 31, 23, 59, 59));
            Price domainPrice = buildDomain(1, "35.50",
                    LocalDateTime.of(2020, 6, 14, 0, 0),
                    LocalDateTime.of(2020, 12, 31, 23, 59, 59));

            when(priceJpaRepository.findTopByCriteria(any(), any(), any()))
                    .thenReturn(Optional.of(entity));
            when(entityMapper.toDomain(entity)).thenReturn(domainPrice);

            Optional<Price> result = adapter.findApplicablePrice(
                    new PriceQuery(35455, 1, DATE_14_AT_10));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(1);
            assertThat(result.get().price()).isEqualByComparingTo("35.50");
            assertThat(result.get().currency()).isEqualTo("EUR");
        }
    }

    @Nested
    @DisplayName("when JPA returns empty")
    class WhenJpaReturnsEmpty {

        @Test
        @DisplayName("returns Optional.empty without calling the mapper")
        void returnsEmptyWithoutCallingMapper() {
            when(priceJpaRepository.findTopByCriteria(any(), any(), any()))
                    .thenReturn(Optional.empty());

            Optional<Price> result = adapter.findApplicablePrice(
                    new PriceQuery(35455, 1, DATE_14_AT_10));

            assertThat(result).isEmpty();
            verifyNoInteractions(entityMapper);
        }

        @Test
        @DisplayName("propagates empty for unknown product")
        void propagatesEmptyForUnknownProduct() {
            when(priceJpaRepository.findTopByCriteria(eq(99999), any(), any()))
                    .thenReturn(Optional.empty());

            Optional<Price> result = adapter.findApplicablePrice(
                    new PriceQuery(99999, 1, DATE_14_AT_10));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("propagates empty for unknown brand")
        void propagatesEmptyForUnknownBrand() {
            when(priceJpaRepository.findTopByCriteria(any(), eq(99), any()))
                    .thenReturn(Optional.empty());

            Optional<Price> result = adapter.findApplicablePrice(
                    new PriceQuery(35455, 99, DATE_14_AT_10));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("interaction contracts")
    class InteractionContracts {

        @Test
        @DisplayName("calls JPA repository exactly once per invocation")
        void callsJpaExactlyOnce() {
            when(priceJpaRepository.findTopByCriteria(any(), any(), any()))
                    .thenReturn(Optional.empty());

            adapter.findApplicablePrice(new PriceQuery(35455, 1, DATE_14_AT_10));

            verify(priceJpaRepository, times(1)).findTopByCriteria(any(), any(), any());
            verifyNoMoreInteractions(priceJpaRepository);
        }

        @Test
        @DisplayName("calls mapper exactly once when entity is present")
        void callsMapperExactlyOnce() {
            PriceEntity entity = buildEntity(1, 0, "35.50",
                    LocalDateTime.of(2020, 6, 14, 0, 0),
                    LocalDateTime.of(2020, 12, 31, 23, 59, 59));
            when(priceJpaRepository.findTopByCriteria(any(), any(), any()))
                    .thenReturn(Optional.of(entity));
            when(entityMapper.toDomain(entity))
                    .thenReturn(buildDomain(1, "35.50",
                            LocalDateTime.of(2020, 6, 14, 0, 0),
                            LocalDateTime.of(2020, 12, 31, 23, 59, 59)));

            adapter.findApplicablePrice(new PriceQuery(35455, 1, DATE_14_AT_10));

            verify(entityMapper, times(1)).toDomain(entity);
            verifyNoMoreInteractions(entityMapper);
        }
    }
}