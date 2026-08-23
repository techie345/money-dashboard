package com.techie345.moneys.imports;
import com.techie345.moneys.imports.provider.*; import java.time.Instant; import java.util.UUID;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/v1/imports") @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.imports.enabled",havingValue="true",matchIfMissing=true) public class ImportController {
 private final ImportService service; public ImportController(ImportService service){this.service=service;}
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<ImportResponse> create(@RequestParam MultipartFile file,@RequestParam(defaultValue="generic") String profile,@RequestParam(required=false) UUID accountId,java.security.Principal principal)throws Exception{return ResponseEntity.status(HttpStatus.CREATED).body(service.create(ProviderInput.file(file.getOriginalFilename(),profile,Instant.now(),file.getBytes()).withAccount(accountId),user(principal)));}
 @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE) public ResponseEntity<ImportResponse> createManual(@RequestBody ManualRequest request,java.security.Principal principal){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(ProviderInput.manual("manual",Instant.now(),request.entries()).withAccount(request.accountId()),user(principal)));}
 @GetMapping("/{id}/preview") public ImportResponse preview(@PathVariable UUID id,java.security.Principal principal){return service.preview(id,user(principal));}
 @PostMapping("/{id}/commit") public ImportResponse commit(@PathVariable UUID id,@RequestBody CommitRequest request,java.security.Principal principal){return service.commit(id,user(principal),request.confirmationToken(),request.version());}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void discard(@PathVariable UUID id,java.security.Principal principal){service.discard(id,user(principal));}
 @ExceptionHandler(ImportNotFoundException.class) ResponseEntity<ImportApiError> notFound(){return error(HttpStatus.NOT_FOUND,"IMPORT_NOT_FOUND","Import was not found.");}
  @ExceptionHandler(ImportConfirmationException.class) ResponseEntity<ImportApiError> invalid(ImportConfirmationException e){HttpStatus status="IMPORT_VALIDATION_FAILED".equals(e.getMessage())||"IMPORT_DUPLICATES_FOUND".equals(e.getMessage())?HttpStatus.UNPROCESSABLE_ENTITY:HttpStatus.CONFLICT;return error(status,e.getMessage(),"Import confirmation was rejected.");}
  @ExceptionHandler(ImportFailureException.class) ResponseEntity<ImportApiError> failure(ImportFailureException e){HttpStatus status=e.code().equals("IMPORT_VALIDATION_FAILED")||e.code().equals("IMPORT_PROVIDER_UNSUPPORTED")?HttpStatus.UNPROCESSABLE_ENTITY:HttpStatus.INTERNAL_SERVER_ERROR;return error(status,e.code(),e.getMessage());}
  @ExceptionHandler(com.techie345.moneys.imports.provider.UnsupportedProviderException.class) ResponseEntity<ImportApiError> unsupported(Exception e){return error(HttpStatus.UNPROCESSABLE_ENTITY,"IMPORT_PROVIDER_UNSUPPORTED","The import provider is not supported.");}
  @ExceptionHandler(Exception.class) ResponseEntity<ImportApiError> unexpected(Exception e){return error(HttpStatus.INTERNAL_SERVER_ERROR,"IMPORT_FAILED","The import could not be completed.");}
 private ResponseEntity<ImportApiError> error(HttpStatus status,String code,String message){return ResponseEntity.status(status).body(new ImportApiError(status.value(),code,message,java.util.List.of(),UUID.randomUUID().toString()));}
 private UUID user(java.security.Principal p){return UUID.fromString(p.getName());}
 public record CommitRequest(String confirmationToken,long version){}
 public record ManualRequest(UUID accountId,java.util.List<ManualEntry> entries){}
}
