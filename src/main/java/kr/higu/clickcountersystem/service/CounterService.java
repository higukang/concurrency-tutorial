package kr.higu.clickcountersystem.service;

import kr.higu.clickcountersystem.repository.CounterRepository;
import kr.higu.clickcountersystem.entity.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CounterService {

    private static final Long COUNTER_ID = 1L;

    private final CounterRepository counterRepository;

    @Transactional(readOnly = true)
    public Long getCount() {
        Counter counter = counterRepository.findById(COUNTER_ID)
                .orElseThrow(() -> new IllegalStateException("Counter row does not exist"));
        return counter.getClickCount();
    }

    @Transactional
    public void increase() {
        Counter counter = counterRepository.findById(COUNTER_ID)
                .orElseThrow(() -> new IllegalStateException("Counter row does not exist"));

        counter.increase();
    }

    // 개선 step1 - synchronized 적용
    @Transactional
    public synchronized void increaseSync() {
        Counter counter = counterRepository.findById(COUNTER_ID)
                .orElseThrow();

        counter.increase();
    }
}
