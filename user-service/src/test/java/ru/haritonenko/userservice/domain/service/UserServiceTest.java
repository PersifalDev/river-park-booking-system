package ru.haritonenko.userservice.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.haritonenko.userservice.api.dto.UserRegistration;
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.UserRole;
import ru.haritonenko.userservice.domain.db.entity.UserEntity;
import ru.haritonenko.userservice.domain.db.repository.UserRepository;
import ru.haritonenko.userservice.domain.exception.UserAlreadyRegisteredException;
import ru.haritonenko.userservice.domain.exception.UserNotFoundException;
import ru.haritonenko.userservice.domain.mapper.UserEntityMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEntityMapper mapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldGetUserById() {
        UserEntity entity = userEntity();
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(new User(1L, "watson", UserRole.USER));

        User actual = userService.getUserById(1L);

        assertEquals("watson", actual.login());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void shouldRegisterNewUserWithEncodedPassword() {
        UserRegistration registration = new UserRegistration("watson", "secret");
        UserEntity entityToSave = userEntity();
        entityToSave.setId(null);
        UserEntity savedEntity = userEntity();
        when(userRepository.existsByLogin("watson")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(mapper.toEntity(registration, "hashed")).thenReturn(entityToSave);
        when(userRepository.save(entityToSave)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(new User(1L, "watson", UserRole.USER));

        User actual = userService.register(registration);

        assertEquals(1L, actual.id());
        verify(passwordEncoder).encode("secret");
    }

    @Test
    void shouldRejectDuplicateLogin() {
        when(userRepository.existsByLogin("watson")).thenReturn(true);

        assertThrows(
                UserAlreadyRegisteredException.class,
                () -> userService.register(new UserRegistration("watson", "secret"))
        );
    }

    @Test
    void shouldUpdateUserRole() {
        UserEntity entity = userEntity();
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(new User(1L, "watson", UserRole.BOOKING_MANAGER));

        User actual = userService.updateUserRole(1L, UserRole.BOOKING_MANAGER);

        assertEquals(UserRole.BOOKING_MANAGER, actual.role());
        assertEquals(UserRole.BOOKING_MANAGER, entity.getUserRole());
        verify(userRepository).save(entity);
    }

    private UserEntity userEntity() {
        return UserEntity.builder()
                .id(1L)
                .login("watson")
                .key("hashed")
                .userRole(UserRole.USER)
                .build();
    }
}
