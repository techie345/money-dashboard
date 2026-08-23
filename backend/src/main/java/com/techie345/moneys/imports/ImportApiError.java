package com.techie345.moneys.imports;
import java.util.List;
public record ImportApiError(int status,String code,String message,List<?> details,String traceId) { }
