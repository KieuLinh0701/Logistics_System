package com.logistics.scheduler.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PickupScheduler {

    private static final int STAGE_1_DELAY_MINUTES = 1;    // 15
    private static final int STAGE_2_DELAY_MINUTES = 2;     // 60
    private static final int URGENT_DELAY_MINUTES  = 3;    // 90

    private final PickupStageProcessor processor;

    @Scheduled(fixedDelay = 60 * 1000)      // 5 phút 5*60 * 1000
    public void processPickupNotifications() {
        LocalDateTime now = LocalDateTime.now();
        processor.processStage1(now, STAGE_1_DELAY_MINUTES);
        processor.processStage2(now, STAGE_2_DELAY_MINUTES);
        processor.processUrgent(now, URGENT_DELAY_MINUTES);
    }
}