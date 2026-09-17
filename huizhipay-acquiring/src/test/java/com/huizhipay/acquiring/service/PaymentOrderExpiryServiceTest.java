package com.huizhipay.acquiring.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.huizhipay.acquiring.entity.PaymentOrder;
import com.huizhipay.acquiring.mapper.PaymentOrderMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOrderExpiryServiceTest {
    @Mock PaymentOrderMapper mapper;

    @BeforeAll static void metadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), PaymentOrder.class);
    }

    @Test void expiresPendingCheckoutOrdersIncludingLegacyRowsWithoutExpireAt() {
        when(mapper.update(isNull(), any())).thenReturn(2);

        int expired = new PaymentOrderExpiryService(mapper).expirePendingOrders();

        assertThat(expired).isEqualTo(2);
        @SuppressWarnings("unchecked") ArgumentCaptor<Wrapper<PaymentOrder>> update = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).update(isNull(), update.capture());
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) update.getValue();
        assertThat(update.getValue().getSqlSegment()).contains("status", "channel", "expire_at", "created_at");
        assertThat(wrapper.getParamNameValuePairs().values())
                .contains(PaymentOrder.PaymentStatus.PENDING, "DUMMY", TestPaymentService.CHANNEL,
                        PaymentOrder.PaymentStatus.FAILED, "EXPIRED");
    }
}
