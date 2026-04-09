package kr.higu.clickcountersystem.controller;

import kr.higu.clickcountersystem.service.Step2CounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class Step2CounterController {

    private final Step2CounterService step2CounterService;

    @PostMapping("/click2")
    public void click() {
        step2CounterService.increase();
    }
}
