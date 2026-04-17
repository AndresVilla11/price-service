package com.inditex.price.infrastructure.adapter.outbound.persistence.mapper;

import com.inditex.price.domain.model.PriceDto;
import com.inditex.price.infrastructure.adapter.outbound.persistence.entity.PriceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PriceMapper {
    PriceDto toDomain(PriceEntity entity);
}
