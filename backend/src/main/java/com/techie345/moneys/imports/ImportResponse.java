package com.techie345.moneys.imports;
import java.util.UUID;
import com.techie345.moneys.imports.provider.ProviderPreview;
public record ImportResponse(UUID id,String confirmationToken,long version,String status,ProviderPreview preview) {
    public ImportResponse(UUID id,String token,long version,String status){this(id,token,version,status,null);}
}
