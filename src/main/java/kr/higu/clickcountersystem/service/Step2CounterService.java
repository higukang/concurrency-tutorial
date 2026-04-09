package kr.higu.clickcountersystem.service;

import kr.higu.clickcountersystem.entity.Counter;
import kr.higu.clickcountersystem.repository.CounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class Step2CounterService {

    private static final Long COUNTER_ID = 1L;

    private final CounterRepository counterRepository;

    @Transactional(readOnly = true)
    public Long getCount() {
        Counter counter = counterRepository.findById(COUNTER_ID)
                .orElseThrow(() -> new IllegalStateException("Counter row does not exist"));
        return counter.getClickCount();
    }

    // step2 - synchronized
    // JVM 안에서는 한 번에 하나의 스레드만 이 메서드에 진입하게 함
    // DB 커밋 시점까지 잠그는 것은 아니므로 정합성을 완전히 보장하지는 못함
    @Transactional
    public synchronized void increase() {
        Counter counter = counterRepository.findById(COUNTER_ID)
                .orElseThrow(() -> new IllegalStateException("Counter row does not exist"));

        counter.increase();
    }
}
