package com.huizhipay.acquiring.controller;

import com.huizhipay.acquiring.service.TransFiCheckoutWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TransFiWebhookController {
    public static final String HEADER_TRANSFI_HMAC="X-Transfi-Hmac-Hash";
    private final TransFiCheckoutWebhookService service;

    @PostMapping(path="/webhook/transfi",consumes="application/json")
    public ResponseEntity<String> handle(@RequestBody byte[] rawBody,@RequestHeader(value=HEADER_TRANSFI_HMAC,required=false)String signature){
        if(!service.enabled()) return ResponseEntity.status(503).body("Webhook disabled");
        if(!service.signatureValid(rawBody,signature)) return ResponseEntity.status(401).body("Invalid signature");
        try{
            TransFiCheckoutWebhookService.Result result=service.receive(rawBody);
            return switch(result){
                case INVALID -> ResponseEntity.badRequest().body("Invalid Checkout event");
                case CONFLICT -> ResponseEntity.status(409).body("Conflicting event");
                default -> ResponseEntity.ok("OK");
            };
        }catch(Exception e){return ResponseEntity.status(500).body("Processing failed");}
    }
}
