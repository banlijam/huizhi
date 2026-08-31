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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<?>> handleBiz(BizException e) {
        return response(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<R<?>> handleAuth(AuthException e) {
        return response(400, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<R<?>> handleBadCredentials(BadCredentialsException e) {
        return response(401, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<?>> handleValidation(MethodArgumentNotValidException e) {
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
        return response(400, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<?>> handleSys(Exception e) {
        log.error("Unhandled request failure", e);
        return response(500, I18nUtils.get("system.busy"));
    }

    private ResponseEntity<R<?>> response(int code, String message) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            status = code >= 500 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(R.fail(code, message));
    }
}
