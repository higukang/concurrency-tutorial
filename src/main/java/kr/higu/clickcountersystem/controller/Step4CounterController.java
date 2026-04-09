package kr.higu.clickcountersystem.controller;

import kr.higu.clickcountersystem.service.Step4CounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class Step4CounterController {

    private final Step4CounterService step4CounterService;

    @PostMapping("/click4")
    public void click() {
        step4CounterService.increase();
    }

    @GetMapping("/count4")
    public Long getCount() {
        return step4CounterService.getCount();
    }

    @PostMapping("/flush")
    public void flush() {
        step4CounterService.flushToDb();
    }
}
