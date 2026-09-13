package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.MerchantWebhookService;
import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.model.R;
import com.huizhipay.common.security.MerchantAccessGuard;
import com.huizhipay.common.security.MerchantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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
    @Value("${app.frontend.url:http://127.0.0.1:3000}") private String frontendUrl;

    @GetMapping
    public R<Overview> overview(){String merchant=requireRead();return R.ok(new Overview(service.getConfig(merchant),service.list(merchant)));}
    @GetMapping("/{eventId}/attempts")
    public R<List<Map<String,Object>>> attempts(@PathVariable String eventId){return R.ok(service.attempts(requireRead(),eventId));}
    @PutMapping
    public R<MerchantWebhookService.ConfigView> save(@RequestBody SaveRequest body,HttpServletRequest request){requireWrite(request);return R.ok(service.save(accessGuard.requireAnyRole(OWNER,ADMIN).merchantId(),body.endpointUrl(),body.enabled()));}
    @PostMapping("/test")
    public R<Map<String,String>> test(HttpServletRequest request){requireWrite(request);String id=service.enqueueTest(accessGuard.requireAnyRole(OWNER,ADMIN).merchantId());if(id==null)throw new BizException(409,"Enable a webhook endpoint first");return R.ok(Map.of("eventId",id));}
    @PostMapping("/{eventId}/retry")
    public R<Void> retry(@PathVariable String eventId,HttpServletRequest request){requireWrite(request);service.retry(accessGuard.requireAnyRole(OWNER,ADMIN).merchantId(),eventId);return R.ok("Retry queued");}
    private String requireRead(){MerchantResolver.MerchantAccess a=accessGuard.requireAnyRole(OWNER,ADMIN,ANALYST,READONLY);return a.merchantId();}
    private void requireWrite(HttpServletRequest request){
        String origin=request.getHeader("Origin"),token=request.getHeader("X-HuizhiPay-CSRF");
        try{URI expected=URI.create(frontendUrl),actual=URI.create(origin==null?"":origin);if(!"1".equals(token)||!sameOrigin(expected,actual))throw new IllegalArgumentException();}
        catch(RuntimeException e){throw new BizException(403,"CSRF validation failed");}
    }
    private boolean sameOrigin(URI a,URI b){return a.getScheme()!=null&&a.getScheme().equalsIgnoreCase(b.getScheme())&&a.getHost()!=null&&a.getHost().equalsIgnoreCase(b.getHost())&&port(a)==port(b);}
    private int port(URI uri){if(uri.getPort()!=-1)return uri.getPort();return "https".equalsIgnoreCase(uri.getScheme())?443:80;}
    public record SaveRequest(String endpointUrl,boolean enabled){}
    public record Overview(MerchantWebhookService.ConfigView config,List<Map<String,Object>> deliveries){}
}
