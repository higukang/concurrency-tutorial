package kr.higu.clickcountersystem;

import kr.higu.clickcountersystem.service.Step1CounterService;
import kr.higu.clickcountersystem.service.Step4CounterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class Step4Test {

    @Autowired
    private Step4CounterService step4CounterService;

    @Autowired
    private Step1CounterService step1CounterService;

    @Test
    void 동시에_1000번_증가_redis_flush() throws InterruptedException {
        int threadCount = 1000;

        // 이전 테스트에서 남아 있을 수 있는 delta를 먼저 DB로 반영해 기준점을 맞춘다.
        step4CounterService.flushToDb();

        Long dbBefore = step1CounterService.getCount();
        Long redisBefore = step4CounterService.getCount();

        ExecutorService executorService = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    step4CounterService.increase();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Long redisAfter = step4CounterService.getCount();
        assertEquals(threadCount, redisAfter - redisBefore);

        step4CounterService.flushToDb();

        Long dbAfter = step1CounterService.getCount();
        assertEquals(threadCount, dbAfter - dbBefore);

        System.out.println("Redis 증가분 = " + (redisAfter - redisBefore));
        System.out.println("DB 증가분 = " + (dbAfter - dbBefore));
    }
}
