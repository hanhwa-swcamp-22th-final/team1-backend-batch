package com.conk.batch.billing.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.conk.batch.billing.client.dto.BinCountSummaryResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import feign.FeignException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class WmsBillingQueryClientIntegrationTest {

    private static final AtomicReference<HttpHandler> CURRENT_HANDLER = new AtomicReference<>(exchange ->
            writeResponse(exchange, 404, "not-found", "text/plain"));
    private static final AtomicReference<String> LAST_REQUEST_PATH = new AtomicReference<>();

    private static HttpServer mockWmsServer;

    @Autowired
    private WmsBillingQueryClient wmsBillingQueryClient;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        ensureServerStarted();
        registry.add("app.clients.wms.base-url", () -> "http://127.0.0.1:" + mockWmsServer.getAddress().getPort());
    }

    @BeforeEach
    void setUp() {
        ensureServerStarted();
        LAST_REQUEST_PATH.set(null);
    }

    @AfterAll
    static void tearDown() {
        if (mockWmsServer != null) {
            mockWmsServer.stop(0);
            mockWmsServer = null;
        }
    }

    @Test
    @DisplayName("WMS 내부 bin count API 호출 성공: baseDate 쿼리로 요청하고 seller별 점유 bin 수 응답을 역직렬화한다")
    void getBinCountSummaries_success() {
        CURRENT_HANDLER.set(exchange -> writeJsonResponse(exchange, 200, """
                [
                  {
                    "sellerId": "SELLER-001",
                    "warehouseId": "WH-001",
                    "occupiedBinCount": 3
                  },
                  {
                    "sellerId": "SELLER-002",
                    "warehouseId": "WH-001",
                    "occupiedBinCount": 1
                  }
                ]
                """));

        List<BinCountSummaryResponse> result = wmsBillingQueryClient.getBinCountSummaries(LocalDate.of(2026, 4, 10));

        assertThat(LAST_REQUEST_PATH.get()).isEqualTo("/wms/internal/billing/bin-counts?baseDate=2026-04-10");
        assertThat(result)
                .containsExactly(
                        new BinCountSummaryResponse("SELLER-001", "WH-001", 3),
                        new BinCountSummaryResponse("SELLER-002", "WH-001", 1)
                );
    }

    @Test
    @DisplayName("WMS 내부 bin count API 호출 실패: 서버가 500을 반환하면 Feign 예외를 전파한다")
    void getBinCountSummaries_whenWmsReturns500_thenThrow() {
        CURRENT_HANDLER.set(exchange -> writeJsonResponse(exchange, 500, """
                {
                  "success": false,
                  "code": "COMMON-002",
                  "message": "internal error"
                }
                """));

        assertThatThrownBy(() -> wmsBillingQueryClient.getBinCountSummaries(LocalDate.of(2026, 4, 10)))
                .isInstanceOfSatisfying(FeignException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(500);
                    assertThat(exception.getMessage()).contains("Internal Server Error");
                });
        assertThat(LAST_REQUEST_PATH.get()).isEqualTo("/wms/internal/billing/bin-counts?baseDate=2026-04-10");
    }

    private static synchronized void ensureServerStarted() {
        if (mockWmsServer != null) {
            return;
        }

        try {
            mockWmsServer = HttpServer.create(new InetSocketAddress(0), 0);
            mockWmsServer.createContext("/wms/internal/billing/bin-counts", exchange -> {
                LAST_REQUEST_PATH.set(exchange.getRequestURI().toString());
                CURRENT_HANDLER.get().handle(exchange);
            });
            mockWmsServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to start mock WMS server", exception);
        }
    }

    private static void writeJsonResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        writeResponse(exchange, statusCode, body, "application/json");
    }

    private static void writeResponse(
            HttpExchange exchange,
            int statusCode,
            String body,
            String contentType
    ) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBody);
        } finally {
            exchange.close();
        }
    }
}
