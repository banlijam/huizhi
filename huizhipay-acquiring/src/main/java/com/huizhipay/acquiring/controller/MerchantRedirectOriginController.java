package com.huizhipay.acquiring.controller;
import com.huizhipay.acquiring.service.MerchantRedirectOriginService;
import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.huizhipay.common.security.MerchantAccessGuard.*;
@RestController @Profile({"dev", "local"}) @RequestMapping("/api/v1/developer/redirect-origins") @RequiredArgsConstructor
public class MerchantRedirectOriginController{
 private final MerchantRedirectOriginService service;private final MerchantAccessGuard guard;
 @GetMapping public R<List<MerchantRedirectOriginService.OriginView>> list(){return R.ok(service.list(guard.requireAnyRole(OWNER,ADMIN,ANALYST,READONLY).merchantId()));}
 @PostMapping public ResponseEntity<R<MerchantRedirectOriginService.Registration>> register(Authentication a,@RequestBody MerchantRedirectOriginService.RegisterCommand c){var m=guard.requireAnyRole(OWNER,ADMIN);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(R.ok(service.register(m.merchantId(),a.getName(),c.environment(),c.origin())));}
 @PostMapping("/{id}/verify") public R<Void> verify(@PathVariable long id){String m=guard.requireAnyRole(OWNER,ADMIN).merchantId();service.verify(m,id);return R.ok("Origin verified");}
}
