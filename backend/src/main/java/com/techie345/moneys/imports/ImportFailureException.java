package com.techie345.moneys.imports;
public class ImportFailureException extends RuntimeException { private final String code; public ImportFailureException(String code,String message,Throwable cause){super(message,cause);this.code=code;} public String code(){return code;} }
