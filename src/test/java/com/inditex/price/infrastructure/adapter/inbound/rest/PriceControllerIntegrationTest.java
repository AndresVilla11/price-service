package com.inditex.price.infrastructure.adapter.inbound.rest;

import com.inditex.price.domain.exception.PriceNotFoundException;
import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;
import com.inditex.price.domain.port.inbound.GetApplicablePricePort;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceRequest;
import com.inditex.price.infrastructure.adapter.inbound.rest.mapper.PriceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static com.inditex.price.utils.ObjectMock.priceQueryFull200Ok;
import static com.inditex.price.utils.ObjectMock.priceResponse200Ok;
import static com.inditex.price.utils.ObjectMock.priceResponseFull200Ok;
import static com.inditex.price.utils.ObjectMock.priceWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriceController.class)
@DisplayName("PriceController — integration (web slice)")
class PriceControllerIntegrationTest {

    private static final String URL = "/prices";

    @MockitoBean
    GetApplicablePricePort getApplicablePricePort;
    @MockitoBean
    PriceMapper priceMapper;

    @Autowired
    private MockMvc mockMvc;

    private String urlWith(String date) {
        return URL + "?productId=35455&brandId=1&applicationDate=" + date;
    }

    @Nested
    @DisplayName("GET /prices — successful responses")
    class HappyPath {

        @Test
        @DisplayName("returns 200 with full price payload")
        void returns200WithFullPayload() throws Exception {

            when(getApplicablePricePort.execute(any(PriceQuery.class))).thenReturn(priceWith(1, "35.50"));
            when(priceMapper.toResponse(any(Price.class))).thenReturn(priceResponseFull200Ok());
            when(priceMapper.toQuery(any(PriceRequest.class))).thenReturn(priceQueryFull200Ok());

            mockMvc.perform(get(urlWith("2020-06-14T10:00:00"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.productId").value(35455))
                    .andExpect(jsonPath("$.brandId").value(1))
                    .andExpect(jsonPath("$.priceList").value(1))
                    .andExpect(jsonPath("$.price").value(35.50))
                    .andExpect(jsonPath("$.currency").value("EUR"))
                    .andExpect(jsonPath("$.startDate").exists())
                    .andExpect(jsonPath("$.endDate").exists());
        }

        @Test
        @DisplayName("maps the price list correctly from use case result")
        void mapsPriceListFromUseCase() throws Exception {
            when(getApplicablePricePort.execute(any(PriceQuery.class))).thenReturn(priceWith(2, "25.45"));
            when(priceMapper.toResponse(any(Price.class))).thenReturn(priceResponse200Ok());
            when(priceMapper.toQuery(any(PriceRequest.class))).thenReturn(priceQueryFull200Ok());

            mockMvc.perform(get(urlWith("2020-06-14T16:00:00"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.priceList").value(2))
                    .andExpect(jsonPath("$.price").value(25.45));
        }
    }

    @Nested
    @DisplayName("GET /prices — error responses")
    class ErrorScenarios {

        @Test
        @DisplayName("returns 404 ProblemDetail when no price found")
        void returns404WhenPriceNotFound() throws Exception {
            when(getApplicablePricePort.execute(any()))
                    .thenThrow(new PriceNotFoundException(35455, 1,
                            LocalDateTime.of(2020, 6, 14, 10, 0)));

            mockMvc.perform(get(urlWith("2020-06-14T10:00:00"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Price Not Found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").isNotEmpty())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("returns 400 when productId is missing")
        void returns400WhenProductIdMissing() throws Exception {
            mockMvc.perform(get(URL + "?brandId=1&applicationDate=2020-06-14T10:00:00")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when brandId is missing")
        void returns400WhenBrandIdMissing() throws Exception {
            mockMvc.perform(get(URL + "?productId=35455&applicationDate=2020-06-14T10:00:00")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when applicationDate is missing")
        void returns400WhenDateMissing() throws Exception {
            mockMvc.perform(get(URL + "?productId=35455&brandId=1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when productId is negative")
        void returns400WhenProductIdNegative() throws Exception {
            mockMvc.perform(get(URL + "?productId=-1&brandId=1&applicationDate=2020-06-14T10:00:00")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.productId").exists());
        }

        @Test
        @DisplayName("returns 400 when applicationDate has invalid format")
        void returns400WhenDateFormatIsInvalid() throws Exception {
            mockMvc.perform(get(URL + "?productId=35455&brandId=1&applicationDate=not-a-date")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }


        @Test
        void whenNegativeBrandIdReturns400() throws Exception {
            mockMvc.perform(get(URL + "?productId=35455&brandId=-1&applicationDate=2020-06-14T10:00:00")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.brandId").exists());
        }

        @Test
        void whenReturns500() throws Exception {
            mockMvc.perform(get(URL + "/?productId=99999&brandId=1&applicationDate=2020-06-14T10:00:00")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        }
    }
}