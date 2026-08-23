package com.techie345.moneys.financial.asset;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface AssetRepository extends JpaRepository<AssetEntity,UUID>{List<AssetEntity> findAllByOwnerId(UUID o); Optional<AssetEntity> findByIdAndOwnerId(UUID id,UUID o);}
