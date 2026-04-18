package com.inditex.price;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql("/data-test.sql")
@DisplayName("Price API — system tests (full context + H2)")
class PriceSystemTest {

    private static final String URL = "/prices";
    private static final int PRODUCT = 35455;
    private static final int BRAND = 1;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Test 1 — 2020-06-14T10:00 → tarifa 1, precio 35.50")
    void test1_day14At10h_returnsPriceList1() throws Exception {
        performRequest("2020-06-14T10:00:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceList", is(1)))
                .andExpect(jsonPath("$.price", is(35.50)))
                .andExpect(jsonPath("$.productId", is(PRODUCT)))
                .andExpect(jsonPath("$.brandId", is(BRAND)))
                .andExpect(jsonPath("$.currency", is("EUR")));
    }

    @Test
    @DisplayName("Test 2 — 2020-06-14T16:00 → tarifa 2, precio 25.45 (mayor prioridad)")
    void test2_day14At16h_returnsPriceList2() throws Exception {
        performRequest("2020-06-14T16:00:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceList", is(2)))
                .andExpect(jsonPath("$.price", is(25.45)));
    }

    @Test
    @DisplayName("Test 3 — 2020-06-14T21:00 → tarifa 1, precio 35.50 (tarifa 2 ya expiró)")
    void test3_day14At21h_returnsPriceList1() throws Exception {
        performRequest("2020-06-14T21:00:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceList", is(1)))
                .andExpect(jsonPath("$.price", is(35.50)));
    }

    @Test
    @DisplayName("Test 4 — 2020-06-15T10:00 → tarifa 3, precio 30.50 (mayor prioridad)")
    void test4_day15At10h_returnsPriceList3() throws Exception {
        performRequest("2020-06-15T10:00:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceList", is(3)))
                .andExpect(jsonPath("$.price", is(30.50)));
    }

    @Test
    @DisplayName("Test 5 — 2020-06-16T21:00 → tarifa 4, precio 38.95")
    void test5_day16At21h_returnsPriceList4() throws Exception {
        performRequest("2020-06-16T21:00:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceList", is(4)))
                .andExpect(jsonPath("$.price", is(38.95)));
    }

    private ResultActions performRequest(String date) throws Exception {
        return mockMvc.perform(get(URL)
                .param("productId", String.valueOf(PRODUCT))
                .param("brandId", String.valueOf(BRAND))
                .param("applicationDate", date)
                .accept(MediaType.APPLICATION_JSON));
    }
}
