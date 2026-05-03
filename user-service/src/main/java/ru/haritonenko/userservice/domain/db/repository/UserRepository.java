package ru.haritonenko.userservice.domain.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.haritonenko.userservice.domain.db.entity.UserEntity;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByLogin(String Login);

    Optional<UserEntity> findByLogin(String login);
}
