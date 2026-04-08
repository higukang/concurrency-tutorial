## 3. DB Atomic Update 적용
> - DB atomic update는 애플리케이션 레벨의 read-modify-write를 제거하고, 증가 연산을 DB 내부의 단일 SQL로 처리함으로써 lost update를 방지

> - 테스트에서는 요청 수와 DB 반영 수가 일치해 정합성이 확보됨을 확인

> - 일반적으로는 트랜잭션 롤백, 네트워크 장애, 중복 요청 등까지 고려하면 항상 절대적 100%를 보장한다고 단정할 수는 없음
---
### 구현 방식
- 애플리케이션에서 `조회 → 값 증가 → 저장`을 수행하지 않고, **DB가 직접 증가 연산을 수행**하도록 변경


- 단일 SQL로 처리:
```sql
update counter 
set click_count = click_count + 1 
where id = 1
```

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update Counter c set c.clickCount = c.clickCount + 1 where c.id = 1")
int increaseAtomic();
```
### 개선 의도
- 애플리케이션 레벨의 `Read → Modify → Write` 제거
- 증가 연산 자체를 `DB에 위임`하여 lost update 방지
- JVM 레벨 동기화 없이도 정합성 확보

### 결과
#### JUnit 테스트:
- 1000번 요청
- 1000 반영

#### k6 테스트
- 28223 요청
- DB 반영 28223

`k6 run click-test3.js  `
```text   
         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 


     execution: local
        script: click-test3.js
        output: -

     scenarios: (100.00%) 1 scenario, 100 max VUs, 40s max duration (incl. graceful stop):
              * default: 100 looping VUs for 10s (gracefulStop: 30s)



  █ TOTAL RESULTS 

    HTTP
    http_req_duration..............: avg=35.45ms min=3.08ms med=34.51ms max=254.7ms  p(90)=39.23ms p(95)=43.62ms
      { expected_response:true }...: avg=35.45ms min=3.08ms med=34.51ms max=254.7ms  p(90)=39.23ms p(95)=43.62ms
    http_req_failed................: 0.00%  0 out of 28223
    http_reqs......................: 28223  2812.011693/s

    EXECUTION
    iteration_duration.............: avg=35.48ms min=3.1ms  med=34.53ms max=257.69ms p(90)=39.26ms p(95)=43.64ms
    iterations.....................: 28223  2812.011693/s
    vus............................: 100    min=100        max=100
    vus_max........................: 100    min=100        max=100

    NETWORK
    data_received..................: 2.1 MB 206 kB/s
    data_sent......................: 2.8 MB 281 kB/s




running (10.0s), 000/100 VUs, 28223 complete and 0 interrupted iterations
default ✓ [======================================] 100 VUs  10s
```

### 분석
- JUnit과 k6 모두 요청 수와 DB 반영 수가 동일하게 나타남
- lost update 문제가 해결됨
- 애플리케이션 레벨에서 값을 읽고 수정하는 과정이 제거되면서, 동일 값 덮어쓰기 문제가 사라짐

### 개선점
- 데이터 정합성 확보
- JVM 레벨 lock 없이도 정확한 증가 처리 가능
- synchronized보다 명확하게 DB 레벨에서 문제를 해결

### 원인
- 증가 연산을 애플리케이션이 아니라 DB가 직접 수행
- count = count + 1 형태의 update는 DB가 row 단위로 처리
- 여러 요청이 동시에 와도 각 update가 누락되지 않고 누적 반영됨
- 
### 한계점
- 단일 row에 대한 update가 계속 발생하므로 write hotspot이 남아 있음 
- 합성은 해결했지만, 처리량(throughput)은 이전보다 감소함 
- 실제로 k6 기준:
  - naive: 40143 req 
  - synchronized: 34861 req 
  - atomic update: 28223 req
- 정확성은 가장 높지만 단일 row write 경쟁으로 인해 성능 병목은 여전히 존재

### 정리
- naive(step1): 가장 빠르지만 정합성이 깨짐 
- synchronized(step2): JVM 내부 경쟁은 줄였지만 DB 정합성은 완전히 해결하지 못함
- atomic update(step3): DB 레벨에서 정합성을 확보했지만 단일 row hotspot으로 인한 성능 한계 존재

| 단계 | 방식 | JUnit 결과 | k6 결과 | 정합성 | 특징 |
|------|------|------------|---------|--------|------|
| 1 | naive | 1000 → 106 | 40143 → 4140 | 매우 낮음 | read-modify-write로 lost update 발생 |
| 2 | synchronized | 1000 → 368 | 34861 → 7643 | 일부 개선 | JVM 내부 직렬화, 하지만 DB 커밋 경계 문제 남음 |
| 3 | atomic update | 1000 → 1000 | 28223 → 28223 | 높음 | DB가 직접 증가 처리, hotspot은 남음 |

---

##  참고: 왜 정합성은 해결됐는데 왜 TPS는 줄었을까?

### 1. naive방식이 빠른 이유

> - 다 같이 막 읽고 막 덮어씀
> - 정합성은 깨지지만, 요청 자체는 빨리 끝남

충돌 비용을 무시하고 밀어넣은 구조

### 2. atomic update
> - 같은 row에 대해 update click_count = click_count + 1를 계속 날림 
> - 해당 row 하나가 hotspot이 됨 
> - DB는 그 row에 대한 쓰기를 충돌 없이 처리해야 하니까 사실상 직렬화에 가까운 처리를 하게 됨

정합성을 얻는 대신 같은 row에 대한 write 경쟁이 DB 한 지점에 집중됨

## row lock / hotspot / DB 병목
### 1. row lock
같은 row를 여러 트랜잭션이 동시에 update하려고 하면 DB는 그 row를 아무나 동시에 막 바꾸게 두지 않음

그래서 어떤 요청은 먼저 row를 잡고 update, 나머지는 기다림

> 이 대기가 누적되면 TPS가 떨어진다.

### 2. hotspot
지금 구조는 id=1인 단 하나의 row만 계속 업데이트 쿼리 수행하여 쓰기 대상이 분산되지 않는 상황
> 사용자별 row 10000개에 분산 update면 병렬성 여지가 있음

> 지금은 무조건 row 1 하나만 update함

모든 요청이 한 점으로 몰리는 구조

### 3. DB 병목
결국 DB는
```text
- row lock 관리
- undo/redo log 기록
- commit 처리
- 디스크/버퍼 flush
모두 처리
```
그런데 모든 요청이 같은 row를 건드리면 이 비용이 DB 한 군데에 몰리면서 병목이 됨

> 즉, DB atomic update를 통해 lost update는 제거했지만 모든 요청이 동일한 row를 갱신하면서 write hotspot이 발생

> 그 결과 정합성은 확보됐지만, DB 내부에서 row-level 충돌과 대기가 증가해 전체 TPS는 감소
