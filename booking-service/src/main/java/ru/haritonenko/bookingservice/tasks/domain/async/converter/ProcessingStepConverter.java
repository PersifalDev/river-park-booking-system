package ru.haritonenko.bookingservice.tasks.domain.async.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.haritonenko.bookingservice.tasks.domain.async.status.ProcessingStep;
import ru.haritonenko.commonlibs.utils.EnumUtils;

@Converter
public class ProcessingStepConverter implements AttributeConverter<ProcessingStep, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ProcessingStep statusEnum) {
        return statusEnum == null
                ? null
                : statusEnum.getCode();
    }

    @Override
    public ProcessingStep convertToEntityAttribute(Integer intCode) {
        return intCode == null
                ? null
                : EnumUtils.fromCode(ProcessingStep.class, intCode);
    }
}
