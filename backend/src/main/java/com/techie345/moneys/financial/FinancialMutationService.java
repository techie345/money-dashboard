package com.techie345.moneys.financial;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import com.techie345.moneys.audit.AuditService;
import com.techie345.moneys.audit.ChangeOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialMutationService {
    private final AuditService audit;

    public FinancialMutationService() { this.audit = null; }
    public FinancialMutationService(AuditService audit) { this.audit = audit; }

    @Autowired
    public FinancialMutationService(ObjectProvider<AuditService> audit) { this.audit = audit.getIfAvailable(); }

    @Transactional
    public <T> T create(JpaRepository<T, UUID> repository, T entity) {
        T saved = repository.save(entity);
        repository.flush();
        record(saved, ChangeOperation.CREATED, null);
        return saved;
    }

    @Transactional
    public <T> T create(JpaRepository<T, UUID> repository, T entity, Consumer<T> afterSave) {
        T saved = repository.save(entity);
        afterSave.accept(saved);
        saved = repository.save(saved);
        repository.flush();
        record(saved, ChangeOperation.CREATED, null);
        return saved;
    }

    @Transactional
    public <T extends FinancialEntity> T update(Supplier<T> lookup, long expectedVersion,
                                                 Consumer<T> mutation, JpaRepository<T, UUID> repository) {
        return update(lookup, expectedVersion, mutation, repository, FinancialEntity::getVersion, value -> null);
    }

    @Transactional
    public <T extends FinancialEntity> T update(Supplier<T> lookup, long expectedVersion,
                                                 Consumer<T> mutation, JpaRepository<T, UUID> repository,
                                                 Function<T, Object> currentRepresentation) {
        return update(lookup, expectedVersion, mutation, repository, FinancialEntity::getVersion,
                currentRepresentation);
    }

    @Transactional
    public <T> T update(Supplier<T> lookup, long expectedVersion, Consumer<T> mutation,
                        JpaRepository<T, UUID> repository, ToLongFunction<T> version) {
        return update(lookup, expectedVersion, mutation, repository, version, value -> null);
    }

    @Transactional
    public <T> T update(Supplier<T> lookup, long expectedVersion, Consumer<T> mutation,
                        JpaRepository<T, UUID> repository, ToLongFunction<T> version,
                        Function<T, Object> currentRepresentation) {
        T entity = lookup.get();
        if (entity == null) return null;
        if (version.applyAsLong(entity) != expectedVersion)
            throw new com.techie345.moneys.financial.api.VersionConflictException(currentRepresentation.apply(entity));
        mutation.accept(entity);
        T saved = repository.save(entity);
        repository.flush();
        record(saved, ChangeOperation.UPDATED, null);
        return saved;
    }

    @Transactional
    public <T extends FinancialEntity> boolean delete(Supplier<T> lookup, long expectedVersion,
                                                       JpaRepository<T, UUID> repository) {
        T entity = lookup.get();
        if (entity == null) return false;
        if (entity.getVersion() != expectedVersion) throw new com.techie345.moneys.financial.api.VersionConflictException();
        record(entity, ChangeOperation.DELETED, null);
        repository.delete(entity);
        return true;
    }

    @Transactional
    public <T> boolean delete(Supplier<T> lookup, long expectedVersion, ToLongFunction<T> version,
                              JpaRepository<T, UUID> repository) {
        return delete(lookup, expectedVersion, version, repository, value -> null);
    }

    @Transactional
    public <T> boolean delete(Supplier<T> lookup, long expectedVersion, ToLongFunction<T> version,
                              JpaRepository<T, UUID> repository, Function<T, Object> currentRepresentation) {
        T entity = lookup.get();
        if (entity == null) return false;
        if (version.applyAsLong(entity) != expectedVersion)
            throw new com.techie345.moneys.financial.api.VersionConflictException(currentRepresentation.apply(entity));
        record(entity, ChangeOperation.DELETED, null);
        repository.delete(entity);
        return true;
    }

    private void record(Object entity, ChangeOperation operation, UUID importId) {
        if (audit != null) audit.recordEntity(entity, operation, importId);
    }
}
