## 1. naive 구현

### 구현 방식
```text
- 단일 row (id=1) 기반 카운터
- 요청마다:
  1. DB에서 count 조회
  2. +1
  3. 다시 저장
```
```java
@Transactional
public void increase() {
    Counter counter = counterRepository.findById(1L).orElseThrow();
    counter.increase();
}
```

### 문제
->  **동시 요청 시 카운트 유실 발생**

### 결과
**약 90% 유실**
#### 1. JUnit 테스트
    - 1000번 요청 (200개 스레드 풀 순차적으로 요청)
    - 106 만 반영됨
#### 2. K6 테스트
    - 40143 요청
    - 4140 반영
`k6 run click-test.js`
```text
         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 


     execution: local
        script: click-test.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 40s max duration (incl. graceful stop):
              * default: 100 looping VUs for 10s (gracefulStop: 30s)



  █ TOTAL RESULTS 

    HTTP
    http_req_duration..............: avg=24.89ms min=1.58ms med=23.4ms  max=199.23ms p(90)=35.41ms p(95)=42.18ms
      { expected_response:true }...: avg=24.89ms min=1.58ms med=23.4ms  max=199.23ms p(90)=35.41ms p(95)=42.18ms
    http_req_failed................: 0.00%  0 out of 40143
    http_reqs......................: 40143  4006.384186/s

    EXECUTION
    iteration_duration.............: avg=24.92ms min=1.59ms med=23.42ms max=199.25ms p(90)=35.43ms p(95)=42.2ms 
    iterations.....................: 40143  4006.384186/s
    vus............................: 100    min=100        max=100
    vus_max........................: 100    min=100        max=100

    NETWORK
    data_received..................: 2.9 MB 293 kB/s
    data_sent......................: 4.0 MB 397 kB/s




running (10.0s), 000/100 VUs, 40143 complete and 0 interrupted iterations
default ✓ [======================================] 100 VUs  10s
```

### 원인
-  핵심문제: `Read → Modify → Write 구조`

- **Lost Update 발생**
```text
Thread A: count=100 읽음
Thread B: count=100 읽음

Thread A: 101 저장
Thread B: 101 저장

실제 2번 증가 → 1번만 반영
```

### naive 방식 정리
- `동시성 제어` 없음
- `DB overwrite` 발생
- `high contention`에서 거의 문제 발생