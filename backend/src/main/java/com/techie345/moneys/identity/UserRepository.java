package com.techie345.moneys.identity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<UserEntity, UUID> {
    <S extends UserEntity> S save(S user);

    <S extends UserEntity> S saveAndFlush(S user);

    Optional<UserEntity> findById(UUID userId);

    Optional<UserEntity> findByGoogleSubject(String googleSubject);
}
