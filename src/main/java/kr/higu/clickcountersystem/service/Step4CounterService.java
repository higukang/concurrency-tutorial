package kr.higu.clickcountersystem.service;

import kr.higu.clickcountersystem.repository.CounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class Step4CounterService {
    // step4 - Redis에서 실시간 카운트로 사용할 key
    private static final String REDIS_COUNT_KEY = "counter:count";
    // step4 - 마지막 DB flush 이후 누적 증가분을 저장할 key
    private static final String REDIS_DELTA_KEY = "counter:delta";

    private final CounterRepository counterRepository;
    private final StringRedisTemplate redisTemplate;

    // step4 - Redis를 실시간 카운터로 사용
    // 요청마다 DB에 직접 요청하지 않고 Redis의 원자적 증가 연산으로 count를 올린다.
    public void increase() {
        initializeIfAbsent();

        redisTemplate.opsForValue().increment(REDIS_COUNT_KEY);
        redisTemplate.opsForValue().increment(REDIS_DELTA_KEY);
    }

    // step4 - Step4 기준 현재 카운트 조회
    // DB가 아니라 Redis에 있는 실시간 값을 반환한다.
    public Long getCount() {
        initializeIfAbsent();

        String value = redisTemplate.opsForValue().get(REDIS_COUNT_KEY);
        if (value == null) {
            return 0L;
        }

        return Long.parseLong(value);
    }

    // step4 - Redis에 쌓인 delta를 DB로 반영
    // flush 시점에는 delta만큼 DB에 한 번에 누적 반영한다.
    // count 전체를 덮어쓰지 않고 증가분만 더하는 방식이라 DB 정합성을 유지하기 쉽다.
    @Transactional
    public void flushToDb() {
        initializeIfAbsent();

        String deltaValue = redisTemplate.opsForValue().getAndDelete(REDIS_DELTA_KEY);
        if (deltaValue == null) {
            redisTemplate.opsForValue().setIfAbsent(REDIS_DELTA_KEY, "0");
            return;
        }

        long delta = Long.parseLong(deltaValue);
        if (delta <= 0) {
            redisTemplate.opsForValue().set(REDIS_DELTA_KEY, "0");
            return;
        }

        try {
            int updatedRowCount = counterRepository.increaseBy(delta);
            if (updatedRowCount != 1) {
                throw new IllegalStateException("Counter update failed");
            }
        } catch (RuntimeException e) {
            redisTemplate.opsForValue().increment(REDIS_DELTA_KEY, delta);
            throw e;
        }

        redisTemplate.opsForValue().setIfAbsent(REDIS_DELTA_KEY, "0");
    }

    // step4 - 앱 시작 직후 또는 첫 요청 시 Redis 초기화
    // Redis 값이 비어 있으면 DB count를 기준으로 count key를 맞춘다.
    public void initializeIfAbsent() {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_COUNT_KEY))
                && Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_DELTA_KEY))) {
            return;
        }

        Long dbCount = counterRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Counter row does not exist"))
                .getClickCount();

        redisTemplate.opsForValue().setIfAbsent(REDIS_COUNT_KEY, String.valueOf(dbCount));
        redisTemplate.opsForValue().setIfAbsent(REDIS_DELTA_KEY, "0");
    }
}
