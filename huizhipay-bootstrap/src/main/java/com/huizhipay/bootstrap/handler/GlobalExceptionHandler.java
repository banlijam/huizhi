package com.huizhipay.bootstrap.handler;

import com.huizhipay.common.exceptions.BizException;
import com.huizhipay.common.i18n.I18nUtils;
import com.huizhipay.common.model.R;
import com.huizhipay.user.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BizException.class)
    public R<?> handleBiz(BizException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    public R<?> handleAuth(AuthException e) {
        return R.fail(400, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public R<?> handleBadCredentials(BadCredentialsException e) {
        return R.fail(400, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> {
                    String msg = error.getDefaultMessage();
                    try {
                        return messageSource.getMessage(msg, null, msg, LocaleContextHolder.getLocale());
                    } catch (Exception ex) {
                        return msg;
                    }
                })
                .orElse(I18nUtils.get("validate.failed"));
        return R.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleSys(Exception e) {
        log.error("", e);
        return R.fail(500, I18nUtils.get("system.busy"));
    }
}