package com.techie345.moneys.financial.transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findAllByOwnerId(UUID ownerId);
    Optional<TransactionEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
