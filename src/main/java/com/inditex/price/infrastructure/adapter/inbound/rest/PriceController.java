package com.inditex.price.infrastructure.adapter.inbound.rest;

import com.inditex.price.domain.port.inbound.GetApplicablePricePort;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceRequest;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/prices")
public class PriceController {

    private final GetApplicablePricePort getApplicablePricePort;

    public PriceController(GetApplicablePricePort getApplicablePricePort) {
        this.getApplicablePricePort = getApplicablePricePort;
    }

    @Operation(
            summary = "Get applicable price",
            description = "Returns the price with highest priority for a given product, brand and date"
    )
    @GetMapping
    public PriceResponse getApplicablePrice(@Valid @ParameterObject PriceRequest request) {
        return getApplicablePricePort.execute(request);
    }
}
