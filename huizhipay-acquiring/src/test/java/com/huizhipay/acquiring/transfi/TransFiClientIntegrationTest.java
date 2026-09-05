package com.huizhipay.acquiring.transfi;

import com.huizhipay.acquiring.config.AppConfig;
import com.huizhipay.acquiring.transfi.dto.OrderListData;
import com.huizhipay.acquiring.transfi.dto.TransFiOrder;
import com.huizhipay.acquiring.transfi.dto.TransFiResponse;
import com.huizhipay.acquiring.transfi.dto.TransFiUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TransFiClient 真实接口集成测试
 *
 * <p>调用 TransFi Sandbox 环境 API，验证：
 * <ul>
 *   <li>HTTP 连接是否可达</li>
 *   <li>MID / Authorization header 是否正确</li>
 *   <li>接口返回的 JSON 能否正确反序列化到 DTO</li>
 * </ul>
 *
 * <p>默认跳过（CI 环境不带此变量）。需要执行时设置环境变量即可：
 * <pre>
 *   Windows: $env:TRANSFI_LIVE_TEST="true"; mvn test -Dtest=TransFiClientIntegrationTest
 *   Linux/Mac: TRANSFI_LIVE_TEST=true mvn test -Dtest=TransFiClientIntegrationTest
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "TRANSFI_LIVE_TEST", matches = "true")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AppConfig.class)
@TestPropertySource(properties = {
        "client.transfi.url=https://sandbox-api.transfi.com/v3",
        "client.transfi.mid=HODQSB_NA_NA",
        "client.transfi.authorization=Basic aG9uZ2tvbmdodWl6aGl0ZWNobm9sb2d5ZGV2ZWxvcG1lbnRsaW1pdGVkOnpsTkdXaVFBT3dpcFN3"
})
class TransFiClientIntegrationTest {

    @Autowired
    TransFiClient client;

    // ==================== Users ====================

    @Test
    void queryUsers_individual_returnsSuccess() {
        TransFiResponse<List<TransFiUser>> response = client.queryUsers(1, 10, null, null, null);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData()).isNotNull();
        if (!response.getData().isEmpty()) {
            TransFiUser first = response.getData().getFirst();
            System.out.println("[queryUsers] first userId=" + first.getUserId()
                    + ", type=" + first.getType()
                    + ", email=" + first.getEmail());
        }
    }

    @Test
    void listBusinessUsers_returnsSuccess() {
        TransFiResponse<List<TransFiUser>> response = client.listBusinessUsers(1, 5, null, null, null);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData()).isNotNull();
    }

    // ==================== Orders ====================

    @Test
    void listOrders_returnsSuccess() {
        TransFiResponse<OrderListData> response = client.listOrders(1, 10, null, null, null, null, null, null);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData()).isNotNull();
        List<TransFiOrder> transfers = response.getData().getTransfers();
        if (!transfers.isEmpty()) {
            TransFiOrder first = transfers.getFirst();
            System.out.println("[listOrders] first id=" + first.getId()
                    + ", type=" + first.getType()
                    + ", status=" + first.getStatus());
        }
    }
}
