package com.inditex.price.infrastructure.adapter.inbound.rest;

import com.inditex.price.domain.port.inbound.GetApplicablePricePort;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceRequest;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceResponse;
import com.inditex.price.infrastructure.adapter.inbound.rest.mapper.PriceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/prices")
public class PriceController {

    private final GetApplicablePricePort getApplicablePricePort;
    private final PriceMapper priceMapper;

    public PriceController(GetApplicablePricePort getApplicablePricePort, PriceMapper priceMapper) {
        this.getApplicablePricePort = getApplicablePricePort;
        this.priceMapper = priceMapper;
    }

    @Operation(
            summary = "Get applicable price",
            description = """
                    Returns the price with the highest priority applicable to a product
                    for a given brand and application date.
                    
                    If multiple price lists overlap in the given date range, the one with
                    the highest `priority` value takes precedence.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Applicable price found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PriceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "productId": 35455,
                                      "brandId":   1,
                                      "priceList": 1,
                                      "startDate": "2020-06-14T00:00:00",
                                      "endDate":   "2020-12-31T23:59:59",
                                      "price":     35.50,
                                      "currency":  "EUR"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No applicable price for the given criteria",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ProblemDetail")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or missing request parameters",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ProblemDetail")
                    )
            )
    })
    @GetMapping
    public ResponseEntity<PriceResponse> getApplicablePrice(@Valid @ParameterObject PriceRequest request) {
        final PriceResponse response = priceMapper.toResponse(
                getApplicablePricePort.execute(priceMapper.toQuery(request))
        );
        return ResponseEntity.ok(response);
    }
}
