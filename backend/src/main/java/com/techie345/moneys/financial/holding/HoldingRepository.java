package com.techie345.moneys.financial.holding;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface HoldingRepository extends JpaRepository<HoldingEntity,UUID>{List<HoldingEntity> findAllByOwnerId(UUID o); Optional<HoldingEntity> findByIdAndOwnerId(UUID id,UUID o);}
