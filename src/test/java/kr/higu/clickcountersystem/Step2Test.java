package kr.higu.clickcountersystem;

import kr.higu.clickcountersystem.service.Step2CounterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class Step2Test {

    @Autowired
    private Step2CounterService step2CounterService;

    @Test
    void 동시에_1000번_증가() throws InterruptedException {
        int threadCount = 1000;

        ExecutorService executorService = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    step2CounterService.increase();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Long count = step2CounterService.getCount();
        System.out.println("최종 count = " + count);
    }
}
