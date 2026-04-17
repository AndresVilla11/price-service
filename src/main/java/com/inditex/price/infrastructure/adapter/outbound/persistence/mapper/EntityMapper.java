package com.inditex.price.infrastructure.adapter.outbound.persistence.mapper;

import com.inditex.price.domain.model.Price;
import com.inditex.price.infrastructure.adapter.outbound.persistence.entity.PriceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EntityMapper {
    @Mapping(source = "productId", target = "productId")
    @Mapping(source = "brandId", target = "brandId")
    @Mapping(source = "priceList", target = "priceList")
    @Mapping(source = "startDate", target = "startDate")
    @Mapping(source = "endDate", target = "endDate")
    @Mapping(source = "price", target = "price")
    @Mapping(source = "currency", target = "currency")
    Price toDomain(PriceEntity entity);
}
