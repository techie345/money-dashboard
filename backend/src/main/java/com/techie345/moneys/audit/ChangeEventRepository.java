package com.techie345.moneys.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface ChangeEventRepository extends JpaRepository<ChangeEventEntity, UUID> {
    List<ChangeEventEntity> findByOwnerIdAndSequenceGreaterThanOrderBySequenceAsc(UUID ownerId, long sequence, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = """
            INSERT INTO change_event_sequence(owner_id, next_sequence) VALUES (:ownerId, 1)
            ON CONFLICT (owner_id) DO UPDATE SET next_sequence = change_event_sequence.next_sequence + 1
            RETURNING next_sequence
            """, nativeQuery = true)
    long nextSequence(@org.springframework.data.repository.query.Param("ownerId") UUID ownerId);
}
