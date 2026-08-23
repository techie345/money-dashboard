package com.techie345.moneys.financial.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findAllByOwnerId(UUID ownerId);
    Optional<AccountEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
