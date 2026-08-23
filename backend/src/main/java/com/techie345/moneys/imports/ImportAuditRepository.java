package com.techie345.moneys.imports;
import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ImportAuditRepository extends JpaRepository<ImportAuditEntity,UUID> { }
