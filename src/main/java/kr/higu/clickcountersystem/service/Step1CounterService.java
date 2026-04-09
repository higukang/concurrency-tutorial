package kr.higu.clickcountersystem.service;

import kr.higu.clickcountersystem.entity.Counter;
import kr.higu.clickcountersystem.repository.CounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class Step1CounterService {

    private static final Long COUNTER_ID = 1L;

    private final CounterRepository counterRepository;

    @Transactional(readOnly = true)
    public Long getCount() {
        Counter counter = counterRepository.findById(COUNTER_ID)
                .orElseThrow(() -> new IllegalStateException("Counter row does not exist"));
        return counter.getClickCount();
    }

    // step1 - naive
    // 가장 단순한 read -> modify -> write
    // 동시 요청이 몰리면 같은 값을 읽고 서로 덮어쓰는 lost update가 발생할 수 있음
    @Transactional
    public void increase() {
        Counter counter = counterRepository.findById(COUNTER_ID)
                .orElseThrow(() -> new IllegalStateException("Counter row does not exist"));

        counter.increase();
    }
}
