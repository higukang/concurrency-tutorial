package kr.higu.clickcountersystem.controller;

import kr.higu.clickcountersystem.service.CounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CounterApiController {

    private final CounterService counterService;

    @PostMapping("/click")
    public void click() {
        counterService.increase();
    }

    // 개선 step1 - synchronized 적용
    @PostMapping("/click2")
    public void click2() {
        counterService.increaseSync();
    }

    @PostMapping("/click3")
    public void clickAtomic() {
        counterService.increaseAtomic();
    }

    @GetMapping("/count")
    public Long getCount() {
        return counterService.getCount();
    }
}