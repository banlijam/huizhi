package com.huizhipay.risk.service;

import com.huizhipay.risk.dto.DeveloperQueryLogResponse;
import com.huizhipay.risk.mapper.QueryLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeveloperQueryLogService {

    private final QueryLogMapper queryLogMapper;

    public List<DeveloperQueryLogResponse> latestForMerchant(String merchantId) {
        return queryLogMapper.selectLatestForMerchant(merchantId);
    }
}
