package com.techie345.moneys.imports.provider;
import java.time.LocalDate;
public record ManualEntry(LocalDate date, String merchant, String description, long amountCents, String category, String sourceId) { }
