package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * TransFi API 统一响应体
 *
 * @param <T> 数据体类型
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransFiResponse<T> {
    /** 请求状态：success / failure */
    private String status;
    /** 响应数据 */
    private T data;
    /** 分页信息（列表接口返回） */
    private Pagination pagination;
    /** 链路追踪 ID */
    private String traceId;
}
