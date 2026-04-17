package com.inditex.price.infrastructure.adapter.inbound.rest.mapper;

import com.inditex.price.domain.model.Price;
import com.inditex.price.domain.model.PriceQuery;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceRequest;
import com.inditex.price.infrastructure.adapter.inbound.rest.dto.PriceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PriceMapper {

    @Mapping(source = "productId", target = "productId")
    @Mapping(source = "brandId", target = "brandId")
    @Mapping(source = "applicationDate", target = "applicationDate")
    PriceQuery toQuery(PriceRequest request);

    @Mapping(source = "productId", target = "productId")
    @Mapping(source = "brandId", target = "brandId")
    @Mapping(source = "priceList", target = "priceList")
    @Mapping(source = "startDate", target = "startDate")
    @Mapping(source = "endDate", target = "endDate")
    @Mapping(source = "price", target = "price")
    @Mapping(source = "currency", target = "currency")
    PriceResponse toResponse(Price price);
}