package com.techie345.moneys.financial.liability;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface LiabilityRepository extends JpaRepository<LiabilityEntity,UUID>{List<LiabilityEntity> findAllByOwnerId(UUID o); Optional<LiabilityEntity> findByIdAndOwnerId(UUID id,UUID o);}
