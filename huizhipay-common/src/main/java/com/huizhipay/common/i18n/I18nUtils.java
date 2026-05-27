package com.huizhipay.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class I18nUtils {

    private static MessageSource messageSource;

    public I18nUtils(MessageSource messageSource) {
        I18nUtils.messageSource = messageSource;
    }

    public static String get(String key) {
        return get(key, (Object[]) null);
    }

    public static String get(String key, Object... args) {
        if (messageSource == null) {
            return key;
        }
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(key, args, key, locale);
        } catch (Exception e) {
            return key;
        }
    }

    public static String get(String key, Locale locale, Object... args) {
        if (messageSource == null) {
            return key;
        }
        try {
            return messageSource.getMessage(key, args, key, locale);
        } catch (Exception e) {
            return key;
        }
    }
}
