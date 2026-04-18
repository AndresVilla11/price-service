package com.inditex.price.application.usecase;

import com.inditex.price.domain.exception.PriceNotFoundException;
import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;
import com.inditex.price.domain.port.outbound.PriceRepositoryPort;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetApplicablePriceUseCase")
class GetApplicablePriceUseCaseTest {

    @Mock
    PriceRepositoryPort priceRepositoryPort;

    @InjectMocks
    private GetApplicablePriceUseCase useCase;

    private static final Integer PRODUCT_ID = 35455;
    private static final Integer BRAND_ID = 1;
    private static final LocalDateTime APPLICATION_DATE =
            LocalDateTime.of(2020, 6, 14, 10, 0, 0);

    private PriceQuery buildQuery() {
        return new PriceQuery(PRODUCT_ID, BRAND_ID, APPLICATION_DATE);
    }

    private Price buildPrice(int priceList, BigDecimal price) {
        return new Price(
                PRODUCT_ID,
                BRAND_ID,
                priceList,
                LocalDateTime.of(2020, 6, 14, 0, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59, 59),
                price,
                "EUR"
        );
    }

    @Nested
    @DisplayName("when a matching price exists")
    class WhenPriceExists {

        @Test
        @DisplayName("returns the price returned by the repository")
        void returnsThePriceFromRepository() {
            Price expected = buildPrice(1, new BigDecimal("35.50"));
            when(priceRepositoryPort.findApplicablePrice(any())).thenReturn(Optional.of(expected));

            Price result = useCase.execute(buildQuery());

            assertThat(result).isEqualTo(expected);
            verify(priceRepositoryPort, times(1)).findApplicablePrice(buildQuery());
        }

        @Test
        @DisplayName("delegates to the repository with the exact same query")
        void delegatesQueryToRepository() {
            PriceQuery query = buildQuery();
            when(priceRepositoryPort.findApplicablePrice(query))
                    .thenReturn(Optional.of(buildPrice(2, new BigDecimal("25.45"))));

            useCase.execute(query);

            verify(priceRepositoryPort).findApplicablePrice(query);
            verifyNoMoreInteractions(priceRepositoryPort);
        }
    }

    @Nested
    @DisplayName("when no price matches the query")
    class WhenPriceNotFound {

        @Test
        @DisplayName("throws PriceNotFoundException with descriptive message")
        void throwsPriceNotFoundException() {
            when(priceRepositoryPort.findApplicablePrice(any(PriceQuery.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(buildQuery()))
                    .isInstanceOf(PriceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PRODUCT_ID))
                    .hasMessageContaining(String.valueOf(BRAND_ID));
        }

        @Test
        @DisplayName("never returns null — always throws on empty")
        void neverReturnsNull() {
            when(priceRepositoryPort.findApplicablePrice(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(buildQuery()))
                    .isInstanceOf(PriceNotFoundException.class);
        }
    }
}