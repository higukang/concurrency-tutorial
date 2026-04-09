package kr.higu.clickcountersystem.service;

import kr.higu.clickcountersystem.entity.Counter;
import kr.higu.clickcountersystem.repository.CounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class Step3CounterService {

    private static final Long COUNTER_ID = 1L;

    private final CounterRepository counterRepository;

    @Transactional(readOnly = true)
    public Long getCount() {
        Counter counter = counterRepository.findById(COUNTER_ID)
                .orElseThrow(() -> new IllegalStateException("Counter row does not exist"));
        return counter.getClickCount();
    }

    // step3 - DB atomic update
    // 애플리케이션이 값을 읽고 수정하지 않고, DB가 count = count + 1 을 직접 수행
    // lost update는 막을 수 있지만 단일 row write hotspot이 존재
    @Transactional
    public void increase() {
        int updatedRowCount = counterRepository.increaseAtomic();
        if (updatedRowCount != 1) {
            throw new IllegalStateException("Counter update failed");
        }
    }
}
