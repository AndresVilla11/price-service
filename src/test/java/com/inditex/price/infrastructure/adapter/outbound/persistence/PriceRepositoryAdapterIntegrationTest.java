package com.inditex.price.infrastructure.adapter.outbound.persistence;

import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;
import com.inditex.price.infrastructure.adapter.outbound.persistence.mapper.EntityMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        PriceRepositoryAdapter.class,
        EntityMapperImpl.class
})
@Transactional
@Sql("/data-test.sql")
@DisplayName("PriceRepositoryAdapter — JPA integration")
class PriceRepositoryAdapterIntegrationTest {

    @Autowired
    private PriceRepositoryAdapter adapter;

    private Optional<Price> query(int productId, int brandId, LocalDateTime date) {
        return adapter.findApplicablePrice(new PriceQuery(productId, brandId, date));
    }

    @Nested
    @DisplayName("Priority disambiguation")
    class PriorityDisambiguation {

        @Test
        @DisplayName("returns priceList=2 (priority=1) over priceList=1 (priority=0) at 16:00 day 14")
        void returnsHighestPriorityWhenTwoRangesOverlap() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 14, 16, 0));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(2);
            assertThat(result.get().price()).isEqualByComparingTo("25.45");
        }

        @Test
        @DisplayName("returns priceList=1 at 10:00 day 14 — priceList=2 not yet active")
        void returnsOnlyCandidateWhenNoOverlap() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 14, 10, 0));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(1);
            assertThat(result.get().price()).isEqualByComparingTo("35.50");
        }

        @Test
        @DisplayName("returns priceList=3 (priority=1) over priceList=1 (priority=0) at 10:00 day 15")
        void returnsHighestPriorityOnDay15() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 15, 10, 0));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(3);
            assertThat(result.get().price()).isEqualByComparingTo("30.50");
        }

        @Test
        @DisplayName("returns priceList=4 (priority=1) over priceList=1 (priority=0) at 21:00 day 16")
        void returnsHighestPriorityOnDay16() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 16, 21, 0));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(4);
            assertThat(result.get().price()).isEqualByComparingTo("38.95");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Límites de rango de fechas — inclusivo en ambos extremos
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Date range boundary inclusion")
    class DateRangeBoundaries {

        @Test
        @DisplayName("includes start_date boundary — priceList=2 active exactly at 15:00")
        void includesStartDateBoundary() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 14, 15, 0, 0));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(2);
        }

        @Test
        @DisplayName("includes end_date boundary — priceList=2 still active exactly at 18:30")
        void includesEndDateBoundary() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 14, 18, 30, 0));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(2);
        }

        @Test
        @DisplayName("falls back to priceList=1 one second after priceList=2 expires")
        void fallsBackOneSecondAfterExpiry() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 14, 18, 30, 1));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(1);
            assertThat(result.get().price()).isEqualByComparingTo("35.50");
        }

        @Test
        @DisplayName("priceList=3 active exactly at its start_date (day 15 00:00)")
        void priceList3ActiveAtItsStartDate() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 15, 0, 0, 0));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(3);
        }

        @Test
        @DisplayName("only priceList=1 active in gap between priceList=3 and priceList=4 on day 15 at 12:00")
        void onlyPriceList1ActiveInGapBetweenPriceList3And4() {
            // priceList=3 ends at 11:00, priceList=4 starts at 16:00
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 15, 12, 0));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(1);
        }

        @Test
        @DisplayName("priceList=4 wins over priceList=1 at last valid second (31 Dec 23:59:59)")
        void lastSecondOfValidityPriceList4Wins() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 12, 31, 23, 59, 59));

            assertThat(result).isPresent();
            assertThat(result.get().priceList()).isEqualTo(4);
            assertThat(result.get().price()).isEqualByComparingTo("38.95");
        }

        @Test
        @DisplayName("returns empty when date is before all valid ranges")
        void returnsEmptyBeforeAllRanges() {
            assertThat(query(35455, 1, LocalDateTime.of(2019, 1, 1, 0, 0))).isEmpty();
        }

        @Test
        @DisplayName("returns empty when date is after all valid ranges")
        void returnsEmptyAfterAllRanges() {
            assertThat(query(35455, 1, LocalDateTime.of(2021, 1, 1, 0, 0))).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Aislamiento por producto y marca
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Product and brand isolation")
    class ProductAndBrandIsolation {

        @Test
        @DisplayName("returns empty for unknown productId")
        void returnsEmptyForUnknownProduct() {
            assertThat(query(99999, 1, LocalDateTime.of(2020, 6, 14, 10, 0))).isEmpty();
        }

        @Test
        @DisplayName("returns empty for unknown brandId")
        void returnsEmptyForUnknownBrand() {
            assertThat(query(35455, 99, LocalDateTime.of(2020, 6, 14, 10, 0))).isEmpty();
        }

        @Test
        @DisplayName("returns empty when both productId and brandId are unknown")
        void returnsEmptyWhenBothUnknown() {
            assertThat(query(99999, 99, LocalDateTime.of(2020, 6, 14, 10, 0))).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Completitud del mapeo — ningún campo nulo, valores correctos
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Domain mapping completeness")
    class DomainMappingCompleteness {

        @Test
        @DisplayName("all domain fields are populated — no nulls after entity-to-domain mapping")
        void allDomainFieldsPopulated() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 14, 10, 0));

            assertThat(result).isPresent();
            Price price = result.get();

            assertThat(price.productId()).isEqualTo(35455);
            assertThat(price.brandId()).isEqualTo(1);
            assertThat(price.priceList()).isNotNull().isPositive();
            assertThat(price.startDate()).isNotNull();
            assertThat(price.endDate()).isNotNull();
            assertThat(price.price()).isNotNull().isPositive();
            assertThat(price.currency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("start_date is always strictly before end_date")
        void startDateIsBeforeEndDate() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 14, 10, 0));

            assertThat(result).isPresent();
            assertThat(result.get().startDate()).isBefore(result.get().endDate());
        }

        @Test
        @DisplayName("price value matches exactly what was inserted — no precision loss")
        void priceValueMatchesInsertedData() {
            Optional<Price> result = query(35455, 1, LocalDateTime.of(2020, 6, 14, 10, 0));

            assertThat(result).isPresent();
            assertThat(result.get().price())
                    .isGreaterThan(BigDecimal.ZERO)
                    .isEqualByComparingTo(new BigDecimal("35.50"));
        }
    }
}