package com.inditex.price.domain.service;

import com.inditex.price.application.usecase.GetApplicablePriceUseCase;
import com.inditex.price.domain.exception.PriceNotFoundException;
import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;
import com.inditex.price.domain.port.inbound.GetApplicablePricePort;
import com.inditex.price.domain.port.outbound.PriceRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GetApplicablePriceUseCaseImplTest {

    private PriceRepositoryPort repositoryPort;
    private GetApplicablePricePort useCase;

    @BeforeEach
    void setUp() {
        repositoryPort = Mockito.mock(PriceRepositoryPort.class);
        useCase = new GetApplicablePriceUseCase(repositoryPort);
    }

    @Test
    void whenPriceExists_thenReturnsIt() {
        var query = new PriceQuery(35455, 1, LocalDateTime.of(2020, 6, 14, 10, 0));
        var expected = new Price(35455, 1, 1, LocalDateTime.of(2020, 6, 14, 0, 0),
                LocalDateTime.of(2020, 12, 31, 23, 59, 59), new BigDecimal("35.50"), "EUR");
        given(repositoryPort.findApplicablePrice(query)).willReturn(Optional.of(expected));

        Price result = useCase.execute(query);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void whenNoPriceExists_thenThrowsPriceNotFoundException() {
        var query = new PriceQuery(99999, 1, LocalDateTime.now());
        given(repositoryPort.findApplicablePrice(query)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(PriceNotFoundException.class)
                .hasMessageContaining("99999");
    }

    @Test
    void whenHigherPriorityExists_thenRepositoryReceivesQueryUnchanged() {
        var query = new PriceQuery(35455, 1, LocalDateTime.of(2020, 6, 14, 16, 0));
        given(repositoryPort.findApplicablePrice(query)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(PriceNotFoundException.class);
        verify(repositoryPort, times(1)).findApplicablePrice(query);
    }
}