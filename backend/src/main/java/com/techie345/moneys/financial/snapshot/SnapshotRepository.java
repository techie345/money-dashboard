package com.techie345.moneys.financial.snapshot;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface SnapshotRepository extends JpaRepository<SnapshotEntity,UUID>{List<SnapshotEntity> findAllByOwnerId(UUID o); Optional<SnapshotEntity> findByIdAndOwnerId(UUID id,UUID o);}
