package com.techie345.moneys.imports;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="import_staging") public class ImportEntity {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(name="owner_id",nullable=false) private UUID ownerId;
 @Column(nullable=false) private String status; @Column(nullable=false) private String filename; @Column(nullable=false) private String profile;
 @Column(name="imported_at",nullable=false) private Instant importedAt; @Column(name="confirmation_token",nullable=false) private String confirmationToken;
  @Column(name="normalized_json",nullable=false,columnDefinition="text") private String normalizedJson; @Version @Column(nullable=false) private long version;
  @Column(name="expires_at",nullable=false) private Instant expiresAt;
 @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected ImportEntity(){}
  public ImportEntity(UUID ownerId,String filename,String profile,Instant importedAt,String token,String json){this.ownerId=ownerId;this.filename=filename;this.profile=profile;this.importedAt=importedAt;this.confirmationToken=token;this.normalizedJson=json;this.expiresAt=Instant.now().plus(java.time.Duration.ofMinutes(30));this.status="PREVIEW";}
 @PrePersist void created(){createdAt=updatedAt=Instant.now();}@PreUpdate void updated(){updatedAt=Instant.now();}
  public UUID getId(){return id;} public UUID getOwnerId(){return ownerId;} public String getStatus(){return status;} public String getFilename(){return filename;} public String getProfile(){return profile;} public Instant getImportedAt(){return importedAt;} public String getConfirmationToken(){return confirmationToken;} public String getNormalizedJson(){return normalizedJson;} public long getVersion(){return version;} public Instant getExpiresAt(){return expiresAt;}
  public void commit(){status="COMMITTED";} public void discard(){status="DISCARDED";}
}
