package ru.haritonenko.catalogservice.services.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.haritonenko.catalogservice.services.domain.type.ServiceItemType;
import ru.haritonenko.commonlibs.utils.EnumUtils;

@Converter()
public class ServiceItemTypeConverter implements AttributeConverter<ServiceItemType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ServiceItemType serviceItemType)  {
        return serviceItemType == null ? null : serviceItemType.getCode();
    }

    @Override
    public ServiceItemType convertToEntityAttribute(Integer intCode) {
        return intCode == null ? null : EnumUtils.fromCode(ServiceItemType.class, intCode);
    }
}
