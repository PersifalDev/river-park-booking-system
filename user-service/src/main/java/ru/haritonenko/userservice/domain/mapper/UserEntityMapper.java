package ru.haritonenko.userservice.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.haritonenko.userservice.api.dto.UserRegistration;
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.UserRole;
import ru.haritonenko.userservice.domain.db.entity.UserEntity;

import java.util.ArrayList;

@Mapper(componentModel = "spring", imports = {UserRole.class, ArrayList.class})
public interface UserEntityMapper {

    @Mapping(target = "role", source = "userRole")
    User toDomain(UserEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "key", source = "hashedPassword")
    @Mapping(target = "userRole", expression = "java(UserRole.USER)")
    UserEntity toEntity(UserRegistration dto, String hashedPassword);
}
