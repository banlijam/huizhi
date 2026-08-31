package com.huizhipay.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import org.slf4j.MDC;

@Data
@AllArgsConstructor
@Accessors(chain = true)
public class R<T> {
    private Integer code = 200;
    private String message;
    private T data;
    private Long timestamp;
    private String traceId;

    public R() {
        setTimestamp(System.currentTimeMillis());
        setTraceId(MDC.get("traceId"));
    }

    public R(T data) {
        this();
        setCode(200);
        setMessage("SUCCESS");
        setData(data);
    }

    public R(Integer code, String message) {
        this();
        setCode(code);
        setMessage(message);
    }

    public static R<Void> ok(String msg) {
        return new R<Void>().setMessage(msg);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(data);
    }

    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message);
    }
}
