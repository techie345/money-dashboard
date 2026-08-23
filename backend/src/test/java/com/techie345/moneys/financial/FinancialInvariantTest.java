package com.techie345.moneys.financial;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.techie345.moneys.financial.asset.AssetEntity;
import com.techie345.moneys.financial.AssetKind;
import com.techie345.moneys.financial.transaction.TransactionEntity;

class FinancialInvariantTest {
    @Test void rejectsNegativeAssetValue() { assertThatThrownBy(() -> new AssetEntity(UUID.randomUUID(), "x", AssetKind.OTHER, -1)).isInstanceOf(IllegalArgumentException.class); }
    @Test void rejectsMissingTransactionOwner() { assertThatThrownBy(() -> new TransactionEntity(null, UUID.randomUUID(), java.time.LocalDate.now(), "m", "d", 1, TransactionKind.SPENDING, "food", CategorySource.MANUAL, "s", "f", null)).isInstanceOf(IllegalArgumentException.class); }
}
