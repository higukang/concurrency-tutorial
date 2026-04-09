## 4. Redis Atomic Increment 적용

> - Step4는 실시간 증가를 DB가 아니라 Redis에서 먼저 처리하고, DB에는 누적된 증가분만 반영
> - 클릭 요청 경로를 가볍게 만들어 throughput을 크게 끌어올리는 방향
> - 대신 DB는 즉시 최신값이 아니라 flush 이후에 따라오는 구조

---

### 구현 방식

- 요청마다 DB row를 직접 update하지 않음
- Redis에서 실시간 count와 미반영 증가분(delta)을 함께 관리
- flush 시점에 delta만큼 DB에 한 번에 반영

```text
POST /api/click4
  -> INCR counter:count
  -> INCR counter:delta

POST /api/flush
  -> GETDEL counter:delta
  -> DB에 click_count = click_count + delta
```

```java
public void increase() {
    initializeIfAbsent();

    redisTemplate.opsForValue().increment(REDIS_COUNT_KEY);
    redisTemplate.opsForValue().increment(REDIS_DELTA_KEY);
}

@Transactional
public void flushToDb() {
    initializeIfAbsent();

    String deltaValue = redisTemplate.opsForValue().getAndDelete(REDIS_DELTA_KEY);
    long delta = Long.parseLong(deltaValue);

    int updatedRowCount = counterRepository.increaseBy(delta);
    if (updatedRowCount != 1) {
        throw new IllegalStateException("Counter update failed");
    }
}
```

### 개선 의도

- 요청마다 DB 단일 row를 직접 갱신하지 않음
- 실시간 증가 처리를 Redis의 원자적 연산으로 처리
- DB에는 누적 증가분만 반영해 write 부담 감소
- Step3의 단일 row hotspot 문제를 완화

### 결과

#### 1. JUnit 테스트

- 1000번 요청
- Redis 증가분: 1000
- DB 증가분: 1000

```text
Redis 증가분 = 1000
DB 증가분 = 1000
BUILD SUCCESSFUL in 4s
```

#### 2. k6 테스트

- 151683 요청
- Redis count: 151683
- Redis delta: 151683

`k6 run click-test4.js`

```text
         /\      Grafana   /‾‾/
    /\  /  \     |\  __   /  /
   /  \/    \    | |/ /  /   ‾‾\
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/


     execution: local
        script: click-test4.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 40s max duration (incl. graceful stop):
              * default: 100 looping VUs for 10s (gracefulStop: 30s)



  █ TOTAL RESULTS

    HTTP
    http_req_duration..............: avg=6.56ms min=2.14ms med=5.89ms max=582.48ms p(90)=7.94ms p(95)=8.91ms
      { expected_response:true }...: avg=6.56ms min=2.14ms med=5.89ms max=582.48ms p(90)=7.94ms p(95)=8.91ms
    http_req_failed................: 0.00%  0 out of 151683
    http_reqs......................: 151683 15163.211226/s

    EXECUTION
    iteration_duration.............: avg=6.58ms min=2.15ms med=5.91ms max=585.08ms p(90)=7.97ms p(95)=8.93ms
    iterations.....................: 151683 15163.211226/s
    vus............................: 100    min=100         max=100
    vus_max........................: 100    min=100         max=100

    NETWORK
    data_received..................: 11 MB  1.1 MB/s
    data_sent......................: 15 MB  1.5 MB/s




running (10.0s), 000/100 VUs, 151683 complete and 0 interrupted iterations
default ✓ [======================================] 100 VUs  10s
```

### 분석

- JUnit 기준 Redis와 DB 증가분이 모두 1000으로 맞음
- k6 기준 Step3보다 훨씬 많은 요청을 같은 시간 안에 처리
- 평균 응답 시간이 약 6.56ms로 크게 감소
- Redis는 `INCR`로 실시간 증가를 처리하므로 유실 없이 빠르게 요청을 받음

### Step3 대비 개선 정도

Step3와 Step4의 k6 결과를 직접 비교하면 다음과 같습니다.

| 항목 | Step3 | Step4 | 변화 |
|------|-------|-------|------|
| 총 요청 수 | 28223 | 151683 | 약 5.38배 증가 |
| 평균 응답 시간 | 35.45ms | 6.56ms | 약 81.5% 감소 |
| 정합성 | 요청 수와 반영 수 일치 | Redis/DB 증가분 모두 일치 | 둘 다 확보 |

즉 Step4는 Step3와 비교했을 때 정합성은 유지하면서도, 같은 10초 동안 처리한 요청 수가 약 5배 이상 증가했습니다.

### 왜 빨라졌는가

- Step3는 요청마다 DB row 하나를 직접 update
- 같은 row에 write가 몰리면서 DB 내부 대기와 lock 경합이 발생
- Step4는 실시간 증가를 Redis 메모리 연산으로 처리
- DB는 나중에 누적된 delta만 반영하므로 요청 경로가 훨씬 가벼움

조금 더 풀어보면:

- Step3는 요청 1건마다 DB가 직접 `click_count = click_count + 1` 수행
- 결국 같은 row 하나가 write hotspot이 됨
- row lock, transaction, commit, log 기록 비용 때문에 응답 시간이 늘어남
- k6에서는 VU가 응답을 기다리는 시간이 길어져 같은 시간 안에 보낼 수 있는 총 요청 수도 줄어듦

반면 Step4는:

- 요청 1건마다 Redis `INCR`만 수행
- Redis도 단일 key hotspot은 맞지만, 메모리 기반 원자 연산이라 처리 비용이 훨씬 작음
- DB는 매 요청마다 직접 쓰지 않고, 나중에 `delta`만 한 번에 반영
- 그 결과 실시간 요청 경로가 가벼워지고 throughput이 크게 증가

### 장점

- 정합성 확보
- 높은 throughput
- 낮은 응답 시간
- DB direct write 빈도 감소

### 한계점

- Redis도 단일 key 기준으로는 hotspot
- DB와 Redis 값이 flush 전에는 다를 수 있음
- 현재 k6 결과의 `counter:delta = 151683`은 아직 DB flush 전 상태를 의미
- 즉시 일관성이 아니라 eventual consistency 구조

### 언제 Redis를 쓰고, 언제 DB atomic update를 쓰면 좋은가

둘 다 저장소 레벨의 원자적 연산을 사용한다는 점은 같습니다.  
차이는 실시간 증가를 어디서 처리하느냐, 그리고 즉시 일관성을 얼마나 중요하게 보느냐입니다.

| 기준 | Step3 - DB atomic update | Step4 - Redis atomic increment |
|------|---------------------------|--------------------------------|
| 실시간 증가 처리 위치 | DB | Redis |
| 정합성 | 강한 정합성에 가까움 | 실시간은 Redis 기준, DB는 flush 후 반영 |
| 응답 경로 | 요청마다 DB write | 요청마다 Redis write |
| throughput | 단일 row hotspot에 취약 | Step3보다 훨씬 유리 |
| 운영 복잡도 | 상대적으로 단순 | Redis 운영, flush, 장애 복구 고려 필요 |
| 적합한 상황 | 정확한 DB 반영이 즉시 필요할 때 | 매우 높은 트래픽에서 빠른 증가 처리가 필요할 때 |

DB atomic update를 선택하면 좋은 경우:

- 요청이 끝나는 시점에 DB 값이 바로 맞아야 할 때
- 구조를 단순하게 가져가고 싶을 때
- 트래픽이 아주 높지 않거나 단일 row 병목이 아직 문제되지 않을 때

Redis atomic increment를 선택하면 좋은 경우:

- 클릭, 좋아요, 조회수처럼 매우 많은 증가 요청을 빠르게 처리해야 할 때
- DB direct write 병목을 줄여야 할 때
- DB가 즉시 최신값일 필요는 없고, flush 기반 eventual consistency를 받아들일 수 있을 때
- Redis와 flush 운영 복잡도를 감수할 수 있을 때

### 정리

- Step4는 Redis의 원자적 증가 연산으로 실시간 클릭을 처리
- Step3와 마찬가지로 저장소 레벨 atomic operation을 사용하지만, 처리 위치가 DB에서 Redis로 바뀜
- 그 결과 정합성은 유지하면서 throughput을 크게 끌어올릴 수 있음
- 대신 DB는 실시간 원본이 아니라 flush 이후 최종 반영 저장소 역할을 하게 됨
