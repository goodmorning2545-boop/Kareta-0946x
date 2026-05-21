package ru.gr0946x.net.repository;

import ru.gr0946x.net.entity.UserEntity;
import java.util.Optional;

public interface UserRepository {
    Optional<UserEntity> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    UserEntity save(UserEntity user);
}