package com.huizhipay.acquiring.service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huizhipay.acquiring.entity.MerchantRedirectOrigin;
import com.huizhipay.acquiring.mapper.MerchantRedirectOriginMapper;
import com.huizhipay.common.exceptions.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.net.InetAddress;

@Service @RequiredArgsConstructor
public class MerchantRedirectOriginService {
 private final MerchantRedirectOriginMapper mapper; private final Environment springEnvironment; private final SecureRandom random=new SecureRandom();
 public List<OriginView> list(String merchant){return mapper.selectList(Wrappers.<MerchantRedirectOrigin>lambdaQuery().eq(MerchantRedirectOrigin::getMerchantId,merchant).isNull(MerchantRedirectOrigin::getDisabledAt)).stream().map(r->new OriginView(r.getId(),r.getEnvironment(),r.getOrigin(),Boolean.TRUE.equals(r.getVerified()),r.getCreatedAt(),r.getVerifiedAt())).toList();}
 public Registration register(String merchant,String actor,String env,String value){
  String origin=canonical(value); byte[] b=new byte[24];random.nextBytes(b);String token="hzp-origin-"+Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  MerchantRedirectOrigin row=new MerchantRedirectOrigin().setMerchantId(merchant).setEnvironment(environment(env)).setOrigin(origin).setVerified(false).setVerificationTokenHash(hash(token)).setCreatedBy(actor).setCreatedAt(LocalDateTime.now());
  mapper.insert(row);return new Registration(row.getId(),origin,token,"Publish this exact token at "+origin+"/.well-known/huizhipay-verification.txt");
 }
 public void verify(String merchant,long id){
  MerchantRedirectOrigin row=mapper.selectOne(Wrappers.<MerchantRedirectOrigin>lambdaQuery().eq(MerchantRedirectOrigin::getId,id).eq(MerchantRedirectOrigin::getMerchantId,merchant).isNull(MerchantRedirectOrigin::getDisabledAt));
  if(row==null)throw new BizException(404,"Return origin not found");
  requireSafeVerificationTarget(row.getOrigin());
  String body;try{body=RestClient.create().get().uri(row.getOrigin()+"/.well-known/huizhipay-verification.txt").retrieve().body(String.class);}catch(RuntimeException e){throw new BizException(400,"Could not read the origin verification file");}
  if(body==null||!MessageDigest.isEqual(hash(body.trim()).getBytes(StandardCharsets.US_ASCII),row.getVerificationTokenHash().getBytes(StandardCharsets.US_ASCII)))throw new BizException(400,"Origin verification token does not match");
  mapper.updateById(new MerchantRedirectOrigin().setId(id).setVerified(true).setVerifiedAt(LocalDateTime.now()).setVerificationTokenHash(null));
 }
 public void requireAllowed(String merchant,String env,String url){
  String origin=canonical(url);boolean ok=mapper.exists(Wrappers.<MerchantRedirectOrigin>lambdaQuery().eq(MerchantRedirectOrigin::getMerchantId,merchant).eq(MerchantRedirectOrigin::getEnvironment,environment(env)).eq(MerchantRedirectOrigin::getOrigin,origin).eq(MerchantRedirectOrigin::getVerified,true).isNull(MerchantRedirectOrigin::getDisabledAt));
  if(!ok)throw new BizException(400,"Redirect URL origin is not verified for this merchant and environment");
 }
 private String canonical(String value){try{URI u=URI.create(value);if(!Set.of("https","http").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new Exception();if("http".equals(u.getScheme())&&!Set.of("localhost","127.0.0.1","::1").contains(u.getHost()))throw new Exception();int p=u.getPort();return u.getScheme().toLowerCase()+"://"+u.getHost().toLowerCase()+(p<0?"":":"+p);}catch(Exception e){throw new BizException(400,"A valid HTTPS origin is required; HTTP is only allowed for loopback development");}}
 private void requireSafeVerificationTarget(String origin){try{URI u=URI.create(origin);boolean local=Arrays.stream(springEnvironment.getActiveProfiles()).anyMatch(p->p.equals("local")||p.equals("dev"));for(InetAddress a:InetAddress.getAllByName(u.getHost()))if(!local&&(a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress()||a.isMulticastAddress()))throw new BizException(400,"Private network origins cannot be verified outside local development");}catch(BizException e){throw e;}catch(Exception e){throw new BizException(400,"Origin host could not be safely resolved");}}
 private String environment(String e){String x=e==null?"TEST":e.toUpperCase(Locale.ROOT);if(!"TEST".equals(x))throw new BizException(400,"Only TEST redirect origins are available");return x;}
 private String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 public record Registration(Long id,String origin,String verificationToken,String instructions){}
 public record RegisterCommand(String environment,String origin){}
 public record OriginView(Long id,String environment,String origin,boolean verified,LocalDateTime createdAt,LocalDateTime verifiedAt){}
}
