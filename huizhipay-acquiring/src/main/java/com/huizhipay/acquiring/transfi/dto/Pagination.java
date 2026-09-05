package com.huizhipay.acquiring.transfi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 分页信息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pagination {
    /** 总记录数 */
    private Integer total;
    /** 总页数 */
    private Integer pages;
    /** 当前页码 */
    private Integer currentPage;
    /** 每页记录数 */
    private Integer limit;
    /** 是否有下一页 */
    private Boolean hasNext;
    /** 是否有上一页 */
    private Boolean hasPrev;
}
