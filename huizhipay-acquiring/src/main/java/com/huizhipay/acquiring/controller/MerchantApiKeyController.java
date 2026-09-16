package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.MerchantApiKeyService;
import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantKybGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.huizhipay.common.security.MerchantAccessGuard.*;

@RestController @Profile({"dev", "local"}) @RequestMapping("/api/v1/developer/api-keys") @RequiredArgsConstructor
public class MerchantApiKeyController {
    private final MerchantApiKeyService service;
    private final MerchantAccessGuard accessGuard;
    private final MerchantKybGuard kybGuard;

    @GetMapping public R<List<MerchantApiKeyService.KeyView>> list(){ return R.ok(service.list(accessGuard.requireAnyRole(OWNER,ADMIN,ANALYST,READONLY).merchantId())); }

    @PostMapping public ResponseEntity<R<MerchantApiKeyService.IssuedKey>> issue(Authentication auth,@RequestBody(required=false) MerchantApiKeyService.IssueCommand command){
        String merchantId=accessGuard.requireAnyRole(OWNER,ADMIN).merchantId(); kybGuard.requireApproved(merchantId);
        return noStore(R.ok(service.issuePending(merchantId,auth.getName(),command)));
    }

    @PostMapping("/{keyId}/activate") public R<MerchantApiKeyService.KeyView> activate(@PathVariable long keyId,Authentication auth,@RequestBody MerchantApiKeyService.ActivateCommand command){
        String merchantId=accessGuard.requireAnyRole(OWNER,ADMIN).merchantId(); kybGuard.requireApproved(merchantId);
        return R.ok(service.activate(merchantId,keyId,auth.getName(),command.secretKey()));
    }

    @DeleteMapping("/{keyId}") public R<Void> revoke(@PathVariable long keyId,Authentication auth,@RequestBody(required=false) MerchantApiKeyService.RevokeCommand command){
        String merchantId=accessGuard.requireAnyRole(OWNER,ADMIN).merchantId(); service.revoke(merchantId,keyId,auth.getName(),command==null?null:command.reason()); return R.ok("API key revoked");
    }

    private <T> ResponseEntity<T> noStore(T body){ return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.PRAGMA,"no-cache").body(body); }
}
