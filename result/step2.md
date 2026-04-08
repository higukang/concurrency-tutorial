## 2. 메서드에 synchronized 적용

### 구현 방식
```java
@Transactional
public synchronized void increaseSync() {
    Counter counter = counterRepository.findById(1L).orElseThrow();
    counter.increase();
}
```

### 개선 의도
- JVM 레벨: 한 번에 하나의 스레드만 메서드 실행
- 기대효과: 동시성 충돌 감소

### 결과
#### 1. JUnit 테스트
    - 1000번 요청 (200개 스레드 풀 순차적으로 요청)
    - 368 만 반영
#### 2. K6 테스트
    - 34861 요청
    - 7643 반영
`k6 run click-test2.js`
```text
         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 


     execution: local
        script: click-test2.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 40s max duration (incl. graceful stop):
              * default: 100 looping VUs for 10s (gracefulStop: 30s)



  █ TOTAL RESULTS 

    HTTP
    http_req_duration..............: avg=28.67ms min=726µs    med=26.53ms max=203.18ms p(90)=41.43ms p(95)=49.37ms
      { expected_response:true }...: avg=28.67ms min=726µs    med=26.53ms max=203.18ms p(90)=41.43ms p(95)=49.37ms
    http_req_failed................: 0.00%  0 out of 34861
    http_reqs......................: 34861  3477.324624/s

    EXECUTION
    iteration_duration.............: avg=28.71ms min=751.95µs med=26.56ms max=204.04ms p(90)=41.45ms p(95)=49.41ms
    iterations.....................: 34861  3477.324624/s
    vus............................: 100    min=100        max=100
    vus_max........................: 100    min=100        max=100

    NETWORK
    data_received..................: 2.6 MB 254 kB/s
    data_sent......................: 3.5 MB 348 kB/s


running (10.0s), 000/100 VUs, 34861 complete and 0 interrupted iterations
default ✓ [======================================] 100 VUs  10s
```
### 분석
유실률 감소:
- JUnit: 89% → 63%
- k6: 89% → 78%

### 개선점
- JVM 레벨 경쟁 감소
    - 동시에 같은 값을 읽는 상황 감소
    - race condition 완화

### 원인: JVM lock과 DB 트랜잭션 경계 불일치
- synchronized ≠ 트랜잭션 전체 보호
```text
트랜잭션 시작
→ synchronized 진입
→ 값 변경
→ synchronized 종료
→ 트랜잭션 커밋
```
- 문제:
    - 메서드 끝나면 lock 해제됨
    - DB 커밋은 그 이후
    - Thread B가 커밋 전에 들어와서 이전 값 읽을 수 있음

### 한계점
- lost update 여전히 발생
- synchronized 키워드로는 DB 정합성 완전 보장 못함
- 성능 저하: 모든 요청 직렬화되어 throughput 감소
- 분산환경에서는 synchronized 공유되지 않기 때문에 무의미함

### 정리
- naive 방식은 동시성 환경에서 데이터 정합성을 보장하지 못함
- synchronized는 JVM 내부 경쟁은 완화하지만 DB 레벨 문제는 해결하지 못함
- 애플리케이션 레벨 동기화만으로는 완전한 해결이 어렵고, DB 레벨에서 수정이 필요함