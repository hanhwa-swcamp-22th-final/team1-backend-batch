package com.conk.batch.billing.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.conk.batch.billing.service.MonthlyBillingCalculationService;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MonthlyBillingSchedulerTest {

    @Mock
    private MonthlyBillingCalculationService monthlyBillingCalculationService;

    @InjectMocks
    private MonthlyBillingScheduler monthlyBillingScheduler;

    @Test
    @DisplayName("월 정산 스케줄러 실행 시 서울 시간 기준 전월 정산을 호출한다")
    void run_success() {
        ReflectionTestUtils.setField(monthlyBillingScheduler, "zone", "Asia/Seoul");

        monthlyBillingScheduler.run();

        ArgumentCaptor<YearMonth> captor = ArgumentCaptor.forClass(YearMonth.class);
        verify(monthlyBillingCalculationService).calculateAndPublish(captor.capture());
        assertEquals(
                YearMonth.from(LocalDate.now(ZoneId.of("Asia/Seoul")).minusMonths(1)),
                captor.getValue()
        );
    }

    @Test
    @DisplayName("월 정산 스케줄러 실행 실패: 서비스 예외를 삼키지 않고 그대로 전파한다")
    void run_whenServiceThrows_thenPropagate() {
        ReflectionTestUtils.setField(monthlyBillingScheduler, "zone", "Asia/Seoul");
        doThrow(new BusinessException(
                BatchErrorCode.MONTHLY_BILLING_CALCULATION_FAILED,
                "monthly billing failed"
        ))
                .when(monthlyBillingCalculationService)
                .calculateAndPublish(any(YearMonth.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> monthlyBillingScheduler.run());

        assertEquals(BatchErrorCode.MONTHLY_BILLING_CALCULATION_FAILED, exception.getErrorCode());
        assertEquals("monthly billing failed", exception.getMessage());
    }
}
