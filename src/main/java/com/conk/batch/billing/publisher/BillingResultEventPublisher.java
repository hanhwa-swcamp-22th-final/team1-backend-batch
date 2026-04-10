package com.conk.batch.billing.publisher;

import com.conk.batch.billing.domain.MonthlyFeeSnapshot;
import com.conk.batch.billing.domain.SellerMonthlyBilling;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 월 정산 결과를 Kafka로 발행한다.
 */
@Component
@RequiredArgsConstructor
public class BillingResultEventPublisher {

    @Value("${app.kafka.topics.billing-monthly-result:billing.monthly.result.v1}")
    private String billingMonthlyResultTopic;

    private final KafkaTemplate<String, BillingMonthlyResultEvent> billingResultKafkaTemplate;

    public void publish(SellerMonthlyBilling billing, MonthlyFeeSnapshot feeSnapshot) {
        BillingMonthlyResultEvent event = BillingMonthlyResultEvent.from(billing, feeSnapshot);
        billingResultKafkaTemplate.send(billingMonthlyResultTopic, event.kafkaKey(), event);
    }
}
