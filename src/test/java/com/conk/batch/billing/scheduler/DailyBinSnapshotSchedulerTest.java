package com.conk.batch.billing.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.conk.batch.billing.service.DailyBinSnapshotService;
import com.conk.batch.common.exception.BatchErrorCode;
import com.conk.batch.common.exception.BusinessException;
import java.time.LocalDate;
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
class DailyBinSnapshotSchedulerTest {

    @Mock
    private DailyBinSnapshotService dailyBinSnapshotService;

    @InjectMocks
    private DailyBinSnapshotScheduler dailyBinSnapshotScheduler;

    @Test
    @DisplayName("일별 bin snapshot 스케줄러 실행 시 서울 시간 기준 오늘 날짜로 적재를 호출한다")
    void run_success() {
        ReflectionTestUtils.setField(dailyBinSnapshotScheduler, "zone", "Asia/Seoul");

        dailyBinSnapshotScheduler.run();

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(dailyBinSnapshotService).captureDailySnapshots(captor.capture());
        assertEquals(LocalDate.now(ZoneId.of("Asia/Seoul")), captor.getValue());
    }

    @Test
    @DisplayName("일별 bin snapshot 스케줄러 실행 실패: 서비스 예외를 삼키지 않고 그대로 전파한다")
    void run_whenServiceThrows_thenPropagate() {
        ReflectionTestUtils.setField(dailyBinSnapshotScheduler, "zone", "Asia/Seoul");
        doThrow(new BusinessException(BatchErrorCode.WMS_BIN_COUNT_FETCH_FAILED, "snapshot failed"))
                .when(dailyBinSnapshotService)
                .captureDailySnapshots(any(LocalDate.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> dailyBinSnapshotScheduler.run());

        assertEquals(BatchErrorCode.WMS_BIN_COUNT_FETCH_FAILED, exception.getErrorCode());
        assertEquals("snapshot failed", exception.getMessage());
    }
}
