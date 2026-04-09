package kr.higu.clickcountersystem.controller;

import kr.higu.clickcountersystem.service.Step3CounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class Step3CounterController {

    private final Step3CounterService step3CounterService;

    @PostMapping("/click3")
    public void click() {
        step3CounterService.increase();
    }
}
