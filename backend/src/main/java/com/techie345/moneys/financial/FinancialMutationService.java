package com.techie345.moneys.financial;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.function.Consumer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialMutationService {
    @Transactional
    public <T> T create(JpaRepository<T, UUID> repository, T entity) {
        return repository.save(entity);
    }

    @Transactional
    public <T> T create(JpaRepository<T, UUID> repository, T entity, Consumer<T> afterSave) {
        T saved = repository.save(entity);
        afterSave.accept(saved);
        return repository.save(saved);
    }

    @Transactional
    public <T extends FinancialEntity> T update(Supplier<T> lookup, long expectedVersion,
                                                Consumer<T> mutation, JpaRepository<T, UUID> repository) {
        T entity = lookup.get();
        if (entity == null) return null;
        if (entity.getVersion() != expectedVersion) throw new com.techie345.moneys.financial.api.VersionConflictException();
        mutation.accept(entity);
        return repository.save(entity);
    }

    @Transactional
    public <T extends FinancialEntity> boolean delete(Supplier<T> lookup, long expectedVersion,
                                                       JpaRepository<T, UUID> repository) {
        T entity = lookup.get();
        if (entity == null) return false;
        if (entity.getVersion() != expectedVersion) throw new com.techie345.moneys.financial.api.VersionConflictException();
        repository.delete(entity);
        return true;
    }

    @Transactional
    public <T> boolean delete(Supplier<T> lookup, long expectedVersion, ToLongFunction<T> version,
                              JpaRepository<T, UUID> repository) {
        T entity = lookup.get();
        if (entity == null) return false;
        if (version.applyAsLong(entity) != expectedVersion) throw new com.techie345.moneys.financial.api.VersionConflictException();
        repository.delete(entity);
        return true;
    }
}
