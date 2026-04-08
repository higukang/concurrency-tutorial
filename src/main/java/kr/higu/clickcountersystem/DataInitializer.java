package kr.higu.clickcountersystem;

import jakarta.annotation.PostConstruct;
import kr.higu.clickcountersystem.entity.Counter;
import kr.higu.clickcountersystem.repository.CounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final CounterRepository counterRepository;

    @PostConstruct
    public void init() {
        if (!counterRepository.existsById(1L)) {
            counterRepository.save(new Counter(1L, 0L));
        }
    }
}
