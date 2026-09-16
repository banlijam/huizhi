package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.MerchantWebhookService;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.huizhipay.common.security.MerchantAccessGuard.*;

@RestController
@RequestMapping("/api/v1/developer/webhooks")
@RequiredArgsConstructor
public class MerchantWebhookController {
    private final MerchantWebhookService service;
    private final MerchantAccessGuard accessGuard;
    private final MerchantResolver merchantResolver;

    @GetMapping
    public R<Overview> overview(){String merchant=requireRead();return R.ok(new Overview(service.getConfig(merchant),service.list(merchant)));}
    @GetMapping("/{eventId}/attempts")
    public R<List<Map<String,Object>>> attempts(@PathVariable String eventId){return R.ok(service.attempts(requireRead(),eventId));}
    @PutMapping
    public R<MerchantWebhookService.ConfigView> save(@RequestBody SaveRequest body){return R.ok(service.save(accessGuard.requireAnyRole(OWNER,ADMIN).merchantId(),body.endpointUrl(),body.enabled()));}
    @PostMapping("/test")
    public R<Map<String,String>> test(){String id=service.enqueueTest(accessGuard.requireAnyRole(OWNER,ADMIN).merchantId());if(id==null)throw new BizException(409,"Enable a webhook endpoint first");return R.ok(Map.of("eventId",id));}
    @PostMapping("/{eventId}/retry")
    public R<Void> retry(@PathVariable String eventId){service.retry(accessGuard.requireAnyRole(OWNER,ADMIN).merchantId(),eventId);return R.ok("Retry queued");}
    private String requireRead(){MerchantResolver.MerchantAccess a=accessGuard.requireAnyRole(OWNER,ADMIN,ANALYST,READONLY);return a.merchantId();}
    public record SaveRequest(String endpointUrl,boolean enabled){}
    public record Overview(MerchantWebhookService.ConfigView config,List<Map<String,Object>> deliveries){}
}
