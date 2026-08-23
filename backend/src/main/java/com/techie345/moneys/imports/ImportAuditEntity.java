package com.techie345.moneys.imports;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="import_audit") public class ImportAuditEntity {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(name="import_id",nullable=false) private UUID importId; @Column(name="owner_id",nullable=false) private UUID ownerId;
 @Column(nullable=false) private String action; @Column(name="source_filename",nullable=false) private String sourceFilename; @Column(nullable=false) private String profile; @Column(name="imported_at",nullable=false) private Instant importedAt; @Column(name="source_identifiers") private String sourceIdentifiers; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected ImportAuditEntity(){} public ImportAuditEntity(UUID importId,UUID ownerId,String action,String sourceFilename,String profile,Instant importedAt,String sourceIdentifiers){this.importId=importId;this.ownerId=ownerId;this.action=action;this.sourceFilename=sourceFilename;this.profile=profile;this.importedAt=importedAt;this.sourceIdentifiers=sourceIdentifiers;}
 @PrePersist void created(){createdAt=Instant.now();}
}
