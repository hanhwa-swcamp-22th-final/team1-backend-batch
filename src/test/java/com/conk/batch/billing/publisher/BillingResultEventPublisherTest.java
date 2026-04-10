package com.conk.batch.billing.publisher;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.domain.SellerMonthlyBilling;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BillingResultEventPublisherTest {

    @Mock
    private KafkaTemplate<String, BillingMonthlyResultEvent> billingResultKafkaTemplate;

    @InjectMocks
    private BillingResultEventPublisher billingResultEventPublisher;

    @Test
    @DisplayName("월 정산 결과 발행 성공: topic, key, payload로 KafkaTemplate.send를 호출한다")
    void publish_success() {
        ReflectionTestUtils.setField(
                billingResultEventPublisher,
                "billingMonthlyResultTopic",
                "billing.monthly.result.v1"
        );
        SellerMonthlyBilling billing = SellerMonthlyBilling.calculated(
                "2026-03",
                "SELLER-001",
                "WH-001",
                87,
                new BigDecimal("2.81"),
                new BigDecimal("80085"),
                3,
                new BigDecimal("7500"),
                1,
                new BigDecimal("2500")
        );
        MonthlyFeeSnapshot feeSnapshot = MonthlyFeeSnapshot.of(
                "2026-03",
                "SELLER-001",
                "WH-001",
                new BigDecimal("28500"),
                new BigDecimal("2500"),
                new BigDecimal("2500")
        );

        BillingMonthlyResultEvent event = BillingMonthlyResultEvent.from(billing, feeSnapshot);

        billingResultEventPublisher.publish(billing, feeSnapshot);

        verify(billingResultKafkaTemplate).send(
                eq("billing.monthly.result.v1"),
                eq("2026-03:SELLER-001"),
                eq(event)
        );
    }
}
