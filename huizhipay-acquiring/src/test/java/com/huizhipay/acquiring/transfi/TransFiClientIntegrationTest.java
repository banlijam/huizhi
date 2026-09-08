package com.huizhipay.acquiring.transfi;

import com.huizhipay.acquiring.config.AppConfig;
import com.huizhipay.acquiring.transfi.dto.*;
import com.huizhipay.acquiring.transfi.util.PdfUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Test
    void createOrder() {
        TransFiResponse<TransFiOrder> response = client.createOrder(new CreateOrderRequest()
                .setUserId("test@gmail.com")
                .setPartnerId("test-fi-1")
                .setCustomerMetaData(new HashMap<>())
                .setSourceUrl("https://www.huizhipay.org/")
                .setSuccessRedirectUrl("https://www.huizhipay.org/success")
                .setFailureRedirectUrl("https://www.huizhipay.org/failure")
                .setHeadlessMode(false)
                .setPurposeCode("service_charges")
                .setOrderType("payin")
                .setInvoiceId("IN-260908153234882457856001992")
                .setDeviceDetails(new CreateOrderRequest.DeviceDetails()
                        .setIpInfo(new CreateOrderRequest.IpInfo()
                                .setIp("127.0.0.1")))
                .setSource(new OrderSource()
                        .setUserId("UX-250512094340333")
                        .setCurrency("USD")
                        .setAmount("")
                        .setAdditionalPaymentDetails(new HashMap<>())
                        .setSendersWalletAddress("")
                        .setPaymentType("card")  // 'bank_transfer' | 'card' | 'local_wallet'
                        .setPaymentCode(""))
                .setDestination(new OrderDestination()
                        .setCurrency("USDT")
                        .setAmount("")
                        .setQrCode("")
                        .setAdditionalPaymentDetails(new HashMap<>())
                        .setWalletAddress("")
                        .setPaymentType("local_wallet") // 'bank_transfer' | 'card' | 'local_wallet'
                        .setPaymentCode("")
                        .setUserId(""))
                .setCustomization(new CreateOrderRequest.Customization("en")));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData()).isNotNull();
        System.out.println(response);
    }

    // ==================== Invoices ====================

    @Test
    void renderInvoice_toLocalFile() throws IOException {
        Map<String, String> data = buildInvoiceData();
        Resource pdf = PdfUtil.renderInvoice(data);

        assertThat(pdf).isNotNull();
        assertThat(pdf.getFilename()).startsWith("invoice-");

        byte[] pdfBytes;
        try (InputStream is = pdf.getInputStream()) {
            pdfBytes = is.readAllBytes();
        }
        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes[0]).isEqualTo((byte) '%'); // PDF magic: %PDF-1.x

        Path target = Paths.get(System.getProperty("java.io.tmpdir"),
                "huizhipay-invoices", pdf.getFilename());
        Files.createDirectories(target.getParent());
        Files.write(target, pdfBytes);

        System.out.println("[renderInvoice_toLocalFile] written=" + target.toAbsolutePath()
                + ", size=" + pdfBytes.length + " bytes");
    }

    @Test
    void uploadInvoice_returnsInvoiceId() {
        Map<String, String> data = buildInvoiceData();
        Resource pdf = PdfUtil.renderInvoice(data);

        assertThat(pdf).isNotNull();
        assertThat(pdf.getFilename()).startsWith("invoice-");
        System.out.println("[uploadInvoice] filename=" + pdf.getFilename());

        TransFiResponse<UploadInvoiceResponse> response = client.uploadInvoice(
                pdf, "deposit", "UX-250512094340333", "invoice");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getInvoiceId()).isNotBlank();
        System.out.println("[uploadInvoice] invoiceId=" + response.getData().getInvoiceId());
    }

    /**
     * 构造发票占位数据，两个测试共用。
     */
    private static Map<String, String> buildInvoiceData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("order_id", "ORD-20260908-0001");
        data.put("created_at", "2026-09-08 15:23:41 UTC");
        data.put("buyer_name", "Alice Demo");
        data.put("buyer_email_masked", "a****@example.com");
        data.put("buyer_country", "US");
        data.put("merchant_clean_name", "HuizhiPay Demo Merchant");
        data.put("safe_product_description", "Digital Gift Card — $100");
        data.put("mapped_sku", "SKU-GC-100");
        data.put("fiat_amount", "100.00");
        data.put("fiat_currency", "USD");
        return data;
    }
}
