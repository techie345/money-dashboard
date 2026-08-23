package com.techie345.moneys.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.techie345.moneys.financial.asset.*;
import com.techie345.moneys.financial.api.VersionConflictException;

class FinancialMutationServiceTest {
    @Test
    void persistsCreateMetadataBeforeTransactionCompletes() {
        AssetRepository repository = mock(AssetRepository.class);
        AssetEntity asset = new AssetEntity(UUID.randomUUID(), "Home", AssetKind.PROPERTY, 100);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AssetEntity saved = new FinancialMutationService().create(repository, asset, value -> value.update("Updated", AssetKind.PROPERTY, 200));
        assertThat(saved.getName()).isEqualTo("Updated");
        verify(repository, times(2)).save(asset);
    }

    @Test
    void staleUpdateCarriesSafeCurrentRepresentation() {
        AssetRepository repository = mock(AssetRepository.class);
        AssetEntity asset = new AssetEntity(UUID.randomUUID(), "Home", AssetKind.PROPERTY, 100);

        assertThatThrownBy(() -> new FinancialMutationService().update(() -> asset, 9,
                value -> { }, repository, AssetEntity::getVersion, value -> "safe-response"))
                .isInstanceOfSatisfying(VersionConflictException.class,
                        exception -> assertThat(exception.getCurrentRepresentation()).isEqualTo("safe-response"));
    }
}
